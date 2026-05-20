You are a senior Java engineer enforcing guardrails on this Spring Boot 3.5 / Java 21 codebase. Run each check in order, report every violation, and give an actionable fix for each one. Do not soften findings. This is for an expert audience — skip obvious basics and focus on the non-obvious traps.

---

## 1. Dependency health

Run:
```bash
./mvnw dependency:tree -q 2>/dev/null | head -80
./mvnw versions:display-dependency-updates -q 2>/dev/null | grep -v "^\[INFO\] $" | head -40
```

Check:
- **hypersistence-utils** version must match the Hibernate version bundled in `spring-boot-starter-parent:3.5.0`. Spring Boot 3.5 ships Hibernate 6.6.x — the correct artifact is `hypersistence-utils-hibernate-63`. Flag if mismatched.
- **springdoc-openapi** `2.8.x` is the correct line for Spring Boot 3.x. Flag `1.x` (Spring Boot 2 only).
- **Cucumber 7.20.x** — check there is no version skew between `cucumber-java`, `cucumber-spring`, and `cucumber-junit-platform-engine`. All three must be the same version.
- Flag any dependency that overrides a version already managed by `spring-boot-starter-parent` unless there is a documented reason.
- Flag any `<scope>compile</scope>` that should be `<scope>runtime</scope>` (e.g. the PostgreSQL JDBC driver).

---

## 2. Java 21 usage

Grep the source:
```bash
grep -rn "new Thread\|Executors\." src/main/java --include="*.java"
grep -rn "instanceof.*&&\|instanceof.*cast" src/main/java --include="*.java"
grep -rn "switch.*{" src/main/java --include="*.java" | grep -v "switch (.*) {"
```

Check:
- **Virtual threads** — if `Executors.newFixedThreadPool` or `new Thread` is used for I/O work, flag it. Spring Boot 3.2+ supports `spring.threads.virtual.enabled=true`; Tomcat and `@Async` both benefit.
- **Pattern matching instanceof** — any `if (x instanceof Foo) { Foo f = (Foo) x; }` should use `instanceof Foo f` instead. Flag old-style casts after instanceof.
- **Switch expressions** — prefer `switch` expressions with arrow labels over statement switches for exhaustive cases. Flag statement switches on enums or sealed types.
- **Records** — DTOs and command objects that are pure data carriers with no behaviour should be `record` types. Flag any `class` with only final fields + a canonical constructor + no mutation.
- **Text blocks** — multi-line SQL or JSON strings in tests should use `"""` text blocks. Flag string concatenation for multi-line literals.

---

## 3. Spring Boot 3.5 feature checks

```bash
grep -rn "@Autowired" src/main/java --include="*.java"
grep -rn "SpringApplication.run" src/main/java --include="*.java"
grep -rn "new RestTemplate\b" src/main/java --include="*.java"
grep -rn "@Value\b" src/main/java --include="*.java"
grep -rn "HttpSecurity.*and()" src/main/java --include="*.java"
grep -rn "WebSecurityConfigurerAdapter" src/main/java --include="*.java"
```

Check:
- **`@Autowired` field injection** — constructor injection is mandatory. Flag every `@Autowired` field; the fix is a constructor parameter.
- **`WebSecurityConfigurerAdapter`** — removed in Spring Security 6. Flag if found; use `SecurityFilterChain` bean (already correct in this project).
- **`.and()` chaining in `HttpSecurity`** — deprecated in Spring Security 6. Use lambda DSL (`http.csrf(AbstractHttpConfigurer::disable)`) which is already the pattern here. Flag any `.and()` usage.
- **`@Value` on non-`@Configuration` beans** — `@Value` for complex config should be replaced with `@ConfigurationProperties`. Flag `@Value` usage on `@Service` or `@Component` classes for anything more than a single simple property.
- **`new RestTemplate()`** — if HTTP client calls are added, flag direct instantiation. Use `RestClient` (Spring Boot 3.2+) or inject a `RestTemplate` bean built via `RestTemplateBuilder`.
- **`@SpringBootApplication` placement** — must be in `com.notifyengine` (the root package) so component scanning covers all sub-packages. Flag if moved.
- **Actuator security** — `/actuator/**` is excluded from the API key filter. Confirm `management.endpoint.health.show-details` is not set to `always` in a production profile — this leaks internal state. Flag if only `application.properties` (no profile separation) has `show-details=always`.

---

## 4. JPA and Hibernate guardrails

```bash
grep -rn "FetchType.EAGER" src/main/java --include="*.java"
grep -rn "@Transactional" src/main/java --include="*.java"
grep -rn "findAll()" src/main/java --include="*.java"
grep -rn "entityManager\|EntityManager" src/main/java --include="*.java"
```

Check:
- **`FetchType.EAGER`** — flag every occurrence. EAGER fetching on collections causes N+1 queries and is almost never correct. Use `JOIN FETCH` in JPQL queries where eager loading is intentionally needed.
- **`@Transactional` on `@Controller` or `@Component` filter classes** — flag it. Transactions belong on `@Service` methods.
- **`@Transactional` on `public` methods of non-Spring-proxied classes** — self-invocation bypasses the proxy. Flag any `@Transactional` method called from within the same class.
- **`findAll()` without pagination** — flag unbounded `findAll()` calls in service or controller code. Any list endpoint must use `Pageable`.
- **Multi-tenancy entity mapping**:
  - `Notification` — `@Table(name = "notifications")` with no `schema` attribute. ✓ Correct. Flag if `schema` is added.
  - `Tenant`, `ApiKey` — must have `schema = "system"`. Flag if missing.
- **`@GeneratedValue(strategy = GenerationType.UUID)`** — correct for PostgreSQL UUID PKs with Hibernate 6. Flag `AUTO` or `IDENTITY` on UUID columns.
- **Lombok on entities** — `@Getter` is safe. `@Data`, `@EqualsAndHashCode`, `@ToString(callSuper=true)` on entities with lazy associations will trigger `LazyInitializationException` outside a session. Flag any of these.

---

## 5. Multi-tenancy correctness

```bash
grep -rn "TenantContext" src/main/java --include="*.java"
grep -rn "search_path" src/main/java --include="*.java"
grep -rn "setTenant\|getTenant\|clear()" src/main/java --include="*.java"
```

Check:
- `TenantContext.setTenant` must only be called from `ApiKeyAuthFilter`. Flag any other call site.
- `TenantContext.clear()` must be in a `finally` block. A missing `finally` leaks the tenant into the next request on the same thread (thread-pool reuse).
- `SchemaMultiTenantConnectionProvider.getConnection` must validate the schema name with `^[a-zA-Z0-9_]+$` before interpolating into `SET search_path TO`. Flag if the regex check is removed or weakened.
- `releaseConnection` must reset `search_path` to `public`. Flag if it sets it to anything else or omits the reset.
- `TenantIdentifierResolver.resolveCurrentTenantIdentifier` must fall back to `"public"` (not `null`, not `""`) — Hibernate will throw on a null tenant identifier.

---

## 6. Security filter chain

```bash
grep -rn "permitAll\|authenticated\|denyAll" src/main/java --include="*.java"
grep -rn "shouldNotFilter" src/main/java --include="*.java"
```

Check:
- `.anyRequest().permitAll()` in `SecurityConfig` is intentional — the API key filter enforces auth, not Spring Security's authorization rules. Flag if this is changed to `.authenticated()` without also removing the API key filter, as it would create a double-auth path.
- `shouldNotFilter` in `ApiKeyAuthFilter` must exclude `/actuator`, `/swagger-ui`, and `/v3/api-docs`. Flag if any of these paths are removed from the exclusion list.
- CSRF is disabled (`AbstractHttpConfigurer::disable`). This is correct for a stateless API — flag if re-enabled.
- `httpBasic` and `formLogin` are disabled. Flag if re-enabled.

---

## 7. Test quality

```bash
grep -rn "Thread.sleep\|@Ignore\|@Disabled" src/test --include="*.java"
grep -rn "hardcoded\|\"notify-engine-local" src/test --include="*.java"
grep -rn "static.*API_KEY\|VALID_API_KEY" src/test --include="*.java"
```

Check:
- **`Thread.sleep` in tests** — flag every occurrence. Use RestAssured's `await()` or `Awaitility` for async assertions.
- **`@Disabled` / `@Ignore`** — flag any disabled test without a TODO comment referencing a ticket or date.
- **Hardcoded API key strings** — the test key `notify-engine-local-test-key` must only appear via `VALID_API_KEY` constant in `NotificationSteps`. Flag any literal use elsewhere.
- **Cucumber `@ActiveProfiles("test")`** — both `CucumberSpringConfiguration` and `NotifyEngineApplicationTests` must carry this annotation. Without it, tests connect to the wrong database.
- **`ScenarioContext` scope** — must be `@Scope("cucumber-glue")`. If changed to singleton, response state leaks between scenarios.

---

## Report format

For each violation:
```
[SEVERITY] File:line — finding
Fix: one-sentence actionable correction
```

Severity levels:
- `[CRITICAL]` — correctness or security issue that will cause a bug or data leak in production
- `[WARNING]` — convention violation or pattern that will cause problems at scale
- `[INFO]` — improvement opportunity; does not affect correctness

End with a one-line score: `X critical, Y warnings, Z info items found.`
If the codebase is clean on a category, print `✓ <Category>` and move on.
