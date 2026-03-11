# 5. Building Block View

This section shows the static decomposition of VSD into building blocks (modules, components, subsystems) and their relationships.

---

## 5.1 Whitebox Overall System (Level 1)

### System Context

VSD is decomposed into five primary subsystems that work together to provide a complete visual site building platform.

```mermaid
graph TB
    subgraph "Visual Site Designer System"
        CORE[Core Platform]
        FRONTEND[Frontend Builder]
        SDK[Plugin SDK]
        PLUGINS[Plugin Ecosystem]
        RUNTIME[Site Runtime]
    end

    USER[Site Builder] --> FRONTEND
    FRONTEND <--> CORE
    PLUGINS --> SDK
    CORE --> PLUGINS
    EXPORTED[Exported Sites] --> RUNTIME
    DEVS[Plugin Developers] --> SDK

    DB[(Database)] <--> CORE
    FS[File System] <--> CORE
    AUTH[Auth Providers] <--> CORE

    style CORE fill:#4a90e2,color:#fff
    style FRONTEND fill:#95e1d3
    style SDK fill:#f38181,color:#fff
    style PLUGINS fill:#ffd89b
    style RUNTIME fill:#a8dadc
```

### Contained Building Blocks

| Building Block | Responsibility | Interface |
|---------------|----------------|-----------|
| **Core Platform** | Plugin management, authentication, persistence, REST API | HTTP REST API, Plugin API |
| **Frontend Builder** | Visual editor, drag-and-drop, component palette, properties panel | Web UI (React) |
| **Plugin SDK** | Interfaces and contracts for plugin development | Java API (`UIComponentPlugin`, `ComponentManifest`) |
| **Plugin Ecosystem** | Collection of UI component plugins (button, label, navbar, etc.) | Implements SDK interfaces |
| **Site Runtime** | Runtime library for exported sites, data fetching, caching | Java Library API |

### Important Interfaces

| Interface | Description |
|-----------|-------------|
| **REST API** | HTTP endpoints for authentication, sites, pages, components, plugins |
| **Plugin API** | `UIComponentPlugin` interface for component lifecycle and metadata |
| **Component Registry API** | Dynamic component registration and retrieval |
| **WebSocket API** | Real-time updates for preview window (BroadcastChannel) |
| **Export API** | Generate static HTML or Spring Boot projects |

---

## 5.2 Level 2: Core Platform (Whitebox)

The Core Platform is the heart of VSD, managing the plugin ecosystem and providing services to the frontend.

```mermaid
graph TB
    subgraph "Core Platform"
        subgraph "Web Layer"
            AUTH_CTRL[AuthController]
            SITE_CTRL[SiteController]
            PAGE_CTRL[PageController]
            COMP_CTRL[ComponentController]
            PLUGIN_CTRL[PluginController]
        end

        subgraph "Service Layer"
            AUTH_SVC[AuthenticationService]
            SITE_SVC[SiteService]
            PAGE_SVC[PageService]
            COMP_REG[ComponentRegistry]
            PLUGIN_MGR[PluginManager]
            EXPORT_SVC[ExportService]
        end

        subgraph "Plugin System"
            LOADER[PluginLoader]
            VALIDATOR[PluginValidator]
            CLASSLOADER[IsolatedClassLoader]
            LIFECYCLE[PluginLifecycleManager]
        end

        subgraph "Security"
            JWT_FILTER[JwtAuthenticationFilter]
            OAUTH2_HANDLER[OAuth2AuthenticationSuccessHandler]
            SEC_CONFIG[CmsSecurityConfig]
        end

        subgraph "Persistence"
            SITE_REPO[SiteRepository]
            PAGE_REPO[PageRepository]
            USER_REPO[UserRepository]
            COMP_REG_REPO[ComponentRegistryRepository]
        end

        AUTH_CTRL --> AUTH_SVC
        SITE_CTRL --> SITE_SVC
        PAGE_CTRL --> PAGE_SVC
        COMP_CTRL --> COMP_REG
        PLUGIN_CTRL --> PLUGIN_MGR

        PLUGIN_MGR --> LOADER
        PLUGIN_MGR --> VALIDATOR
        PLUGIN_MGR --> LIFECYCLE
        LOADER --> CLASSLOADER

        AUTH_SVC --> JWT_FILTER
        AUTH_SVC --> OAUTH2_HANDLER
        SEC_CONFIG --> JWT_FILTER
        SEC_CONFIG --> OAUTH2_HANDLER

        SITE_SVC --> SITE_REPO
        PAGE_SVC --> PAGE_REPO
        AUTH_SVC --> USER_REPO
        COMP_REG --> COMP_REG_REPO
    end

    FRONTEND[Frontend] --> AUTH_CTRL
    FRONTEND --> SITE_CTRL
    FRONTEND --> PAGE_CTRL
    FRONTEND --> COMP_CTRL
    FRONTEND --> PLUGIN_CTRL

    PLUGINS[Plugin JARs] --> PLUGIN_MGR
    DB[(Database)] <--> SITE_REPO
    DB <--> PAGE_REPO
    DB <--> USER_REPO
    DB <--> COMP_REG_REPO

    style PLUGIN_MGR fill:#f38181,color:#fff
    style COMP_REG fill:#ffd89b
```

### Contained Building Blocks

#### Web Layer (Controllers)

| Component | Responsibility |
|-----------|----------------|
| **AuthController** | User login, registration, token refresh, OAuth2 callbacks |
| **SiteController** | CRUD operations for sites |
| **PageController** | Page management, versioning, rollback |
| **ComponentController** | List components, get manifests, serve plugin bundles |
| **PluginController** | Upload, activate, deactivate, uninstall plugins |

#### Service Layer

| Component | Responsibility |
|-----------|----------------|
| **AuthenticationService** | JWT generation, user validation, role mapping |
| **SiteService** | Site business logic, ownership validation |
| **PageService** | Page CRUD, version management, content validation |
| **ComponentRegistry** | Register and lookup components, cache manifests |
| **PluginManager** | Load, activate, deactivate plugins, hot-reload |
| **ExportService** | Generate static HTML or Spring Boot projects |

#### Plugin System

| Component | Responsibility |
|-----------|----------------|
| **PluginLoader** | Scan plugins directory, read `plugin.yml`, instantiate plugins |
| **PluginValidator** | Validate plugin structure, dependencies, manifest integrity |
| **IsolatedClassLoader** | Load plugin classes in isolated classloader to prevent conflicts |
| **PluginLifecycleManager** | Call lifecycle methods (`onLoad`, `onActivate`, `onDeactivate`, `onUninstall`) |

#### Security

| Component | Responsibility |
|-----------|----------------|
| **JwtAuthenticationFilter** | Intercept requests, validate JWT tokens (HS256 for local) |
| **OAuth2AuthenticationSuccessHandler** | Handle OAuth2 login success, create/link users, issue JWT |
| **CmsSecurityConfig** | Configure security filter chain, role mapping embedded in OAuth2 success handler |

#### Persistence

| Component | Responsibility |
|-----------|----------------|
| **SiteRepository** | JPA repository for sites |
| **PageRepository** | JPA repository for pages and versions |
| **UserRepository** | JPA repository for users |
| **ComponentRegistryRepository** | JPA repository for registered components |

---

## 5.3 Level 2: Frontend Builder (Whitebox)

The Frontend Builder provides the visual interface for creating and managing sites.

```mermaid
graph TB
    subgraph "Frontend Builder"
        subgraph "Routes"
            LOGIN[LoginPage]
            SITES[SitesPage]
            BUILDER[BuilderPage]
            PREVIEW[StandalonePreviewPage]
        end

        subgraph "Builder Components"
            PALETTE[ComponentPalette]
            CANVAS[BuilderCanvas]
            PROPS[PropertiesPanel]
            TOOLBAR[Toolbar]
            LAYERS[LayersPanel]
        end

        subgraph "Canvas Rendering"
            CANVAS_RENDERER[CanvasRenderer]
            COMP_WRAPPER[ComponentWrapper]
            DND[DragDropManager]
            RESIZE[ResizeHandler]
        end

        subgraph "State Management"
            PAGE_STORE[pageStore]
            COMPONENT_STORE[componentRegistryStore]
            AUTH_STORE[authStore]
            PREVIEW_STORE[multiPagePreviewStore]
        end

        subgraph "Services"
            API_CLIENT[apiClient]
            PLUGIN_LOADER_SVC[PluginLoaderService]
            PREVIEW_BROADCAST[PreviewBroadcastService]
        end

        BUILDER --> PALETTE
        BUILDER --> CANVAS
        BUILDER --> PROPS
        BUILDER --> TOOLBAR
        BUILDER --> LAYERS

        CANVAS --> CANVAS_RENDERER
        CANVAS_RENDERER --> COMP_WRAPPER
        CANVAS_RENDERER --> DND
        CANVAS_RENDERER --> RESIZE

        PALETTE --> COMPONENT_STORE
        CANVAS --> PAGE_STORE
        PROPS --> PAGE_STORE
        PREVIEW --> PREVIEW_STORE

        COMPONENT_STORE --> API_CLIENT
        PAGE_STORE --> API_CLIENT
        AUTH_STORE --> API_CLIENT

        CANVAS_RENDERER --> PLUGIN_LOADER_SVC
        PREVIEW --> PREVIEW_BROADCAST
    end

    USER[User] --> LOGIN
    USER --> SITES
    USER --> BUILDER
    USER --> PREVIEW

    API_CLIENT --> CORE_API[Core REST API]

    style CANVAS fill:#95e1d3
    style PAGE_STORE fill:#ffd89b
    style PLUGIN_LOADER_SVC fill:#f38181,color:#fff
```

### Contained Building Blocks

#### Pages/Routes

| Component | Responsibility |
|-----------|----------------|
| **LoginPage** | User authentication (local + OAuth2) |
| **SitesPage** | List and manage sites |
| **BuilderPage** | Main visual editor with palette, canvas, properties |
| **StandalonePreviewPage** | Separate window for real-time preview |

#### Builder Components

| Component | Responsibility |
|-----------|----------------|
| **ComponentPalette** | Display available components by category, drag to canvas |
| **BuilderCanvas** | Drop zone for components, visual editing area |
| **PropertiesPanel** | Configure component props, styles, layout, events |
| **Toolbar** | Page actions (save, export, preview, undo/redo) |
| **LayersPanel** | Tree view of page components, hierarchy management |

#### Canvas Rendering

| Component | Responsibility |
|-----------|----------------|
| **CanvasRenderer** | Render page definition as interactive canvas |
| **ComponentWrapper** | Wrap each component with selection, resize handles |
| **DragDropManager** | Handle drag-and-drop using @dnd-kit |
| **ResizeHandler** | Handle component resizing with resize handles |

#### State Management (Zustand)

| Store | Responsibility |
|-------|----------------|
| **pageStore** | Current page definition, components tree, undo/redo history |
| **componentRegistryStore** | Registered components, manifests, loaded plugin bundles |
| **authStore** | User authentication state, JWT tokens |
| **multiPagePreviewStore** | Preview mode state, navigation history |

#### Services

| Service | Responsibility |
|---------|----------------|
| **apiClient** | Axios-based HTTP client with JWT interceptor |
| **PluginLoaderService** | Dynamically load plugin bundles from `/api/plugins/{pluginId}/bundle.js` |
| **PreviewBroadcastService** | Cross-window communication using BroadcastChannel API |

---

## 5.4 Level 2: Plugin SDK (Whitebox)

The Plugin SDK defines the contract between the core platform and plugins. It supports two plugin types: **UI Component Plugins** (provide visual components) and **Context Provider Plugins** (provide shared state/services for feature domains).

```mermaid
graph TB
    subgraph "Plugin SDK"
        subgraph "Interfaces"
            PLUGIN_IF[Plugin Interface]
            UI_COMP_IF[UIComponentPlugin Interface]
            CTX_PROV_IF[ContextProviderPlugin Interface]
            DATA_FETCHER_IF[DataFetcher Interface]
        end

        subgraph "Data Models"
            MANIFEST[ComponentManifest]
            CAPABILITIES[ComponentCapabilities]
            PROP_DEF[PropDefinition]
            STYLE_DEF[StyleDefinition]
            SIZE_CONST[SizeConstraints]
            VALIDATION[ValidationResult]
            CTX_DESC[ContextDescriptor]
        end

        subgraph "Annotations"
            UI_COMP_ANN["@UIComponent"]
        end

        subgraph "Context"
            PLUGIN_CTX[PluginContext]
        end

        UI_COMP_IF --|extends| PLUGIN_IF
        CTX_PROV_IF --|extends| PLUGIN_IF
        UI_COMP_IF --> MANIFEST
        MANIFEST --> CAPABILITIES
        MANIFEST --> PROP_DEF
        MANIFEST --> STYLE_DEF
        MANIFEST --> SIZE_CONST
        UI_COMP_IF --> VALIDATION
        UI_COMP_IF --> UI_COMP_ANN
        PLUGIN_IF --> PLUGIN_CTX
        CTX_PROV_IF --> CTX_DESC
    end

    PLUGIN_IMPL[UI Plugin Implementation] -.implements.-> UI_COMP_IF
    CTX_PLUGIN_IMPL[Context Plugin Implementation] -.implements.-> CTX_PROV_IF
    CORE[Core Platform] --> PLUGIN_IF

    style UI_COMP_IF fill:#f38181,color:#fff
    style CTX_PROV_IF fill:#a855f7,color:#fff
    style MANIFEST fill:#ffd89b
    style CTX_DESC fill:#c4b5fd
```

### Contained Building Blocks

#### Core Interfaces

```java
// Plugin Lifecycle Interface
public interface Plugin {
    void onLoad(PluginContext context) throws Exception;
    void onActivate(PluginContext context) throws Exception;
    void onDeactivate(PluginContext context) throws Exception;
    void onUninstall(PluginContext context) throws Exception;

    String getPluginId();
    String getName();
    String getVersion();
    String getDescription();
}

// UI Component Interface (extends Plugin)
public interface UIComponentPlugin extends Plugin {
    ComponentManifest getComponentManifest();
    List<ComponentManifest> getComponentManifests(); // Multi-component support
    String getReactComponentPath();
    byte[] getComponentThumbnail();
    ValidationResult validateProps(Map<String, Object> props);

    // Optional hooks
    default String renderToHTML(Map<String, Object> props, Map<String, String> styles);
    default void onComponentAdded(PluginContext context, Long pageId, String instanceId);
    default void onComponentRemoved(PluginContext context, Long pageId, String instanceId);
    default void onPropsUpdated(PluginContext context, String instanceId,
                               Map<String, Object> oldProps, Map<String, Object> newProps);
}

// Context Provider Interface (extends Plugin) — NEW
public interface ContextProviderPlugin extends Plugin {
    // Unique context identifier (e.g., "auth", "cart")
    String getContextId();

    // React context provider component path (e.g., "AuthProvider.js")
    String getProviderComponentPath();

    // API endpoints this context exposes
    List<ApiEndpoint> getApiEndpoints();

    // Dependencies on other contexts (e.g., cart needs auth)
    default List<String> getRequiredContexts() { return List.of(); }
}
```

#### Data Models

| Class | Purpose |
|-------|---------|
| **ComponentManifest** | Complete component metadata (id, name, props, styles, capabilities, constraints) |
| **ComponentCapabilities** | Behavioral flags driving builder behavior (canHaveChildren, isContainer, hasDataSource, autoHeight, isResizable, supportsIteration, supportsTemplateBindings) |
| **PropDefinition** | Define a configurable property (type, label, options, default, validation) |
| **StyleDefinition** | Define a configurable CSS style (property, type, category, units) |
| **SizeConstraints** | Define component sizing rules (resizable, min/max width/height) |
| **ValidationResult** | Result of prop validation (valid, errors, warnings) |
| **ContextDescriptor** | Context metadata: contextId, providerComponentPath, apiEndpoints, requiredContexts |

#### Annotations

```java
@UIComponent(
    componentId = "label",           // Unique component identifier
    displayName = "Label",            // Display name in palette
    category = "ui",                  // Category: ui, layout, form, navigation
    icon = "L",                       // Icon identifier
    resizable = true,                 // Whether component is resizable
    defaultWidth = "200px",           // Default width
    defaultHeight = "auto",           // Default height
    minWidth = "50px",                // Minimum width
    maxWidth = "100%",                // Maximum width
    minHeight = "20px",               // Minimum height
    maxHeight = "500px"               // Maximum height
)
public class LabelComponentPlugin implements UIComponentPlugin { ... }
```

---

## 5.5 Level 2: Plugin Ecosystem (Whitebox)

The Plugin Ecosystem consists of individual plugin modules that implement the SDK interfaces.

```mermaid
graph TB
    subgraph "Plugin Ecosystem"
        subgraph "UI Plugins"
            LABEL[label-component-plugin]
            BUTTON[button-component-plugin]
            IMAGE[image-component-plugin]
        end

        subgraph "Layout Plugins"
            CONTAINER[container-layout-plugin]
            SCROLLABLE[scrollable-container-plugin]
            PAGE_LAYOUT[page-layout-plugin]
            HROW[horizontal-row-plugin]
        end

        subgraph "Form Plugins"
            TEXTBOX[textbox-component-plugin]
            NEWSLETTER[newsletter-form-plugin]
        end

        subgraph "Navigation Plugins"
            NAVBAR[navbar-component-plugin]
        end

        subgraph "Auth Plugins"
            AUTH_COMP[auth-component-plugin]
        end
    end

    SDK[Plugin SDK] <-- LABEL
    SDK <-- BUTTON
    SDK <-- IMAGE
    SDK <-- CONTAINER
    SDK <-- SCROLLABLE
    SDK <-- PAGE_LAYOUT
    SDK <-- HROW
    SDK <-- TEXTBOX
    SDK <-- NEWSLETTER
    SDK <-- NAVBAR
    SDK <-- AUTH_COMP

    style SDK fill:#f38181,color:#fff
    style LABEL fill:#95e1d3
    style BUTTON fill:#95e1d3
    style CONTAINER fill:#ffd89b
    style NEWSLETTER fill:#a8dadc
```

### Plugin Structure (Example: label-component-plugin)

Each plugin follows a standard structure:

```
label-component-plugin/
├── pom.xml                                  # Maven build configuration
├── src/main/
│   ├── java/dev/mainul35/plugins/ui/
│   │   └── LabelComponentPlugin.java       # Plugin implementation
│   └── resources/
│       ├── plugin.yml                       # Plugin metadata
│       └── frontend/
│           └── bundle.js                    # React component (built)
└── frontend/
    ├── package.json                         # npm dependencies
    ├── vite.config.ts                       # Vite build config
    ├── tsconfig.json                        # TypeScript config
    └── src/
        ├── index.ts                         # Entry point
        ├── types.ts                         # TypeScript interfaces
        └── renderers/
            └── LabelRenderer.tsx            # React component
```

### Plugin Types

#### Simple Plugin (Label Component)

```mermaid
graph LR
    subgraph "Label Plugin JAR"
        JAVA[LabelComponentPlugin.java]
        YML[plugin.yml]
        BUNDLE[bundle.js<br/>React Component]
    end

    JAVA -.implements.-> SDK[UIComponentPlugin]
    JAVA --> MANIFEST[ComponentManifest]
    MANIFEST --> PROPS[Props: text, variant]
    MANIFEST --> STYLES[Styles: color, fontSize]
    BUNDLE --> RENDERER[LabelRenderer.tsx]

    style SDK fill:#f38181,color:#fff
```

#### Compound Plugin (Newsletter Form)

```mermaid
graph LR
    subgraph "Newsletter Plugin JAR"
        JAVA[NewsletterFormPlugin.java]
        YML[plugin.yml]
        BUNDLE[bundle.js<br/>Compound Component]
    end

    JAVA -.implements.-> SDK[UIComponentPlugin]
    BUNDLE --> COMP_RENDERER[NewsletterFormRenderer.tsx]
    COMP_RENDERER --> LABEL_R[import LabelRenderer]
    COMP_RENDERER --> BUTTON_R[import ButtonRenderer]
    COMP_RENDERER --> TEXTBOX_R[import TextboxRenderer]

    LABEL_R -.from.-> GEN_TYPES[generated-types/]
    BUTTON_R -.from.-> GEN_TYPES
    TEXTBOX_R -.from.-> GEN_TYPES

    style SDK fill:#f38181,color:#fff
    style GEN_TYPES fill:#ffd89b
```

### Contained Plugins

| Plugin | Type | Description |
|--------|------|-------------|
| **label-component-plugin** | Simple | Text label with variants (h1-h6, p, span) |
| **button-component-plugin** | Simple | Button with variants, sizes, states |
| **image-component-plugin** | Simple | Image display with alt text, lazy loading |
| **container-layout-plugin** | Simple | Layout container with flex/grid |
| **scrollable-container-plugin** | Simple | Container with overflow scroll |
| **page-layout-plugin** | Simple | Page-level layout with header/footer/sidebar slots |
| **horizontal-row-plugin** | Simple | Horizontal flex row container |
| **textbox-component-plugin** | Simple | Text input with validation |
| **newsletter-form-plugin** | Compound | Newsletter form (composes label, textbox, button) |
| **navbar-component-plugin** | Simple | Navigation bar with logo, links |
| **auth-component-plugin** | Simple | Login/register forms, social login buttons |

---

## 5.6 Level 2: Context Provider Plugins (Whitebox) — Planned

Context Provider Plugins are a new plugin type that provides **shared state and services** for feature domains. Unlike UI Component Plugins (which render visual components), Context Provider Plugins supply React context providers and backend API endpoints that multiple UI component plugins can consume.

### Architecture Overview

```mermaid
graph TB
    subgraph "Context Provider Plugins"
        subgraph "Auth Domain"
            AUTH_CTX[auth-context-plugin<br/>Provides: AuthContext]
        end

        subgraph "E-commerce Domain (Future)"
            CART_CTX[cart-context-plugin<br/>Provides: CartContext]
            ORDER_CTX[order-context-plugin<br/>Provides: OrderContext]
        end
    end

    subgraph "UI Component Plugins (Consumers)"
        LOGIN[login-form-plugin]
        REGISTER[register-form-plugin]
        PROFILE[user-profile-plugin]
        PRODUCT[product-card-plugin]
        CART_WIDGET[cart-widget-plugin]
        CHECKOUT[checkout-form-plugin]
    end

    LOGIN -->|consumes| AUTH_CTX
    REGISTER -->|consumes| AUTH_CTX
    PROFILE -->|consumes| AUTH_CTX

    PRODUCT -->|consumes| CART_CTX
    CART_WIDGET -->|consumes| CART_CTX
    CHECKOUT -->|consumes| CART_CTX

    CART_CTX -.depends on.-> AUTH_CTX
    ORDER_CTX -.depends on.-> AUTH_CTX

    SDK[Plugin SDK] <-- AUTH_CTX
    SDK <-- CART_CTX
    SDK <-- ORDER_CTX

    style AUTH_CTX fill:#a855f7,color:#fff
    style CART_CTX fill:#a855f7,color:#fff
    style ORDER_CTX fill:#a855f7,color:#fff
    style SDK fill:#f38181,color:#fff
    style LOGIN fill:#95e1d3
    style PRODUCT fill:#95e1d3
```

### Context Provider vs UI Component Plugin

| Aspect | UI Component Plugin | Context Provider Plugin |
|--------|-------------------|----------------------|
| **Purpose** | Render visual UI on the canvas | Provide shared state/services for a feature domain |
| **Interface** | `UIComponentPlugin` | `ContextProviderPlugin` |
| **Frontend output** | React renderer component | React context provider |
| **Backend output** | ComponentManifest (props, styles) | API endpoints + ContextDescriptor |
| **Canvas visibility** | Yes (appears in palette) | No (invisible infrastructure) |
| **Example** | `label-component-plugin` | `auth-context-plugin` |

### Context Provider Plugin Structure

```
auth-context-plugin/
├── pom.xml
├── src/main/
│   ├── java/dev/mainul35/plugins/context/auth/
│   │   ├── AuthContextPlugin.java        # implements ContextProviderPlugin
│   │   ├── AuthController.java           # REST endpoints (/api/ctx/auth/*)
│   │   └── AuthService.java              # Session, token, user state logic
│   └── resources/
│       ├── plugin.yml
│       └── frontend/
│           └── provider-bundle.js        # React AuthProvider + useAuth hook
└── frontend/
    └── src/
        ├── AuthProvider.tsx              # React context provider
        ├── useAuth.ts                    # useAuth() hook
        └── types.ts                      # AuthState, AuthActions types
```

### Context Dependency Graph

Context plugins can declare dependencies on other contexts. The runtime resolves the dependency graph to determine provider wrapping order.

```mermaid
graph LR
    AUTH[AuthContext] --> CART[CartContext]
    AUTH --> ORDER[OrderContext]
    CART --> CHECKOUT_CTX[CheckoutContext]
    AUTH --> CHECKOUT_CTX

    style AUTH fill:#a855f7,color:#fff
    style CART fill:#a855f7,color:#fff
    style ORDER fill:#a855f7,color:#fff
    style CHECKOUT_CTX fill:#a855f7,color:#fff
```

**Resolution Order** (inner to outer): `AuthProvider → CartProvider → OrderProvider → CheckoutProvider → Page`

### UI Component Context Dependencies

UI component manifests declare which contexts they require via `requiredContexts`:

```java
ComponentManifest.builder()
    .componentId("loginForm")
    .pluginId("login-form-plugin")
    .requiredContexts(List.of("auth"))  // Needs AuthContext
    .capabilities(ComponentCapabilities.builder()
        .canHaveChildren(false)
        .build())
    .build();
```

The builder verifies that all required contexts are provided by active context plugins before allowing the component to be used.

### Frontend Context Provider Tree

At page render time, the runtime wraps the component tree with all active context providers:

```tsx
// Automatically assembled by ContextProviderTree
<AuthProvider>           {/* from auth-context-plugin */}
  <CartProvider>         {/* from cart-context-plugin */}
    <Page>
      <LoginForm />      {/* uses usePluginContext('auth') */}
      <CartWidget />     {/* uses usePluginContext('cart') */}
      <ProductCard />    {/* uses usePluginContext('cart') */}
    </Page>
  </CartProvider>
</AuthProvider>
```

### Planned Context Provider Plugins

| Plugin | Context ID | Provides | Required Contexts | Status |
|--------|-----------|----------|-------------------|--------|
| **auth-context-plugin** | `auth` | AuthContext (session, tokens, user info, login/logout) | — | Planned |
| **cart-context-plugin** | `cart` | CartContext (items, add/remove, totals) | `auth` | Future |
| **order-context-plugin** | `order` | OrderContext (order history, tracking) | `auth` | Future |

---

## 5.7 Level 2: Site Runtime (Whitebox)

The Site Runtime library provides runtime functionality for exported sites.

```mermaid
graph TB
    subgraph "Site Runtime Library"
        subgraph "Configuration"
            PROPS[SiteRuntimeProperties]
            AUTO_CONFIG[SiteRuntimeAutoConfiguration]
            SEC_CONFIG[SecurityAutoConfiguration]
        end

        subgraph "Data Layer"
            DF_IF[DataFetcher Interface]
            REST_DF[RestApiDataFetcher]
            JPA_DF[JpaDataFetcher]
            MONGO_DF[MongoDataFetcher]
            STATIC_DF[StaticDataFetcher]
            CONTEXT_DF[ContextDataFetcher]
            DS_REG[DataSourceRegistry]
        end

        subgraph "Services"
            PAGE_DATA_SVC[PageDataService]
            CACHE[InMemoryCacheProvider]
        end

        DF_IF <|-- REST_DF
        DF_IF <|-- JPA_DF
        DF_IF <|-- MONGO_DF
        DF_IF <|-- STATIC_DF
        DF_IF <|-- CONTEXT_DF

        DS_REG --> REST_DF
        DS_REG --> JPA_DF
        DS_REG --> MONGO_DF
        DS_REG --> STATIC_DF
        DS_REG --> CONTEXT_DF

        PAGE_DATA_SVC --> DS_REG
        PAGE_DATA_SVC --> CACHE

        AUTO_CONFIG --> DS_REG
        AUTO_CONFIG --> PAGE_DATA_SVC
        SEC_CONFIG --> PROPS
    end

    EXPORTED[Exported Site] --> AUTO_CONFIG

    style DS_REG fill:#ffd89b
    style PAGE_DATA_SVC fill:#95e1d3
```

### Contained Building Blocks

#### Configuration

| Component | Responsibility |
|-----------|----------------|
| **SiteRuntimeProperties** | Configuration properties for runtime (database, cache, auth) |
| **SiteRuntimeAutoConfiguration** | Spring Boot auto-configuration for data fetchers, cache |
| **SecurityAutoConfiguration** | Auto-configure authentication (social login, SSO) |

#### Data Fetchers

| Component | Responsibility |
|-----------|----------------|
| **DataFetcher (Interface)** | Abstract data fetching strategy |
| **RestApiDataFetcher** | Fetch data from REST APIs (HTTP GET/POST) |
| **JpaDataFetcher** | Fetch data from SQL databases (JPA queries) |
| **MongoDataFetcher** | Fetch data from MongoDB (Mongo queries) |
| **StaticDataFetcher** | Return static/hardcoded data |
| **ContextDataFetcher** | Extract data from request context (user, session) |
| **DataSourceRegistry** | Register and lookup data fetchers by type |

#### Services

| Component | Responsibility |
|-----------|----------------|
| **PageDataService** | Aggregate data from multiple sources for page rendering |
| **InMemoryCacheProvider** | In-memory cache with TTL for fetched data |

---

## 5.8 Level 3: Plugin Lifecycle (Deep Dive)

This section provides a detailed look at the plugin loading and registration process.

```mermaid
sequenceDiagram
    participant App as Spring Boot App
    participant PM as PluginManager
    participant PL as PluginLoader
    participant CL as IsolatedClassLoader
    participant Plugin as Plugin Instance
    participant CR as ComponentRegistry
    participant DB as Database

    App->>PM: applicationStartup()
    PM->>PL: scanPluginsDirectory()
    PL->>PL: Find all .jar files

    loop For each plugin JAR
        PL->>PL: Read plugin.yml
        PL->>CL: Create isolated ClassLoader
        CL->>Plugin: Load main-class
        Plugin->>Plugin: new Instance()

        PM->>Plugin: onLoad(context)
        Plugin->>Plugin: buildComponentManifest()
        Plugin-->>PM: return manifest

        PM->>CR: registerComponent(manifest)
        CR->>DB: Save to cms_component_registry

        PM->>Plugin: onActivate(context)
        Plugin-->>PM: Plugin ready
    end

    PM-->>App: All plugins loaded
```

### Plugin Loading Steps

1. **Discovery**: Scan `plugins/` directory for `.jar` files
2. **Validation**: Read `plugin.yml`, validate structure and dependencies
3. **ClassLoader Creation**: Create `IsolatedClassLoader` with parent=system classloader
4. **Instantiation**: Load plugin main class, call no-arg constructor
5. **Load Phase**: Call `onLoad(context)`, plugin builds manifest
6. **Registration**: Register component in `ComponentRegistry`, persist to database
7. **Activation**: Call `onActivate(context)`, plugin is ready for use

### Hot Reload Process

```mermaid
sequenceDiagram
    participant IDE as IntelliJ Plugin
    participant API as Plugin API
    participant PM as PluginManager
    participant CL as ClassLoader
    participant CR as ComponentRegistry

    IDE->>IDE: Build plugin JAR
    IDE->>API: POST /api/plugins/upload
    API->>PM: hotReload(pluginId)

    PM->>PM: Find existing plugin
    PM->>Plugin: onDeactivate()
    PM->>CL: Close classloader
    PM->>CR: Unregister component

    PM->>PM: Load new version
    PM->>CL: Create new ClassLoader
    PM->>Plugin: onLoad(context)
    PM->>Plugin: onActivate(context)
    PM->>CR: Register component

    PM-->>API: Hot reload complete
    API-->>IDE: Success
```

---

## 5.9 Level 3: Authentication Flow (Deep Dive)

This section details the authentication system with local and OAuth2 support.

### Local Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant JWT as JwtService

    User->>FE: Enter username/password
    FE->>AC: POST /api/auth/login
    AC->>AS: authenticate(username, password)
    AS->>UR: findByUsername(username)
    UR-->>AS: User entity
    AS->>AS: BCrypt.checkPassword(password, hash)
    AS->>JWT: generateAccessToken(user)
    AS->>JWT: generateRefreshToken(user)
    JWT-->>AS: Tokens (HS256)
    AS-->>AC: AuthResponse(accessToken, refreshToken)
    AC-->>FE: 200 OK with tokens
    FE->>FE: Store tokens in localStorage
    FE->>User: Redirect to sites page
```

### OAuth2 SSO Flow

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant Core as VSD Core
    participant Auth as Auth Server
    participant OAuth2 as OAuth2Handler
    participant AS as AuthService
    participant UR as UserRepository

    User->>FE: Click "Sign in with SSO"
    FE->>Core: GET /oauth2/authorization/vsd-auth
    Core->>Auth: Redirect to /oauth2/authorize
    Auth->>User: Show login page
    User->>Auth: Enter credentials
    Auth->>Core: Redirect with auth code
    Core->>Auth: Exchange code for tokens
    Auth-->>Core: Access token (RS256) + user info
    Core->>OAuth2: OAuth2 success callback
    OAuth2->>AS: processOAuth2User(userInfo)
    AS->>UR: findByEmail(email)
    alt User exists
        AS->>AS: Update user from SSO
    else New user
        AS->>UR: createUserFromSSO(userInfo)
    end
    AS->>AS: Map roles (SSO → VSD)
    AS->>AS: Generate local JWT (HS256)
    OAuth2->>FE: Redirect with JWT token
    FE->>FE: Store token in localStorage
    FE->>User: Redirect to sites page
```

### Dual Authentication Mode

VSD supports both local JWT (HS256) and SSO tokens (RS256) simultaneously:

```mermaid
graph LR
    subgraph "Authentication Filter Chain"
        REQ[Request] --> JWT_FILTER[JwtAuthenticationFilter]
        JWT_FILTER --> TOKEN_CHECK{Token Type?}
        TOKEN_CHECK -->|HS256| LOCAL[Local JWT Validation]
        TOKEN_CHECK -->|RS256| OAUTH2[OAuth2 Resource Server]
        LOCAL --> AUTH[Authenticated]
        OAUTH2 --> AUTH
    end

    style TOKEN_CHECK fill:#ffd89b
    style AUTH fill:#95e1d3
```

---

## 5.10 Level 3: Export Process (Deep Dive)

This section details how sites are exported to standalone applications.

### Static HTML Export

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant API as ExportController
    participant ES as ExportService
    participant PD as PageDefinition
    participant FS as FileSystem

    User->>FE: Click "Export as Static HTML"
    FE->>API: POST /api/sites/{id}/export?type=html
    API->>ES: exportAsStaticHTML(site)

    loop For each page
        ES->>PD: Load page definition
        ES->>ES: Generate HTML from components
        ES->>ES: Inline styles
        ES->>ES: Bundle React runtime
        ES->>FS: Write index.html
    end

    ES->>FS: Copy assets (images, fonts)
    ES->>FS: Create .zip archive
    ES-->>API: export.zip
    API-->>FE: Download .zip
    FE->>User: Save file
```

### Spring Boot Export

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant API as ExportController
    participant ES as ExportService
    participant TPL as TemplateEngine
    participant MVN as Maven Project
    participant FS as FileSystem

    User->>FE: Click "Export as Spring Boot"
    FE->>API: POST /api/sites/{id}/export?type=springboot
    API->>ES: exportAsSpringBoot(site)

    ES->>MVN: Create project structure
    ES->>MVN: Add site-runtime dependency
    ES->>MVN: Add pom.xml

    loop For each page
        ES->>TPL: Generate Thymeleaf template
        ES->>FS: Write to templates/
    end

    ES->>FS: Copy static assets
    ES->>FS: Create application.properties
    ES->>FS: Create main Application class
    ES->>FS: Create .zip archive
    ES-->>API: springboot-app.zip
    API-->>FE: Download .zip
    FE->>User: Save file
```

---

## 5.11 Cross-Cutting Concepts

### Plugin Isolation

Each plugin runs in an isolated classloader to prevent conflicts:

```mermaid
graph TB
    subgraph "System ClassLoader"
        JDK[JDK Classes]
        SPRING[Spring Boot]
        SDK[Plugin SDK]
    end

    subgraph "Plugin ClassLoader 1"
        P1[Button Plugin]
        P1_DEP[Plugin Dependencies]
    end

    subgraph "Plugin ClassLoader 2"
        P2[Label Plugin]
        P2_DEP[Plugin Dependencies]
    end

    SDK -->|Parent| P1
    SDK -->|Parent| P2
    P1 --> P1_DEP
    P2 --> P2_DEP

    style SDK fill:#f38181,color:#fff
    style P1 fill:#95e1d3
    style P2 fill:#95e1d3
```

**Benefits**:
- Plugins can use different versions of same library
- Plugin conflicts impossible
- Hot-reload via classloader disposal

**Trade-offs**:
- Memory overhead (one classloader per plugin)
- Debugging complexity across classloaders

### Component Registration

Components are registered in database for fast lookup:

```
cms_component_registry
├── id (PK)
├── plugin_id
├── component_id
├── display_name
├── category
├── manifest_json (JSONB)
└── created_at
```

**Benefits**:
- Fast component lookup without loading JARs
- Persistent across restarts
- Query by category, plugin, etc.

---

[← Previous: Solution Strategy](04-solution-strategy.md) | [Back to Index](README.md) | [Next: Runtime View →](06-runtime-view.md)
