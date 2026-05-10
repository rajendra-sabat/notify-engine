# ADR-002: Spring Boot Project Structure Decisions

**Date:** 2026-05-02  
**Status:** Accepted

## Context

I am scaffolding the Spring Boot project for NotifyEngine. Before generating the project I need to settle five structural decisions that are hard to change later: build tool, module layout, package structure, starter dependencies, and how to wire schema-per-tenant into the Spring Data JPA stack.

## Decision

### Build tool: Maven

I chose Maven over Gradle. Spring Initializr defaults to Maven. Enterprise Java teams use it almost universally, so any reviewer can read the POM without context-switching. Gradle is more flexible but adds tool familiarity surface area that does not pay off at this project size.

### Single module

NotifyEngine is a single deployable. A multi-module Maven project would require a parent POM and inter-module dependency declarations with no architectural benefit at this stage. I start with one module and split only if the delivery layer needs to be independently deployable.

### Package structure: layered by technical concern

I will organize packages by layer, not by feature.

```
com.notifyengine
  config/        Spring beans, security, datasource wiring
  controller/    REST endpoints
  service/       Business logic
  repository/    Spring Data JPA repositories
  domain/        JPA entities and value objects
  filter/        Servlet filters (tenant resolution, API key auth)
```

Feature packaging (e.g. `notifications/`, `tenants/`) makes sense when a team needs to delineate ownership boundaries. Layered packaging is immediately readable to any Spring developer reviewing the code and keeps the initial structure simple.

### Starter dependencies

| Dependency | Reason |
|---|---|
| Spring Web | REST API layer |
| Spring Data JPA | ORM and repository abstraction |
| PostgreSQL Driver | Database connectivity |
| Flyway | Schema migrations. Required for schema-per-tenant provisioning. |
| Spring Boot Actuator | `/health` and `/metrics` endpoints out of the box |
| Spring Security | API key filter chain |
| Lombok | Reduce entity and DTO boilerplate |
| Validation (jakarta.validation) | Request body validation |

### Multi-tenancy wiring: Hibernate SPI with schema switching per connection

Spring Data JPA sits on top of Hibernate. Hibernate exposes two interfaces for multi-tenancy:

- `CurrentTenantIdentifierResolver` — returns the tenant identifier for the current thread
- `MultiTenantConnectionProvider` — acquires a connection and switches the schema before handing it to Hibernate

The request flow is:

1. `TenantFilter` extracts the `X-Tenant-ID` header and stores it in a `ThreadLocal`
2. `CurrentTenantIdentifierResolver` reads the `ThreadLocal`
3. `MultiTenantConnectionProvider` executes `SET search_path TO <tenant_schema>` on each acquired connection

This is the canonical Hibernate multi-tenancy pattern. It integrates with Spring Data JPA without custom query logic and is recognizable to any reviewer familiar with the ecosystem.

I considered Spring's `AbstractRoutingDataSource` as an alternative. It routes at the DataSource level, which is cleaner for switching between separate databases but adds complexity when the goal is schema switching within one database. The Hibernate SPI is the better fit here.

## Consequences

These decisions enable:

- A Maven POM that any Java interviewer can read without explanation
- Flyway migrations that provision per-tenant schemas on first tenant registration
- A filter-based tenant resolution that keeps tenant identity out of service and repository code
- Standard Actuator endpoints that serve as the observability baseline

These decisions rule out:

- Gradle (can be adopted later without changing project structure)
- Multi-module layout (can be introduced if delivery layer needs independent deployment)
- Feature-based packaging (can be refactored if team ownership boundaries emerge)
