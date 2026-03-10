# Visual Site Designer (VSD)

A visual drag-and-drop website builder platform with a plugin-based architecture for creating and exporting sites.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6.2-blue.svg)](https://www.typescriptlang.org/)

---

## 🚀 Quick Start

### Prerequisites

- **Java**: JDK 21 or higher
- **Maven**: 3.6+
- **Node.js**: 20.10.0+ (for frontend)
- **npm**: 10.2.3+

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/mainul35/dynamic-site-builder.git
cd dynamic-site-builder

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

## 📚 Documentation

### Getting Started

- **[Quick Start Guide](#-quick-start)** - Get up and running in 5 minutes
- **[Running the Application](docs/guides/running-the-app.md)** - Development and production setup
- **[Builder Features](docs/guides/builder-features.md)** - How to use the visual builder

### For Developers

- **[Plugin Development Guide](docs/guides/plugin-development.md)** - Create custom components
- **[API Reference](docs/api/README.md)** - REST API documentation
- **[Testing Guide](docs/guides/testing.md)** - Testing strategies and examples

### Architecture Documentation

**Complete arc42 architecture documentation is available at:**

📖 **[Architecture Documentation (arc42)](docs/architecture/arc42/README.md)**

This comprehensive documentation follows the arc42 standard and includes:

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

## 🏗️ Architecture Overview

### System Components

```text
visual-site-designer/
├── core/                     # Main Spring Boot application
├── frontend/                 # React visual builder
├── vsd-cms-bom/             # Bill of Materials
├── flashcard-cms-plugin-sdk/ # Plugin development SDK
├── site-runtime/             # Runtime library for exported sites
└── plugins/                  # Bundled UI component plugins
```

### Plugin System

VSD's extensibility comes from its plugin architecture:

- **Isolated ClassLoaders** - Each plugin runs in isolation
- **Hot Reload** - Update plugins without restarting
- **Spring Integration** - Plugins can register controllers, services, entities
- **React Components** - UI components built with React
- **Type Safety** - Auto-generated TypeScript types

**Learn more**: [Building Block View](docs/architecture/arc42/05-building-block-view.md)

### Export Options

| Format | Description | Use Case |
|--------|-------------|----------|
| **Static HTML** | Self-contained HTML/CSS/JS files | Static hosting (Netlify, Vercel, S3) |
| **Spring Boot + Thymeleaf** | Server-side rendered application | Dynamic sites with database |

---

## 🔌 Plugin Development

### Quick Plugin Creation

```bash
# Use the plugin template
cp -r plugins/label-component-plugin plugins/my-component-plugin

# Implement the plugin interface
public class MyComponentPlugin implements UIComponentPlugin {
    @Override
    public ComponentManifest getComponentManifest() {
        return ComponentManifest.builder()
            .componentId("myComponent")
            .displayName("My Component")
            .category("ui")
            .build();
    }
}

# Build and deploy
cd plugins/my-component-plugin
mvn clean package
cp target/*.jar ../../core/plugins/
```

**Full Guide**: [Plugin Development](docs/guides/plugin-development.md)

---

## 🐳 Docker Deployment

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

## 🔐 Authentication

VSD supports multiple authentication methods:

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

**Configuration**: See [System Scope and Context](docs/architecture/arc42/03-system-scope-and-context.md#ti-01-oauth2-authentication)

---

## 🧪 Testing

```bash
# Backend tests
cd core && mvn test

# Frontend tests
cd frontend && npm test

# All tests
mvn test -pl core,flashcard-cms-plugin-sdk
```

**Testing Guide**: [Cross-cutting Concepts - Testing](docs/architecture/arc42/08-crosscutting-concepts.md#testing-strategy)

---

## 🛠️ Technology Stack

### Backend

- **Java 21** - Modern JVM features
- **Spring Boot 4.0.0** - Enterprise application framework
- **Spring Security** - Authentication & authorization
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
- **IntelliJ IDEA** - IDE with VSD plugin
- **Docker** - Containerization

---

## 📊 Project Statistics

- **Languages**: Java, TypeScript, JavaScript
- **Lines of Code**: ~50,000+
- **Modules**: 5 (core, frontend, SDK, site-runtime, BOM)
- **Plugins**: 11 bundled components
- **Documentation**: 280KB+ arc42 documentation

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

**Development Conventions**: [Architecture Constraints](docs/architecture/arc42/02-architecture-constraints.md#23-conventions)

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🔗 Links

- **GitHub**: [https://github.com/mainul35/dynamic-site-builder](https://github.com/mainul35/dynamic-site-builder)
- **Architecture Documentation**: [docs/architecture/arc42/](docs/architecture/arc42/)
- **Issue Tracker**: [GitHub Issues](https://github.com/mainul35/dynamic-site-builder/issues)

---

## 📞 Support

- **Documentation**: Check the [arc42 architecture docs](docs/architecture/arc42/README.md)
- **Issues**: Report bugs via [GitHub Issues](https://github.com/mainul35/dynamic-site-builder/issues)
- **Discussions**: Join discussions on GitHub Discussions

---

## 🎯 Roadmap

### Current Version: 1.0.0-SNAPSHOT

### Planned Features

- Plugin marketplace integration
- Advanced component library
- Template system
- Enhanced internationalization
- Improved E2E testing coverage

**See**: [Risks and Technical Debt](docs/architecture/arc42/11-risks-and-technical-debt.md#technical-debt-reduction-plan)

---

Built with ❤️ by the VSD Team
