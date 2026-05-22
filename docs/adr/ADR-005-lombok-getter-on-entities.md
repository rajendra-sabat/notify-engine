# ADR-005: Lombok @Getter on JPA Entities

**Date:** 2026-05-20  
**Status:** Accepted

## Context

JPA entities require getters for Hibernate to read field values during
serialization, lazy-loading proxies, and JSON serialization. Writing
these by hand produces significant boilerplate that obscures the actual
domain model and must be updated whenever a field is added or renamed.

Lombok's `@Getter` generates all getters at compile time with no runtime
overhead. The alternative — a full `@Data` or `@lombok.Value` — is
unsafe on JPA entities because it generates `equals`/`hashCode` based
on all fields, which breaks identity semantics and causes issues with
Hibernate proxy objects.

Explicit setters and the all-args constructor are kept hand-written
because Hibernate requires a no-args constructor, and controlled
mutation (explicit setters) keeps write paths intentional and visible.
`@Setter` is intentionally omitted for the same reason: blanket setters
on all fields obscure which fields are mutable vs. effectively final
after construction.

## Decision

Apply `@Getter` at the class level on JPA entities. Do not use `@Setter`,
`@Data`, `@EqualsAndHashCode`, or `@ToString` from Lombok on entities.
Keep `equals`/`hashCode` hand-written and identity-based (by `id`).

## Consequences

These decisions enable:
- Eliminating getter boilerplate without compromising equals/hashCode safety
- Explicit, readable write paths via hand-written setters
- Safe use with Hibernate lazy-loading proxies (no all-fields hashCode)

These decisions rule out:
- Using `@Data` or `@AllArgsConstructor` on entities
- Lombok-generated `equals`/`hashCode` on any entity class
