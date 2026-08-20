package dev.codescape.mcp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Minimal HTTP client for the codescape-service API. The MCP adapter is a
 * thin transport layer only — all source/Git/GitHub/index/build/workspace
 * logic lives in the service (see AGENTS.md). This client will grow method
 * by method alongside the service's API surface (Stage 8 of ROADMAP.md).
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
}
