package dev.codescape.mcp;

import java.net.URI;

/**
 * Entry point for the CodeScape MCP adapter.
 *
 * <p>With no arguments, runs the real MCP stdio server ({@link
 * McpStdioServer}) — this is what an IDE's MCP config should point at
 * (see docs/ide-setup.md). {@code list}/{@code add} remain available as
 * CLI subcommands, a pre-Stage-8 scaffold for exercising {@link
 * CodescapeServiceClient} directly without a real MCP client. The adapter
 * does no source/Git/GitHub/index/build/workspace logic of its own (see
 * AGENTS.md) — every action here is a straight pass-through to
 * codescape-service.
 */
public final class CodescapeMcpMain {

    private CodescapeMcpMain() {
    }

    public static void main(String[] args) {
        URI serviceUri = URI.create(System.getenv().getOrDefault("CODESCAPE_SERVICE_URL", "http://localhost:8085"));
        CodescapeServiceClient client = new CodescapeServiceClient(serviceUri);

        if (args.length == 0) {
            new McpStdioServer(client).run(System.in, System.out);
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
