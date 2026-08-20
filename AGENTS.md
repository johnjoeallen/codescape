# AGENTS.md

Instructions for AI coding agents working in this repository.

## Instruction routing rule

Add new instructions here (`AGENTS.md`) by default. Only add instructions to
`CLAUDE.md` if they are specific to Claude Code / Claude-based agents and do
not apply to other agents. `CLAUDE.md` should otherwise just point back here.

## Project overview

CodeScape is a managed local source estate. It does not operate directly on
developer-owned repositories or folders. Instead, when a source is
registered, CodeScape creates and owns a distinct **managed copy**, and all
analysis, indexing, Git/GitHub operations, builds, tests, and agent
experimentation happen against that managed copy.

## Core architecture

### Core behaviour

CodeScape manages **source collections**. A source collection may be:

- a Git repository
- a plain source directory
- an extracted source archive
- a vendor source drop
- another filesystem-based source tree

When a source is registered, CodeScape creates and owns a distinct managed
copy. The original developer-owned source is treated as **read-only input**.

CodeScape performs all analysis, indexing, Git operations, GitHub
operations, builds, tests, and agent experimentation against its
**managed copy**, not against the developer's source.

### Fundamental safety rule

> CodeScape may read developer-owned source collections, but it must never
> modify them.

For Git repositories, CodeScape is allowed to inspect the original
repository using read-only Git operations during registration, snapshot
creation, or synchronisation. It must never perform state-changing Git
operations against the developer's repository. All state-changing Git
activity occurs within CodeScape-owned copies and workspaces.

### Source model

The primary abstraction is `SourceCollection`, not `Repository`.

A source may have capabilities:

```
SourceCollection
    |
    +-- Filesystem capability
    |
    +-- Git capability
    |
    +-- GitHub capability
    |
    +-- Build capability
    |
    +-- Index capability
```

A plain source directory may support only:

```
Filesystem
Index
Search
Snapshot
Workspace
Build
```

A Git repository may additionally support:

```
Branches
Commits
Diff
Blame
Tags
Refresh
Worktrees
```

A GitHub-hosted Git repository may additionally support:

```
Pull requests
Issues
Checks
Workflow runs
```

### Managed copy model

A developer may register:

```
/home/user/git/orders-api
```

CodeScape creates:

```
~/.codescape/sources/orders-api/
    base/
    branches/
```

The developer-owned repository remains untouched. CodeScape can freely
perform operations such as `git fetch`, `git checkout`, `git switch`,
`git reset`, `git worktree`, `git diff`, `git log`, `git blame`, `git clean`
within `~/.codescape/`, because that area belongs to CodeScape. It must not
perform state-changing equivalents against the developer's repository path.

### GitHub sources

If the managed Git source has a GitHub remote and `gh` is available,
CodeScape may use `git` for repository operations and `gh` for
GitHub-specific operations (`gh pr list`, `gh pr view`, `gh issue list`,
`gh run list`, etc.). Authentication remains delegated to the developer's
existing `git` and `gh` environment. CodeScape does not store GitHub
credentials.

### Plain folder sources

A plain source folder does not require Git, e.g.
`/opt/vendor/source-drop-2025/` may be registered as `type = DIRECTORY`.
CodeScape creates `~/.codescape/sources/vendor-source-drop/base/` using a
controlled filesystem snapshot/copy. CodeScape can then browse, index
(Lucene), search, snapshot, create disposable workspaces, build if
configured, and allow agent experimentation against a workspace.
Git-specific capabilities are simply unavailable.

### Capability-based design

Do not scatter checks such as `if (source.getType() == GIT)` throughout the
application. Prefer explicit capabilities.

Conceptually:

```
Source
  id
  name
  type
  sourcePath
  managedPath
  capabilities
```

Possible capabilities: `FILESYSTEM`, `SEARCH`, `GIT`, `GITHUB`, `BRANCHES`,
`HISTORY`, `BUILD`, `WORKSPACE`.

UI and RPC behaviour should be driven by capability, e.g.:

```
Git source:
  FILESYSTEM, SEARCH, GIT, BRANCHES, HISTORY, WORKSPACE

GitHub Git source:
  FILESYSTEM, SEARCH, GIT, GITHUB, BRANCHES, HISTORY, WORKSPACE

Plain source dump:
  FILESYSTEM, SEARCH, WORKSPACE
```

### Correct architectural rule

> The MCP adapter performs no source operations itself. All source, Git,
> GitHub, indexing, workspace, build, and analysis operations are performed
> by the CodeScape Spring Boot service against CodeScape-managed source
> copies and workspaces.

This is different from saying that CodeScape performs no Git operations.
CodeScape deliberately performs Git operations — it simply performs them
within its own controlled area, never against developer-owned source.

### Process architecture

```
                  Developer-owned source
                         READ ONLY
                             |
                             | import / synchronise
                             v
                    +------------------+
                    |    CodeScape     |
                    | Managed Sources  |
                    +--------+---------+
                             |
              +--------------+---------------+
              |              |               |
             git            gh            filesystem
              |              |               |
              +--------------+---------------+
                             |
                         Lucene index
                             |
                       workspaces/builds
                             |
                             v
                    Spring Boot Service
                             ^
                             |
                       JSON-RPC 2.0
                             |
                      CodeScape MCP
                             ^
                             |
                            MCP
                             |
                         AI Agent
```

### Git branch model

For Git sources, keep the managed base repository on the default branch:

```
~/.codescape/sources/orders-api/
    base/
        # main/master/default branch

    branches/
        feature-a/
        release-2.4/
```

Use Git worktrees for branch-specific views. CodeScape may create, refresh,
and delete these worktrees because they live entirely within
CodeScape-managed storage.

### Branch lifecycle

The default branch is persistent. Other branches are cached. CodeScape may:

- create a branch worktree on first access
- refresh active branches automatically
- maintain branch-aware indexes
- expire branches after inactivity
- remove worktrees for deleted remote branches
- preserve pinned branches indefinitely

Deleting a cached branch means deleting only the CodeScape-managed worktree
and associated index state. It never deletes or modifies a developer
branch.

### Index model

Lucene documents should be source and revision aware. Common fields:

```
sourceId
revisionId
path
filename
extension
language
content
```

Git sources additionally have:

```
ref
commitSha
```

Example:

```
sourceId = orders-api
ref = refs/heads/feature/new-api
commitSha = 18ba79...
path = src/main/java/.../OrderService.java
```

Plain directory sources use a generated snapshot revision:

```
sourceId = legacy-source
revisionId = snapshot-83fd92
path = src/parser.c
```

### Operational boundary

The important boundary is not "CodeScape does not modify repositories." It
is:

```
Developer-owned source
        |
      READ ONLY
        |
        v
CodeScape-managed source
        |
        +-- Git operations
        +-- GitHub operations
        +-- branch worktrees
        +-- indexing
        +-- builds
        +-- tests
        +-- modifications
        +-- experiments
```

That is the core safety model of CodeScape: not merely an indexer sitting
beside repositories, but a managed local source estate. Git repos get
richer capabilities via `git` and optionally `gh`; plain folders still
participate in the same browsing, Lucene search, grouping, and workspace
model.

## Technology choices

These are the current (speculative, subject to revision) technology
choices for CodeScape:

- **Database**: [H2](https://www.h2database.com/), embedded, file-backed
  under `~/.codescape/`. Chosen for a zero-install, single-process local
  deployment — no external database server required. Stores source/
  workspace/branch/snapshot metadata, not file content.
- **Indexing**: [Apache Lucene](https://lucene.apache.org/), embedded, one
  index (or index-per-source) under `~/.codescape/`. Chosen for the same
  zero-install reasoning as H2, and because it's the standard embeddable
  full-text/code search engine on the JVM.
- **Service**: Spring Boot, packaged as a single runnable jar.
- **MCP adapter**: separate process/jar, thin JSON-RPC 2.0 transport over
  the Spring Boot service's HTTP API (see [Process architecture](#process-architecture)).

Both H2 and Lucene data live under `~/.codescape/` alongside managed
source copies — see [Release packaging](./RELEASE.md) for how this maps to
the installed layout.

## Working in this repo

- Never perform state-changing operations (write, checkout, reset, clean,
  etc.) against a developer-owned source path. All mutation happens inside
  `~/.codescape/`-managed copies.
- Model new source-type support as capabilities, not type-code branches.
- Keep the MCP adapter a thin transport layer; put source/Git/GitHub/index/
  build/workspace logic in the Spring Boot service.
