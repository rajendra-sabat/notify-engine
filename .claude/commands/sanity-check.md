Run a full sanity check on the current state of the codebase. Perform each step in order and report results:

1. **Compile** — run `./mvnw compile -q` and report any errors.

2. **Package structure** — verify all Java source files under `src/main/java/com/notifyengine/` belong to one of the seven canonical packages: `config`, `controller`, `domain`, `dto`, `filter`, `repository`, `service`. Flag any file that does not.

3. **Imports** — grep for any `import com.notifyengine.notification`, `import com.notifyengine.tenancy`, or `import com.notifyengine.security` references left over from the DDD refactor. There should be none.

4. **TenantContext safety** — grep for `TenantContext.setTenant` calls outside of `filter/ApiKeyAuthFilter.java`. There should be none.

5. **TenantContext cleanup** — confirm `TenantContext.clear()` is called in a `finally` block in `ApiKeyAuthFilter.doFilterInternal`.

6. **API key header consistency** — confirm `X-API-Key` (exact casing) is used in `ApiKeyAuthFilter`, `OpenApiConfig`, and any Cucumber step definitions. Report any mismatch.

7. **No raw API keys** — confirm `keyHash` is always a SHA-256 hex string and no plaintext key is stored in any entity or repository.

8. **Flyway migrations** — list all files under `src/main/resources/db/migration/` and confirm they are sequentially numbered with no gaps.

Report a pass/fail summary for each check. If anything fails, explain the issue and suggest the fix.
