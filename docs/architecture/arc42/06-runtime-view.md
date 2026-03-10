# 6. Runtime View

This section describes the behavior and interaction of VSD's building blocks at runtime through important scenarios and workflows.

---

## 6.1 Plugin Lifecycle

### 6.1.1 Plugin Load and Activation

This scenario shows how a plugin is discovered, loaded, and activated when VSD starts.

```mermaid
sequenceDiagram
    participant Boot as Spring Boot
    participant PM as PluginManager
    participant FS as File System
    participant CL as IsolatedClassLoader
    participant Plugin
    participant CR as ComponentRegistry
    participant DB as Database

    Boot->>PM: @PostConstruct loadAllPlugins()
    PM->>FS: Scan plugins/ directory
    FS-->>PM: List of .jar files

    loop For each plugin.jar
        PM->>FS: Read plugin.yml from JAR
        FS-->>PM: PluginMetadata

        Note over PM: Validate plugin structure
        PM->>PM: validatePlugin(metadata)

        PM->>CL: Create IsolatedClassLoader(plugin.jar)
        CL->>CL: Set parent=SystemClassLoader
        CL-->>PM: ClassLoader instance

        PM->>CL: loadClass(mainClass)
        CL-->>PM: Plugin class

        PM->>Plugin: new Instance()
        Plugin-->>PM: Plugin instance

        Note over PM,Plugin: Load Phase
        PM->>Plugin: onLoad(PluginContext)
        Plugin->>Plugin: buildComponentManifest()
        Plugin->>Plugin: Initialize resources
        Plugin-->>PM: (void)

        Note over PM,CR: Registration Phase
        PM->>Plugin: getComponentManifest()
        Plugin-->>PM: ComponentManifest
        PM->>CR: registerComponent(manifest)
        CR->>DB: INSERT INTO cms_component_registry
        DB-->>CR: Success

        Note over PM,Plugin: Activation Phase
        PM->>Plugin: onActivate(PluginContext)
        Plugin->>Plugin: Start background tasks (if any)
        Plugin-->>PM: (void)

        PM->>PM: Store plugin instance in registry
    end

    PM-->>Boot: All plugins loaded successfully
```

**Key Steps**:
1. **Discovery**: Scan `plugins/` directory for `.jar` files
2. **Validation**: Read `plugin.yml`, validate required fields (`plugin-id`, `main-class`, `version`)
3. **ClassLoader Creation**: Create isolated classloader to prevent conflicts
4. **Instantiation**: Load plugin class, call no-arg constructor
5. **Load**: Call `onLoad(context)`, plugin builds manifest and initializes
6. **Registration**: Register component in database for fast lookup
7. **Activation**: Call `onActivate(context)`, plugin is ready for use

**Preconditions**:
- Plugin JAR exists in `plugins/` directory
- Plugin has valid `plugin.yml` with all required fields
- Main class implements `UIComponentPlugin` interface

**Postconditions**:
- Plugin instance stored in memory
- Component registered in database
- Frontend can fetch component metadata and bundle

---

### 6.1.2 Plugin Hot Reload

This scenario shows how a plugin can be reloaded without restarting VSD.

```mermaid
sequenceDiagram
    participant IDE as IntelliJ Plugin
    participant API as PluginController
    participant PM as PluginManager
    participant Old as Old Plugin Instance
    participant CL as Old ClassLoader
    participant CR as ComponentRegistry
    participant DB as Database
    participant New as New Plugin Instance
    participant NewCL as New ClassLoader

    IDE->>IDE: Build plugin JAR
    IDE->>API: POST /api/plugins/{id}/reload
    API->>PM: reloadPlugin(pluginId)

    Note over PM,Old: Deactivation Phase
    PM->>Old: onDeactivate(context)
    Old->>Old: Stop background tasks
    Old-->>PM: (void)

    Note over PM,CR: Unregistration Phase
    PM->>CR: unregisterComponent(pluginId)
    CR->>DB: DELETE FROM cms_component_registry WHERE plugin_id=?
    DB-->>CR: Success

    Note over PM,CL: ClassLoader Disposal
    PM->>CL: close()
    CL->>CL: Release resources
    PM->>PM: Clear references to old plugin
    PM->>PM: Trigger GC to unload classes

    Note over PM,NewCL: Reload Phase
    PM->>FS: Read new plugin.jar
    PM->>NewCL: Create new IsolatedClassLoader
    NewCL-->>PM: ClassLoader instance

    PM->>NewCL: loadClass(mainClass)
    NewCL-->>PM: Plugin class (new version)

    PM->>New: new Instance()
    New-->>PM: New plugin instance

    Note over PM,New: Re-load and Re-activate
    PM->>New: onLoad(context)
    New->>New: buildComponentManifest()
    New-->>PM: (void)

    PM->>New: getComponentManifest()
    New-->>PM: ComponentManifest
    PM->>CR: registerComponent(manifest)
    CR->>DB: INSERT INTO cms_component_registry
    DB-->>CR: Success

    PM->>New: onActivate(context)
    New-->>PM: (void)

    PM-->>API: Reload successful
    API-->>IDE: 200 OK
```

**Key Steps**:
1. **Deactivation**: Call `onDeactivate()` on old plugin instance
2. **Unregistration**: Remove component from database
3. **ClassLoader Disposal**: Close old classloader, release resources, trigger GC
4. **Reload**: Create new classloader, load new version of plugin
5. **Re-activation**: Call `onLoad()` and `onActivate()` on new instance

**Benefits**:
- No server restart required
- Faster development cycle
- Zero downtime for plugin updates

**Limitations**:
- Cannot change plugin ID (would be treated as different plugin)
- Cannot change package structure (breaks classloading)
- Memory leaks possible if plugin holds static references

---

### 6.1.3 Plugin Deactivation and Uninstall

```mermaid
sequenceDiagram
    participant Admin as Admin User
    participant API as PluginController
    participant PM as PluginManager
    participant Plugin
    participant CR as ComponentRegistry
    participant DB as Database
    participant FS as File System

    Admin->>API: POST /api/plugins/{id}/deactivate
    API->>PM: deactivatePlugin(pluginId)

    Note over PM,Plugin: Deactivation
    PM->>Plugin: onDeactivate(context)
    Plugin->>Plugin: Stop background tasks
    Plugin->>Plugin: Release resources
    Plugin-->>PM: (void)

    PM->>PM: Mark plugin as inactive
    PM-->>API: Deactivation successful
    API-->>Admin: 200 OK

    Note over Admin: Later...
    Admin->>API: DELETE /api/plugins/{id}
    API->>PM: uninstallPlugin(pluginId)

    Note over PM,Plugin: Uninstallation
    PM->>Plugin: onUninstall(context)
    Plugin->>Plugin: Clean up data
    Plugin->>Plugin: Remove database entries (if any)
    Plugin-->>PM: (void)

    PM->>CR: unregisterComponent(pluginId)
    CR->>DB: DELETE FROM cms_component_registry WHERE plugin_id=?
    DB-->>CR: Success

    PM->>FS: Delete plugin.jar
    FS-->>PM: File deleted

    PM->>PM: Close classloader
    PM->>PM: Clear plugin from registry

    PM-->>API: Uninstall successful
    API-->>Admin: 200 OK
```

**Key Steps**:
1. **Deactivation**: Call `onDeactivate()`, plugin stops but remains installed
2. **Uninstallation**: Call `onUninstall()`, plugin performs cleanup
3. **Unregistration**: Remove from component registry
4. **File Deletion**: Delete JAR file from plugins directory
5. **Cleanup**: Close classloader, clear references

---

## 6.2 Authentication Flows

### 6.2.1 Local JWT Authentication

This scenario shows how a user logs in with username and password.

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant BC as BCryptPasswordEncoder
    participant JWT as JwtService
    participant DB as Database

    User->>FE: Enter username "admin" and password
    FE->>AC: POST /api/auth/login<br/>{username, password}

    AC->>AS: authenticate(username, password)
    AS->>UR: findByUsernameOrEmail(username)
    UR->>DB: SELECT * FROM users WHERE username=?
    DB-->>UR: User entity
    UR-->>AS: User

    alt User not found
        AS-->>AC: throw BadCredentialsException
        AC-->>FE: 401 Unauthorized
        FE->>User: Show "Invalid credentials"
    else User found
        AS->>AS: Check user.status == APPROVED
        alt User not approved
            AS-->>AC: throw AccountNotApprovedException
            AC-->>FE: 403 Forbidden
            FE->>User: Show "Account pending approval"
        else User approved
            AS->>BC: matches(password, user.passwordHash)
            BC-->>AS: boolean

            alt Password incorrect
                AS-->>AC: throw BadCredentialsException
                AC-->>FE: 401 Unauthorized
                FE->>User: Show "Invalid credentials"
            else Password correct
                AS->>JWT: generateAccessToken(user)
                JWT->>JWT: Create JWT with HS256
                JWT->>JWT: Set claims (sub, roles, exp=15min)
                JWT-->>AS: accessToken

                AS->>JWT: generateRefreshToken(user)
                JWT->>JWT: Create refresh token (exp=7days)
                JWT-->>AS: refreshToken

                AS-->>AC: AuthResponse(accessToken, refreshToken, user)
                AC-->>FE: 200 OK + tokens
                FE->>FE: localStorage.setItem("token", accessToken)
                FE->>FE: localStorage.setItem("refreshToken", refreshToken)
                FE->>User: Redirect to /sites
            end
        end
    end
```

**JWT Token Structure (HS256)**:
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "1",
    "username": "admin",
    "email": "admin@localhost",
    "roles": ["ADMIN"],
    "iat": 1709217600,
    "exp": 1709218500
  },
  "signature": "..."
}
```

**Key Steps**:
1. User submits credentials
2. Backend looks up user by username/email
3. Validates user status (must be APPROVED)
4. Compares password hash using BCrypt
5. Generates JWT access token (HS256, 15 min expiry)
6. Generates refresh token (7 day expiry)
7. Returns tokens to frontend
8. Frontend stores in localStorage

---

### 6.2.2 OAuth2 SSO Authentication

This scenario shows how a user logs in via VSD Auth Server (or other OAuth2 provider).

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant Core as VSD Core
    participant Auth as VSD Auth Server
    participant OAuth2 as OAuth2SuccessHandler
    participant AS as AuthService
    participant UR as UserRepository
    participant RM as RoleMapper
    participant JWT as JwtService
    participant DB as Database

    User->>FE: Click "Sign in with VSD Auth Server"
    FE->>Core: GET /oauth2/authorization/vsd-auth
    Core->>Auth: Redirect to /oauth2/authorize?client_id=vsd-cms&...
    Auth->>User: Show login page
    User->>Auth: Enter credentials
    Auth->>Auth: Authenticate user
    Auth->>Core: Redirect to /login/oauth2/code/vsd-auth?code=...

    Note over Core,Auth: Token Exchange
    Core->>Auth: POST /oauth2/token<br/>{code, client_id, client_secret}
    Auth->>Auth: Validate authorization code
    Auth-->>Core: {access_token (RS256), id_token, refresh_token}

    Note over Core,Auth: User Info
    Core->>Auth: GET /userinfo<br/>Authorization: Bearer {access_token}
    Auth-->>Core: {sub, email, name, roles: ["ROLE_ADMIN"]}

    Note over Core,OAuth2: OAuth2 Success Handler
    Core->>OAuth2: onAuthenticationSuccess(OAuth2User)
    OAuth2->>AS: processOAuth2User(userInfo)

    AS->>UR: findByEmail(userInfo.email)
    UR->>DB: SELECT * FROM users WHERE email=?
    DB-->>UR: User or null
    UR-->>AS: Optional<User>

    alt User exists (linked account)
        AS->>AS: Update user info from SSO
        AS->>DB: UPDATE users SET name=?, updated_at=?
    else New user (first SSO login)
        AS->>AS: Create new user from SSO
        AS->>AS: user.status = APPROVED (auto-approve SSO users)
        AS->>AS: user.provider = OAUTH2
        AS->>DB: INSERT INTO users (email, name, status, provider)
    end

    Note over AS,RM: Role Mapping
    AS->>RM: mapRoles(userInfo.roles)
    RM->>RM: Map "ROLE_ADMIN" → "ADMIN"
    RM->>RM: Map "ROLE_CMS_EDITOR" → "EDITOR"
    RM-->>AS: List<RoleName>

    AS->>AS: Assign roles to user
    AS->>DB: INSERT INTO user_roles (user_id, role_id)

    Note over AS,JWT: Local JWT Generation
    AS->>JWT: generateAccessToken(user)
    JWT->>JWT: Create JWT with HS256 (local format)
    JWT-->>AS: accessToken (local)

    AS-->>OAuth2: User with JWT
    OAuth2->>FE: Redirect to /oauth2/callback?token={accessToken}
    FE->>FE: Parse token from URL
    FE->>FE: localStorage.setItem("token", accessToken)
    FE->>User: Redirect to /sites
```

**Role Mapping Table**:

| Auth Server Role | VSD Role | Description |
|-----------------|----------|-------------|
| `ROLE_ADMIN` or `ROLE_CMS_ADMIN` | `ADMIN` | Full system access |
| `ROLE_EDITOR` or `ROLE_CMS_EDITOR` | `EDITOR` | Content editing |
| `ROLE_VIEWER` or `ROLE_CMS_VIEWER` | `VIEWER` | Read-only access |
| `ROLE_DESIGNER` or `ROLE_CMS_DESIGNER` | `DESIGNER` | Design permissions |
| (default) | `USER` | Basic user |

**Key Steps**:
1. User initiates OAuth2 flow
2. Redirect to auth server login page
3. Auth server authenticates user
4. Redirect back with authorization code
5. Exchange code for access token (RS256)
6. Fetch user info from auth server
7. Create or update local user account
8. Map external roles to VSD roles
9. Auto-approve SSO users (status = APPROVED)
10. Generate local JWT token (HS256)
11. Redirect to frontend with token

**Dual Auth Mode**: After SSO login, users receive a local HS256 JWT token for subsequent requests. This allows VSD to work seamlessly whether the user logged in locally or via SSO.

---

### 6.2.3 Authenticated API Request

This scenario shows how JWT tokens are validated on each API request.

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant Filter as JwtAuthenticationFilter
    participant JWT as JwtService
    participant UR as UserRepository
    participant SEC as SecurityContext
    participant Controller
    participant Service
    participant DB as Database

    FE->>Controller: GET /api/sites<br/>Authorization: Bearer {token}
    Controller->>Filter: doFilterInternal(request)

    Note over Filter,JWT: Token Extraction
    Filter->>Filter: Extract token from Authorization header
    Filter->>JWT: validateToken(token)
    JWT->>JWT: Parse JWT signature
    JWT->>JWT: Check expiration

    alt Token invalid or expired
        JWT-->>Filter: throw JwtException
        Filter-->>FE: 401 Unauthorized
    else Token valid
        JWT-->>Filter: Claims (sub, roles, exp)

        Note over Filter,UR: Load User
        Filter->>JWT: getUsernameFromToken(token)
        JWT-->>Filter: username
        Filter->>UR: findByUsername(username)
        UR->>DB: SELECT * FROM users WHERE username=?
        DB-->>UR: User
        UR-->>Filter: User

        Note over Filter,SEC: Set Security Context
        Filter->>Filter: Create Authentication object
        Filter->>SEC: setAuthentication(authentication)
        SEC-->>Filter: (void)

        Filter->>Controller: Continue filter chain
        Controller->>Controller: Check @PreAuthorize or role requirements
        Controller->>Service: Method call
        Service->>DB: Database operation
        DB-->>Service: Result
        Service-->>Controller: Response
        Controller-->>FE: 200 OK + data
    end
```

**Key Steps**:
1. Frontend sends request with JWT token in `Authorization: Bearer {token}` header
2. `JwtAuthenticationFilter` intercepts request
3. Extract and validate token signature and expiration
4. Load user from database by username claim
5. Create `Authentication` object and set in `SecurityContext`
6. Controller checks role requirements (`@PreAuthorize`, `@Secured`)
7. Process request if authorized

---

## 6.3 Page Rendering Pipeline

### 6.3.1 Builder Canvas Rendering

This scenario shows how the builder canvas renders a page definition.

```mermaid
sequenceDiagram
    participant User
    participant Builder as BuilderPage
    participant Store as pageStore
    participant Canvas as BuilderCanvas
    participant Loader as PluginLoaderService
    participant Renderer as CanvasRenderer
    participant API as Core API
    participant React

    User->>Builder: Open page in builder
    Builder->>Store: loadPageDefinition(pageId)
    Store->>API: GET /api/sites/{siteId}/pages/{pageId}
    API-->>Store: PageDefinition
    Store->>Store: Set current page
    Store-->>Builder: Page loaded

    Note over Builder,Canvas: Initial Render
    Builder->>Canvas: Render canvas
    Canvas->>Store: Get page.components[]
    Store-->>Canvas: Component tree

    loop For each component in tree
        Canvas->>Loader: ensurePluginLoaded(pluginId)
        Loader->>API: GET /api/plugins/{pluginId}/bundle.js
        API-->>Loader: bundle.js content
        Loader->>Loader: eval(bundle.js) to register in window
        Loader-->>Canvas: Plugin ready

        Canvas->>Renderer: renderComponent(component)
        Renderer->>Renderer: Look up renderer: window[pluginId][componentId + "Renderer"]
        Renderer->>React: createElement(Renderer, props)
        React-->>Renderer: React element

        alt Component has children
            loop For each child
                Renderer->>Renderer: Recursively render child
            end
        end

        Renderer-->>Canvas: Rendered component
    end

    Canvas->>User: Display interactive canvas

    Note over User,Canvas: User Interaction
    User->>Canvas: Drag component
    Canvas->>Canvas: Handle @dnd-kit onDragEnd
    Canvas->>Store: updateComponentPosition(id, x, y)
    Store->>Store: Update in-memory tree
    Store-->>Canvas: Tree updated
    Canvas->>Canvas: Re-render (React state change)
    Canvas->>User: Updated canvas
```

**Component Tree Example**:
```json
{
  "components": [
    {
      "id": "comp-1",
      "pluginId": "page-layout-plugin",
      "componentId": "pageLayout",
      "children": {
        "header": [
          {
            "id": "comp-2",
            "pluginId": "navbar-component-plugin",
            "componentId": "navbar",
            "props": { "variant": "sticky" }
          }
        ],
        "center": [
          {
            "id": "comp-3",
            "pluginId": "label-component-plugin",
            "componentId": "label",
            "props": { "text": "Welcome", "variant": "h1" }
          }
        ]
      }
    }
  ]
}
```

**Key Steps**:
1. Load page definition from API
2. Store in Zustand `pageStore`
3. For each component, load plugin bundle if not already loaded
4. Look up renderer function in `window[pluginId][componentId + "Renderer"]`
5. Create React element with component props
6. Recursively render children
7. Handle user interactions (drag, resize, select)
8. Update store, trigger re-render

---

### 6.3.2 Standalone Preview Window Synchronization

This scenario shows how the standalone preview window stays synchronized with the builder.

```mermaid
sequenceDiagram
    participant Builder as BuilderPage
    participant BC as BroadcastChannel
    participant Preview as StandalonePreviewPage
    participant Store as multiPagePreviewStore
    participant Canvas as BuilderCanvas

    Builder->>Builder: User clicks "Open Preview Window"
    Builder->>Preview: window.open("/preview/{siteId}/{pageId}")
    Preview->>Preview: Page loads

    Note over Preview,BC: Preview Ready
    Preview->>BC: postMessage({type: "PREVIEW_READY"})
    BC->>Builder: onMessage("PREVIEW_READY")
    Builder->>Builder: Store preview window reference

    Note over Builder,Preview: Initial Sync
    Builder->>BC: postMessage({type: "PAGE_UPDATE", payload: {page, pageMeta}})
    BC->>Preview: onMessage("PAGE_UPDATE")
    Preview->>Store: setPreviewPage(page)
    Preview->>Store: setPageMeta(pageMeta)
    Store-->>Preview: State updated
    Preview->>Canvas: Render page
    Canvas->>Preview: Display page

    Note over Builder,Preview: User edits component
    Builder->>Builder: User changes label text
    Builder->>Builder: Debounce 300ms
    Builder->>BC: postMessage({type: "PAGE_UPDATE", payload: {page, pageMeta}})
    BC->>Preview: onMessage("PAGE_UPDATE")
    Preview->>Store: setPreviewPage(page)
    Store-->>Preview: State updated
    Preview->>Canvas: Re-render
    Canvas->>Preview: Updated display

    Note over Builder,Preview: User navigates to another page
    Builder->>Builder: User selects different page
    Builder->>BC: postMessage({type: "PAGE_NAVIGATE", payload: {pageRoute}})
    BC->>Preview: onMessage("PAGE_NAVIGATE")
    Preview->>Store: navigateToPage(pageRoute)
    Store-->>Preview: Current page changed
    Preview->>Canvas: Render new page
    Canvas->>Preview: Display new page

    Note over Builder,Preview: Connection Check (Ping/Pong)
    Builder->>BC: postMessage({type: "BUILDER_PING"})
    BC->>Preview: onMessage("BUILDER_PING")
    Preview->>BC: postMessage({type: "PREVIEW_PONG"})
    BC->>Builder: onMessage("PREVIEW_PONG")
    Builder->>Builder: Preview is alive
```

**BroadcastChannel Messages**:

| Message Type | Direction | Payload | Description |
|-------------|-----------|---------|-------------|
| `PREVIEW_READY` | Preview → Builder | (none) | Preview window loaded and ready |
| `PAGE_UPDATE` | Builder → Preview | `{page, pageMeta}` | Page content updated |
| `PAGE_NAVIGATE` | Builder → Preview | `{pageRoute}` | Navigate to different page |
| `PAGES_LIST` | Builder → Preview | `{pages, currentPageId, siteId}` | List of all pages updated |
| `BUILDER_PING` | Builder → Preview | (none) | Check if preview is alive |
| `PREVIEW_PONG` | Preview → Builder | (none) | Response to ping |
| `CLOSE_PREVIEW` | Builder → Preview | (none) | Close preview window |

**Key Features**:
- **BroadcastChannel API**: Cross-window communication without WebSocket
- **localStorage Fallback**: For browsers without BroadcastChannel support
- **Debounced Updates**: 300ms debounce to prevent excessive messages
- **Ping/Pong**: Periodic health check to detect closed windows
- **Same-Origin Only**: Works only when both windows on same origin

**Benefits**:
- No server infrastructure required
- Works through proxies (Cloudflare Tunnel)
- Real-time synchronization
- Low latency

---

## 6.4 Export Process

### 6.4.1 Static HTML Export

This scenario shows how a site is exported to static HTML files.

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant API as ExportController
    participant ES as ExportService
    participant PR as PageRepository
    participant CR as ComponentRegistry
    participant PL as PluginLoaderService
    participant FS as FileSystem
    participant ZIP as ZipOutputStream

    User->>FE: Click "Export as Static HTML"
    FE->>API: POST /api/sites/{siteId}/export?type=html
    API->>ES: exportAsStaticHTML(siteId)

    Note over ES,PR: Load Site Data
    ES->>PR: findAllBySiteId(siteId)
    PR-->>ES: List<Page>

    Note over ES,ZIP: Create Export Directory
    ES->>FS: Create temp directory
    ES->>ZIP: new ZipOutputStream()

    loop For each page
        Note over ES,CR: Render Page to HTML
        ES->>ES: Generate HTML structure
        ES->>ES: Add <!DOCTYPE html>
        ES->>ES: Add <head> with meta tags

        loop For each component in page.components
            ES->>CR: getComponentManifest(pluginId, componentId)
            CR-->>ES: ComponentManifest

            ES->>PL: Load plugin renderer
            PL-->>ES: Renderer function

            ES->>ES: renderToHTML(component, props, styles)
            ES->>ES: Append to HTML body
        end

        ES->>ES: Inline all styles
        ES->>ES: Bundle React runtime (production build)
        ES->>ES: Add hydration script

        ES->>ZIP: addEntry(page.route + "/index.html", html)
    end

    Note over ES,ZIP: Copy Assets
    ES->>FS: Copy images from content repository
    ES->>ZIP: addEntry("assets/images/*")
    ES->>FS: Copy fonts
    ES->>ZIP: addEntry("assets/fonts/*")

    Note over ES,ZIP: Finalize
    ES->>ZIP: close()
    ES->>FS: Create site-export.zip
    FS-->>ES: File path

    ES-->>API: byte[] zipFile
    API-->>FE: Download response
    FE->>User: Save "site-export.zip"

    Note over User: User extracts and deploys
    User->>User: Extract site-export.zip
    User->>User: Upload to web server (Nginx, S3, Netlify)
```

**Generated File Structure**:
```
site-export.zip
├── index.html           (homepage, route="/")
├── about/
│   └── index.html       (route="/about")
├── contact/
│   └── index.html       (route="/contact")
├── assets/
│   ├── images/
│   │   ├── logo.png
│   │   └── hero.jpg
│   ├── fonts/
│   │   └── roboto.woff2
│   └── js/
│       ├── react.production.min.js
│       └── runtime.js
└── README.txt
```

**Key Steps**:
1. Load all pages for site
2. For each page, render component tree to HTML
3. Inline all styles (no external CSS)
4. Bundle React runtime (production build)
5. Copy assets (images, fonts)
6. Create ZIP archive
7. Return as download

---

### 6.4.2 Spring Boot Export

This scenario shows how a site is exported as a Spring Boot application.

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant API as ExportController
    participant ES as ExportService
    participant PR as PageRepository
    participant TPL as TemplateEngine
    participant MVN as MavenProject
    participant FS as FileSystem
    participant ZIP as ZipOutputStream

    User->>FE: Click "Export as Spring Boot"
    FE->>API: POST /api/sites/{siteId}/export?type=springboot
    API->>ES: exportAsSpringBoot(siteId)

    Note over ES,MVN: Create Project Structure
    ES->>MVN: Create Maven project
    ES->>MVN: Generate pom.xml with site-runtime dependency
    ES->>FS: Create src/main/java/
    ES->>FS: Create src/main/resources/

    Note over ES,PR: Load Pages
    ES->>PR: findAllBySiteId(siteId)
    PR-->>ES: List<Page>

    loop For each page
        Note over ES,TPL: Generate Thymeleaf Template
        ES->>TPL: Create template for page
        TPL->>TPL: Add Thymeleaf namespace
        TPL->>TPL: Add component placeholders
        TPL->>TPL: Add data binding expressions

        ES->>FS: Write to templates/{route}.html
    end

    Note over ES,FS: Generate Controllers
    ES->>FS: Create PageController.java
    ES->>FS: Add @GetMapping for each route
    ES->>FS: Add data fetching logic

    Note over ES,FS: Generate Main Application Class
    ES->>FS: Create Application.java
    ES->>FS: Add @SpringBootApplication
    ES->>FS: Add main() method

    Note over ES,FS: Configuration
    ES->>FS: Create application.properties
    ES->>FS: Add database config (H2 by default)
    ES->>FS: Add server.port=8080
    ES->>FS: Add site-runtime config

    Note over ES,FS: Copy Static Assets
    ES->>FS: Copy to src/main/resources/static/
    ES->>FS: Copy images, fonts, CSS, JS

    Note over ES,FS: Documentation
    ES->>FS: Create README.md with deployment instructions
    ES->>FS: Create Dockerfile for containerization

    Note over ES,ZIP: Package
    ES->>ZIP: Create site-springboot.zip
    ES->>ZIP: Add all project files
    ES->>ZIP: close()

    ES-->>API: byte[] zipFile
    API-->>FE: Download response
    FE->>User: Save "site-springboot.zip"

    Note over User: User builds and runs
    User->>User: Extract site-springboot.zip
    User->>User: mvn clean package
    User->>User: java -jar target/site-app.jar
```

**Generated Project Structure**:
```
site-springboot/
├── pom.xml
├── Dockerfile
├── README.md
└── src/main/
    ├── java/com/example/site/
    │   ├── Application.java
    │   ├── controller/
    │   │   └── PageController.java
    │   └── config/
    │       └── SiteRuntimeConfig.java
    └── resources/
        ├── application.properties
        ├── templates/
        │   ├── index.html (Thymeleaf)
        │   ├── about.html
        │   └── contact.html
        └── static/
            ├── css/
            ├── js/
            └── images/
```

**Generated pom.xml Dependencies**:
```xml
<dependencies>
    <!-- Site Runtime -->
    <dependency>
        <groupId>dev.mainul35</groupId>
        <artifactId>site-runtime</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- H2 Database (can be changed) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

**Key Steps**:
1. Create Maven project structure
2. Generate pom.xml with site-runtime dependency
3. For each page, generate Thymeleaf template
4. Generate Spring Boot controllers with route mappings
5. Generate main application class
6. Create application.properties with configuration
7. Copy static assets
8. Create README with deployment instructions
9. Package as ZIP

**Deployment Options for Exported Site**:
- Run locally: `mvn spring-boot:run`
- Build JAR: `mvn clean package`, then `java -jar target/site-app.jar`
- Docker: `docker build -t my-site .`, then `docker run -p 8080:8080 my-site`
- Cloud: Deploy to Heroku, AWS Elastic Beanstalk, Google App Engine

---

## 6.5 Page Versioning and Rollback

This scenario shows how page versions are saved and restored.

```mermaid
sequenceDiagram
    participant User
    participant FE as Frontend
    participant API as PageController
    participant PS as PageService
    participant PR as PageRepository
    participant VR as PageVersionRepository
    participant DB as Database

    Note over User,FE: User makes changes
    User->>FE: Edit page (add/modify components)
    FE->>API: PUT /api/sites/{siteId}/pages/{pageId}
    API->>PS: updatePage(pageId, pageDefinition)

    Note over PS,PR: Save Current Version
    PS->>PR: findById(pageId)
    PR->>DB: SELECT * FROM pages WHERE id=?
    DB-->>PR: Current page
    PR-->>PS: Page

    PS->>VR: Create new PageVersion
    PS->>VR: version.content = currentPage.content
    PS->>VR: version.versionNumber = currentPage.version + 1
    PS->>VR: version.createdBy = currentUser
    PS->>VR: version.createdAt = now()
    PS->>VR: save(version)
    VR->>DB: INSERT INTO page_versions
    DB-->>VR: Version saved

    Note over PS,PR: Update Current Page
    PS->>PR: Update page.content = newDefinition
    PS->>PR: Update page.version++
    PS->>PR: save(page)
    PR->>DB: UPDATE pages SET content=?, version=?
    DB-->>PR: Page updated

    PS-->>API: Updated page
    API-->>FE: 200 OK
    FE->>User: Page saved

    Note over User,FE: Later... rollback needed
    User->>FE: View version history
    FE->>API: GET /api/sites/{siteId}/pages/{pageId}/versions
    API->>VR: findAllByPageId(pageId)
    VR->>DB: SELECT * FROM page_versions WHERE page_id=? ORDER BY version DESC
    DB-->>VR: List<PageVersion>
    VR-->>API: Versions
    API-->>FE: Version list
    FE->>User: Display version history

    User->>FE: Select version to restore
    FE->>API: POST /api/sites/{siteId}/pages/{pageId}/rollback<br/>{versionId}
    API->>PS: rollbackToVersion(pageId, versionId)

    PS->>VR: findById(versionId)
    VR->>DB: SELECT * FROM page_versions WHERE id=?
    DB-->>VR: PageVersion
    VR-->>PS: Version

    Note over PS,PR: Save current as new version (before rollback)
    PS->>PR: findById(pageId)
    PR-->>PS: Current page
    PS->>VR: Create version from current
    PS->>VR: save(currentVersion)
    VR->>DB: INSERT INTO page_versions

    Note over PS,PR: Restore from version
    PS->>PR: Update page.content = version.content
    PS->>PR: Update page.version++
    PS->>PR: save(page)
    PR->>DB: UPDATE pages SET content=?, version=?
    DB-->>PR: Page rolled back

    PS-->>API: Rolled back page
    API-->>FE: 200 OK
    FE->>User: Page restored to version {versionNumber}
```

**Database Schema**:
```sql
-- pages table
CREATE TABLE pages (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL,
    name VARCHAR(255),
    route VARCHAR(255),
    content JSONB,  -- Page definition
    version INT DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- page_versions table
CREATE TABLE page_versions (
    id BIGSERIAL PRIMARY KEY,
    page_id BIGINT NOT NULL,
    version_number INT,
    content JSONB,  -- Snapshot of page definition
    created_by BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (page_id) REFERENCES pages(id) ON DELETE CASCADE
);
```

**Key Steps**:
1. Before updating page, save current version to `page_versions` table
2. Increment version number
3. Update page with new content
4. On rollback, save current state as new version first (safety)
5. Restore content from selected version
6. Increment version number again

**Benefits**:
- Full version history
- Safe rollback (always creates version before rollback)
- Audit trail (who changed what when)
- No data loss

---

## 6.6 Real-time Preview Synchronization (Technical Deep Dive)

This section provides additional technical details on the preview synchronization mechanism.

### BroadcastChannel API vs WebSocket

| Feature | BroadcastChannel | WebSocket |
|---------|-----------------|-----------|
| **Communication** | Browser-local (same origin) | Server-mediated |
| **Setup** | No server infrastructure | Requires server |
| **Latency** | Very low (in-process) | Network latency |
| **Proxy Compatibility** | Always works | May require proxy config |
| **Cross-tab** | ✅ Yes | ❌ No (without server) |
| **Browser Support** | Modern browsers | All browsers |

### localStorage Fallback

When `BroadcastChannel` is not available:

```typescript
// Fallback to localStorage events
if (!('BroadcastChannel' in window)) {
  // Sender
  const sendMessage = (message: Message) => {
    localStorage.setItem('vsd-preview-message', JSON.stringify(message));
    localStorage.removeItem('vsd-preview-message'); // Trigger event
  };

  // Receiver
  window.addEventListener('storage', (event) => {
    if (event.key === 'vsd-preview-message' && event.newValue) {
      const message = JSON.parse(event.newValue);
      handleMessage(message);
    }
  });
}
```

### Message Flow Timing

```
Builder                             Preview
  |                                    |
  |--- PAGE_UPDATE (t=0) ------------->|
  |                                    | (Render starts)
  |                                    | (Load plugins: 50-200ms)
  |                                    | (React render: 10-50ms)
  |<-- Preview visible (t=60-250ms) ---|
  |                                    |
  |--- PAGE_UPDATE (t=500) ----------->|  (Debounced)
  |                                    | (Re-render: 5-20ms)
  |<-- Updated (t=505-520ms) ----------|
```

**Performance Optimizations**:
- Debounce updates: 300ms
- Only send changed properties
- Plugins cached after first load
- React reconciliation minimizes DOM updates

---

[← Previous: Building Block View](05-building-block-view.md) | [Back to Index](README.md) | [Next: Deployment View →](07-deployment-view.md)
