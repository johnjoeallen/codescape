package dev.codescape.mcp.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesJsonRpcRequestShape() {
        Object parsed = Json.parse("""
                {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_sources","arguments":{}}}""");

        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) parsed;
        assertThat(obj.get("jsonrpc")).isEqualTo("2.0");
        assertThat(obj.get("id")).isEqualTo(1L);
        assertThat(obj.get("method")).isEqualTo("tools/call");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) obj.get("params");
        assertThat(params.get("name")).isEqualTo("list_sources");
    }

    @Test
    void parsesStringEscapesAndUnicode() {
        Object parsed = Json.parse("\"line1\\nline2\\t\\u0041\\\"quoted\\\"\"");
        assertThat(parsed).isEqualTo("line1\nline2\tA\"quoted\"");
    }

    @Test
    void parsesIntegerAndFloatNumbersDistinctly() {
        assertThat(Json.parse("42")).isEqualTo(42L);
        assertThat(Json.parse("-7")).isEqualTo(-7L);
        assertThat(Json.parse("3.14")).isEqualTo(3.14);
        assertThat(Json.parse("1e3")).isEqualTo(1000.0);
    }

    @Test
    void parsesBooleansAndNull() {
        assertThat(Json.parse("true")).isEqualTo(Boolean.TRUE);
        assertThat(Json.parse("false")).isEqualTo(Boolean.FALSE);
        assertThat(Json.parse("null")).isNull();
    }

    @Test
    void parsesNestedArraysAndObjects() {
        Object parsed = Json.parse("{\"a\":[1,2,{\"b\":true}],\"c\":[]}");
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) parsed;
        @SuppressWarnings("unchecked")
        List<Object> a = (List<Object>) obj.get("a");
        assertThat(a).hasSize(3);
        assertThat(a.get(0)).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<Object> c = (List<Object>) obj.get("c");
        assertThat(c).isEmpty();
    }

    @Test
    void rejectsTrailingContent() {
        assertThatThrownBy(() -> Json.parse("{}garbage"))
                .isInstanceOf(Json.JsonParseException.class);
    }

    @Test
    void writesObjectsAndEscapesStrings() {
        String written = Json.write(Map.of("msg", "hello \"world\"\n"));
        assertThat(written).isEqualTo("{\"msg\":\"hello \\\"world\\\"\\n\"}");
    }

    @Test
    void writesIntegerIdsWithoutTrailingDecimal() {
        assertThat(Json.write(Map.of("id", 1L))).isEqualTo("{\"id\":1}");
    }

    @Test
    void rawIsSplicedVerbatimWithoutReEncoding() {
        String written = Json.write(Map.of("body", new Json.Raw("[{\"name\":\"x\"}]")));
        assertThat(written).isEqualTo("{\"body\":[{\"name\":\"x\"}]}");
    }

    @Test
    void roundTripsThroughParseAndWrite() {
        String original = "{\"a\":1,\"b\":[true,false,null],\"c\":\"x\"}";
        Object parsed = Json.parse(original);
        String rewritten = Json.write(parsed);
        assertThat(Json.parse(rewritten)).isEqualTo(parsed);
    }
}
