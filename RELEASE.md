# Release & Packaging

This document describes CodeScape's release workflow and the shape of a
distributed release. It is speculative/forward-looking until Stage 8+ of
the [ROADMAP](./ROADMAP.md) lands — treat it as the target to build
towards, not a description of an existing pipeline.

## What gets released

A CodeScape release is a single zip containing two independently runnable
Java components:

1. **codescape-service** — the Spring Boot repository-management app
   (source registration, managed copies, Git/GitHub operations, Lucene
   indexing, H2 metadata store, builds, workspaces). Packaged as an
   executable jar (Spring Boot fat jar).
2. **codescape-mcp** — the MCP adapter. A thin JSON-RPC 2.0 / MCP process
   that talks to `codescape-service` over HTTP. Packaged as an executable
   jar.

Both jars require only a JVM; no external database or search server needs
to be installed, since H2 and Lucene are embedded inside
`codescape-service`.

## Release archive layout

```
codescape-<version>.zip
├── bin/
│   ├── codescape-service        # launcher script (Linux/macOS)
│   ├── codescape-service.bat    # launcher script (Windows)
│   ├── codescape-mcp
│   └── codescape-mcp.bat
├── lib/
│   ├── codescape-service-<version>.jar
│   └── codescape-mcp-<version>.jar
├── config/
│   └── application.yml          # default config (ports, ~/.codescape path, etc.)
├── LICENSE
├── README.md
├── AGENTS.md
└── INSTALL.md
```

At first run, `codescape-service` creates its managed-data root at
`~/.codescape/` (configurable), containing:

```
~/.codescape/
├── db/            # H2 database file(s)
├── index/         # Lucene index(es)
├── sources/       # managed source copies (base/ + branches/ per source)
└── workspaces/    # disposable agent workspaces
```

## Versioning

Semantic versioning (`MAJOR.MINOR.PATCH`). Until Stage 8 (MCP adapter) is
complete, releases stay pre-1.0 (`0.MINOR.PATCH`); breaking changes to the
managed-data layout or API bump `MINOR`.

## Build & release workflow (target)

1. Tag a release commit on `main` (`vX.Y.Z`).
2. CI builds both modules (`mvn -pl codescape-service,codescape-mcp
   package` or Gradle equivalent), producing the two fat jars.
3. CI assembles the release zip (jars + launcher scripts + default config
   + docs) as described above.
4. CI publishes the zip as a GitHub Release asset attached to the tag,
   with release notes generated from merged PRs/commits since the last
   tag.
5. (Optional, later) publish checksums (`sha256sum`) alongside the zip for
   integrity verification.

No Docker image is planned for the initial release line — the goal is a
zero-dependency local install (unzip + run), consistent with CodeScape's
local-first, developer-machine-scoped design (see
[AGENTS.md](./AGENTS.md)).

## Installation instructions (target, for `INSTALL.md`)

```
# 1. Prerequisites: Java 25+ on PATH.
java -version

# 2. Unzip the release.
unzip codescape-<version>.zip -d codescape
cd codescape

# 3. Start the repository-management service.
#    On first run this creates ~/.codescape/ (db, index, sources, workspaces).
./bin/codescape-service

# 4. In a separate terminal (or as a background/managed process),
#    start the MCP adapter, which talks to the service over HTTP.
./bin/codescape-mcp

# 5. Point your MCP-compatible AI agent/client at the codescape-mcp process
#    (stdio or configured transport — see codescape-mcp --help).
```

Configuration (ports, `~/.codescape` location override, log level) lives
in `config/application.yml`; environment variables or `--` flags override
individual keys at launch.

## Upgrades

- H2 and Lucene data are versioned alongside the managed-data schema.
  A version mismatch on startup should trigger either an automatic
  migration or a clear error naming the required CodeScape version —
  never a silent/partial read.
- Managed source copies and workspaces are disposable/regenerable from
  the original developer source, so a failed upgrade should never risk
  data the developer doesn't already have elsewhere.
