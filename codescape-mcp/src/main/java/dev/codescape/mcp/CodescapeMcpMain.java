package dev.codescape.mcp;

import java.net.URI;

/**
 * Entry point for the CodeScape MCP adapter.
 *
 * <p>This is a pre-Stage-8 scaffold: it drives {@link CodescapeServiceClient}
 * from plain CLI args ({@code list} / {@code add <name> <sourcePath>
 * <type>}) rather than real MCP stdio JSON-RPC framing, so the thin-client
 * surface can be exercised before the actual MCP tool/transport layer lands
 * in Stage 8 (see ROADMAP.md). The adapter still does no source/Git/GitHub/
 * index/build/workspace logic of its own (see AGENTS.md) — every action
 * here is a straight pass-through to codescape-service.
 */
public final class CodescapeMcpMain {

    private CodescapeMcpMain() {
    }

    public static void main(String[] args) {
        URI serviceUri = URI.create(System.getenv().getOrDefault("CODESCAPE_SERVICE_URL", "http://localhost:8085"));
        CodescapeServiceClient client = new CodescapeServiceClient(serviceUri);

        if (args.length == 0) {
            System.out.println("codescape-mcp: connecting to codescape-service at " + serviceUri);
            if (client.isReachable()) {
                System.out.println("codescape-mcp: service reachable. Usage: list | add <name> <sourcePath> <type>");
            } else {
                System.err.println("codescape-mcp: could not reach codescape-service at " + serviceUri
                        + " — start codescape-service first.");
                System.exit(1);
            }
            return;
        }

        try {
            switch (args[0]) {
                case "list" -> System.out.println(client.listSources());
                case "add" -> {
                    if (args.length != 4) {
                        System.err.println("Usage: add <name> <sourcePath> <type>");
                        System.exit(1);
                        return;
                    }
                    System.out.println(client.addSource(args[1], args[2], args[3]));
                }
                default -> {
                    System.err.println("Unknown command: " + args[0] + " — usage: list | add <name> <sourcePath> <type>");
                    System.exit(1);
                }
            }
        } catch (RuntimeException e) {
            System.err.println("codescape-mcp: " + e.getMessage());
            System.exit(1);
        }
    }
}
