# ADR-001: Project Domain and Core Design Decisions

**Date:** 2026-05-01  
**Status:** Accepted

## Context

I needed to select a project that demonstrates senior backend engineering skills, is universally understood by interviewers, and naturally justifies modern technology choices — async processing, multi-tenancy, observability, and AWS integration — within a two-week build window.

I evaluated three options:

- **Loan processing platform** — familiar domain, but skews toward batch processing and compliance logic rather than distributed systems patterns
- **Telecom self-service platform** — reflects recent experience, but the domain is niche and requires interviewer context to appreciate
- **Multi-tenant notification engine** — universally understood, technically rich, and every interviewer has either built one or depended on one

## Decision

I am building a multi-tenant notification engine supporting Email and SMS delivery channels.

### Multi-tenancy: shared database, separate PostgreSQL schemas per tenant

A shared database with schema-per-tenant gives each tenant genuine data isolation — no cross-tenant query risk — without the operational overhead of provisioning and migrating separate database instances. It is the right tradeoff at this scale and mirrors how services like Notion and early Stripe handled tenant separation.

### Auth: API keys per tenant

Machine-to-machine callers do not benefit from JWT. JWT adds token refresh logic, clock skew handling, and key rotation complexity that is only warranted when a human identity provider is in the loop. API keys scoped per tenant — hashed and stored, never logged — give equivalent security for server-to-server calls with a fraction of the implementation surface. This is the same pattern Stripe and Twilio use.

### Channels: Email via AWS SES, SMS via AWS SNS

Two channels demonstrate integration breadth without over-engineering. SES and SNS are production-grade, widely recognized in interviews, and share enough structural similarity (async dispatch, delivery receipts, failure handling) to justify shared abstractions in the delivery layer.

## Consequences

These decisions enable:
- Clean tenant isolation at the database layer
- Simple, auditable auth with no token lifecycle management
- Async notification delivery with per-tenant rate limiting
- A natural observability story: per-tenant metrics, delivery status tracking, audit logs

These decisions rule out:
- Per-tenant databases (too much operational complexity for the scope)
- JWT for M2M auth (adds complexity without security gain)
- Additional channels (Slack, push) within the initial build window
