# CodeScape Staged Development Plan

This plan sequences CodeScape's build-out so that each stage produces a
working, testable increment. See [AGENTS.md](./AGENTS.md) for the
architecture these stages implement.

Each stage should end with: a runnable slice, tests for the new
capability, and no regressions to earlier stages' safety guarantees
(read-only developer source, all mutation confined to `~/.codescape/`).

---

## Stage 0 — Project scaffolding

- Spring Boot service skeleton (module layout, config, logging).
- `~/.codescape/` root layout: `content/`, config store.
- `SourceCollection` domain model (id, name, type, sourcePath,
  managedPath, capabilities) with persistence.
- Basic CLI/REST entrypoint to register and list sources (no operations
  yet).

**Exit criteria:** can register a source and see it listed; nothing is
copied or mutated yet.

## Stage 1 — Filesystem capability (plain folder sources)

- Managed-copy creation: controlled snapshot/copy of a plain directory
  into `~/.codescape/content/<name>/base/`.
- Read-only enforcement against the original `sourcePath`.
- Filesystem browsing API (list/read files) against the managed copy.
- Capability flags: `FILESYSTEM`.

**Exit criteria:** register a plain directory (e.g. a vendor source drop),
browse its managed copy, confirm the original is untouched.

## Stage 2 — Search/Index capability

- Lucene index integration, schema: `sourceId`, `revisionId`, `path`,
  `filename`, `extension`, `language`, `content`.
- Indexing job over a managed copy; snapshot-based `revisionId` for
  non-Git sources.
- Search API (by source, by path/content).
- Capability flags: add `SEARCH`.

**Exit criteria:** search returns results scoped to a source and
revision.

## Stage 3 — Snapshot & Workspace capabilities

- Snapshot model for plain sources (immutable point-in-time copies).
- Disposable workspace creation from a source/snapshot for agent
  experimentation (write-enabled, isolated from `base/`).
- Workspace teardown/cleanup lifecycle.
- Capability flags: add `SNAPSHOT`, `WORKSPACE`.

**Exit criteria:** an agent can get a disposable workspace, write to it
freely, and discard it without affecting `base/` or the original source.

## Stage 4 — Build capability

- Pluggable build detection/execution against a workspace (e.g. Maven/
  Gradle/npm/make, configurable).
- Build output capture and status reporting.
- Capability flags: add `BUILD`.

**Exit criteria:** a registered source with build config can be built
inside a workspace, with results surfaced via the API.

## Stage 5 — Git capability

- Git-aware source registration: read-only inspection of the original
  repo (remote URL, default branch, HEAD) during registration.
- Managed clone into `~/.codescape/content/<name>/base/` (default branch).
- Git operations (`fetch`, `checkout`, `switch`, `reset`, `diff`, `log`,
  `blame`, `clean`) implemented **only** against managed paths.
- Explicit guard layer preventing any state-changing Git call from ever
  targeting the original `sourcePath`.
- Capability flags: add `GIT`, `BRANCHES`, `HISTORY`.

**Exit criteria:** register a Git repo, confirm the managed clone diverges
freely (branches, resets, etc.) while the original working tree is
provably unchanged (tested explicitly, e.g. hash/mtime checks).

## Stage 6 — Git branch worktree model

- `branches/` worktree management: create on first access, refresh active
  branches, expire on inactivity, pin support, cleanup of worktrees for
  deleted remote branches.
- Branch-aware indexing (`ref`, `commitSha` fields added to Lucene docs).
- Capability flags: extend `BRANCHES` with lifecycle behavior.

**Exit criteria:** switching between branches in the UI/API creates/reuses
worktrees transparently; deleting a cached branch removes only the
CodeScape-managed worktree and its index state.

## Stage 7 — GitHub capability

- Detect GitHub remote on managed Git sources; verify `gh` availability
  and delegate to existing developer `git`/`gh` auth (no credential
  storage).
- Wrap `gh pr list/view`, `gh issue list`, `gh run list`, etc.
- Capability flags: add `GITHUB`.

**Exit criteria:** a GitHub-backed source exposes PR/issue/workflow data
through the same capability-driven API surface.

## Stage 8 — MCP adapter

- Thin JSON-RPC 2.0 / MCP transport layer over the Spring Boot service
  API — no source/Git/index/build logic in the adapter itself.
- Expose capability-driven tool surface to MCP clients (source list,
  search, browse, git ops, workspace, build, GitHub).
- Capability negotiation: only advertise tools a given source actually
  supports.

**Exit criteria:** an MCP-connected AI agent can register a source,
search it, branch/build in a workspace, and inspect GitHub PRs entirely
through MCP tool calls, with zero direct filesystem/Git access to the
developer's original source.

## Stage 9 — Hardening & lifecycle polish

- Automatic expiry/GC of stale branches, workspaces, and snapshots.
- Concurrency/locking around managed-copy mutation (multiple agents/
  sessions touching the same source).
- Audit logging of all state-changing operations (what, on which managed
  path, by which agent/session).
- Config surface for retention policies, build tool detection, and index
  exclusions (`.gitignore`-aware).

**Exit criteria:** long-running use doesn't leak disk/index state; every
mutating operation is attributable and confined to managed storage.

## Stage 10 — Release packaging

- Package `codescape-service` (Spring Boot, H2 + Lucene embedded) and
  `codescape-mcp` as separate executable jars from a shared build.
- Build a custom `jlink` runtime (module list derived from the jars via
  `jdeps`) and bundle it into the release zip so no system JVM is
  required.
- Assemble the release zip (jars, bundled runtime, launcher scripts,
  default config, docs) per [RELEASE.md](./RELEASE.md).
- Wire up CI to build, tag, and publish the zip as a GitHub Release asset
  on `vX.Y.Z` tags.
- Write `INSTALL.md` (unzip + run, no prerequisites).

**Exit criteria:** a clean machine with nothing preinstalled can unzip a
tagged release, start both processes, and connect an MCP client — no
JVM, external database, or search server setup required.

## Stage 11 — Multi-source & scale

- Cross-source search and grouping (e.g. related repos/services).
- Performance tuning for large repos (incremental indexing, partial
  worktree checkouts).
- Optional remote/team-shared managed-source backend (stretch goal, out
  of scope for local-first v1).

---

## Sequencing notes

- Stages 1–4 (plain folder path) intentionally come before Stage 5 (Git),
  so the capability model and safety boundary are proven on the simpler
  filesystem case first.
- Stage 5's original-source-is-never-mutated guarantee is the highest-risk
  property in the whole system; it gets its own explicit test suite before
  any later stage builds on top of it.
- The MCP adapter (Stage 8) is deliberately last among the "core" stages
  so it has a stable, fully-capable service API to wrap rather than
  co-evolving with it.
