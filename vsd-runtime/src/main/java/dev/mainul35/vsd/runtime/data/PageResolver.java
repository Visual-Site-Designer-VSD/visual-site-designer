package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Turns a stored page definition into the one a particular visitor sees: data sources are fetched on the
 * server, {@code templateBindings} are filled in and {@code iteratorConfig} components are repeated.
 *
 * <p>What a page may use:
 * <ul>
 *   <li>{@code dataContext.dataSources}: named sources. Their name is a template root ({@code {{products.items}}}).</li>
 *   <li>{@code dataSourceRef} / {@code dataSource} on a component: its data is {@code data} for that component
 *       and everything inside it.</li>
 *   <li>{@code templateBindings}: {@code {"propName": "Hello {{auth.user.displayName}}"}} sets a prop.</li>
 *   <li>{@code iteratorConfig}: the component is repeated for each item of {@code dataPath}, with the item
 *       (and optionally its index) available under the configured names.</li>
 *   <li>Always available: {@code auth} (isAuthenticated, user), {@code page} (name, path, title),
 *       {@code query} (the visitor's URL parameters, as text) and {@code shared} (this visitor's shared state,
 *       declared in the runtime console: {@code {{shared.cart.total}}}).</li>
 * </ul>
 *
 * <p>The result contains none of the authoring data ({@code dataContext}, {@code dataSource},
 * {@code templateBindings}, {@code iteratorConfig}), because data source headers may hold secrets and the
 * result is sent to the browser. A source that fails to load leaves its value empty and never fails the page.
 */
@Component
public class PageResolver {

    private static final Logger log = LoggerFactory.getLogger(PageResolver.class);
    private static final Set<String> RESERVED = Set.of("auth", "page", "query", "data", "shared");
    private static final Set<String> AUTHORING_ONLY =
            Set.of("dataSource", "dataSourceRef", "templateBindings", "iteratorConfig", "children");
    private static final int MAX_DEPTH = 32;
    private static final int MAX_COMPONENTS = 2000;
    private static final int MAX_ITEMS = 200;
    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private final SourceLoader loader;
    private final Executor executor;
    private final Duration deadline;

    @Autowired
    public PageResolver(DataService data, RuntimeProperties props) {
        this(data, data.executor(), props.getData().getPageDeadline());
    }

    public PageResolver(SourceLoader loader, Executor executor, Duration deadline) {
        this.loader = loader;
        this.executor = executor;
        this.deadline = deadline;
    }

    /**
     * @param definition the stored page definition (not modified)
     * @param user       the signed-in visitor as JSON, or null
     * @param query      the visitor's URL parameters
     */
    public JsonNode resolve(JsonNode definition, JsonNode user, Map<String, String> query, JsonNode pageMeta) {
        return resolve(definition, user, query, pageMeta, null);
    }

    /** @param shared the visitor's shared state by name (declared values, or their starting values), or null for none */
    public JsonNode resolve(JsonNode definition, JsonNode user, Map<String, String> query, JsonNode pageMeta, JsonNode shared) {
        if (definition == null || !definition.isObject()) {
            return definition;
        }
        ObjectNode auth = F.objectNode();
        auth.put("isAuthenticated", user != null && !user.isNull());
        auth.set("user", user == null ? F.nullNode() : user);
        ObjectNode queryNode = F.objectNode();
        query.forEach(queryNode::put);
        Map<String, JsonNode> baseVars = new HashMap<>();
        baseVars.put("auth", auth);
        baseVars.put("page", pageMeta == null ? F.objectNode() : pageMeta);
        baseVars.put("query", queryNode);
        baseVars.put("shared", shared == null || !shared.isObject() ? F.objectNode() : shared);
        Scope base = new Scope(baseVars);

        Map<String, SourceSpec> named = parseNamed(definition.path("dataContext").path("dataSources"));
        Plan plan = new Plan();
        collect(definition.path("components"), named, plan, 0);
        fetchAll(plan, named, base);

        Map<String, JsonNode> namedValues = new HashMap<>();
        for (String name : plan.neededNamed) {
            namedValues.put(name, plan.namedResults.getOrDefault(name, F.nullNode()));
        }
        Scope scope = base.with(namedValues);

        Run run = new Run(plan);
        List<JsonNode> out = new ArrayList<>();
        JsonNode comps = definition.path("components");
        if (comps.isArray()) {
            for (JsonNode c : comps) {
                resolveComponent(c, scope, 0, run, out);
            }
        }
        ObjectNode result = F.objectNode();
        definition.fields().forEachRemaining(e -> {
            if (!e.getKey().equals("components") && !e.getKey().equals("dataContext")) {
                result.set(e.getKey(), e.getValue().deepCopy());
            }
        });
        ArrayNode arr = result.putArray("components");
        out.forEach(arr::add);
        return result;
    }

    // ---- planning ----------------------------------------------------------------------------------------

    private static final class Plan {
        final Set<String> neededNamed = new java.util.LinkedHashSet<>();
        final Map<JsonNode, SourceSpec> inline = new IdentityHashMap<>();
        final Map<String, JsonNode> namedResults = new HashMap<>();
        final Map<JsonNode, JsonNode> inlineResults = new IdentityHashMap<>();
    }

    private static final class Run {
        final Plan plan;
        int components;

        Run(Plan plan) {
            this.plan = plan;
        }
    }

    private Map<String, SourceSpec> parseNamed(JsonNode sources) {
        Map<String, SourceSpec> named = new LinkedHashMap<>();
        if (!sources.isObject()) {
            return named;
        }
        sources.fields().forEachRemaining(e -> {
            String name = e.getKey();
            if (!name.matches("[A-Za-z_][A-Za-z0-9_]*") || RESERVED.contains(name)) {
                log.warn("Ignoring data source '{}': the name is not a valid template root or is reserved", name);
                return;
            }
            try {
                named.put(name, SourceSpec.parse(e.getValue()));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring data source '{}': {}", name, ex.getMessage());
            }
        });
        return named;
    }

    /** Finds which sources the page actually uses, so unused ones are never fetched. */
    private void collect(JsonNode comps, Map<String, SourceSpec> named, Plan plan, int depth) {
        if (!comps.isArray() || depth > MAX_DEPTH) {
            return;
        }
        for (JsonNode c : comps) {
            if (!c.isObject()) {
                continue;
            }
            String ref = c.path("dataSourceRef").asText("");
            if (named.containsKey(ref)) {
                plan.neededNamed.add(ref);
            }
            if (c.path("dataSource").isObject()) {
                try {
                    plan.inline.put(c, SourceSpec.parse(c.get("dataSource")));
                } catch (IllegalArgumentException ex) {
                    log.warn("Ignoring data source on component '{}': {}", c.path("instanceId").asText(), ex.getMessage());
                }
            }
            c.path("templateBindings").fields().forEachRemaining(e -> {
                if (e.getValue().isTextual()) {
                    for (String root : TemplateEngine.roots(e.getValue().asText())) {
                        if (named.containsKey(root)) {
                            plan.neededNamed.add(root);
                        }
                    }
                }
            });
            String dataPath = c.path("iteratorConfig").path("dataPath").asText("");
            if (!dataPath.isEmpty() && named.containsKey(JsonPaths.firstSegment(dataPath))) {
                plan.neededNamed.add(JsonPaths.firstSegment(dataPath));
            }
            collect(c.path("children"), named, plan, depth + 1);
            collect(c.path("iteratorConfig").path("emptyTemplate"), named, plan, depth + 1);
        }
    }

    private void fetchAll(Plan plan, Map<String, SourceSpec> named, Scope base) {
        Map<String, CompletableFuture<JsonNode>> namedFutures = new LinkedHashMap<>();
        Map<JsonNode, CompletableFuture<JsonNode>> inlineFutures = new IdentityHashMap<>();
        for (String name : plan.neededNamed) {
            namedFutures.put(name, produce(named.get(name), base));
        }
        plan.inline.forEach((comp, spec) -> inlineFutures.put(comp, produce(spec, base)));

        List<CompletableFuture<JsonNode>> all = new ArrayList<>(namedFutures.values());
        all.addAll(inlineFutures.values());
        try {
            CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).get(deadline.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("Data sources did not all answer within {}; the page is rendered without the slow ones", deadline);
            all.forEach(f -> f.cancel(true));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            all.forEach(f -> f.cancel(true));
        } catch (java.util.concurrent.ExecutionException e) {
            // individual failures are handled below
        }
        namedFutures.forEach((name, f) -> {
            JsonNode v = value(f, name);
            if (v != null) {
                plan.namedResults.put(name, v);
            }
        });
        inlineFutures.forEach((comp, f) -> {
            JsonNode v = value(f, comp.path("instanceId").asText("?"));
            if (v != null) {
                plan.inlineResults.put(comp, v);
            }
        });
    }

    private static JsonNode value(CompletableFuture<JsonNode> f, String what) {
        if (!f.isDone() || f.isCompletedExceptionally() || f.isCancelled()) {
            if (f.isCompletedExceptionally()) {
                try {
                    f.join();
                } catch (RuntimeException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    log.warn("Data source '{}' failed: {}", what, cause.toString());
                }
            }
            return null;
        }
        return f.join();
    }

    private CompletableFuture<JsonNode> produce(SourceSpec spec, Scope base) {
        return switch (spec.type()) {
            case STATIC -> CompletableFuture.completedFuture(FieldMapper.apply(spec.staticData(), spec.fieldMapping()));
            case CONTEXT -> {
                JsonNode v = spec.contextKey() == null ? null : base.lookup(spec.contextKey());
                yield CompletableFuture.completedFuture(FieldMapper.apply(v, spec.fieldMapping()));
            }
            case API -> CompletableFuture.supplyAsync(() -> {
                try {
                    return FieldMapper.apply(loader.load(spec), spec.fieldMapping());
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            }, executor);
        };
    }

    // ---- building ----------------------------------------------------------------------------------------

    private void resolveComponent(JsonNode comp, Scope scope, int depth, Run run, List<JsonNode> out) {
        if (!comp.isObject() || depth > MAX_DEPTH) {
            return;
        }
        Scope local = scope;
        String ref = comp.path("dataSourceRef").asText("");
        if (run.plan.inline.containsKey(comp)) {
            local = local.with("data", run.plan.inlineResults.getOrDefault(comp, F.nullNode()));
        } else if (!ref.isEmpty() && run.plan.neededNamed.contains(ref)) {
            local = local.with("data", run.plan.namedResults.getOrDefault(ref, F.nullNode()));
        }

        JsonNode iter = comp.get("iteratorConfig");
        if (iter == null || !iter.isObject()) {
            build(comp, local, null, depth, run, out);
            return;
        }
        JsonNode items = lookupItems(iter.path("dataPath").asText(""), local);
        if (items != null && items.isArray() && items.size() > 0) {
            String alias = identifier(iter.path("itemAlias").asText(""), "item");
            String indexAlias = identifier(iter.path("indexAlias").asText(""), null);
            String keyPath = iter.path("keyPath").asText("");
            int n = Math.min(items.size(), MAX_ITEMS);
            Set<String> usedKeys = new java.util.HashSet<>();
            for (int i = 0; i < n; i++) {
                JsonNode item = items.get(i);
                Scope itemScope = local.with(alias, item);
                if (indexAlias != null) {
                    itemScope = itemScope.with(indexAlias, F.numberNode(i));
                }
                String key = iterationKey(item, keyPath, i);
                if (!usedKeys.add(key)) { // a repeated key must not produce a repeated instanceId
                    key = key + "-" + i;
                    usedKeys.add(key);
                }
                build(comp, itemScope, "#" + key, depth, run, out);
            }
        } else if (iter.path("emptyTemplate").isArray()) {
            for (JsonNode t : iter.get("emptyTemplate")) {
                resolveComponent(t, local, depth + 1, run, out);
            }
        }
    }

    private void build(JsonNode comp, Scope scope, String idSuffix, int depth, Run run, List<JsonNode> out) {
        if (++run.components > MAX_COMPONENTS) {
            if (run.components == MAX_COMPONENTS + 1) {
                log.warn("Page exceeds {} components after expansion; the rest is dropped", MAX_COMPONENTS);
            }
            return;
        }
        ObjectNode n = F.objectNode();
        comp.fields().forEachRemaining(e -> {
            if (!AUTHORING_ONLY.contains(e.getKey())) {
                n.set(e.getKey(), e.getValue().deepCopy());
            }
        });
        if (idSuffix != null) {
            n.put("instanceId", comp.path("instanceId").asText("") + idSuffix);
        }
        JsonNode bindings = comp.path("templateBindings");
        if (bindings.isObject() && bindings.size() > 0) {
            ObjectNode props = comp.path("props").isObject() ? (ObjectNode) comp.get("props").deepCopy() : F.objectNode();
            bindings.fields().forEachRemaining(e -> {
                if (e.getValue().isTextual()) {
                    props.set(e.getKey(), TemplateEngine.render(e.getValue().asText(), scope));
                }
            });
            n.set("props", props);
        }
        if (comp.path("children").isArray()) {
            List<JsonNode> kids = new ArrayList<>();
            for (JsonNode child : comp.get("children")) {
                resolveComponent(child, scope, depth + 1, run, kids);
            }
            ArrayNode arr = n.putArray("children");
            kids.forEach(arr::add);
        }
        out.add(n);
    }

    /** The array at {@code dataPath}; falls back to the component's own {@code data} when the path has no known root. */
    private static JsonNode lookupItems(String dataPath, Scope scope) {
        if (dataPath.isEmpty()) {
            return scope.lookup("data");
        }
        JsonNode v = scope.lookup(dataPath);
        if (v == null && !scope.has(JsonPaths.firstSegment(dataPath)) && scope.has("data")) {
            v = scope.lookup("data." + dataPath);
        }
        return v;
    }

    private static String identifier(String name, String fallback) {
        return name.matches("[A-Za-z_][A-Za-z0-9_]{0,31}") && !RESERVED.contains(name) ? name : fallback;
    }

    private static String iterationKey(JsonNode item, String keyPath, int index) {
        if (!keyPath.isEmpty()) {
            JsonNode k = JsonPaths.get(item, keyPath);
            if (k != null && k.isValueNode() && k.asText().matches("[A-Za-z0-9_-]{1,64}")) {
                return k.asText();
            }
        }
        return Integer.toString(index);
    }
}
