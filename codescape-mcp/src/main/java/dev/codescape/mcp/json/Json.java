package dev.codescape.mcp.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser/writer. codescape-mcp deliberately has no JSON
 * library dependency (JDK HttpClient only — see AGENTS.md), but real
 * JSON-RPC framing needs real JSON, not regex. This is scoped to what
 * JSON-RPC/MCP messages actually need: objects, arrays, strings, numbers,
 * booleans, null.
 *
 * <p>Parses into {@code Map<String,Object>}, {@code List<Object>},
 * {@code String}, {@code Long}/{@code Double}, {@code Boolean}, or
 * {@code null}. {@link Raw} lets the writer splice in an already-valid
 * JSON string verbatim, so a response body received from the service
 * (already JSON text) doesn't need to be parsed and re-serialized.
 */
public final class Json {

    private Json() {
    }

    /** Wraps a string containing already-valid JSON, written verbatim. */
    public record Raw(String json) {
    }

    public static Object parse(String input) {
        Parser parser = new Parser(input);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new JsonParseException("Unexpected trailing content at position " + parser.pos);
        }
        return value;
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder out) {
        switch (value) {
            case null -> out.append("null");
            case Raw raw -> out.append(raw.json());
            case String s -> writeString(s, out);
            case Boolean b -> out.append(b.toString());
            case Number n -> out.append(n.toString());
            case Map<?, ?> map -> {
                out.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    writeString(String.valueOf(entry.getKey()), out);
                    out.append(':');
                    writeValue(entry.getValue(), out);
                }
                out.append('}');
            }
            case List<?> list -> {
                out.append('[');
                boolean first = true;
                for (Object item : list) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    writeValue(item, out);
                }
                out.append(']');
            }
            default -> throw new IllegalArgumentException("Cannot serialize value of type " + value.getClass());
        }
    }

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }

    private static final class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
        }

        boolean atEnd() {
            return pos >= input.length();
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        char peek() {
            if (atEnd()) {
                throw new JsonParseException("Unexpected end of input at position " + pos);
            }
            return input.charAt(pos);
        }

        char next() {
            char c = peek();
            pos++;
            return c;
        }

        void expect(char c) {
            char actual = next();
            if (actual != c) {
                throw new JsonParseException("Expected '" + c + "' but got '" + actual + "' at position " + (pos - 1));
            }
        }

        Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    break;
                }
                if (c != ',') {
                    throw new JsonParseException("Expected ',' or '}' at position " + (pos - 1));
                }
            }
            return result;
        }

        List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    break;
                }
                if (c != ',') {
                    throw new JsonParseException("Expected ',' or ']' at position " + (pos - 1));
                }
                skipWhitespace();
            }
            return result;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            String hex = input.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new JsonParseException("Invalid escape '\\" + esc + "' at position " + (pos - 1));
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (input.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonParseException("Invalid literal at position " + pos);
        }

        Object parseNull() {
            if (input.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonParseException("Invalid literal at position " + pos);
        }

        Number parseNumber() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            boolean isFloat = false;
            while (!atEnd() && (Character.isDigit(peek()) || peek() == '.' || peek() == 'e' || peek() == 'E' || peek() == '+' || peek() == '-')) {
                char c = input.charAt(pos);
                if (c == '.' || c == 'e' || c == 'E') {
                    isFloat = true;
                }
                pos++;
            }
            String text = input.substring(start, pos);
            if (text.isEmpty() || "-".equals(text)) {
                throw new JsonParseException("Invalid number at position " + start);
            }
            // Not a ternary: `cond ? Double.parseDouble(x) : Long.parseLong(x)`
            // triggers binary numeric promotion on the conditional
            // expression, silently widening the long branch to double
            // regardless of which branch is actually taken (JLS 15.25).
            if (isFloat) {
                return Double.parseDouble(text);
            }
            return Long.parseLong(text);
        }
    }
}
