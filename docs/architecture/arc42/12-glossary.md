# 12. Glossary

This glossary provides definitions of technical terms, domain-specific terminology, and abbreviations used throughout the Visual Site Designer architecture documentation.

---

## 12.1 Domain Terminology

### Visual Site Designer (VSD) Specific Terms

#### Canvas
The visual editing area in the builder interface where users drag-and-drop components to design pages. The canvas provides a WYSIWYG (What You See Is What You Get) representation of the page being built.

**Related**: Builder, Component, Page

**See also**: [Section 6.3 - Page Building Runtime View](06-runtime-view.md#page-building)

---

#### Component
A reusable UI element (e.g., Button, Label, Container) that can be placed on a page. Components are provided by plugins and registered in the ComponentRegistry.

**Types**:
- **UI Component**: Visual elements (Button, Label, Image)
- **Layout Component**: Container elements (Container, Grid, Flex)
- **Form Component**: Input elements (TextField, Checkbox, Select)
- **Navigation Component**: Navigation elements (Menu, Breadcrumb, Link)
- **Auth Component**: Authentication elements (Login Form, User Profile)

**Related**: Plugin, ComponentRegistry, Manifest

**See also**: [Section 8.1 - Domain Model](08-crosscutting-concepts.md#domain-model)

---

#### Component Manifest
A JSON descriptor that defines a component's properties, styles, and behavior. Plugins provide manifests for each component they register.

**Structure**:
```json
{
  "componentId": "label",
  "displayName": "Label",
  "category": "ui",
  "props": [...],
  "styles": [...],
  "sizeConstraints": {...}
}
```

**Related**: Component, Plugin, ComponentRegistry

**See also**: [Section 8.1.4 - Component Manifest Structure](08-crosscutting-concepts.md#component-manifest-structure)

---

#### Component Palette
The UI panel in the builder that displays available components organized by category. Users drag components from the palette onto the canvas.

**Categories**:
- UI (Button, Label, Image)
- Layout (Container, Grid, Flex)
- Form (TextField, Checkbox)
- Navigation (Menu, Link)
- Auth (Login Form)

**Related**: Builder, Component, Plugin

---

#### ComponentRegistry
A database table and in-memory cache that stores metadata about registered components. Each component entry includes its plugin ID, component ID, display name, category, and manifest JSON.

**Related**: Component, Plugin

**See also**: [Section 8.1 - Domain Model](08-crosscutting-concepts.md#domain-model)

---

#### Export
The process of generating a deployable website from a VSD site definition. VSD supports two export modes:
- **Static HTML Export**: Generates standalone HTML/CSS/JS files
- **Spring Boot Export**: Generates a Spring Boot application with Thymeleaf templates

**Related**: Site, Page, site-runtime

**See also**: [Section 6.4 - Site Export Process](06-runtime-view.md#site-export)

---

#### Hot Reload
The ability to reload a plugin at runtime without restarting the application. Hot reload disposes the plugin's classloader, reloads the JAR, and re-registers components.

**Use Case**: Plugin development (fast iteration cycle)

**Related**: Plugin, ClassLoader

**See also**: [Section 6.1 - Plugin Lifecycle](06-runtime-view.md#plugin-lifecycle)

---

#### Page
An individual web page within a site, identified by a route (e.g., `/home`, `/about`). Pages store their content as JSON with a tree of components.

**Properties**:
- **Name**: Human-readable page name
- **Route**: URL path (e.g., `/contact`)
- **Content**: JSON component tree
- **Version**: Current version number
- **Metadata**: Title, description, keywords

**Related**: Site, Component, PageVersion

**See also**: [Section 8.1 - Domain Model](08-crosscutting-concepts.md#domain-model)

---

#### PageVersion
A historical snapshot of a page's content at a specific point in time. VSD maintains version history to support rollback.

**Properties**:
- **Version Number**: Sequential version number (1, 2, 3, ...)
- **Content**: JSON snapshot of component tree
- **Created By**: User who created this version
- **Created At**: Timestamp

**Related**: Page, Rollback

**See also**: [Section 8.1 - Domain Model](08-crosscutting-concepts.md#domain-model)

---

#### Plugin
A JAR file containing Java code and frontend assets that extends VSD functionality by providing custom components.

**Structure**:
```
plugin.jar
├── plugin.yml              # Metadata
├── dev/mainul35/plugins/   # Java classes
└── frontend/bundle.js      # React component
```

**Lifecycle**: Upload → Validate → Load → Activate → Register Components

**Related**: Component, ClassLoader, UIComponentPlugin

**See also**: [Section 6.1 - Plugin Lifecycle](06-runtime-view.md#plugin-lifecycle)

---

#### Plugin SDK
The Java library (`flashcard-cms-plugin-sdk`) that provides interfaces and utilities for plugin development. Plugins implement the `UIComponentPlugin` interface from the SDK.

**Key Interfaces**:
- `UIComponentPlugin`: Main plugin interface
- `ComponentManifest`: Component metadata
- `PluginContext`: Access to Spring beans and services

**Related**: Plugin, UIComponentPlugin

**See also**: [Section 5.2 - Plugin SDK Module](05-building-block-view.md#plugin-sdk)

---

#### Preview Window
A separate browser window that displays a live preview of the page being edited. The preview synchronizes in real-time with the builder using the BroadcastChannel API.

**Synchronization**: BroadcastChannel API (fallback: localStorage events)

**Related**: Builder, BroadcastChannel, Canvas

**See also**: [Section 6.5 - Real-time Preview](06-runtime-view.md#realtime-preview)

---

#### Properties Panel
The UI panel in the builder that displays editable properties and styles for the selected component. Property editors are generated from the component's manifest.

**Property Types**:
- String (text input)
- Number (numeric input)
- Boolean (checkbox)
- Enum (dropdown)
- Color (color picker)
- Size (size input with units)

**Related**: Component, Manifest, Builder

---

#### Rollback
The ability to restore a page to a previous version from its version history. Users can view version history and select a version to restore.

**Related**: Page, PageVersion

**See also**: [Section 8.1 - Domain Model](08-crosscutting-concepts.md#domain-model)

---

#### Site
The top-level container representing a complete website project. A site contains multiple pages and is owned by a user.

**Properties**:
- **Name**: Site name (e.g., "My Portfolio")
- **Description**: Site description
- **Owner**: User who created the site
- **Status**: DRAFT, PUBLISHED, ARCHIVED
- **Pages**: List of pages in the site

**Related**: Page, User

**See also**: [Section 8.1 - Domain Model](08-crosscutting-concepts.md#domain-model)

---

#### site-runtime
A separate Maven module that provides runtime libraries for exported Spring Boot sites. Includes page rendering, component resolution, and data fetching utilities.

**Use Case**: Exported sites depend on site-runtime to render pages at runtime

**Related**: Export, Spring Boot

**See also**: [Section 5.2 - Site Runtime Module](05-building-block-view.md#site-runtime)

---

#### UIComponentPlugin
The Java interface that plugins must implement. Defines methods for plugin lifecycle (initialize, destroy) and component registration.

**Methods**:
- `void initialize(PluginContext context)`: Called when plugin loads
- `void destroy()`: Called when plugin unloads
- `List<ComponentManifest> getComponents()`: Returns component manifests

**Related**: Plugin, Plugin SDK

**See also**: [Section 5.2 - Plugin SDK](05-building-block-view.md#plugin-sdk)

---

## 12.2 Architecture and Development Terms

### A

#### ADR (Architecture Decision Record)
A document capturing a significant architectural decision, including context, decision, consequences, and alternatives considered. VSD documents ADRs in [Section 9](09-architecture-decisions.md).

**Format**: Context → Decision → Consequences → Alternatives

**Example**: ADR-001: Plugin-based Architecture with Isolated ClassLoaders

---

#### API (Application Programming Interface)
A set of endpoints exposed by the backend for the frontend to interact with the system. VSD provides a RESTful API.

**Base URL**: `/api/v1/`

**Key Endpoints**:
- `/api/v1/sites`: Site management
- `/api/v1/pages`: Page management
- `/api/v1/plugins`: Plugin management
- `/api/v1/components`: Component registry

**Related**: REST, Controller

**See also**: [Section 3.3 - Technical Context](03-system-scope-and-context.md#technical-context)

---

#### arc42
A template for software architecture documentation and communication. VSD uses arc42 to structure its architecture documentation.

**Sections**: Introduction, Constraints, Context, Solution Strategy, Building Blocks, Runtime View, Deployment, Crosscutting Concepts, Decisions, Quality, Risks, Glossary

**Website**: https://arc42.org/

---

### B

#### BOM (Bill of Materials)
A Maven module (`vsd-cms-bom`) that defines dependency versions for the entire project, ensuring consistency across modules.

**Use Case**: All modules import the BOM to inherit dependency versions

**Related**: Maven, Dependency Management

**See also**: [Section 5.2 - BOM Module](05-building-block-view.md#bom)

---

#### BroadcastChannel API
A browser API for cross-tab/cross-window communication within the same origin. VSD uses it for real-time preview synchronization.

**Fallback**: localStorage events for browsers without BroadcastChannel

**Browser Support**: Chrome 54+, Firefox 38+, Safari 15.4+, Edge 79+

**Related**: Preview Window, Real-time Preview

**See also**: [ADR-006: BroadcastChannel API](09-architecture-decisions.md#adr-006)

---

#### Builder
The main visual editing interface where users create and edit pages. Consists of component palette, canvas, properties panel, and preview window.

**Components**:
- Component Palette (left)
- Canvas (center)
- Properties Panel (right)
- Preview Window (separate window)

**Related**: Canvas, Properties Panel, Preview Window

---

### C

#### ClassLoader
A Java mechanism for loading classes at runtime. VSD uses isolated classloaders to load plugins independently, preventing dependency conflicts.

**Hierarchy**:
```
Bootstrap ClassLoader (JDK)
  ↓
System ClassLoader (Spring Boot, Plugin SDK)
  ↓
Plugin ClassLoader 1 (Plugin + dependencies)
  ↓
Plugin ClassLoader 2 (Plugin + dependencies)
```

**Related**: Plugin, Hot Reload, Classloader Isolation

**See also**: [ADR-001: Isolated ClassLoaders](09-architecture-decisions.md#adr-001)

---

#### CORS (Cross-Origin Resource Sharing)
HTTP header-based mechanism that allows a server to indicate which origins can access its resources. VSD configures CORS to allow frontend access from different ports during development.

**Configuration**: `@CrossOrigin` annotations on controllers

**Related**: Security, REST API

**See also**: [Section 8.2 - Security Concepts](08-crosscutting-concepts.md#security)

---

#### CRUD (Create, Read, Update, Delete)
The four basic operations for persistent storage. Most VSD entities (Site, Page, Plugin) support CRUD operations via REST API.

**Related**: REST API, JPA, Repository

---

#### CSRF (Cross-Site Request Forgery)
A web security vulnerability where unauthorized commands are submitted from a user the application trusts. VSD uses stateless JWT authentication, which does not require CSRF protection.

**Mitigation**: JWT tokens in Authorization header (not cookies)

**Related**: Security, JWT

---

### D

#### DTO (Data Transfer Object)
An object that carries data between processes. VSD uses DTOs to transfer data between backend and frontend, avoiding direct exposure of domain entities.

**Examples**:
- `SiteDTO`: Represents a site in API responses
- `PageDTO`: Represents a page in API responses
- `ComponentDTO`: Represents a component in API responses

**Related**: REST API, Domain Model

---

### E

#### E2E Testing (End-to-End Testing)
Testing that validates the entire application flow from start to finish, simulating real user scenarios. VSD currently lacks comprehensive E2E tests (technical debt TD-01).

**Tools**: Cypress, Playwright

**Related**: Testing, Quality Assurance

**See also**: [Section 11.4 - Technical Debt](11-risks-and-technical-debt.md#td-01)

---

### F

#### Flyway
A database migration tool that manages schema versioning. VSD uses Flyway to apply incremental schema changes.

**Migration Files**: `src/main/resources/db/migration/V{version}__{description}.sql`

**Related**: Database, Migration, PostgreSQL

**See also**: [ADR-004: Database Selection](09-architecture-decisions.md#adr-004)

---

### H

#### HMR (Hot Module Replacement)
A feature of modern build tools that updates modules in the browser without a full page reload. Vite provides fast HMR (< 100ms) for VSD frontend.

**Related**: Vite, Development Experience

**See also**: [ADR-007: Vite Build Tool](09-architecture-decisions.md#adr-007)

---

#### HS256 (HMAC-SHA256)
A symmetric algorithm for signing JWT tokens using a shared secret key. VSD uses HS256 for local JWT tokens.

**Related**: JWT, Authentication

**See also**: [Section 8.2 - Security Concepts](08-crosscutting-concepts.md#security)

---

### I

#### IIFE (Immediately Invoked Function Expression)
A JavaScript pattern where a function is defined and executed immediately. VSD plugin frontend bundles use IIFE format to avoid polluting the global scope.

**Example**:
```javascript
(function() {
  // Plugin code
  window.LabelComponentPlugin = { ... };
})();
```

**Related**: Plugin, Frontend Bundle, Vite

**See also**: [ADR-007: Vite Build Tool](09-architecture-decisions.md#adr-007)

---

### J

#### JAR (Java Archive)
A package file format used to aggregate Java classes and resources. VSD plugins are distributed as JAR files.

**Structure**:
```
plugin.jar
├── META-INF/MANIFEST.MF
├── plugin.yml
├── dev/mainul35/plugins/PluginClass.class
└── frontend/bundle.js
```

**Related**: Plugin, Maven

---

#### JPA (Java Persistence API)
A Java specification for object-relational mapping (ORM). VSD uses JPA with Hibernate to interact with the database.

**Annotations**: `@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne`, `@OneToMany`

**Related**: Hibernate, Database, Repository

**See also**: [Section 4.1 - Technology Stack](04-solution-strategy.md#backend-technology-stack)

---

#### JSON (JavaScript Object Notation)
A lightweight data-interchange format. VSD stores page content and component manifests as JSON.

**Use Cases**:
- Page content (component tree)
- Component manifests
- API request/response bodies

**Related**: JSONB, Page, Component

---

#### JSONB
A PostgreSQL data type for storing JSON documents with indexing and query capabilities. VSD uses JSONB for page content storage in production.

**Advantages**: Faster queries than TEXT, supports JSON operators (e.g., `->`, `->>`)

**Development**: H2 uses TEXT (no JSONB support)

**Related**: JSON, PostgreSQL, Page

**See also**: [ADR-004: Database Selection](09-architecture-decisions.md#adr-004)

---

#### JWT (JSON Web Token)
A compact, URL-safe token format for securely transmitting information between parties. VSD uses JWT for stateless authentication.

**Structure**: `header.payload.signature`

**Types**:
- Access Token: 15 minutes expiry
- Refresh Token: 7 days expiry

**Related**: Authentication, HS256, RS256

**See also**: [Section 8.2 - Security Concepts](08-crosscutting-concepts.md#security)

---

### M

#### Maven
A build automation and project management tool for Java. VSD uses Maven with a multi-module structure.

**Modules**:
- `vsd-cms-bom`: Bill of Materials
- `flashcard-cms-plugin-sdk`: Plugin SDK
- `site-runtime`: Runtime library
- `core`: Main application
- `plugins/`: Plugin modules

**Related**: Build Tool, BOM, Multi-module

**See also**: [Section 5.1 - Building Block View](05-building-block-view.md)

---

#### Multi-module
A Maven project structure with multiple related modules sharing a parent POM. VSD uses multi-module structure for core, SDK, plugins, and runtime.

**Benefits**: Shared configuration, consistent versioning, modular architecture

**Related**: Maven, BOM

---

### O

#### OAuth2 (Open Authorization 2.0)
An authorization framework that enables third-party applications to obtain limited access to a service. VSD supports OAuth2/SSO login via Google, Okta, Keycloak, and VSD Auth Server.

**Flow**: Authorization Code Grant

**Related**: SSO, Authentication, JWT

**See also**: [ADR-003: Dual Authentication Mode](09-architecture-decisions.md#adr-003)

---

#### OIDC (OpenID Connect)
An identity layer built on top of OAuth2 that adds authentication. VSD OAuth2 providers typically support OIDC for user info.

**Related**: OAuth2, SSO

---

### R

#### RBAC (Role-Based Access Control)
An access control model where permissions are assigned to roles, and users are assigned roles. VSD implements RBAC with five roles: ADMIN, DESIGNER, EDITOR, VIEWER, USER.

**Related**: Security, Authorization, Role

**See also**: [Section 8.2.2 - Role-Based Access Control](08-crosscutting-concepts.md#rbac)

---

#### Repository
A Spring Data interface for database access. VSD uses repository pattern for CRUD operations on entities.

**Examples**:
- `SiteRepository extends JpaRepository<Site, Long>`
- `PageRepository extends JpaRepository<Page, Long>`
- `UserRepository extends JpaRepository<User, Long>`

**Related**: JPA, Database, Spring Data

---

#### REST (Representational State Transfer)
An architectural style for designing networked applications. VSD provides a RESTful API for frontend-backend communication.

**Principles**:
- Stateless (JWT tokens)
- Resource-based URLs (`/api/v1/sites/{id}`)
- HTTP methods (GET, POST, PUT, DELETE)
- JSON payloads

**Related**: API, HTTP, JSON

**See also**: [Section 3.3 - Technical Context](03-system-scope-and-context.md#technical-context)

---

#### RS256 (RSA-SHA256)
An asymmetric algorithm for signing JWT tokens using RSA key pairs. OAuth2 providers use RS256 for access tokens.

**Related**: JWT, OAuth2, Authentication

**See also**: [Section 8.2 - Security Concepts](08-crosscutting-concepts.md#security)

---

### S

#### SPA (Single-Page Application)
A web application that loads a single HTML page and dynamically updates content as the user interacts. VSD frontend is a React SPA.

**Routing**: Client-side routing with React Router

**Related**: React, Frontend, Vite

---

#### Spring Boot
A framework for building production-ready Java applications with minimal configuration. VSD backend is built with Spring Boot 3.4.1.

**Features**: Auto-configuration, embedded Tomcat, Actuator, Security

**Related**: Java, Backend, Spring Framework

**See also**: [ADR-002: Technology Stack](09-architecture-decisions.md#adr-002)

---

#### Spring Security
A framework for authentication and access control in Spring applications. VSD uses Spring Security for JWT authentication and RBAC.

**Features**: Filter chain, authentication manager, method security

**Related**: Security, Authentication, Authorization

**See also**: [Section 8.2 - Security Concepts](08-crosscutting-concepts.md#security)

---

#### SSO (Single Sign-On)
An authentication method that allows users to log in once and access multiple systems. VSD supports SSO via OAuth2 providers.

**Providers**: Google, Okta, Keycloak, VSD Auth Server

**Related**: OAuth2, Authentication

**See also**: [ADR-003: Dual Authentication Mode](09-architecture-decisions.md#adr-003)

---

### T

#### Thymeleaf
A Java template engine for server-side HTML rendering. VSD uses Thymeleaf for Spring Boot export mode.

**Use Case**: Exported Spring Boot sites render pages using Thymeleaf templates

**Related**: Export, Spring Boot, site-runtime

**See also**: [Section 6.4 - Site Export](06-runtime-view.md#site-export)

---

#### TypeScript
A typed superset of JavaScript that compiles to plain JavaScript. VSD frontend is written in TypeScript for type safety.

**Version**: 5.6.2

**Benefits**: Type safety, better IDE support, fewer runtime errors

**Related**: JavaScript, Frontend, React

**See also**: [ADR-002: Technology Stack](09-architecture-decisions.md#adr-002)

---

### V

#### Vite
A modern build tool that provides fast HMR and optimized production builds. VSD uses Vite 6.0.5 for frontend development and plugin bundling.

**Advantages**: Fast HMR (< 100ms), ESM-native, esbuild-based

**Alternatives**: Webpack, Parcel, esbuild

**Related**: Frontend, Build Tool, HMR

**See also**: [ADR-007: Vite Build Tool](09-architecture-decisions.md#adr-007)

---

### W

#### WYSIWYG (What You See Is What You Get)
A type of editor that shows content during editing exactly as it will appear to the end user. VSD canvas provides WYSIWYG editing for pages.

**Related**: Canvas, Builder, Preview

---

### Z

#### Zustand
A lightweight state management library for React. VSD uses Zustand for frontend state management.

**Advantages**: Simple API, no boilerplate, better performance than Redux

**Stores**:
- Page store (current page, components)
- Component registry store
- Auth store (user, token)
- UI store (selected component, panel visibility)

**Related**: State Management, React, Frontend

**See also**: [ADR-005: Zustand for State Management](09-architecture-decisions.md#adr-005)

---

## 12.3 Abbreviations and Acronyms

### General

| Abbreviation | Full Form | Description |
|--------------|-----------|-------------|
| **ADR** | Architecture Decision Record | Document capturing architectural decisions |
| **API** | Application Programming Interface | Set of endpoints for system interaction |
| **BOM** | Bill of Materials | Maven module for dependency management |
| **CDN** | Content Delivery Network | Distributed network for serving static assets |
| **CI/CD** | Continuous Integration/Continuous Deployment | Automated build and deployment pipeline |
| **CORS** | Cross-Origin Resource Sharing | HTTP mechanism for cross-origin requests |
| **CRUD** | Create, Read, Update, Delete | Basic database operations |
| **CSRF** | Cross-Site Request Forgery | Web security vulnerability |
| **CSS** | Cascading Style Sheets | Stylesheet language for HTML |
| **DTO** | Data Transfer Object | Object for data transfer between layers |
| **E2E** | End-to-End | Testing methodology covering full workflows |
| **HTML** | HyperText Markup Language | Standard markup language for web pages |
| **HTTP** | HyperText Transfer Protocol | Application protocol for web communication |
| **HTTPS** | HTTP Secure | HTTP with TLS/SSL encryption |
| **IDE** | Integrated Development Environment | Software for code development (IntelliJ) |
| **IIFE** | Immediately Invoked Function Expression | JavaScript pattern for plugin isolation |
| **JAR** | Java Archive | Package format for Java applications |
| **JDK** | Java Development Kit | Java development toolkit |
| **JPA** | Java Persistence API | Java specification for ORM |
| **JSON** | JavaScript Object Notation | Lightweight data-interchange format |
| **JSONB** | JSON Binary | PostgreSQL binary JSON data type |
| **JWT** | JSON Web Token | Token format for authentication |
| **JVM** | Java Virtual Machine | Runtime environment for Java bytecode |
| **MFA** | Multi-Factor Authentication | Authentication using multiple factors |
| **MVP** | Minimum Viable Product | Product with core features |
| **NPM** | Node Package Manager | Package manager for JavaScript |
| **OAuth2** | Open Authorization 2.0 | Authorization framework |
| **OIDC** | OpenID Connect | Identity layer on OAuth2 |
| **ORM** | Object-Relational Mapping | Database access via objects |
| **REST** | Representational State Transfer | Architectural style for APIs |
| **RBAC** | Role-Based Access Control | Access control model |
| **SDK** | Software Development Kit | Toolkit for plugin development |
| **SLA** | Service Level Agreement | Commitment to service availability |
| **SPA** | Single-Page Application | Web app with single HTML page |
| **SQL** | Structured Query Language | Database query language |
| **SSO** | Single Sign-On | Authentication for multiple systems |
| **TLS/SSL** | Transport Layer Security/Secure Sockets Layer | Cryptographic protocols |
| **UI** | User Interface | Visual interface for user interaction |
| **URL** | Uniform Resource Locator | Web address |
| **VSD** | Visual Site Designer | The system being documented |
| **WYSIWYG** | What You See Is What You Get | Visual editing mode |
| **XSS** | Cross-Site Scripting | Web security vulnerability |

---

### Technology Stack

| Abbreviation | Full Form | Version | Purpose |
|--------------|-----------|---------|---------|
| **React** | React.js | 18.3.1 | Frontend framework |
| **TS** | TypeScript | 5.6.2 | Type-safe JavaScript |
| **H2** | H2 Database | 2.1+ | Development database |
| **PostgreSQL** | PostgreSQL | 14+ | Production database |
| **Maven** | Apache Maven | 3.8+ | Build tool |
| **Vite** | Vite.js | 6.0.5 | Frontend build tool |
| **Spring** | Spring Boot | 3.4.1 | Backend framework |
| **JUnit** | JUnit | 5.x | Unit testing framework |

---

### Database

| Term | Description |
|------|-------------|
| **DDL** | Data Definition Language (CREATE, ALTER, DROP) |
| **DML** | Data Manipulation Language (SELECT, INSERT, UPDATE, DELETE) |
| **Index** | Database structure for fast lookups |
| **Migration** | Schema change script (Flyway) |
| **ORM** | Object-Relational Mapping (JPA/Hibernate) |
| **Query** | Database request for data |
| **Schema** | Database structure definition |
| **Transaction** | Atomic unit of database work |

---

### Security

| Term | Description |
|------|-------------|
| **Access Token** | Short-lived JWT for API access (15 min) |
| **Refresh Token** | Long-lived token for access token renewal (7 days) |
| **BCrypt** | Password hashing algorithm |
| **HS256** | HMAC-SHA256 (symmetric JWT signing) |
| **RS256** | RSA-SHA256 (asymmetric JWT signing) |
| **Role** | Authorization role (ADMIN, DESIGNER, EDITOR, VIEWER, USER) |
| **Permission** | Authorization to perform an action |
| **Principal** | Authenticated user identity |

---

### Plugin System

| Term | Description |
|------|-------------|
| **Bundle** | Plugin JAR file with embedded assets |
| **ClassLoader** | Java class loading mechanism |
| **Hot Reload** | Runtime plugin reload without restart |
| **Isolation** | Separate classloader per plugin |
| **Manifest** | Component metadata (plugin.yml, component manifest JSON) |
| **Plugin Context** | Access to Spring beans and services |
| **Registry** | Database of registered components |
| **Sandbox** | Isolated execution environment (not implemented) |

---

## 12.4 Related Documentation

For more information, see:

- **Introduction**: [Section 1 - Introduction and Goals](01-introduction-and-goals.md)
- **Architecture**: [Section 5 - Building Block View](05-building-block-view.md)
- **Security**: [Section 8.2 - Security Concepts](08-crosscutting-concepts.md#security)
- **Plugin System**: [Section 6.1 - Plugin Lifecycle](06-runtime-view.md#plugin-lifecycle)
- **Decisions**: [Section 9 - Architecture Decisions](09-architecture-decisions.md)
- **Quality**: [Section 10 - Quality Requirements](10-quality-requirements.md)
- **Risks**: [Section 11 - Risks and Technical Debt](11-risks-and-technical-debt.md)

---

## 12.5 External References

### Documentation

- **arc42 Template**: https://arc42.org/
- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/
- **React Documentation**: https://react.dev/
- **TypeScript Documentation**: https://www.typescriptlang.org/docs/
- **Vite Documentation**: https://vitejs.dev/
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/

### Standards and Specifications

- **JWT (RFC 7519)**: https://datatracker.ietf.org/doc/html/rfc7519
- **OAuth2 (RFC 6749)**: https://datatracker.ietf.org/doc/html/rfc6749
- **OpenID Connect**: https://openid.net/connect/
- **REST**: https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
- **Semantic Versioning**: https://semver.org/

### Tools

- **Maven**: https://maven.apache.org/
- **Flyway**: https://flywaydb.org/
- **Hibernate**: https://hibernate.org/
- **Zustand**: https://github.com/pmndrs/zustand
- **Cypress**: https://www.cypress.io/
- **Playwright**: https://playwright.dev/

---

[← Previous: Risks and Technical Debt](11-risks-and-technical-debt.md) | [Back to Index](README.md)
