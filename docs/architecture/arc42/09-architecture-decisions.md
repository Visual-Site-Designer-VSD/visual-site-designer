# 9. Architecture Decisions

This section documents significant architectural decisions made during the development of Visual Site Designer using the Architecture Decision Record (ADR) format.

---

## ADR Format

Each ADR includes:
- **Context**: What is the issue we're facing?
- **Decision**: What did we decide?
- **Status**: Proposed | Accepted | Deprecated | Superseded
- **Consequences**: What are the implications?
- **Alternatives Considered**: What other options did we evaluate?

---

## ADR-001: Plugin-based Architecture with Isolated ClassLoaders

**Date**: 2025-02-15
**Status**: Accepted
**Decision Makers**: Architecture Team

### Context

Visual Site Designer requires extensibility to support custom UI components without modifying core code. The system must:
- Allow third-party developers to create components
- Support hot-reloading of plugins during development
- Prevent plugin conflicts (different versions of dependencies)
- Maintain system stability when plugins fail
- Enable plugin marketplace in the future

### Decision

Implement a plugin architecture where:
1. Each plugin is packaged as a JAR file with embedded resources
2. Plugins implement the `UIComponentPlugin` interface from the SDK
3. Each plugin loads in an **isolated ClassLoader**
4. Plugin SDK classes are shared from the parent classloader
5. Plugin dependencies are isolated within the plugin's classloader
6. Plugins register components in a centralized `ComponentRegistry`
7. Hot-reload is achieved by disposing classloader and reloading

**Plugin Structure**:
```
label-component-plugin.jar
├── plugin.yml                           # Metadata
├── dev/mainul35/plugins/ui/
│   └── LabelComponentPlugin.class      # Java plugin class
└── frontend/
    └── bundle.js                        # React component
```

**Isolated ClassLoader Hierarchy**:
```
Bootstrap ClassLoader (JDK)
    ↓
System ClassLoader (Spring Boot, Plugin SDK)
    ↓
Plugin ClassLoader 1 (Button Plugin + dependencies)
    ↓
Plugin ClassLoader 2 (Label Plugin + dependencies)
```

### Consequences

**Positive**:
- ✅ Unlimited extensibility - new components without core changes
- ✅ Plugin conflicts impossible - each plugin isolated
- ✅ Hot-reload support - dispose classloader and reload
- ✅ Developer productivity - fast iteration cycle
- ✅ Plugin marketplace feasible - safe to run third-party code
- ✅ Backward compatibility - plugins independent of core

**Negative**:
- ❌ Memory overhead - one classloader per plugin (est. 5-10MB each)
- ❌ Debugging complexity - stack traces cross classloader boundaries
- ❌ ClassLoader disposal risks - memory leaks if plugins hold static references
- ❌ Limited sandboxing - no Java SecurityManager in Java 17+
- ❌ Complexity - classloader management requires expertise

**Risks**:
- **Memory leaks**: Plugins holding static references prevent GC
  - *Mitigation*: Plugin validation, developer guidelines, leak detection tools
- **Malicious plugins**: Plugins can access file system, network
  - *Mitigation*: Plugin validation, ADMIN-only upload, code review for bundled plugins
- **Version conflicts in SDK**: Breaking changes in SDK affect all plugins
  - *Mitigation*: Semantic versioning, deprecation warnings, backward compatibility

### Alternatives Considered

#### Alternative 1: OSGi Framework
**Description**: Use OSGi (Apache Felix/Equinox) for module system
- **Pros**: Mature module system, proven isolation, dynamic lifecycle
- **Cons**: High complexity, steep learning curve, heavy framework overhead
- **Reason for rejection**: Too complex for project needs, overkill for component plugins

#### Alternative 2: Single Classloader (No Isolation)
**Description**: Load all plugins in system classloader
- **Pros**: Simple, no classloader complexity, easier debugging
- **Cons**: Version conflicts between plugins, no hot-reload, brittle
- **Reason for rejection**: Does not meet extensibility requirements

#### Alternative 3: Separate JVM Process per Plugin
**Description**: Run each plugin in separate Java process
- **Pros**: Complete isolation, no memory leak risks
- **Cons**: IPC overhead, high resource usage, complex communication
- **Reason for rejection**: Performance overhead unacceptable

#### Alternative 4: Script-based Plugins (JavaScript)
**Description**: Plugins written in JavaScript (Nashorn/GraalVM)
- **Pros**: Sandboxing possible, lightweight
- **Cons**: Limited access to Java ecosystem, performance, complex Java-JS bridge
- **Reason for rejection**: Limits plugin capabilities, performance concerns

---

## ADR-002: React + Spring Boot Technology Stack

**Date**: 2025-02-10
**Status**: Accepted
**Decision Makers**: Architecture Team

### Context

Need to select technology stack for:
- **Backend**: REST API, plugin runtime, authentication, database access
- **Frontend**: Visual builder, drag-and-drop, real-time preview

Requirements:
- Modern, maintainable, well-documented
- Large ecosystem and community
- Good developer experience
- Suitable for visual editing interfaces
- Plugin-friendly

### Decision

**Backend**: Spring Boot 4.0.0 + Java 21
- Spring Boot for REST API, dependency injection, security
- Java 21 for modern features (records, pattern matching, virtual threads)
- JPA/Hibernate for database access
- Maven for build and dependency management

**Frontend**: React 18.3.1 + TypeScript 5.6.2 + Vite 6.0.5
- React for component-based UI matching plugin model
- TypeScript for type safety and better IDE support
- Vite for fast HMR and modern build
- Zustand for lightweight state management

### Consequences

**Positive**:
- ✅ Spring Boot: De facto standard for enterprise Java, huge ecosystem
- ✅ React: Largest ecosystem, mature, excellent for visual builders
- ✅ TypeScript: Type safety reduces bugs, better developer experience
- ✅ Vite: Fast HMR (< 100ms), modern ESM-based, better than Webpack
- ✅ Well-documented: Abundant tutorials, Stack Overflow support
- ✅ Hiring: Easy to find developers with these skills

**Negative**:
- ❌ Spring Boot overhead: Heavier than lightweight frameworks (Micronaut, Quarkus)
- ❌ React bundle size: Larger than alternatives (Preact, Svelte)
- ❌ Learning curve: Both ecosystems have complexity
- ❌ Frequent updates: Keeping dependencies current requires effort

### Alternatives Considered

#### Alternative 1: Vue.js + Spring Boot
**Description**: Use Vue.js instead of React
- **Pros**: Simpler API, smaller bundle, official router/state
- **Cons**: Smaller ecosystem, fewer libraries, less mature for complex apps
- **Reason for rejection**: React ecosystem better for visual builders

#### Alternative 2: Angular + Spring Boot
**Description**: Use Angular instead of React
- **Pros**: Full framework, TypeScript native, opinionated
- **Cons**: Heavy, steep learning curve, verbose
- **Reason for rejection**: Too opinionated, slower development

#### Alternative 3: Svelte + Spring Boot
**Description**: Use Svelte instead of React
- **Pros**: Smaller bundles, reactive, simple syntax
- **Cons**: Smaller ecosystem, fewer libraries, less mature
- **Reason for rejection**: Ecosystem too small for complex app

#### Alternative 4: React + NestJS (Node.js)
**Description**: Use Node.js backend instead of Java
- **Pros**: Single language (JavaScript/TypeScript), fast development
- **Cons**: No JVM, harder to implement isolated classloaders, less mature plugin systems
- **Reason for rejection**: Java better for plugin isolation

---

## ADR-003: Dual Authentication Mode (Local JWT + OAuth2 SSO)

**Date**: 2025-02-18
**Status**: Accepted
**Decision Makers**: Security Team, Architecture Team

### Context

VSD needs to support multiple deployment scenarios:
1. **Small teams/individuals**: Simple username/password
2. **Enterprises**: SSO integration (Google, Okta, Keycloak, VSD Auth Server)

Requirements:
- Low friction for small users (no SSO setup required)
- Enterprise SSO integration for organizations
- Secure token-based authentication
- Support multiple OAuth2 providers

### Decision

Implement **dual authentication mode**:

1. **Local Authentication**:
   - Username/password stored in database (BCrypt hashed)
   - JWT tokens (HS256) issued on login
   - Access token: 15 min expiry
   - Refresh token: 7 day expiry
   - Token refresh endpoint for renewal

2. **OAuth2/SSO Authentication**:
   - OAuth2 authorization code flow
   - Support multiple providers (Google, Okta, Keycloak, VSD Auth Server)
   - Exchange authorization code for access token (RS256)
   - Fetch user info from provider
   - Create/update local user account
   - Map external roles to VSD roles (ADMIN, DESIGNER, EDITOR, etc.)
   - Issue local JWT token (HS256) for subsequent requests

3. **Unified Authorization**:
   - After OAuth2 login, user receives local JWT token
   - All API requests use local JWT (regardless of login method)
   - Single `JwtAuthenticationFilter` validates tokens
   - RBAC enforced consistently

**Authentication Flow**:
```
Local Login:
  User → Username/Password → JWT (HS256) → API Requests

OAuth2 Login:
  User → OAuth2 Provider → Access Token (RS256) → User Info
    → Create Local User → JWT (HS256) → API Requests
```

### Consequences

**Positive**:
- ✅ Flexibility: Support both small teams and enterprises
- ✅ Low barrier to entry: No SSO setup for small users
- ✅ Enterprise-ready: SSO integration for organizations
- ✅ Unified authorization: Single token format for all requests
- ✅ Role mapping: External roles map to VSD roles
- ✅ Auto-approval: SSO users auto-approved (trusted providers)

**Negative**:
- ❌ Complexity: Two authentication mechanisms to maintain
- ❌ Token validation: Must handle both HS256 and RS256 (during OAuth2 flow)
- ❌ User confusion: Multiple login options may confuse users
- ❌ Security surface: More code = more attack surface

**Security Considerations**:
- Access tokens short-lived (15 min) to minimize exposure
- Refresh token rotation on each refresh
- OAuth2 tokens validated with provider's JWK Set
- Local JWT tokens signed with secret key (256-bit)
- HTTPS required in production (Cloudflare Tunnel)

### Alternatives Considered

#### Alternative 1: OAuth2 Only
**Description**: Require OAuth2 for all authentication
- **Pros**: Single auth mechanism, more secure (delegated auth)
- **Cons**: Requires SSO setup for all users, high barrier to entry
- **Reason for rejection**: Too complex for small users

#### Alternative 2: Local Auth Only
**Description**: Only support username/password
- **Pros**: Simple, no OAuth2 complexity
- **Cons**: No enterprise SSO support, manual user management
- **Reason for rejection**: Does not meet enterprise requirements

#### Alternative 3: Session-based Auth
**Description**: Use server-side sessions instead of JWT
- **Pros**: Easier token revocation, no token expiry issues
- **Cons**: Stateful (harder to scale), requires session store
- **Reason for rejection**: Stateless JWT better for horizontal scaling

#### Alternative 4: OAuth2 with Passport.js (Node.js pattern)
**Description**: Use Spring Security OAuth2 resource server only
- **Pros**: Standard OAuth2, no local auth complexity
- **Cons**: Cannot support local username/password
- **Reason for rejection**: Need both modes

---

## ADR-004: H2 for Development, PostgreSQL for Production

**Date**: 2025-02-12
**Status**: Accepted
**Decision Makers**: Architecture Team

### Context

Need to select database for:
- **Development**: Fast setup, no external dependencies
- **Production**: Reliable, scalable, ACID-compliant

Requirements:
- Zero-config development experience
- Production-grade reliability
- JSON/JSONB support for page content
- Full-text search (future)

### Decision

**Development**: H2 (embedded, file-based)
```properties
spring.datasource.url=jdbc:h2:file:./data/vsddb
spring.datasource.driver-class-name=org.h2.Driver
```

**Production**: PostgreSQL 16
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/vsd_cms
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**Schema Management**: Flyway migrations
- Version-controlled schema definitions
- Automatic migration on startup
- Dialect-specific migrations when needed

### Consequences

**Positive**:
- ✅ Development: Zero setup, fast startup, no installation
- ✅ Production: PostgreSQL mature, reliable, JSONB support
- ✅ Flyway: Database schema version control
- ✅ H2 Console: Built-in web UI for debugging
- ✅ PostgreSQL features: JSONB, full-text search, partitioning

**Negative**:
- ❌ Dialect differences: Some SQL differences between H2 and PostgreSQL
- ❌ H2 limitations: Not suitable for production (single file, no replication)
- ❌ Testing gap: Tests run on H2, production uses PostgreSQL
- ❌ Migration complexity: Flyway scripts may need dialect-specific versions

**Migration Strategy**:
```sql
-- V1__initial_schema.sql
-- Common SQL that works on both H2 and PostgreSQL
CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- V2__add_jsonb_support.sql (PostgreSQL-specific)
-- Add JSONB columns for PostgreSQL
ALTER TABLE pages ADD COLUMN content JSONB;

-- For H2, use TEXT instead (handled by Flyway callbacks)
```

### Alternatives Considered

#### Alternative 1: PostgreSQL for All Environments
**Description**: Use PostgreSQL for development and production
- **Pros**: Same database everywhere, no dialect differences
- **Cons**: Requires Docker/installation, slower setup
- **Reason for rejection**: Higher barrier for new developers

#### Alternative 2: H2 for All Environments
**Description**: Use H2 in production (file-based)
- **Pros**: Simple, consistent across environments
- **Cons**: Not production-ready, no replication, single file risk
- **Reason for rejection**: Not suitable for production

#### Alternative 3: MySQL/MariaDB
**Description**: Use MySQL instead of PostgreSQL
- **Pros**: Popular, well-known, good performance
- **Cons**: JSON support less mature than JSONB, licensing concerns (MySQL)
- **Reason for rejection**: PostgreSQL JSONB superior

#### Alternative 4: MongoDB (NoSQL)
**Description**: Use MongoDB for document storage
- **Pros**: Native JSON, flexible schema
- **Cons**: No ACID transactions (multi-document), relational queries harder
- **Reason for rejection**: Need ACID transactions, relational model better

---

## ADR-005: Zustand for Frontend State Management

**Date**: 2025-02-20
**Status**: Accepted
**Decision Makers**: Frontend Team

### Context

Need state management for:
- Current page definition (component tree)
- Registered components (from plugins)
- User authentication state
- Preview window state

Requirements:
- Simple API (low boilerplate)
- Good performance (minimal re-renders)
- TypeScript support
- DevTools integration

### Decision

Use **Zustand** for state management

**Example Store**:
```typescript
interface PageStore {
  currentPage: PageDefinition | null;
  components: Component[];
  selectedComponentId: string | null;
  setCurrentPage: (page: PageDefinition) => void;
  addComponent: (component: Component) => void;
  selectComponent: (id: string) => void;
}

export const usePageStore = create<PageStore>((set) => ({
  currentPage: null,
  components: [],
  selectedComponentId: null,
  setCurrentPage: (page) => set({ currentPage: page }),
  addComponent: (component) =>
    set((state) => ({ components: [...state.components, component] })),
  selectComponent: (id) => set({ selectedComponentId: id }),
}));
```

**Usage**:
```typescript
function BuilderCanvas() {
  const { currentPage, addComponent } = usePageStore();
  // Component logic
}
```

### Consequences

**Positive**:
- ✅ Simple API: Minimal boilerplate compared to Redux
- ✅ Performance: Fine-grained subscriptions, fewer re-renders
- ✅ TypeScript: Excellent type inference
- ✅ Small bundle: ~1KB (vs Redux ~2KB + Redux Toolkit ~10KB)
- ✅ DevTools: Browser extension for debugging
- ✅ No providers: No wrapping components in context providers

**Negative**:
- ❌ Less ecosystem: Fewer middleware options than Redux
- ❌ Less known: Smaller community than Redux
- ❌ Manual optimization: Must use selectors carefully for performance

### Alternatives Considered

#### Alternative 1: Redux + Redux Toolkit
**Description**: Use Redux for state management
- **Pros**: Industry standard, huge ecosystem, mature
- **Cons**: Boilerplate, complex setup, larger bundle
- **Reason for rejection**: Too much boilerplate for project needs

#### Alternative 2: React Context + useReducer
**Description**: Use built-in React Context API
- **Pros**: No dependencies, built-in
- **Cons**: Performance issues (re-renders), verbose
- **Reason for rejection**: Poor performance for complex state

#### Alternative 3: MobX
**Description**: Use MobX for reactive state
- **Pros**: Observable pattern, minimal boilerplate
- **Cons**: Decorator syntax (non-standard), magic behavior
- **Reason for rejection**: Too opinionated, decorator complexity

#### Alternative 4: Recoil
**Description**: Use Recoil (Facebook)
- **Pros**: Atoms/selectors pattern, good performance
- **Cons**: Still experimental, less stable
- **Reason for rejection**: Not production-ready at time of decision

---

## ADR-006: BroadcastChannel API for Real-time Preview

**Date**: 2025-02-22
**Status**: Accepted
**Decision Makers**: Frontend Team

### Context

Need real-time synchronization between:
- **Builder window**: Main editing interface
- **Preview window**: Separate window showing live preview

Requirements:
- Low latency (< 100ms)
- Works through proxies (Cloudflare Tunnel)
- No server infrastructure
- Bidirectional communication

### Decision

Use **BroadcastChannel API** for cross-window communication

**Implementation**:
```typescript
// Builder window
const channel = new BroadcastChannel('vsd-preview');

// Send page updates to preview
channel.postMessage({
  type: 'PAGE_UPDATE',
  payload: { page, pageMeta }
});

// Preview window
const channel = new BroadcastChannel('vsd-preview');

channel.onmessage = (event) => {
  if (event.data.type === 'PAGE_UPDATE') {
    renderPage(event.data.payload.page);
  }
};
```

**Fallback**: localStorage events for browsers without BroadcastChannel
```typescript
if (!('BroadcastChannel' in window)) {
  // Use localStorage events
  localStorage.setItem('vsd-message', JSON.stringify(message));
  localStorage.removeItem('vsd-message'); // Trigger event
}
```

### Consequences

**Positive**:
- ✅ No server infrastructure: Works entirely in browser
- ✅ Low latency: In-process communication (< 10ms)
- ✅ Proxy-friendly: Works through Cloudflare Tunnel
- ✅ Simple API: Easy to implement
- ✅ Cross-tab: Automatically works across browser tabs

**Negative**:
- ❌ Same-origin only: Both windows must be on same domain
- ❌ Browser support: Not available in older browsers (IE11)
- ❌ No persistence: Messages lost if window closed
- ❌ Manual sync: Must handle reconnection logic

**Browser Support**:
- Chrome: ✅ 54+
- Firefox: ✅ 38+
- Safari: ✅ 15.4+
- Edge: ✅ 79+
- IE11: ❌ (fallback to localStorage)

### Alternatives Considered

#### Alternative 1: WebSocket Server
**Description**: Use WebSocket for real-time sync
- **Pros**: Standard approach, reliable, works cross-origin
- **Cons**: Requires server infrastructure, proxy configuration, higher latency
- **Reason for rejection**: Server overhead, proxy complexity (Cloudflare Tunnel)

#### Alternative 2: Server-Sent Events (SSE)
**Description**: Use SSE for server-to-client push
- **Pros**: Standard, simple
- **Cons**: Unidirectional (server→client), requires server
- **Reason for rejection**: Need bidirectional, server overhead

#### Alternative 3: Polling
**Description**: Preview polls builder for updates
- **Pros**: Simple, no special infrastructure
- **Cons**: High latency (1+ second), inefficient, server load
- **Reason for rejection**: Unacceptable latency

#### Alternative 4: SharedWorker
**Description**: Use SharedWorker for cross-tab communication
- **Pros**: Shared state across tabs
- **Cons**: Complex API, limited browser support, harder debugging
- **Reason for rejection**: BroadcastChannel simpler and sufficient

---

## ADR-007: Vite Build Tool for Frontend

**Date**: 2025-02-08
**Status**: Accepted
**Decision Makers**: Frontend Team

### Context

Need build tool for:
- Development server with HMR
- Production bundling
- Plugin frontend bundling (IIFE format)

Requirements:
- Fast HMR (< 100ms)
- Modern ESM support
- TypeScript support
- Plugin system for custom builds

### Decision

Use **Vite 6.0.5** for frontend builds

**Configuration**:
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  build: {
    outDir: 'dist',
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          ui: ['@dnd-kit/core', '@dnd-kit/sortable']
        }
      }
    }
  }
});
```

**Plugin Build** (IIFE format):
```typescript
// Plugin vite.config.ts
export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.ts',
      name: 'LabelComponentPlugin',
      formats: ['iife'],
      fileName: () => 'bundle.js'
    },
    rollupOptions: {
      external: ['react', 'react-dom'],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM'
        }
      }
    }
  }
});
```

### Consequences

**Positive**:
- ✅ Fast HMR: < 100ms (vs Webpack ~1-5s)
- ✅ Fast cold start: < 1s (vs Webpack ~10s)
- ✅ Modern: ESM-native, tree-shaking
- ✅ TypeScript: Built-in support (esbuild)
- ✅ Plugin ecosystem: Growing ecosystem
- ✅ Dev experience: Instant feedback

**Negative**:
- ❌ Newer: Less mature than Webpack
- ❌ Ecosystem: Some plugins still Webpack-only
- ❌ Breaking changes: More frequent major versions

### Alternatives Considered

#### Alternative 1: Webpack
**Description**: Use Webpack 5
- **Pros**: Mature, huge ecosystem, well-documented
- **Cons**: Slow HMR (1-5s), complex config, slow cold start
- **Reason for rejection**: HMR too slow for visual builder

#### Alternative 2: Parcel
**Description**: Use Parcel bundler
- **Pros**: Zero-config, fast
- **Cons**: Less flexible, smaller ecosystem
- **Reason for rejection**: Less control over build

#### Alternative 3: esbuild
**Description**: Use esbuild directly
- **Pros**: Fastest bundler (Go-based)
- **Cons**: No HMR, no dev server, low-level
- **Reason for rejection**: Vite uses esbuild under the hood, better DX

#### Alternative 4: Turbopack (Next.js)
**Description**: Use Turbopack (Rust-based)
- **Pros**: Very fast, future of Next.js
- **Cons**: Not stable, Next.js-specific
- **Reason for rejection**: Not production-ready

---

## Decision Log Summary

| ADR | Decision | Date | Status |
|-----|----------|------|--------|
| ADR-001 | Plugin-based Architecture with Isolated ClassLoaders | 2025-02-15 | Accepted |
| ADR-002 | React + Spring Boot Technology Stack | 2025-02-10 | Accepted |
| ADR-003 | Dual Authentication Mode (Local JWT + OAuth2 SSO) | 2025-02-18 | Accepted |
| ADR-004 | H2 for Development, PostgreSQL for Production | 2025-02-12 | Accepted |
| ADR-005 | Zustand for Frontend State Management | 2025-02-20 | Accepted |
| ADR-006 | BroadcastChannel API for Real-time Preview | 2025-02-22 | Accepted |
| ADR-007 | Vite Build Tool for Frontend | 2025-02-08 | Accepted |

---

## Future ADRs

Potential future architectural decisions:

- **ADR-008**: Plugin Marketplace Architecture
- **ADR-009**: Multi-tenancy Strategy
- **ADR-010**: GraphQL API Layer
- **ADR-011**: Event-Driven Architecture for Plugin Communication
- **ADR-012**: Caching Strategy (Redis)
- **ADR-013**: Monitoring and Observability Stack
- **ADR-014**: Internationalization (i18n) Approach
- **ADR-015**: Mobile App Strategy (React Native)

---

[← Previous: Cross-cutting Concepts](08-crosscutting-concepts.md) | [Back to Index](README.md) | [Next: Quality Requirements →](10-quality-requirements.md)
