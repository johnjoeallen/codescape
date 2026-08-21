# Release & Packaging

This document describes CodeScape's release workflow and the shape of a
distributed release, as built by [`release.yml`](.github/workflows/release.yml)
on a `vX.Y.Z` tag push.

## What gets released

A CodeScape release is one zip **per platform**, each containing the same
two independently runnable Java components plus a JVM built for that
platform:

1. **codescape-service** — the Spring Boot repository-management app
   (source registration, managed copies, Git/GitHub operations, Lucene
   indexing, H2 metadata store, builds, workspaces). Packaged as an
   executable jar (Spring Boot fat jar).
2. **codescape-mcp** — the MCP adapter. A thin JSON-RPC 2.0 / MCP process
   that talks to `codescape-service` over HTTP. Packaged as an executable
   jar.

Both jars require only a JVM; no external database or search server needs
to be installed, since H2 and Lucene are embedded inside
`codescape-service`. The release zip bundles its own JVM (see
[Bundled runtime](#bundled-runtime)), so a developer machine does not need
Java preinstalled at all.

## Release archive layout

Each platform's zip is named `codescape-<version>-<platform>.zip`, where
`<platform>` is one of:

| Platform       | Built on          | Covers                        |
|----------------|-------------------|--------------------------------|
| `linux-x64`    | `ubuntu-latest`   | Linux, x86_64                  |
| `windows-x64`  | `windows-latest`  | Windows, x86_64                |
| `macos-x64`    | `macos-13`        | macOS, Intel                   |
| `macos-arm64`  | `macos-14`        | macOS, Apple Silicon (M-series)|

`jlink` builds a runtime for whatever platform it runs on — it can't
cross-compile a JVM for a different OS/arch — so each zip is built on a
matching GitHub-hosted runner and bundles a runtime native to it. Pick the
zip matching your machine; running the wrong one's bundled `runtime/` will
fail (the launcher scripts fall back to a system `java` on `PATH` if the
bundled one won't run, which only helps if that system Java happens to be
25+).

Aside from the platform-specific `runtime/`, every zip has the same
layout:

```
codescape-<version>-<platform>.zip
├── bin/
│   ├── codescape-service        # launcher script (Linux/macOS)
│   ├── codescape-service.bat    # launcher script (Windows)
│   ├── codescape-mcp
│   └── codescape-mcp.bat
├── lib/
│   ├── codescape-service-<version>.jar
│   └── codescape-mcp-<version>.jar
├── runtime/
│   └── ...                      # jlink custom JRE (bin/java, etc.)
├── config/
│   └── application.yml          # default config (ports, ~/.codescape path, etc.)
├── LICENSE
├── README.md
├── AGENTS.md
└── INSTALL.md
```

## Bundled runtime

Rather than requiring Java 25+ on `PATH`, the release zip bundles a custom,
minimal JRE built with `jlink`, containing only the modules
`codescape-service` and `codescape-mcp` actually use. The module list is
determined via `jdeps --print-module-deps` — run against the jars
*exploded* first, since `jdeps` can't see dependencies nested under a
Spring Boot fat jar's `BOOT-INF/lib/` — plus `jdk.crypto.ec` for TLS,
which `jdeps` can't see either since it's loaded via SPI. This keeps the
runtime small (tens of MB, not a full JDK) while making the zip fully
self-contained.

Launcher scripts (`bin/codescape-service`, `bin/codescape-mcp`, and their
`.bat` equivalents) prefer `runtime/bin/java` (`runtime\bin\java.exe` on
Windows) relative to the script's own location, when present, and fall
back to `java` on `PATH` otherwise. This keeps the same scripts working
for local development straight out of `dist/`, where no `runtime/`
directory exists.

Native installers (`.deb`, `.rpm`, `.msi`, etc.) via `jpackage` are a
possible later addition but out of scope for the initial release line —
see [ROADMAP](./ROADMAP.md) Stage 10.

At first run, `codescape-service` creates its managed-data root at
`~/.codescape/` (configurable), containing:

```
~/.codescape/
├── db/            # H2 database file(s)
├── index/         # Lucene index(es)
├── content/       # managed source copies (base/ + branches/ per source),
│                  # including web-downloaded content
└── workspaces/    # disposable agent workspaces
```

## Versioning

Semantic versioning (`MAJOR.MINOR.PATCH`). Until Stage 8 (MCP adapter) is
complete, releases stay pre-1.0 (`0.MINOR.PATCH`); breaking changes to the
managed-data layout or API bump `MINOR`.

## Build & release workflow

1. Tag a release commit on `main` (`vX.Y.Z`) and push the tag.
2. A `build` job (on `ubuntu-latest`) runs `mvn verify` then `mvn package`
   once, producing the two platform-independent fat jars, and uploads
   them as a workflow artifact.
3. A `create-release` job creates a **draft** GitHub Release for the tag
   (draft so it isn't visible with partial assets while the matrix below
   is still running).
4. A `package` job runs as a **matrix** across the four platforms in
   [Release archive layout](#release-archive-layout). Each downloads the
   shared jars, derives its own module list via `jdeps --print-module-deps`
   (run against the jars *exploded*, not the fat jars directly — see
   [Bundled runtime](#bundled-runtime)), builds a `jlink` runtime native to
   that platform, assembles that platform's zip, and uploads the zip plus
   its `sha256` checksum to the draft release.
5. Once all four platform jobs succeed, a `publish-release` job un-drafts
   the release. Release notes are generated from merged PRs/commits since
   the last tag.

No Docker image is planned for the initial release line — the goal is a
zero-dependency local install (unzip + run, no Java install required),
consistent with CodeScape's local-first, developer-machine-scoped design
(see [AGENTS.md](./AGENTS.md)).

## Installation instructions (target, for `INSTALL.md`)

```
# 1. Download the zip matching your OS/arch (see the platform table
#    above) and unzip it. No Java install required — a JVM ships inside.
unzip codescape-<version>-<platform>.zip -d codescape
cd codescape

# 2. Start the repository-management service.
#    On first run this creates ~/.codescape/ (db, index, content, workspaces).
./bin/codescape-service

# 3. In a separate terminal (or as a background/managed process),
#    start the MCP adapter, which talks to the service over HTTP.
./bin/codescape-mcp

# 4. Point your MCP-compatible AI agent/client at the codescape-mcp process
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
