<p align="center">
  <img src="assets/logo/codescape-icon-512.png" alt="CodeScape logo" width="96">
</p>

<h1 align="center">CodeScape</h1>
<p align="center">A managed local source estate for AI coding agents</p>

Rather than operating directly on your repositories and source folders,
CodeScape registers each one as a **source collection** and creates a
distinct **managed copy** that it owns. All indexing, search, Git/GitHub
operations, builds, tests, and agent experimentation happen against that
managed copy — your original source is treated as read-only input and is
never modified.

## Why

Letting an AI agent operate directly on your working repository is risky:
stray `git reset`, `git clean`, or file edits can clobber uncommitted work.
CodeScape sidesteps this by giving every registered source its own
sandboxed copy under `~/.codescape/`, so agents get full read/write/Git/
build capability without ever touching the developer-owned original.

## Core model

- **Source collections**: a Git repository, a plain source directory, an
  extracted archive, a vendor source drop, or any other filesystem-based
  source tree.
- **Managed copies**: CodeScape creates and owns a copy of each registered
  source — local or web-downloaded — under `~/.codescape/content/<name>/`.
  All mutation happens here.
- **Capabilities**: instead of branching on source type, behavior is driven
  by capability flags (`FILESYSTEM`, `SEARCH`, `GIT`, `GITHUB`, `BRANCHES`,
  `HISTORY`, `BUILD`, `WORKSPACE`), so plain folders and full Git/GitHub
  repos share the same browsing, search, and workspace model.
- **Branch worktrees**: Git sources keep their default branch checked out
  in `base/`, with other branches cached as worktrees under `branches/`,
  created and expired on demand.
- **Lucene indexing**: content is indexed per source and revision (and per
  Git ref/commit, where applicable) to support fast search across sources.

See [Architecture](AGENTS.md) for the full reference used by coding
agents working in this repo, and [Release & Packaging](RELEASE.md) for how
CodeScape is packaged and installed.

## Data storage

- **Metadata** (sources, branches, snapshots, workspaces): embedded
  [H2](https://www.h2database.com/) database under `~/.codescape/db/`.
- **Search**: embedded [Apache Lucene](https://lucene.apache.org/) index
  under `~/.codescape/index/`.

Both are zero-install, in-process — no external database or search
server is required to run CodeScape.

## Architecture at a glance

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

The MCP adapter is a thin transport layer only. All source, Git, GitHub,
indexing, workspace, build, and analysis logic lives in the Spring Boot
service, operating exclusively on CodeScape-managed copies.

## Status

Early stage — architecture and conventions are being established. See
[Roadmap](ROADMAP.md) for the staged build-out plan, and
[Install](INSTALL.md) to try a release locally.
