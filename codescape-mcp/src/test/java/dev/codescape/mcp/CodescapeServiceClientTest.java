package dev.codescape.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class CodescapeServiceClientTest {

    @Test
    void reportsUnreachableWhenNothingIsListening() {
        CodescapeServiceClient client = new CodescapeServiceClient(URI.create("http://localhost:1"));

        assertThat(client.isReachable()).isFalse();
    }
}
