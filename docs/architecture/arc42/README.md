# Visual Site Designer (VSD) - Architecture Documentation

**Architecture documentation based on the arc42 template**

Version: 1.1.0
Date: 2026-03-12
Status: Living Document

---

## About arc42

arc42 is a template for software and system architecture documentation and communication. It provides a clear structure covering all relevant aspects of software architecture.

## Navigation

### Core Architecture Sections

1. [Introduction and Goals](01-introduction-and-goals.md)
   - Requirements Overview
   - Quality Goals
   - Stakeholders

2. [Architecture Constraints](02-architecture-constraints.md)
   - Technical Constraints
   - Organizational Constraints
   - Conventions

3. [System Scope and Context](03-system-scope-and-context.md)
   - Business Context
   - Technical Context
   - External Interfaces

4. [Solution Strategy](04-solution-strategy.md)
   - Technology Decisions
   - Top-level Decomposition
   - Quality Goals Achievement

5. [Building Block View](05-building-block-view.md)
   - Level 1: System Overview
   - Level 2: Modules (Core, Frontend, Plugins, SDK, Context Providers)
   - Level 3: Component Details

6. [Runtime View](06-runtime-view.md)
   - Plugin Lifecycle (UI + Context Provider)
   - Authentication Flow
   - Page Rendering
   - Export Process
   - Real-time Preview

7. [Deployment View](07-deployment-view.md)
   - Infrastructure Overview
   - Docker Deployment
   - Production Setup
   - Cloudflare Tunnel Integration

8. [Cross-cutting Concepts](08-crosscutting-concepts.md)
   - Domain Model
   - Security
   - Plugin Architecture (UI Components + Context Providers)
   - Cross-Plugin Shared Context Pattern
   - Error Handling
   - Logging & Monitoring
   - Testing Strategy

9. [Architecture Decisions](09-architecture-decisions.md)
   - ADR-001: Plugin-based Architecture
   - ADR-002: React + Spring Boot Stack
   - ADR-003: Dual Authentication Mode
   - ADR-004: H2 for Development, PostgreSQL for Production
   - ADR-005: Zustand for State Management
   - ADR-008: Context Provider Plugins for Cross-Plugin Shared State

10. [Quality Requirements](10-quality-requirements.md)
    - Quality Tree
    - Quality Scenarios
    - Performance Requirements
    - Security Requirements

11. [Risks and Technical Debt](11-risks-and-technical-debt.md)
    - Known Risks
    - Technical Debt
    - Mitigation Strategies

12. [Glossary](12-glossary.md)
    - Technical Terms
    - Domain Terminology
    - Abbreviations

---

## Quick Links

- [Quick Start Guide](../../../README.md) - For developers getting started
- [Plugin Development Guide](01-introduction-and-goals.md#plugin-developers) - For plugin developers
- [API Reference](08-crosscutting-concepts.md#api-design) - REST API documentation
- [Deployment Guide](07-deployment-view.md) - Production deployment

---

## Document Conventions

- **Diagrams**: All diagrams use Mermaid syntax for version control
- **Code Examples**: Include language identifier for syntax highlighting
- **Links**: Use relative links within documentation
- **Status**: Each ADR includes status (Proposed, Accepted, Deprecated, Superseded)

## Contributing to Documentation

When updating architecture documentation:

1. Update the relevant section based on architectural changes
2. Add new ADRs for significant decisions
3. Update diagrams to reflect current state
4. Keep the glossary current
5. Review cross-references between sections

---

**Template**: [arc42](https://arc42.org/)
**License**: MIT
