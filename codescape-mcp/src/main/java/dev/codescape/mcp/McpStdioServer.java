package dev.codescape.mcp;

import dev.codescape.mcp.json.Json;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal MCP server over stdio: newline-delimited JSON-RPC 2.0, per the
 * MCP stdio transport spec. Implements just enough of the protocol
 * (initialize, tools/list, tools/call, ping) to serve one tool,
 * list_sources — see the "Minimal MCP stdio transport + list_sources"
 * issue for what's deliberately not here yet (add_source, search, and
 * everything gated on capabilities the service doesn't have).
 *
 * <p>The adapter does no source/Git/GitHub/index/build/workspace logic of
 * its own (see AGENTS.md) — {@code list_sources} is a straight pass
 * through to {@code codescape-service}'s existing {@code GET /api/sources}.
 */
final class McpStdioServer {

    private static final String SERVER_NAME = "codescape-mcp";
    private static final String SERVER_VERSION = "0.1.0";
    private static final String FALLBACK_PROTOCOL_VERSION = "2024-11-05";

    private final CodescapeServiceClient client;

    McpStdioServer(CodescapeServiceClient client) {
        this.client = client;
    }

    void run(InputStream in, PrintStream out) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                handleLine(line, out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handleLine(String line, PrintStream out) {
        Object parsed;
        try {
            parsed = Json.parse(line);
        } catch (Json.JsonParseException e) {
            writeError(out, null, -32700, "Parse error: " + e.getMessage());
            return;
        }

        if (!(parsed instanceof Map)) {
            writeError(out, null, -32600, "Invalid Request: expected a JSON object");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) parsed;

        boolean hasId = request.containsKey("id");
        Object id = request.get("id");
        Object methodValue = request.get("method");
        if (!(methodValue instanceof String method)) {
            if (hasId) {
                writeError(out, id, -32600, "Invalid Request: missing or non-string method");
            }
            return;
        }

        switch (method) {
            case "initialize" -> {
                if (hasId) {
                    writeResult(out, id, handleInitialize(request));
                }
            }
            case "notifications/initialized" -> {
                // No response required — genuinely nothing to do yet. Kept
                // as an explicit case (rather than falling into "unknown
                // method") so it's clear this is a deliberately-handled
                // no-op, not an oversight.
            }
            case "ping" -> {
                if (hasId) {
                    writeResult(out, id, Map.of());
                }
            }
            case "tools/list" -> {
                if (hasId) {
                    writeResult(out, id, handleToolsList());
                }
            }
            case "tools/call" -> {
                if (hasId) {
                    handleToolsCall(request, id, out);
                }
            }
            default -> {
                if (hasId) {
                    writeError(out, id, -32601, "Method not found: " + method);
                }
            }
        }
    }

    private Map<String, Object> handleInitialize(Map<String, Object> request) {
        String protocolVersion = FALLBACK_PROTOCOL_VERSION;
        Object params = request.get("params");
        if (params instanceof Map<?, ?> paramsMap && paramsMap.get("protocolVersion") instanceof String requested) {
            // Echo back whatever the client proposed rather than asserting
            // a specific version ourselves — this server's surface is
            // small enough (three methods) that there's nothing version-
            // specific in it to negotiate.
            protocolVersion = requested;
        }

        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", protocolVersion);
        result.put("capabilities", Map.of("tools", Map.of()));
        result.put("serverInfo", serverInfo);
        return result;
    }

    private Map<String, Object> handleToolsList() {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", Map.of());
        inputSchema.put("additionalProperties", Boolean.FALSE);

        Map<String, Object> listSourcesTool = new LinkedHashMap<>();
        listSourcesTool.put("name", "list_sources");
        listSourcesTool.put("description", "List all registered CodeScape source collections.");
        listSourcesTool.put("inputSchema", inputSchema);

        return Map.of("tools", List.of(listSourcesTool));
    }

    private void handleToolsCall(Map<String, Object> request, Object id, PrintStream out) {
        Object params = request.get("params");
        String toolName = null;
        if (params instanceof Map<?, ?> paramsMap && paramsMap.get("name") instanceof String name) {
            toolName = name;
        }

        if (!"list_sources".equals(toolName)) {
            writeError(out, id, -32602, "Unknown tool: " + toolName);
            return;
        }

        try {
            String sourcesJson = client.listSources();
            // The MCP text-content type requires "text" to be a JSON
            // *string*, so sourcesJson (already a JSON array as text)
            // gets normal string escaping here — Json.Raw would splice it
            // in as a JSON array value instead, which isn't valid content.
            Map<String, Object> content = Map.of("type", "text", "text", sourcesJson);
            writeResult(out, id, Map.of("content", List.of(content), "isError", Boolean.FALSE));
        } catch (RuntimeException e) {
            // A failure calling codescape-service is a tool execution
            // failure, not a protocol error — report it as an isError
            // result so the model sees it and can react, rather than
            // failing the whole request at the JSON-RPC level.
            Map<String, Object> content = Map.of("type", "text", "text", "list_sources failed: " + e.getMessage());
            writeResult(out, id, Map.of("content", List.of(content), "isError", Boolean.TRUE));
        }
    }

    private void writeResult(PrintStream out, Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        writeLine(out, response);
    }

    private void writeError(PrintStream out, Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", (long) code);
        error.put("message", message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", error);
        writeLine(out, response);
    }

    private void writeLine(PrintStream out, Object response) {
        out.print(Json.write(response));
        out.print('\n');
        out.flush();
    }
}
