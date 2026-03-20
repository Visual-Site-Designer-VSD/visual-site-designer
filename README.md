# Visual Site Designer (VSD)

A visual drag-and-drop website builder platform with a plugin-based architecture for creating and exporting sites.

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6.2-blue.svg)](https://www.typescriptlang.org/)

---

## Quick Start

### Prerequisites

- **Java**: JDK 21 or higher
- **Maven**: 3.6+
- **Node.js**: 20.10.0+ (for frontend)
- **npm**: 10.2.3+

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/Visual-Site-Designer-VSD/visual-site-designer.git
cd visual-site-designer

# 2. Install the BOM and SDK (required once)
cd vsd-cms-bom && mvn clean install && cd ..
cd flashcard-cms-plugin-sdk && mvn clean install && cd ..

# 3. Build everything
mvn clean install -DskipTests

# 4. Start the application
cd core && mvn spring-boot:run
```

### Access the Builder

Open your browser and navigate to:

- **Application**: <http://localhost:8080>
- **H2 Console**: <http://localhost:8080/h2-console> (JDBC URL: `jdbc:h2:file:./data/vsddb`)

### Default Credentials

- **Username**: `admin`
- **Password**: `admin123`

---

## Documentation

### Architecture Documentation

Complete arc42 architecture documentation:

**[Architecture Documentation (arc42)](docs/architecture/arc42/README.md)**

1. [Introduction and Goals](docs/architecture/arc42/01-introduction-and-goals.md) - Requirements, stakeholders, quality goals
2. [Architecture Constraints](docs/architecture/arc42/02-architecture-constraints.md) - Technical and organizational constraints
3. [System Scope and Context](docs/architecture/arc42/03-system-scope-and-context.md) - Business and technical context
4. [Solution Strategy](docs/architecture/arc42/04-solution-strategy.md) - Fundamental decisions and approaches
5. [Building Block View](docs/architecture/arc42/05-building-block-view.md) - System decomposition and modules
6. [Runtime View](docs/architecture/arc42/06-runtime-view.md) - Runtime scenarios and behaviors
7. [Deployment View](docs/architecture/arc42/07-deployment-view.md) - Infrastructure and deployment
8. [Cross-cutting Concepts](docs/architecture/arc42/08-crosscutting-concepts.md) - Domain model, security, patterns
9. [Architecture Decisions](docs/architecture/arc42/09-architecture-decisions.md) - ADRs and design decisions
10. [Quality Requirements](docs/architecture/arc42/10-quality-requirements.md) - Quality scenarios and metrics
11. [Risks and Technical Debt](docs/architecture/arc42/11-risks-and-technical-debt.md) - Known risks and debt items
12. [Glossary](docs/architecture/arc42/12-glossary.md) - Technical terms and abbreviations

---

## Architecture Overview

### System Components

```text
visual-site-designer/
├── core/                      # Main Spring Boot application
├── frontend/                  # React visual builder (npm project)
├── vsd-cms-bom/               # Bill of Materials (version management)
├── flashcard-cms-plugin-sdk/  # Plugin development SDK
├── site-runtime/              # Runtime library for exported sites
├── plugins/                   # 12 bundled UI component plugins
├── generated-types/           # Auto-generated TypeScript types
├── docs/                      # arc42 architecture documentation
└── data/                      # H2 database storage
```

### Maven Modules

The parent POM defines 4 Maven modules: `vsd-cms-bom`, `flashcard-cms-plugin-sdk`, `site-runtime`, `core`. Each plugin under `plugins/` is an independent Maven module.

### Plugin System

VSD's extensibility comes from its plugin architecture:

- **Plugin ClassLoaders** - Each plugin has its own ClassLoader for class isolation, but Spring components (controllers, services, repositories) are registered in the main ApplicationContext to ensure JPA compatibility
- **Hot Reload** - Update plugins without restarting
- **Spring Integration** - Plugins can register controllers, services, entities
- **React Components** - UI components built with React
- **Type Safety** - Auto-generated TypeScript types (in `generated-types/`)
- **Multi-component Plugins** - A single plugin can provide multiple component variants via `getComponentManifests()`
- **Context Providers** - Plugins can provide shared context (e.g., auth, cart) via `ContextProviderPlugin`

### Plugin Types

| Type | Interface | Description |
|------|-----------|-------------|
| **UI Component** | `UIComponentPlugin` | Visual components for the drag-and-drop builder |
| **Context Provider** | `ContextProviderPlugin` | Shared context (auth, data) consumed by UI components |

### Bundled Plugins (12 plugins, 21 component variants)

| Plugin | Variants | Category |
|--------|----------|----------|
| auth-component-plugin | LoginForm, RegisterForm, SocialLoginButtons, ForgotPasswordForm, LogoutButton | form |
| button-component-plugin | Button | ui |
| container-layout-plugin | Container | layout |
| horizontal-row-plugin | HorizontalRow | ui |
| image-component-plugin | Image | ui |
| label-component-plugin | Label | ui |
| mobile-navbar-component-plugin | MobileNavbar | navbar |
| navbar-component-plugin | NavbarDefault, NavbarCentered, NavbarMinimal, NavbarDark, NavbarGlass, NavbarSticky, SidebarNav, TopHeaderBar | navbar |
| newsletter-form-plugin | NewsletterForm | form |
| page-layout-plugin | PageLayout | layout |
| scrollable-container-plugin | ScrollableContainer | layout |
| textbox-component-plugin | Textbox | ui |

### Export Options

| Format | Description | Use Case |
|--------|-------------|----------|
| **Static HTML** | Client-side export via `staticExportService.ts` | Static hosting (Netlify, Vercel, S3) |
| **Spring Boot + Thymeleaf** | Client-side export via `thymeleafExportService.ts` | Dynamic sites with server-side rendering |

Plugins participate in export through `ComponentManifest` template fields: `staticExportTemplate`, `thymeleafExportTemplate`, `hasCustomExport`, and `exportMetadata`.

---

## Plugin Development

### Plugin SDK

The SDK provides two approaches for building plugins:

**1. Annotation-based (recommended for simple plugins):**

Extend `AbstractUIComponentPlugin` and use `@UIComponent` annotation:

```java
@UIComponent(
    componentId = "horizontalRow",
    displayName = "Horizontal Row",
    category = "ui",
    icon = "horizontal_rule"
)
public class HorizontalRowComponentPlugin extends AbstractUIComponentPlugin {
    // Override buildDefaultProps(), buildConfigurableProps(), etc.
}
```

**2. Interface-based (for multi-component plugins):**

Implement `UIComponentPlugin` directly and override `getComponentManifests()`:

```java
public class NavbarComponentPlugin implements UIComponentPlugin {
    @Override
    public List<ComponentManifest> getComponentManifests() {
        return List.of(
            buildNavbarDefault(),
            buildNavbarCentered(),
            buildNavbarMinimal()
            // ... 8 variants total
        );
    }

    private ComponentManifest buildNavbarDefault() {
        return ComponentManifest.builder()
            .componentId("navbarDefault")
            .displayName("Navbar - Default")
            .category("ui")
            .capabilities(ComponentCapabilities.builder()
                .canHaveChildren(false)
                .isContainer(false)
                .isResizable(true)
                .supportsTemplateBindings(false)
                .build())
            .build();
    }
}
```

### ComponentCapabilities

Every component manifest should include `ComponentCapabilities` to control visual builder behavior:

| Capability | Default | Description |
|------------|---------|-------------|
| `canHaveChildren` | `false` | Can contain child components |
| `isContainer` | `false` | Acts as a layout container (accepts drops) |
| `hasDataSource` | `false` | Supports data source bindings |
| `autoHeight` | `false` | Auto-sizes height to fit content |
| `isResizable` | `true` | Can be resized by user in builder |
| `supportsIteration` | `false` | Supports iteration over data collections |
| `supportsTemplateBindings` | `true` | Supports `{{variable}}` template bindings |

### Context Provider Plugins

Plugins can provide shared context consumed by UI components:

```java
public interface ContextProviderPlugin extends Plugin {
    String getContextId();
    String getProviderComponentPath();
    List<ApiEndpoint> getApiEndpoints();
    default List<String> getRequiredContexts() { return List.of(); }
}
```

UI components declare context dependencies via `requiredContexts` in their `ComponentManifest`. The builder validates that all required contexts are available before allowing the component to be used.

### Data Source System

The SDK provides a data-fetching abstraction via `DataFetcher`, `DataSourceConfig`, and `DataSourceResult` for components with `hasDataSource = true`.

### Event System

Plugins can handle events via `EventHandler` and `EventContext` interfaces for inter-component communication.

### Build and Deploy

```bash
cd plugins/my-component-plugin
mvn clean package
cp target/*.jar ../../core/plugins/
```

---

## Docker Deployment

### Development with Docker Compose

```bash
docker-compose up -d
docker-compose logs -f vsd-app
```

### Production Deployment

```bash
# Build production image
docker build -f Dockerfile.prod -t vsd-cms:latest .

# Run with PostgreSQL
docker-compose -f docker-compose.prod.yml up -d
```

**Deployment Guide**: [Deployment View](docs/architecture/arc42/07-deployment-view.md)

---

## Authentication

VSD supports dual authentication - both local JWT tokens and external OAuth2/SSO:

### Local Authentication

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "admin", "password": "admin123"}'
```

### OAuth2 / SSO

Supported providers:

- Google OAuth2
- Okta OIDC
- Keycloak OIDC
- VSD Auth Server (custom)
- Generic OIDC provider

**Configuration**: See [System Scope and Context](docs/architecture/arc42/03-system-scope-and-context.md)

---

## Testing

```bash
# Backend tests (core module)
cd core && mvn test

# Frontend tests
cd frontend && npm test

# SDK + Core tests
mvn test -pl core,flashcard-cms-plugin-sdk
```

**Testing Guide**: [Cross-cutting Concepts - Testing](docs/architecture/arc42/08-crosscutting-concepts.md#testing-strategy)

---

## Technology Stack

### Backend

- **Java 21** - Modern JVM features
- **Spring Boot 4.0.0** - Enterprise application framework
- **Spring Security** - Authentication and authorization
- **JPA/Hibernate** - ORM and persistence
- **Flyway** - Database migrations
- **H2 / PostgreSQL** - Development / production databases

### Frontend

- **React 18.3.1** - UI framework
- **TypeScript 5.6.2** - Type safety
- **Vite 6.0.5** - Build tool and dev server
- **Zustand** - State management
- **@dnd-kit** - Drag and drop
- **Axios** - HTTP client

### Development Tools

- **Maven** - Build and dependency management
- **Docker** - Containerization

---

## Project Statistics

- **Languages**: Java, TypeScript, JavaScript
- **Lines of Code**: ~35,000+
- **Maven Modules**: 4 (core, SDK, site-runtime, BOM) + 12 plugin modules
- **Plugins**: 12 bundled plugins providing 21 component variants
- **Documentation**: 280KB+ arc42 documentation
- **Tests**: 63+ backend tests

---

## Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

**Development Conventions**: [Architecture Constraints](docs/architecture/arc42/02-architecture-constraints.md#23-conventions)

---

## Links

- **GitHub**: [https://github.com/Visual-Site-Designer-VSD/visual-site-designer](https://github.com/Visual-Site-Designer-VSD/visual-site-designer)
- **Architecture Documentation**: [docs/architecture/arc42/](docs/architecture/arc42/)
- **Issue Tracker**: [GitHub Issues](https://github.com/Visual-Site-Designer-VSD/visual-site-designer/issues)

---

## Support

- **Documentation**: Check the [arc42 architecture docs](docs/architecture/arc42/README.md)
- **Issues**: Report bugs via [GitHub Issues](https://github.com/Visual-Site-Designer-VSD/visual-site-designer/issues)
- **Discussions**: Join discussions on GitHub Discussions

---

## Roadmap

### Current Version: 1.0.0-SNAPSHOT

### Planned Features

- Plugin marketplace integration
- Advanced component library
- Template system
- Enhanced internationalization
- Improved E2E testing coverage

**See**: [Risks and Technical Debt](docs/architecture/arc42/11-risks-and-technical-debt.md#technical-debt-reduction-plan)
