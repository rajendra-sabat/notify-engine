# ADR-007: Claude Developer Guardrails via .claude Folder

**Date:** 2026-05-20  
**Status:** Accepted

## Context

As the codebase grows and more sessions involve AI-assisted editing, two
failure modes emerge: changes that compile but violate project conventions
(package structure, multi-tenancy safety, Spring Boot patterns), and
changes that are committed before anyone has reviewed them against the
project's non-obvious invariants.

Standard linters (Checkstyle, SpotBugs) catch syntax and style but do not
understand project-specific rules such as "TenantContext.clear() must be in
a finally block" or "schema name must be validated before SET search_path
interpolation". A human reviewer catches these but is not always in the loop
for AI-assisted sessions.

The `.claude` folder is Claude Code's mechanism for project-scoped
configuration: shared `settings.json` for hooks, and `commands/` for
named slash-command skills. These run inside the Claude session rather
than in CI, which means they can catch issues at the moment of change
rather than after a push.

Three tools were considered for post-edit feedback:
- CI pipeline checks — correct but slow; feedback arrives after a push
- Pre-commit hooks (Husky / git hooks) — runs on commit, not on every edit
- Claude Code hooks + skills — runs immediately after each file change
  within the session, before the user even saves or commits

## Decision

Add a `.claude/` folder with the following components, all committed to the
repository so every contributor and every AI session gets the same guardrails:

**`.claude/settings.json`** — shared hook configuration:
- `PreToolUse` on `Bash`: warns before destructive shell commands
  (`rm -rf`, `git reset --hard`, `git push --force`, etc.)
- `PostToolUse` on `Edit`/`Write`: runs `./mvnw compile -q` after every
  Java file edit and, on success, prompts Claude to self-review the change
  against the project's security and convention checklist

**`.claude/hooks/pre-bash.sh`** — reads the pending Bash command from stdin
(JSON), greps for destructive patterns, and prints a warning. Non-blocking
(exit 0) — the intent is to surface risk, not to silently prevent action.

**`.claude/hooks/post-edit.sh`** — runs a compile check after every `.java`
edit. On compile failure it surfaces the errors immediately. On success it
outputs a targeted review prompt covering multi-tenancy safety, security,
and Spring Boot conventions so Claude performs an inline self-review without
needing a separate command invocation.

**`.claude/commands/sanity-check.md`** (`/sanity-check`) — an 8-point
checklist skill: compile, package structure, leftover DDD imports,
TenantContext safety, header naming consistency, API key hashing, and
Flyway migration sequencing.

**`.claude/commands/code-review.md`** (`/code-review [PR number]`) — a PR
review skill that fetches the diff via `gh pr diff`, analyses it against
project criteria, and posts inline comments directly on the GitHub PR using
`gh api .../pulls/{n}/reviews`. Approves if clean; requests changes with
inline annotations if not.

**`.claude/commands/java-guard.md`** (`/java-guard`) — a seven-section
static analysis skill covering: dependency health (version alignment,
scope correctness), Java 21 feature usage (records, pattern matching,
virtual threads, text blocks), Spring Boot 3.5 conventions, JPA/Hibernate
traps (EAGER fetching, self-invocation, unbounded findAll), multi-tenancy
correctness, security filter chain integrity, and test quality. Reports
findings as `[CRITICAL]` / `[WARNING]` / `[INFO]` with one-line fixes.

`.claude/settings.local.json` remains uncommitted (gitignored) for
per-developer permission overrides.

## Consequences

These decisions enable:
- Compile errors surfaced immediately after each file edit, before commit
- Automated self-review of every Java change against project-specific
  invariants without a separate review step
- PR reviews posted as GitHub inline comments by a single slash command,
  replacing manual review of AI-generated changes
- Consistent guardrails across all contributors and AI sessions without
  requiring CI pipeline changes
- Destructive command warnings before irreversible shell operations

These decisions rule out:
- Silent acceptance of changes that break multi-tenancy safety or package
  structure conventions
- Using `.claude/settings.local.json` for shared rules — local settings
  are developer-specific and not committed
- Blocking destructive commands automatically — hooks warn but do not
  prevent; blocking would require exit code 2 in pre-hooks, which was
  rejected as too disruptive for legitimate force operations
