# Installing CodeScape

CodeScape ships as a zip containing two independently runnable components:
`codescape-service` (the repository-management Spring Boot app) and
`codescape-mcp` (the MCP adapter). Both need only a JVM — no external
database or search server is required, since the H2 metadata store and
Lucene index are embedded in `codescape-service`.

## Prerequisites

- Java 25 or newer on `PATH`.

```
java -version
```

## Install

```
unzip codescape-<version>.zip -d codescape
cd codescape
```

## Run

Start the repository-management service first. On first run it creates
`~/.codescape/` (db, index, sources, workspaces):

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
