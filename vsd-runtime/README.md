# VSD Live Runtime

Serves a **published site** to visitors and keeps their **session and state** in one place so
plugins can share them. It is the live counterpart of the builder: the same React renderers from
the plugin bundles draw the page, and each visitor has a server-side session that every plugin
(and every open tab) sees.

```
Publish  ->  Site Snapshot (pages JSON + pinned plugin versions)
                  |
        +---------v-----------+        +-----------------------------+
        |  vsd-runtime        |  HTML  |  Browser                    |
        |  PluginHost         |------->|  runtime.js hydrates the    |
        |  SessionStore       |  SSE   |  plugins' own renderers     |
        |  Event + State API  |<------>|  window.VSD.runtime         |
        +---------------------+  POST  +-----------------------------+
```

## Run the demo

Needs Java 21, Maven, and Node (the client and plugin bundles are built with the root
`frontend/` toolchain, as the plugins always were).

```bash
# once: SDK + BOM into the local Maven repo, frontend toolchain
mvn -q -DskipTests install -pl vsd-cms-bom,flashcard-cms-plugin-sdk -am
(cd frontend && npm ci)

# browser client -> vsd-runtime/src/main/resources/vsd-client/
(cd vsd-runtime/client && npm run build)

# plugin bundles (each plugin: plugins/<name>/frontend, `npm install && npm run build` as usual),
# and the newsletter plugin's server handler:
(cd plugins/newsletter-form-plugin && mvn -q compile)

# run (from vsd-runtime/), then open http://localhost:8090
cd vsd-runtime && mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Demo user: `demo@example.com` / `demo-pass` (profile `demo` only).
Smoke test against the running server: `python scripts/smoke_test.py`.

## What it guarantees

| Concern | Behaviour |
|---|---|
| **One renderer** | The browser runs the plugins' own bundles. There is no second, server-side implementation of a component. |
| **Pinned plugins** | A snapshot lists `plugin -> version`. Startup fails if a plugin is missing, differs in version, or a page uses an unpinned plugin. Only pinned plugins can be fetched or receive events. |
| **Session** | `VSDSESSID` cookie (HttpOnly, SameSite=Lax, Secure on HTTPS), 256-bit id, idle timeout, id and CSRF token rotated on sign-in/out. |
| **State** | Versioned key/value state per session in scopes `SESSION`, `PAGE`, `PLUGIN`, `COMPONENT`. Every change gets a session-wide, strictly increasing revision and is pushed over SSE to all tabs; clients apply a change only if it is newer, so duplicates and reordering are harmless. Reconnecting clients resume from their last revision (or get a snapshot). |
| **Identity** | Stored on the session and published as the reserved entry `SESSION/$auth/user`. Clients can read it, never write it. |
| **Events** | `POST /_vsd/api/events` routes `{pluginId, type}` to a server-side `RuntimeEventHandler`. Events of one session run one at a time, a retried `eventId` returns the original answer, and a handler's state writes are applied all-or-nothing. |
| **Browser safety** | CSRF header + Origin check on every POST, no CORS headers, size limits, rate limits (events, logins), `nosniff`/frame headers, authored assets served with a CSP sandbox. |

## Snapshot format

```
site.json      { siteName, title, snapshotVersion, defaultPage,
                 plugins: { "<plugin-id>": "<version>" },
                 pages:   [ { name, path, title, file } ] }
pages/*.json   builder PageDefinition documents, unchanged
assets/        optional static files, served under /assets/
```

## Writing a plugin for the runtime

**Server side** (optional): implement `RuntimeEventHandler` from the SDK and list it in
`META-INF/services/dev.mainul35.cms.sdk.runtime.RuntimeEventHandler`.

```java
public class NewsletterSubscribeHandler implements RuntimeEventHandler {
    public String pluginId() { return "newsletter-form-plugin"; }
    public Set<String> eventTypes() { return Set.of("subscribe"); }
    public RuntimeResult handle(RuntimeContext ctx, RuntimeEvent event) {
        // validate, then write shared state; ctx.user() is the signed-in visitor, if any
        ctx.state().set(StateKey.plugin(pluginId(), "subscription"), Map.of("email", "..."));
        return RuntimeResult.ok("Subscribed");
    }
}
```

**Browser side**: use `window.VSD.runtime` when it exists and fall back to local behaviour when
it does not (builder, previews). See `plugins/*/frontend/src/runtimeBridge.ts`.

```ts
const rt = (globalThis as any).VSD?.runtime;
rt?.useSharedState('PLUGIN', 'my-plugin', 'key');   // hook, re-renders on any tab's change
rt?.useAuth();                                      // { user, login, logout }
await rt?.emit('subscribe', { pluginId: 'my-plugin' }, { email });
```

## Not done yet (be aware)

* **No publish step.** The snapshot is a directory you provide (`demo-site/` is hand-written in
  the builder's page format). Nothing in `core` generates one yet.
* **Data sources and `{{template}}` bindings are not wired.** `site-runtime`'s `PageDataService`
  and fetchers are not used by the runtime yet.
* **Plugin host is new, not extracted from `core`.** `core`'s `PluginManager` is untouched, so the
  two hosts exist side by side. The old `@EventHandler`/`/api/events` system is not bridged.
* **Single node.** Sessions live in memory. A shared store also needs cross-node fan-out of
  patches to open streams.
* **Client-rendered.** No server-side HTML for components, so no SEO beyond title/description.
* **Builder has no fake `window.VSD`.** Plugins just take their old path there.
* **Sign-in is the dev `ConfigIdentityProvider`** (plain-text users). Provide your own
  `IdentityProvider` bean (for example one calling the VSD auth server) for real use.
* `runtimeBridge.ts` is copied into each plugin that uses it; it belongs in a shared package.
