# ADR-006: Cucumber for API Integration Tests

**Date:** 2026-05-20  
**Status:** Accepted

## Context

The notify-engine exposes a single REST endpoint whose correctness
depends on three cross-cutting concerns working together: API key
authentication, tenant schema resolution, and notification persistence.
Unit tests cannot verify this chain end-to-end. An integration test
layer is needed that exercises the full HTTP request lifecycle against
a real database.

Two options were considered: plain Spring `@SpringBootTest` with
`MockMvc`/`RestAssured` test methods, and Cucumber BDD scenarios backed
by the same stack. Plain test methods are simpler to write but scatter
the behaviour specification across Java code. Cucumber externalises
scenarios into `.feature` files written in Gherkin, making the
acceptance criteria readable without opening source files, and allowing
scenarios to be reviewed or authored alongside the API contract.

Testcontainers was the first choice for the database but was ruled out
because the Docker socket path on macOS Docker Desktop
(`~/.docker/run/docker.sock`) differs from the default
`/var/run/docker.sock` that Testcontainers expects, causing failures in
the local development environment. The docker-compose postgres instance
(port `5431`) is already running for local development, so tests are
pointed at it via a Spring `test` profile instead.

## Decision

Use Cucumber 7 (`cucumber-java`, `cucumber-spring`,
`cucumber-junit-platform-engine`) with RestAssured for HTTP assertions.
Tests run against the docker-compose postgres on `localhost:5431` via
`@ActiveProfiles("test")`. Scenario state shared across step definitions
is held in a `ScenarioContext` component scoped to `cucumber-glue`.

## Consequences

These decisions enable:
- Human-readable acceptance scenarios in `src/test/resources/features/`
- Full HTTP + auth + multi-tenancy + persistence coverage in each scenario
- No Testcontainers Docker socket dependency

These decisions rule out:
- Running integration tests without the docker-compose postgres running
- Testcontainers-based isolated database spin-up per test run
- MockMvc (tests go through the real servlet container on a random port)
