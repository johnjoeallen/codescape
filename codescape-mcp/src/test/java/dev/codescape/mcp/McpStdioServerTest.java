package dev.codescape.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.codescape.mcp.json.Json;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises McpStdioServer's JSON-RPC handling end to end, against a real
 * (if trivial) HTTP server standing in for codescape-service — not a mock
 * of CodescapeServiceClient — so this also proves the request/response
 * shapes actually round-trip through real JSON, not just through Java
 * objects we constructed ourselves.
 */
class McpStdioServerTest {

    private HttpServer fakeService;
    private McpStdioServer server;

    @BeforeEach
    void startFakeService() throws Exception {
        fakeService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        fakeService.createContext("/api/sources", exchange -> {
            byte[] body = "[{\"name\":\"orders-api\",\"type\":\"GIT\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        fakeService.start();

        URI serviceUri = URI.create("http://localhost:" + fakeService.getAddress().getPort());
        server = new McpStdioServer(new CodescapeServiceClient(serviceUri));
    }

    @AfterEach
    void stopFakeService() {
        fakeService.stop(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> run(String... requestLines) {
        String input = String.join("\n", requestLines) + "\n";
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        server.run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), new PrintStream(outBytes, true, StandardCharsets.UTF_8));

        String output = outBytes.toString(StandardCharsets.UTF_8);
        if (output.isEmpty()) {
            return List.of();
        }
        return output.lines()
                .map(line -> (Map<String, Object>) Json.parse(line))
                .toList();
    }

    @Test
    void initializeEchoesRequestedProtocolVersionAndAdvertisesToolsCapability() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26"}}""");

        assertThat(responses).hasSize(1);
        Map<String, Object> result = (Map<String, Object>) responses.get(0).get("result");
        assertThat(result.get("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(result.get("capabilities")).isEqualTo(Map.of("tools", Map.of()));
        assertThat(((Map<String, Object>) result.get("serverInfo")).get("name")).isEqualTo("codescape-mcp");
    }

    @Test
    void notificationsInitializedGetsNoResponse() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","method":"notifications/initialized"}""");

        assertThat(responses).isEmpty();
    }

    @Test
    void toolsListAdvertisesExactlyListSources() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}""");

        Map<String, Object> result = (Map<String, Object>) responses.get(0).get("result");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).get("name")).isEqualTo("list_sources");
    }

    @Test
    void toolsCallListSourcesReturnsServiceDataAsTextContent() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_sources","arguments":{}}}""");

        Map<String, Object> result = (Map<String, Object>) responses.get(0).get("result");
        assertThat(result.get("isError")).isEqualTo(Boolean.FALSE);
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("type")).isEqualTo("text");
        assertThat((String) content.get(0).get("text")).contains("orders-api");
    }

    @Test
    void toolsCallUnknownToolReturnsJsonRpcError() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"nonexistent_tool"}}""");

        Map<String, Object> error = (Map<String, Object>) responses.get(0).get("error");
        assertThat(error.get("code")).isEqualTo(-32602L);
        assertThat((String) error.get("message")).contains("nonexistent_tool");
    }

    @Test
    void unknownMethodReturnsMethodNotFoundError() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","id":5,"method":"totally/unknown"}""");

        Map<String, Object> error = (Map<String, Object>) responses.get(0).get("error");
        assertThat(error.get("code")).isEqualTo(-32601L);
    }

    @Test
    void malformedJsonReturnsParseErrorWithNullId() {
        List<Map<String, Object>> responses = run("{not valid json");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).get("id")).isNull();
        Map<String, Object> error = (Map<String, Object>) responses.get(0).get("error");
        assertThat(error.get("code")).isEqualTo(-32700L);
    }

    @Test
    void multipleRequestsOnSeparateLinesEachGetAResponseInOrder() {
        List<Map<String, Object>> responses = run(
                """
                {"jsonrpc":"2.0","id":1,"method":"ping"}""",
                """
                {"jsonrpc":"2.0","id":2,"method":"ping"}""");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).get("id")).isEqualTo(1L);
        assertThat(responses.get(1).get("id")).isEqualTo(2L);
    }
}
