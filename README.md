# NotifyEngine

A multi-tenant notification engine for delivering Email and SMS at scale.

**Status:** In Development

---

## What it does

NotifyEngine is a backend service that lets multiple tenants send Email
and SMS notifications through a single API. Each tenant is fully isolated
at the database level. The service is designed for machine-to-machine
use — a server calls the API with an API key and NotifyEngine handles
delivery via AWS SES and AWS SNS.

---

## Architecture

Each tenant gets a dedicated PostgreSQL schema. There are no shared
application tables between tenants — a query for Tenant A can never
touch Tenant B's data by design. Schema routing happens at the Hibernate
connection level via a custom MultiTenantConnectionProvider, so
controllers and services have no tenant-aware code.

Authentication uses API keys scoped per tenant. Keys are hashed with
SHA-256 before storage — the raw key is never persisted. This is the
same pattern used by Stripe and Twilio for machine-to-machine auth.

Key decisions are documented in the [ADR index](#adr-index).

---

## Tech Stack

- Java 21, Spring Boot 3.x
- PostgreSQL 15, schema-per-tenant multi-tenancy
- Flyway for schema migrations
- AWS SES for Email, AWS SNS for SMS
- Docker and docker-compose for local development
- Spring Actuator for health and metrics

---

## How to Run

**Prerequisites:** Java 21, Docker Desktop

```bash
# Start PostgreSQL
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

Health check:
```bash
curl http://localhost:8080/actuator/health
```

Send a notification (requires a valid API key):
```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{
    "type": "EMAIL",
    "recipientEmail": "user@example.com",
    "recipientName": "John Doe",
    "templateVariables": {
      "brandName": "Acme Corp"
    }
  }'
```

---

## ADR Index

| # | Title | Status |
|---|-------|--------|
| [ADR-001](docs/adr/ADR-001-project-domain.md) | Project Domain and Core Design Decisions | Accepted |
| [ADR-002](docs/adr/ADR-002-spring-boot-structure.md) | Spring Boot Project Structure Decisions | Accepted |
| [ADR-003](docs/adr/ADR-003-api-key-hashing.md) | API Key Hashing Strategy | Accepted |
| [ADR-004](docs/adr/ADR-004-jsonb-template-variables.md) | JSONB for Template Variables | Accepted |

---

*Developed with AI-assisted tooling.*