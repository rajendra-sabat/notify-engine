# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the app locally (requires postgres from docker-compose)
./mvnw spring-boot:run

# Build JAR (skip tests)
./mvnw package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=NotifyEngineApplicationTests

# Start postgres only
docker compose up postgres -d

# Build and start everything
docker compose up --build -d

# Tail app logs
docker compose logs app -f
```

## Architecture

### Multi-Tenancy (the core design)

Tenant isolation is implemented at the **PostgreSQL schema level**. Each tenant owns a separate schema (e.g., `tenant_acme`) with its own `notifications` table. The `public` schema holds shared system tables (`tenants`, `api_keys`).

The Hibernate SPI wires this together:
- `TenantContext` — ThreadLocal holding the current tenant's `schemaName`
- `TenantIdentifierResolver` — feeds `TenantContext` into Hibernate; defaults to `"public"`
- `SchemaMultiTenantConnectionProvider` — issues `SET search_path TO <schema>` on every connection acquire, resets to `public` on release; validates the identifier is alphanumeric + underscores only
- `HibernateMultiTenancyConfig` — wires the above as Spring beans into Hibernate properties

### Request lifecycle

1. **`ApiKeyAuthFilter`** (`OncePerRequestFilter`) extracts the `X-API-Key` header, SHA-256 hashes it, and looks it up in `api_keys` (system schema). If valid and not expired, it sets `TenantContext` with the tenant's `schemaName`. The finally block always clears the context.
2. All JPA queries for `Notification` automatically target the resolved tenant schema because Hibernate uses the `search_path` set by the connection provider.
3. `TenantContext` is cleared after the response — never leaks between requests.

Actuator endpoints (`/actuator/**`) bypass the filter entirely.

### Package layout

```
config/     — Hibernate multi-tenancy SPI, Security, OpenAPI bean definitions
controller/ — REST layer (thin; delegates to service)
domain/     — JPA entities: Notification (tenant schema), Tenant + ApiKey (system schema)
dto/        — Request/response records
filter/     — ApiKeyAuthFilter
repository/ — Spring Data JPA repositories
service/    — Business logic (NotificationService)
```

### Database schema

Two layers of SQL schema:

| Postgres schema | Tables | Purpose |
|---|---|---|
| `system` | `tenants`, `api_keys` | Shared metadata, resolved by `ApiKeyAuthFilter` |
| `tenant_template` (and per-tenant copies) | `notifications` | Per-tenant data, isolated by `search_path` |

Flyway manages migrations (`V1` = system schema, `V2` = tenant schema template). New tenant schemas must be provisioned manually or via a future onboarding flow — there is no admin API yet.

### API key hashing

SHA-256 (not BCrypt) is used deliberately — see `ADR-003`. API keys are long random strings, not passwords; SHA-256 is fast and safe here. The raw key is never stored; only `keyHash` (hex) and a `keyPrefix` (display hint) are persisted.

### JSONB for template variables

`templateVariables` on `Notification` is stored as PostgreSQL `jsonb` via `hypersistence-utils` (`@Type(JsonType.class)`). This avoids a separate key-value table for ad-hoc template substitutions — see `ADR-004`.

### OpenAPI / Swagger

Available at `http://localhost:8080/swagger-ui.html`. Configured in `OpenApiConfig` with a global `X-Api-Key` header security scheme. The `/v3/api-docs/**` and `/swagger-ui/**` paths are explicitly whitelisted in `SecurityConfig`.
