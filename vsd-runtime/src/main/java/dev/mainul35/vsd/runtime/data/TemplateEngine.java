package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills {@code {{path}}} placeholders from a {@link Scope}.
 *
 * <ul>
 *   <li>A template that is exactly one placeholder gives the value itself, so numbers, booleans, arrays
 *       and objects keep their type.</li>
 *   <li>Otherwise placeholders are spliced into the text: scalars as text, objects and arrays as JSON,
 *       and missing values as nothing.</li>
 * </ul>
 *
 * Only lookups, no logic and no code. Values are inserted as data, never re-scanned for placeholders.
 */
public final class TemplateEngine {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_-]+|\\[\\d{1,6}\\])*)\\s*}}");
    private static final int MAX_RESULT_CHARS = 64 * 1024;
    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private TemplateEngine() {
    }

    public static JsonNode render(String template, Scope scope) {
        if (template == null) {
            return F.nullNode();
        }
        Matcher m = PLACEHOLDER.matcher(template);
        if (m.matches()) {
            JsonNode v = scope.lookup(m.group(1));
            return v == null ? F.nullNode() : v;
        }
        StringBuilder out = new StringBuilder();
        int last = 0;
        m.reset();
        while (m.find()) {
            out.append(template, last, m.start());
            JsonNode v = scope.lookup(m.group(1));
            if (v != null && !v.isNull()) {
                out.append(v.isValueNode() ? v.asText() : v.toString());
            }
            last = m.end();
            if (out.length() > MAX_RESULT_CHARS) {
                break;
            }
        }
        out.append(template, last, template.length());
        String s = out.length() > MAX_RESULT_CHARS ? out.substring(0, MAX_RESULT_CHARS) : out.toString();
        return F.textNode(s);
    }

    /** Root names used by the placeholders in a template ({@code auth} for {@code {{auth.user.id}}}). */
    public static Set<String> roots(String template) {
        Set<String> roots = new LinkedHashSet<>();
        if (template != null) {
            Matcher m = PLACEHOLDER.matcher(template);
            while (m.find()) {
                roots.add(JsonPaths.firstSegment(m.group(1)));
            }
        }
        return roots;
    }
}
