# Installing CodeScape

CodeScape ships as one zip per platform, each containing two independently
runnable components: `codescape-service` (the repository-management
Spring Boot app) and `codescape-mcp` (the MCP adapter). Every zip bundles
its own minimal JVM (built with `jlink`, native to that platform) under
`runtime/`, so no external database, search server, or Java install is
required — the launcher scripts use the bundled runtime automatically.

## Prerequisites

None, as long as you download the zip matching your machine. Releases
publish four platform-specific zips — pick the one for your OS/arch:

| Your machine            | Download                              |
|--------------------------|----------------------------------------|
| Linux, x86_64            | `codescape-<version>-linux-x64.zip`    |
| Windows, x86_64          | `codescape-<version>-windows-x64.zip`  |
| macOS, Intel             | `codescape-<version>-macos-x64.zip`    |
| macOS, Apple Silicon     | `codescape-<version>-macos-arm64.zip`  |

The bundled `runtime/` only runs on the platform it was built for — using
the wrong zip's runtime will fail to launch. If you'd rather use a system
JVM (Java 25+) instead of the bundled one, remove or rename the `runtime/`
directory and the launcher scripts fall back to `java` on `PATH`.

## Install

```
unzip codescape-<version>-<platform>.zip -d codescape
cd codescape
```

## Run

Start the repository-management service first. On first run it creates
`~/.codescape/` (db, index, content, workspaces):

```
./bin/codescape-service
```

In a separate terminal (or as a background/managed process), start the MCP
adapter, which talks to the service over HTTP:

```
./bin/codescape-mcp
```

Then point your MCP-compatible AI agent/client at the `codescape-mcp`
process.

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
