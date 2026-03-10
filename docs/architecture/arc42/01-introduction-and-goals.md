# 1. Introduction and Goals

## 1.1 Requirements Overview

### What is Visual Site Designer (VSD)?

Visual Site Designer is a **visual drag-and-drop website builder platform** that enables users to create websites visually without writing code, while providing developers with a powerful plugin system to extend functionality.

### Core Features

| Feature Category | Capabilities |
|-----------------|--------------|
| **Visual Building** | Drag-and-drop interface, WYSIWYG editor, real-time preview |
| **Component Library** | Extensible component system via plugins (UI, Layout, Form, Navigation, Auth) |
| **Multi-page Sites** | Multiple pages per site, page versioning, rollback capability |
| **Export Options** | Static HTML/CSS/JS, Spring Boot + Thymeleaf applications |
| **Authentication** | Local JWT authentication, OAuth2/SSO (Google, Okta, Keycloak, VSD Auth Server) |
| **Plugin System** | Hot-reloadable plugins, isolated classloaders, Spring component registration |
| **Content Management** | Image/file repository, content versioning, media management |
| **Developer Tools** | IntelliJ plugin for development, TypeScript types generation, SDK |

### Key Requirements

#### Functional Requirements

1. **FR-01**: Users shall create websites visually using drag-and-drop
2. **FR-02**: System shall support multi-page sites with navigation
3. **FR-03**: Users shall configure component properties through a visual editor
4. **FR-04**: System shall export sites as static HTML or Spring Boot applications
5. **FR-05**: Developers shall extend functionality through plugins
6. **FR-06**: System shall support multiple authentication methods (local, OAuth2, SSO)
7. **FR-07**: System shall provide real-time preview of changes
8. **FR-08**: Users shall manage page versions with rollback capability
9. **FR-09**: System shall support responsive design configuration
10. **FR-10**: Plugins shall be hot-reloadable without system restart

#### Non-Functional Requirements

1. **NFR-01**: System shall load plugin bundles dynamically at runtime
2. **NFR-02**: Frontend shall provide sub-second response to user interactions
3. **NFR-03**: System shall isolate plugin code to prevent conflicts
4. **NFR-04**: System shall support concurrent users building sites
5. **NFR-05**: Exported sites shall be production-ready and deployable
6. **NFR-06**: System shall validate plugin integrity before loading
7. **NFR-07**: Documentation shall enable plugin development within 1 hour
8. **NFR-08**: System shall support Docker-based deployment

---

## 1.2 Quality Goals

The top quality goals for VSD architecture, ordered by priority:

### Quality Goal 1: Extensibility (Priority: Highest)

**Definition**: The system shall allow new components to be added without modifying core code.

**Rationale**:
- Primary value proposition is extensibility
- Plugin ecosystem enables community contributions
- Business model may depend on marketplace

**Measures**:
- New component plugins can be developed independently
- Plugin installation requires no core code changes
- Plugins load via hot-reload without system restart
- Plugin conflicts are prevented through isolated classloaders

**Related Requirements**: FR-05, FR-10, NFR-01, NFR-03

---

### Quality Goal 2: Usability (Priority: High)

**Definition**: Non-technical users shall create professional websites without coding knowledge.

**Rationale**:
- Target audience includes designers, marketers, small business owners
- Visual editing reduces time-to-market
- Lowering technical barriers increases adoption

**Measures**:
- Visual drag-and-drop interface
- WYSIWYG preview
- Property editor with validation
- Clear error messages
- Onboarding documentation

**Related Requirements**: FR-01, FR-03, FR-07

---

### Quality Goal 3: Maintainability (Priority: High)

**Definition**: The system shall be easy to understand, modify, and extend.

**Rationale**:
- Long-term product evolution
- Multiple developers will contribute
- Plugin SDK must be stable and well-documented

**Measures**:
- Clear module separation (core, frontend, SDK, runtime)
- Comprehensive documentation (arc42, README, API docs)
- Consistent coding conventions
- Automated testing (unit, integration)
- Bill of Materials for dependency consistency

**Related Requirements**: FR-05, NFR-07

---

### Quality Goal 4: Performance (Priority: Medium)

**Definition**: The system shall respond quickly to user interactions and handle reasonable load.

**Rationale**:
- Visual builders require responsive UIs
- Multi-user collaboration support
- Large sites with many components

**Measures**:
- Frontend interactions respond in <500ms
- Plugin loading completes in <2 seconds
- Page rendering handles 100+ components
- Export process completes within reasonable time
- Database queries optimized with indexes

**Related Requirements**: NFR-02, NFR-04

---

### Quality Goal 5: Security (Priority: Medium)

**Definition**: The system shall protect user data and prevent unauthorized access.

**Rationale**:
- Multi-tenant system with user authentication
- Plugin code execution requires sandboxing
- OAuth2 integration for enterprise SSO

**Measures**:
- JWT authentication with token expiration
- Role-based access control (ADMIN, DESIGNER, EDITOR, VIEWER, USER)
- OAuth2/SSO integration
- Plugin validation before loading
- CORS configuration for API security
- SQL injection prevention through JPA

**Related Requirements**: FR-06, NFR-06

---

### Quality Goal 6: Portability (Priority: Medium)

**Definition**: Exported sites shall run in various environments without VSD dependency.

**Rationale**:
- Exported sites should be standalone
- Support multiple deployment targets (static hosting, Spring Boot)
- Site-runtime library provides portability

**Measures**:
- Static HTML export works on any web server
- Spring Boot export uses standard Java runtime
- Site-runtime supports multiple databases (H2, PostgreSQL, MongoDB)
- Docker deployment for standardized environments

**Related Requirements**: FR-04, NFR-05, NFR-08

---

## 1.3 Stakeholders

### Primary Stakeholders

| Role | Goals | Concerns |
|------|-------|----------|
| **End Users (Site Builders)** | Create professional websites visually without coding | Ease of use, component availability, export quality, learning curve |
| **Plugin Developers** | Extend VSD with custom components, monetize plugins | Clear SDK, documentation, hot-reload, debugging tools, marketplace |
| **VSD Core Developers** | Maintain platform, ensure stability, add features | Code quality, maintainability, backward compatibility, performance |
| **Enterprise Administrators** | Deploy VSD for organization, integrate with SSO | Security, scalability, authentication integration, support |
| **DevOps Engineers** | Deploy and maintain VSD infrastructure | Deployment simplicity, monitoring, backup/recovery, containerization |

### Secondary Stakeholders

| Role | Goals | Concerns |
|------|-------|----------|
| **Designers** | Create visually appealing components | Component styling flexibility, responsive design, preview accuracy |
| **Content Editors** | Manage site content | Content repository, versioning, media management |
| **System Architects** | Evaluate VSD for enterprise use | Architecture quality, integration capabilities, licensing |
| **Open Source Contributors** | Contribute to VSD project | Contribution guidelines, code review process, documentation |

---

## 1.4 Target Audience for This Documentation

This arc42 documentation is intended for:

1. **Software Architects** - Understanding system design and making architectural decisions
2. **Senior Developers** - Working on core platform or complex plugins
3. **Plugin Developers** - Building advanced plugins requiring deep system knowledge
4. **DevOps Engineers** - Deploying and operating VSD in production
5. **Technical Decision Makers** - Evaluating VSD for adoption
6. **Quality Assurance** - Understanding system for comprehensive testing
7. **New Team Members** - Onboarding to VSD architecture

For quick start and basic usage, see the [main README](../../../README.md).

---

## 1.5 Business Context

### Market Position

VSD positions itself as:
- **Open-source alternative** to proprietary website builders (Wix, Squarespace, Webflow)
- **Developer-friendly platform** with extensibility through plugins
- **Self-hosted solution** for organizations requiring data sovereignty
- **Export-oriented builder** - sites can be exported and hosted anywhere

### Use Cases

#### UC-01: Small Business Website Creation
**Actor**: Small business owner
**Goal**: Create a professional website without hiring developers
**Steps**: Select template → Add pages → Configure components → Publish

#### UC-02: Enterprise Marketing Site
**Actor**: Marketing team with SSO
**Goal**: Build landing pages quickly with brand components
**Steps**: SSO login → Use custom brand components → Preview → Export to Spring Boot → Deploy

#### UC-03: Plugin Marketplace Developer
**Actor**: Plugin developer
**Goal**: Create and distribute custom components
**Steps**: Develop plugin using SDK → Test with hot-reload → Package JAR → Distribute

#### UC-04: Web Design Agency
**Actor**: Web designers
**Goal**: Rapidly prototype client sites
**Steps**: Create multi-page site → Real-time preview → Export HTML → Handoff to client

---

## 1.6 Technical Context (Overview)

VSD integrates with:
- **Authentication Providers**: OAuth2 (Google, Okta, Keycloak), VSD Auth Server
- **Databases**: H2 (development), PostgreSQL (production), MongoDB (via plugins)
- **Container Platforms**: Docker, Docker Compose
- **Reverse Proxies**: Cloudflare Tunnel
- **Build Tools**: Maven (backend), Vite (frontend)
- **IDEs**: IntelliJ IDEA (with VSD plugin)

Detailed technical context in [Section 3](03-system-scope-and-context.md).

---

[← Back to Index](README.md) | [Next: Architecture Constraints →](02-architecture-constraints.md)
