# IDE setup (VS Code, IntelliJ IDEA)

!!! info "One tool supported so far: `list_sources`"
    `codescape-mcp` speaks real MCP over stdio now (newline-delimited
    JSON-RPC 2.0 — `initialize`, `tools/list`, `tools/call`), but only one
    tool is wired up: **`list_sources`**, a read-only list of what's
    registered in `codescape-service`. That's it — no search, no
    registering a source through MCP yet, no Git/GitHub/build tools. The
    rest of [Roadmap](ROADMAP.md) Stage 8's surface still needs the
    backend capabilities (search indexing, etc.) it depends on. The config
    below is fully real and usable today for `list_sources`; expect more
    tools to show up in `tools/list` as they land, with no config changes
    needed on your end.

## How this fits together

MCP-aware IDEs don't expect you to start the server yourself — the IDE
spawns it over **stdio** based on a config entry, manages its lifecycle,
and talks JSON-RPC over stdin/stdout. That's `codescape-mcp`.

`codescape-service` is different: it holds the actual index/database and
should be a long-running background process, not something the IDE spawns
per session — start it once and leave it running (see
[Install](INSTALL.md#run)), the same way you'd run a local database
daemon. Every IDE window can then point its `codescape-mcp` config at the
same running service.

```
codescape-service   (start once, keep running)
        ^
        | HTTP, default http://localhost:8085
        |
codescape-mcp        (IDE spawns this, per session, over stdio)
        ^
        | stdio / JSON-RPC
        |
    Your IDE
```

## Prerequisites

- [`codescape-service` running](INSTALL.md#run) — start it before configuring
  your IDE; `codescape-mcp` has nothing to talk to otherwise.
- A [release](https://github.com/johnjoeallen/codescape/releases) unzipped
  somewhere — note the full path to `bin/codescape-mcp`
  (`bin\codescape-mcp.bat` on Windows).
- If no system Java 25+ is on `PATH`/`JAVA_HOME`, pass
  `--java-home <path>` once (see [Install](INSTALL.md#prerequisites)) —
  it's saved to a `.java-home` file next to the binaries, so the IDE
  config below doesn't need to repeat it on every launch once you have.

## VS Code

VS Code's MCP config lives in `.vscode/mcp.json` (workspace-scoped) or
your user profile (available to every workspace). The top-level key is
`servers` (not `mcpServers`, unlike Claude Desktop/Cursor/JetBrains).

```json title=".vscode/mcp.json"
{
  "servers": {
    "codescape": {
      "command": "/absolute/path/to/codescape-0.0.5/bin/codescape-mcp",
      "args": []
    }
  }
}
```

On Windows, point `command` at the `.bat` launcher instead:

```json title=".vscode/mcp.json (Windows)"
{
  "servers": {
    "codescape": {
      "command": "C:\\Users\\you\\tools\\codescape-0.0.5\\bin\\codescape-mcp.bat",
      "args": []
    }
  }
}
```

Alternatively, run **MCP: Add Server** from the Command Palette and point
it at the same binary — VS Code writes the config file for you and lets
you choose workspace vs. user scope. Once added, VS Code shows inline
start/stop/restart controls when you open `mcp.json`.

If `codescape-service` isn't on the default port, set
`CODESCAPE_SERVICE_URL` via `env` in the same server entry:

```json
{
  "servers": {
    "codescape": {
      "command": "/absolute/path/to/codescape-0.0.5/bin/codescape-mcp",
      "args": [],
      "env": { "CODESCAPE_SERVICE_URL": "http://localhost:9090" }
    }
  }
}
```

## IntelliJ IDEA (and other JetBrains IDEs)

This is the AI Assistant's MCP *client* configuration (connecting to an
external server) — not **Settings → Tools → MCP Server**, which is the
IDE exposing *itself* as an MCP server to other tools; that's the reverse
direction and unrelated to this setup.

1. **Settings → Tools → AI Assistant → Model Context Protocol**
2. Click **Add**, and paste:

   ```json
   {
     "mcpServers": {
       "codescape": {
         "command": "/absolute/path/to/codescape-0.0.5/bin/codescape-mcp",
         "args": []
       }
     }
   }
   ```

3. Choose **global** (available in every project) or **project-level**
   scope, click **OK**, then **Apply**.
4. Restart the IDE (or the AI Assistant tool window) for the change to
   take effect.

This configuration is shared across JetBrains IDEs on the same machine
(IntelliJ, PyCharm, WebStorm, etc.) — set it up once.

On Windows, use the `.bat` launcher path as in the VS Code example above.

## Troubleshooting

- **Only `list_sources` shows up in the tool list**: expected — see the
  callout at the top of this page. That's every tool that exists so far.
- **`list_sources` returns an error / empty result**: confirm
  `codescape-service` is actually running (`curl http://localhost:8085/api/sources`
  should return `[]` or your registered sources) and that
  `CODESCAPE_SERVICE_URL` matches its actual port if you changed it from
  the default.
- **Process fails to start at all**: run the exact `command`/`args` in a
  terminal yourself first. If it fails there, the IDE can't launch it
  either — this isolates IDE-config problems from `codescape-mcp`
  problems. See [Install](INSTALL.md) for the bundled-runtime/
  `--java-home` fallback chain if the launcher can't find a Java 25+
  install.
