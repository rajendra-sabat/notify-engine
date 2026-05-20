You are a code reviewer posting a formal GitHub PR review. Follow these steps exactly.

## Step 1 — Identify the PR

If `$ARGUMENTS` is a number, use it as the PR number.
Otherwise run:
```bash
gh pr view --json number,title,body,headRefName,baseRefName 2>/dev/null
```
If no open PR exists for the current branch, tell the user and stop.

## Step 2 — Gather context

Run these commands to understand the change:
```bash
gh pr view $PR_NUMBER --json number,title,body,additions,deletions,changedFiles
gh pr diff $PR_NUMBER
gh pr view $PR_NUMBER --json commits --jq '.commits[].messageHeadline'
```

## Step 3 — Review the diff

Analyse every changed file against the criteria below. For each issue, note the **file path** and **line number** from the diff so you can post an inline comment.

### Security
- `TenantContext.clear()` must be called in a `finally` block in `ApiKeyAuthFilter`. Any path that skips it is a cross-tenant data leak.
- Schema name in `SchemaMultiTenantConnectionProvider.getConnection` must be validated against `^[a-zA-Z0-9_]+$` before interpolation into `SET search_path TO`.
- API keys must only be compared by SHA-256 hash. Flag any log statement, response body, or field that exposes a raw key.
- Flag any new `@RequestHeader("X-API-Key")` parameter on a controller — the filter owns auth, controllers must not re-validate it.

### Correctness
- Every new `@Entity` field needs a matching column in a Flyway migration (or must map to an existing column). Flag orphans.
- New DTO `@NotNull`/`@NotBlank` constraints must be consistent with what the Cucumber feature file sends — a tightened constraint with no matching test scenario will cause silent 400s.
- `ApiKeyAuthFilter` must override `doFilterInternal`, not `doFilter`. Flag if changed.

### Spring Boot conventions
- Constructor injection only. Flag `@Autowired` on fields.
- Controllers must be thin: validate input, call one service method, return response. Flag business logic in controllers.
- No `@Transactional` on filter or config classes.
- Lombok `@Getter` is acceptable on entities; `@Data`, `@Setter`, and `@EqualsAndHashCode` are not — they break Hibernate proxy equality.

### Multi-tenancy
- `Notification` entity must have no `schema` attribute on `@Table` — routing is via `search_path`.
- `Tenant` and `ApiKey` must have `schema = "system"` on `@Table`.
- `TenantIdentifierResolver.resolveCurrentTenantIdentifier` must fall back to `"public"` when the ThreadLocal is empty.

### Package structure
- New files must land in exactly one of: `config / controller / domain / dto / filter / repository / service`.
- No sub-packages, no DDD contexts (`notification/`, `tenancy/`, `security/`).

### Test coverage
- New behaviour needs a matching scenario in `src/test/resources/features/notifications.feature`.
- Step definitions must reference `VALID_API_KEY` constant, not a hardcoded string.

## Step 4 — Post the review

Post **inline comments** for each specific finding using the GitHub API:
```bash
gh api repos/{owner}/{repo}/pulls/$PR_NUMBER/reviews \
  --method POST \
  --field event="COMMENT" \
  --field body="<overall summary>" \
  --field "comments[][path]=<file>" \
  --field "comments[][position]=<diff position>" \
  --field "comments[][body]=<finding>"
```

Get the repo owner/name with:
```bash
gh repo view --json nameWithOwner --jq '.nameWithOwner'
```

Diff position is the line number within the unified diff output (counting from 1 at the first `@@` hunk header), not the file line number.

If there are no inline findings, post a single approval:
```bash
gh pr review $PR_NUMBER --approve --body "No issues found. LGTM."
```

If there are findings, post all inline comments in one API call, then summarise:
```bash
gh pr review $PR_NUMBER --comment --body "<summary of all findings>"
```

## Step 5 — Report back

Print a summary of what was posted: number of inline comments, their files and severities. Label each finding as **Must fix**, **Should fix**, or **Note**.
