package dev.codescape.mcp;

import java.net.URI;

/**
 * Entry point for the CodeScape MCP adapter.
 *
 * <p>This is a Stage 0 placeholder: it confirms it can reach
 * codescape-service and exits. The actual JSON-RPC 2.0 / MCP stdio
 * transport and tool surface land in Stage 8 (see ROADMAP.md) — the
 * adapter stays a thin transport layer with no source/Git/GitHub/index/
 * build/workspace logic of its own (see AGENTS.md).
 */
public final class CodescapeMcpMain {

    private CodescapeMcpMain() {
    }

    public static void main(String[] args) {
        URI serviceUri = URI.create(System.getenv().getOrDefault("CODESCAPE_SERVICE_URL", "http://localhost:8085"));
        CodescapeServiceClient client = new CodescapeServiceClient(serviceUri);

        System.out.println("codescape-mcp: connecting to codescape-service at " + serviceUri);
        if (client.isReachable()) {
            System.out.println("codescape-mcp: service reachable. MCP tool surface not yet implemented (Stage 8).");
        } else {
            System.err.println("codescape-mcp: could not reach codescape-service at " + serviceUri
                    + " — start codescape-service first.");
            System.exit(1);
        }
    }
}
