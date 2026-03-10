# 2. Architecture Constraints

This section describes constraints that limit design and implementation freedom.

---

## 2.1 Technical Constraints

### TC-01: Java Version

| Constraint | Java 21 or higher required |
|------------|----------------------------|
| **Background** | Modern language features (records, pattern matching, virtual threads) |
| **Impact** | Limits deployment to Java 21+ environments |
| **Mitigation** | Containerization ensures consistent runtime |

### TC-02: Spring Boot Framework

| Constraint | Spring Boot 4.0.0 as core framework |
|------------|-------------------------------------|
| **Background** | Enterprise-grade framework with extensive ecosystem |
| **Impact** | Plugin developers must understand Spring concepts (DI, AOP, configurations) |
| **Mitigation** | Provide plugin SDK that abstracts Spring complexity |

### TC-03: React as Frontend Framework

| Constraint | React 18.3.1 with TypeScript |
|------------|------------------------------|
| **Background** | Industry standard for component-based UIs |
| **Impact** | Plugin developers must write React components, learning curve for non-React developers |
| **Mitigation** | Comprehensive examples, generated TypeScript types, IntelliJ plugin support |

### TC-04: Maven Build System

| Constraint | Maven for Java build, npm/Vite for frontend |
|------------|---------------------------------------------|
| **Background** | Standard Java build tool with dependency management |
| **Impact** | Multi-module coordination required, build times can increase with many plugins |
| **Mitigation** | Bill of Materials (BOM) for consistency, incremental builds |

### TC-05: H2 Database for Development

| Constraint | Embedded H2 database in development mode |
|------------|------------------------------------------|
| **Background** | Zero-configuration development database |
| **Impact** | Limited to single-node, file-based storage |
| **Mitigation** | PostgreSQL recommended for production |

### TC-06: Browser Compatibility

| Constraint | Modern browsers (Chrome 90+, Firefox 88+, Safari 14+, Edge 90+) |
|------------|----------------------------------------------------------------|
| **Background** | Use of modern Web APIs (BroadcastChannel, ES2020+ features) |
| **Impact** | IE11 not supported, older browsers require polyfills |
| **Mitigation** | Clear browser requirements in documentation |

### TC-07: JVM Memory Requirements

| Constraint | Minimum 1GB heap for core application, additional 256MB per plugin |
|------------|-------------------------------------------------------------------|
| **Background** | Plugin classloaders hold class metadata in memory |
| **Impact** | Memory usage scales with plugin count |
| **Mitigation** | Monitor memory, provide guidelines for plugin optimization |

---

## 2.2 Organizational Constraints

### OC-01: Open Source License

| Constraint | MIT License |
|------------|-------------|
| **Background** | Permissive license for maximum adoption |
| **Impact** | Commercial use allowed, minimal restrictions |
| **Mitigation** | Contributors must agree to license terms |

### OC-02: Development Team Size

| Constraint | Small core team (1-5 developers assumed) |
|------------|------------------------------------------|
| **Background** | Open source project with limited dedicated resources |
| **Impact** | Prioritization critical, community contributions important |
| **Mitigation** | Clear contribution guidelines, good documentation |

### OC-03: Documentation Language

| Constraint | English as primary language |
|------------|------------------------------|
| **Background** | International audience |
| **Impact** | Non-English speakers may face barrier |
| **Mitigation** | Community translations welcome |

### OC-04: Release Cadence

| Constraint | Semantic versioning (MAJOR.MINOR.PATCH) |
|------------|------------------------------------------|
| **Background** | Clear version compatibility signals |
| **Impact** | Breaking changes only in major versions |
| **Mitigation** | Deprecation warnings in minor versions |

### OC-05: Support Model

| Constraint | Community support via GitHub Issues |
|------------|--------------------------------------|
| **Background** | Open source project without commercial support contract |
| **Impact** | No SLA guarantees for issue resolution |
| **Mitigation** | Active community engagement, comprehensive docs |

---

## 2.3 Conventions

### CV-01: Code Style

| Convention | Google Java Style Guide (backend), Airbnb JavaScript Style (frontend) |
|------------|-----------------------------------------------------------------------|
| **Enforcement** | Checkstyle (Java), ESLint (JavaScript/TypeScript) |
| **Rationale** | Consistency across codebase, easier code reviews |

### CV-02: Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| **Plugin ID** | kebab-case | `button-component-plugin` |
| **Component ID** | camelCase | `label`, `newsletterForm` |
| **Java Packages** | Reverse domain | `dev.mainul35.cms.plugin` |
| **REST Endpoints** | kebab-case | `/api/sites/{siteId}/pages/{pageId}` |
| **Database Tables** | snake_case | `cms_component_registry` |
| **React Components** | PascalCase | `ButtonRenderer` |

### CV-03: Git Workflow

| Convention | Feature branch workflow with PR reviews |
|------------|----------------------------------------|
| **Branch Naming** | `feature/description`, `bugfix/description`, `hotfix/description` |
| **Commit Messages** | Conventional Commits (feat:, fix:, docs:, refactor:) |
| **Rationale** | Clear history, automated changelog generation |

### CV-04: Versioning Strategy

| Component | Versioning |
|-----------|-----------|
| **Core Platform** | Semantic versioning synchronized across all modules |
| **Plugins** | Independent versioning with dependency declaration |
| **Plugin SDK** | Backward compatibility within major version |
| **Site Runtime** | Synchronized with core platform version |

### CV-05: Documentation Standards

| Convention | Markdown for all documentation, Mermaid for diagrams |
|------------|-----------------------------------------------------|
| **Structure** | arc42 for architecture, README for quick start, inline JavaDoc/JSDoc |
| **Rationale** | Version controllable, easy to read on GitHub, diagram-as-code |

### CV-06: Testing Standards

| Test Type | Requirement | Framework |
|-----------|------------|-----------|
| **Unit Tests** | >70% coverage for core logic | JUnit 5, Jest |
| **Integration Tests** | Critical flows (auth, plugin loading, export) | Spring Test, React Testing Library |
| **Plugin Tests** | Plugin developers should provide tests | Plugin choice |

### CV-07: API Design

| Convention | RESTful API with JSON payloads |
|------------|--------------------------------|
| **HTTP Methods** | GET (read), POST (create), PUT (update), DELETE (remove) |
| **Response Format** | `{"data": {...}, "error": null}` or `{"data": null, "error": {...}}` |
| **Error Codes** | Standard HTTP status codes (400, 401, 403, 404, 500) |
| **Pagination** | `page`, `size`, `sort` query parameters |

### CV-08: Security Standards

| Convention | OWASP Top 10 compliance |
|------------|-------------------------|
| **Authentication** | JWT tokens with RS256 (OAuth2) or HS256 (local) |
| **Authorization** | Role-based access control (RBAC) |
| **Input Validation** | Validate all inputs, sanitize outputs |
| **Secrets Management** | Environment variables for production, never hardcode |

---

## 2.4 External Constraints

### EC-01: Third-party Dependencies

| Constraint | Limited use of proprietary dependencies |
|------------|------------------------------------------|
| **Background** | Open source project, avoid vendor lock-in |
| **Impact** | Prefer Apache 2.0, MIT, BSD licensed libraries |
| **Mitigation** | Dependency review before adoption |

### EC-02: OAuth2 Provider Requirements

| Constraint | OAuth2 providers must support Authorization Code flow |
|------------|-------------------------------------------------------|
| **Background** | Spring Security OAuth2 client requirement |
| **Impact** | Not all identity providers supported out-of-box |
| **Mitigation** | Generic OIDC configuration for custom providers |

### EC-03: Database Compatibility

| Constraint | JPA-compatible databases for production |
|------------|------------------------------------------|
| **Supported** | PostgreSQL (recommended), MySQL, MariaDB |
| **Not Supported** | Oracle (licensing), NoSQL as primary (plugin-specific OK) |
| **Rationale** | Open source preference, JPA standardization |

### EC-04: Container Platform

| Constraint | Docker as primary containerization |
|------------|-------------------------------------|
| **Background** | Industry standard, cross-platform |
| **Impact** | Kubernetes/OpenShift supported but not required |
| **Mitigation** | Provide Docker Compose for local, Kubernetes manifests optional |

### EC-05: Node.js Version

| Constraint | Node.js 20.10.0+ for frontend build |
|------------|-------------------------------------|
| **Background** | Vite build tool requirement |
| **Impact** | CI/CD must have Node.js installed |
| **Mitigation** | Use nvm for version management, document in README |

---

## 2.5 Development Constraints

### DC-01: IDE Requirement

| Constraint | IntelliJ IDEA recommended (with VSD plugin) |
|------------|---------------------------------------------|
| **Background** | VSD IntelliJ plugin provides development acceleration |
| **Impact** | VS Code users lack tooling (generated types, one-click build) |
| **Mitigation** | Core development possible without IDE plugin, manual build scripts provided |

### DC-02: Plugin Development Learning Curve

| Constraint | Plugin developers must understand both Java and React |
|------------|------------------------------------------------------|
| **Background** | Plugins have backend (Java) and frontend (React) components |
| **Impact** | Higher barrier than single-language frameworks |
| **Mitigation** | Comprehensive examples, template plugins, SDK documentation |

### DC-03: Build Time

| Constraint | Full multi-module build takes 3-5 minutes |
|------------|-------------------------------------------|
| **Background** | Maven + npm builds, multiple modules |
| **Impact** | CI/CD pipeline time, developer productivity |
| **Mitigation** | Incremental builds, build caching, parallel builds |

### DC-04: Hot Reload Limitations

| Constraint | Plugin hot-reload requires REST endpoint, not automatic |
|------------|--------------------------------------------------------|
| **Background** | Classloader isolation makes automatic reload complex |
| **Impact** | Manual trigger required for plugin updates |
| **Mitigation** | IntelliJ plugin automates trigger, REST API provided |

---

## 2.6 Operational Constraints

### OP-01: Minimum Server Specification

| Resource | Requirement |
|----------|-------------|
| **CPU** | 2 cores (4 recommended) |
| **RAM** | 2GB (4GB+ for production) |
| **Disk** | 10GB (20GB+ for sites with media) |
| **Network** | HTTPS recommended for OAuth2 |

### OP-02: Backup Requirements

| Constraint | File-based database requires filesystem backup |
|------------|-----------------------------------------------|
| **H2 Database** | Backup `data/vsddb.mv.db` file |
| **PostgreSQL** | Standard pg_dump backup |
| **Plugins** | Backup `plugins/` directory |
| **Uploads** | Backup `uploads/` directory |

### OP-03: SSL/TLS for OAuth2

| Constraint | OAuth2 providers require HTTPS redirect URIs in production |
|------------|-----------------------------------------------------------|
| **Background** | Security requirement from identity providers |
| **Impact** | Development uses HTTP (localhost exception), production needs SSL |
| **Mitigation** | Cloudflare Tunnel, Let's Encrypt, reverse proxy with SSL |

### OP-04: Session Management

| Constraint | Stateless JWT authentication, no server-side sessions |
|------------|------------------------------------------------------|
| **Background** | Horizontal scaling support |
| **Impact** | Token revocation requires blocklist or short expiration |
| **Mitigation** | Refresh token rotation, 15-minute access token expiry |

---

## 2.7 Constraint Summary

### High Impact Constraints

These constraints have the most significant impact on architecture:

1. **Java 21 Requirement** - Modern runtime, limits legacy environment support
2. **Plugin Architecture** - Core design principle, affects all major components
3. **React Frontend** - UI technology lock-in, plugin developers must use React
4. **Spring Boot** - Framework lock-in, but provides extensive capabilities
5. **JWT Authentication** - Stateless design, scalability benefit

### Constraint Trade-offs

| Constraint | Benefit | Cost |
|-----------|---------|------|
| Plugin isolation (classloaders) | Security, conflict prevention | Memory overhead, complexity |
| Modern Java 21 | Performance, language features | Limits legacy deployments |
| React + TypeScript | Type safety, developer experience | Learning curve, build complexity |
| H2 for development | Zero config, fast setup | Production migration required |
| Open source (MIT) | Maximum adoption | No commercial support guarantee |

---

[← Previous: Introduction and Goals](01-introduction-and-goals.md) | [Back to Index](README.md) | [Next: System Scope and Context →](03-system-scope-and-context.md)
