<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo/codescape-logo-dark.svg">
  <img alt="CodeScape" src="assets/logo/codescape-logo-light.svg" height="48">
</picture>

CodeScape is a search engine over a curated, multi-source dataset for AI
coding agents — the code you're working on, related repos, internal docs
and wikis, indexed websites, and more, all searchable from one place. Git
repos are a special case, not a different kind of thing: instead of one
snapshot, a git source can have multiple indexed revisions (branches,
worktrees), each searchable on its own or, by default, together across
every currently-indexed branch.

Docs: **[johnjoeallen.github.io/codescape](https://johnjoeallen.github.io/codescape/)**

Register anything as a **source collection** — a git repo, a plain
folder, an extracted archive, a vendor drop, or (planned) a crawled
website or Confluence space — and CodeScape indexes it. For sources it
can safely mutate (git repos, other filesystem sources), it works against
a **managed copy** it owns, never the original, so Git/build/workspace
operations never put your working tree at risk.

## Why

An AI agent answering a question about your code usually needs more than
the one repo open in your editor: a sibling service's API, an internal
style guide, a wiki page explaining why something is the way it is.
CodeScape lets you register all of that once and search across it as a
single dataset, instead of the agent being scoped to whatever folder
happens to be open.

For the sources CodeScape can mutate — git repos and other filesystem
sources — letting an agent operate directly on your working copy is
risky: stray `git reset`, `git clean`, or file edits can clobber
uncommitted work. CodeScape sidesteps this by working against its own
sandboxed copy under `~/.codescape/`, so agents get full read/write/Git/
build capability without ever touching the developer-owned original.

## Core model

- **Source collections**: anything indexable — a git repository, a plain
  source directory, an extracted archive, a vendor source drop, or
  (planned) a crawled website or Confluence space.
- **Revisions**: the actual unit of indexing. Most sources have one
  revision at a time (a snapshot); git sources can have many — one per
  branch or worktree — each indexed separately. Search can be scoped to
  one revision or, by default, run across every currently-indexed branch
  together. That's the one structural difference between a git source and
  everything else: more revisions, not a different kind of source.
- **Managed copies**: for sources CodeScape can mutate, it creates and
  owns a copy — local or web-downloaded — under
  `~/.codescape/content/<name>/`. All mutation happens here; the original
  stays read-only input.
- **Capabilities**: instead of branching on source type, behavior is
  driven by capability flags (`FILESYSTEM`, `SEARCH`, `GIT`, `GITHUB`,
  `BRANCHES`, `HISTORY`, `BUILD`, `WORKSPACE`). `SEARCH` is universal —
  every source type gets it; `GIT`/`BRANCHES` are what let a source
  produce multiple revisions instead of just one.
- **Branch worktrees**: git sources keep their default branch checked out
  in `base/`, with other branches cached as worktrees under `branches/`,
  created and expired on demand — this is *how* a git source produces the
  multiple revisions described above.
- **Lucene indexing**: content is indexed per source and revision (and
  per git ref/commit, where applicable) to support fast search across
  everything registered.

See [AGENTS.md](./AGENTS.md) for the full architecture reference used by
coding agents working in this repo, and [RELEASE.md](./RELEASE.md) for how
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

Early stage — architecture and conventions are being established. Source
registration exists today; cross-source search (the actual point of the
project) is not implemented yet — see [AGENTS.md](./AGENTS.md) for the
current design and rules for contributing agents.

## License

See [LICENSE](./LICENSE).
