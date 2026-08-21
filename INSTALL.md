# Installing CodeScape

CodeScape ships as one zip per platform, each containing two independently
runnable components: `codescape-service` (the repository-management
Spring Boot app) and `codescape-mcp` (the MCP adapter). Every zip bundles
its own minimal JVM (built with `jlink`, native to that platform) under
`runtime/`, so no external database, search server, or Java install is
strictly required. The launcher scripts prefer a compliant system Java
25+ install when one's available, falling back to the bundled runtime
otherwise — see [Prerequisites](#prerequisites) for why that's the
preferred order, not the other way around.

## Prerequisites

None, as long as you download the zip matching your machine. Releases
publish platform-specific zips — pick the one for your OS/arch:

| Your machine            | Download                              |
|--------------------------|----------------------------------------|
| Linux, x86_64            | `codescape-<version>-linux-x64.zip`    |
| Windows, x86_64          | `codescape-<version>-windows-x64.zip`  |
| macOS, Apple Silicon     | `codescape-<version>-macos-arm64.zip`  |

No Intel Mac build currently — see
[RELEASE.md](RELEASE.md#release-archive-layout) for why.

The launcher scripts look for a Java 25+ install in this order, using the
bundled `runtime/` only as a last resort:

1. `--java-home <path>`, e.g. `./bin/codescape-service --java-home /opt/jdk-25`
2. A `.java-home` file in the install root, written automatically the
   first time `--java-home` validates successfully — so you only need to
   pass the flag once; every run after that picks it up on its own.
3. The `JAVA_HOME` environment variable.
4. `java` on `PATH`.
5. The bundled `runtime/` — printing a warning first — which only runs on
   the platform it was built for (the wrong platform's zip will fail to
   launch in this fallback case).

This order is deliberate: some corporate endpoint security software
(e.g. Carbon Black) blocks execution of unsigned/bundled binaries, so on
a locked-down machine a compliant system Java install may be the only
thing that actually runs. `--java-home`/`.java-home` exist so you can
point at one without changing your existing `JAVA_HOME`, which other
tools may depend on.

## Install

The zip's contents are already under a `codescape/` folder (not
`codescape-<version>/`) — unzipping a new version over an old one lands
at the same path, rather than piling up a differently-named folder per
release.

```
unzip codescape-<version>-<platform>.zip
cd codescape
```

## Run

Start the repository-management service first. On first run it creates
`~/.codescape/` (db, index, content, workspaces):

```
./bin/codescape-service
```

`codescape-mcp` is the MCP adapter, which talks to the service over HTTP.
You don't normally run it yourself: with no arguments it starts a real
MCP stdio JSON-RPC server that blocks waiting for input, meant to be
spawned by an MCP-aware IDE via a config entry (using the *full absolute
path* to `bin/codescape-mcp` — the IDE won't run it with your shell's
working directory) rather than run directly in a terminal.

To manually confirm `codescape-mcp` can reach the service without
configuring a whole IDE, use its CLI subcommands instead, which print a
result and exit immediately:

```
./bin/codescape-mcp list
```

For VS Code and IntelliJ IDEA specifically, see [IDE Setup](ide-setup.md)
for the exact config entry each expects.

On Windows, use `bin\codescape-service.bat` and `bin\codescape-mcp.bat`.

## Configuration

Defaults live in `config/application.yml`:

- `server.port` — port `codescape-service` listens on (default `8085`).
- `codescape.home` — root of CodeScape's managed-data area (default
  `~/.codescape`, overridable via the `CODESCAPE_HOME` environment
  variable).

`codescape-mcp` reads `CODESCAPE_SERVICE_URL` (default
`http://localhost:8085`) to locate the service.

## Verify

```
curl http://localhost:8085/api/sources
```

should return `[]` on a fresh install (or your registered sources).

## Uninstall

Delete the unzipped `codescape/` directory and, if you want to remove all
managed data (indexed sources, metadata, workspaces), `~/.codescape/` —
this does not touch any developer-owned source you registered.
