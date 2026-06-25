# NotifyEngine

A multi-tenant notification engine for delivering Email notifications at scale.

**Status:** In Development

---

## What it does

NotifyEngine is a backend service that lets multiple tenants send Email notifications through a single API. Each tenant is isolated at the database level. The service is designed for machine-to-machine use — a server calls the API with an API key and NotifyEngine handles delivery via AWS SES.

---

## How to Run

**Prerequisites:** Java 21, Docker Desktop

```bash
docker-compose up -d
./mvnw spring-boot:run
```

Health check:
```bash
curl http://localhost:8080/actuator/health
```

Send a notification:
```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your_api_key" \
  -d '{
    "type": "EMAIL",
    "recipientEmail": "user@example.com",
    "recipientName": "John Doe",
    "subject": "Hello from NotifyEngine",
    "body": "This is a test notification.",
    "templateVariables": { "brandName": "Acme Corp" }
  }'
```

---

## Docs

Architecture, ADRs, and design decisions are in the [`docs/`](docs/) folder.

---

*Developed with AI-assisted tooling.*
