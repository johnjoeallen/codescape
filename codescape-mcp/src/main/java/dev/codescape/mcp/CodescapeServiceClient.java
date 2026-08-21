package dev.codescape.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Minimal HTTP client for the codescape-service API. The MCP adapter is a
 * thin transport layer only — all source/Git/GitHub/index/build/workspace
 * logic lives in the service (see AGENTS.md). This client will grow method
 * by method alongside the service's API surface (Stage 8 of ROADMAP.md).
 *
 * <p>Responses are passed through as raw JSON text rather than parsed,
 * since this module deliberately has no JSON library dependency (JDK
 * {@link HttpClient} only — see AGENTS.md).
 */
public class CodescapeServiceClient {

    private final HttpClient httpClient;
    private final URI baseUri;

    public CodescapeServiceClient(URI baseUri) {
        this(baseUri, HttpClient.newHttpClient());
    }

    CodescapeServiceClient(URI baseUri, HttpClient httpClient) {
        this.baseUri = baseUri;
        this.httpClient = httpClient;
    }

    /** Confirms the service is reachable at {@code baseUri}. */
    public boolean isReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/sources"))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /** Returns the raw JSON array of registered sources from {@code GET /api/sources}. */
    public String listSources() {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/sources"))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Listing sources failed (HTTP " + response.statusCode() + "): " + response.body());
        }
        return response.body();
    }

    /** Registers a source via {@code POST /api/sources}, returning the raw JSON response. */
    public String addSource(String name, String sourcePath, String type) {
        String json = "{\"name\":\"%s\",\"sourcePath\":\"%s\",\"type\":\"%s\"}"
                .formatted(escapeJson(name), escapeJson(sourcePath), escapeJson(type));
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/sources"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Adding source failed (HTTP " + response.statusCode() + "): " + response.body());
        }
        return response.body();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling codescape-service", e);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
