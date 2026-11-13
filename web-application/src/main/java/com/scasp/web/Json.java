package com.scasp.web;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON writer for the mobile API (no extra library on the server). Supports maps (objects), collections
 * (arrays), strings, numbers, booleans, enums, java.time values (ISO strings) and null. Strings are escaped per
 * RFC 8259, including &lt; &gt; &amp; so a response can never be interpreted as HTML.
 */
public final class Json {
    private Json() {}

    /** Ordered object builder: Json.obj("a", 1, "b", "x"). */
    public static Map<String, Object> obj(Object... keyValues) {
        if (keyValues.length % 2 != 0) throw new IllegalArgumentException("key/value pairs expected");
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) m.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        return m;
    }

    public static String write(Object value) { StringBuilder sb = new StringBuilder(); write(sb, value); return sb.toString(); }

    private static void write(StringBuilder sb, Object v) {
        if (v == null) sb.append("null");
        else if (v instanceof String s) quote(sb, s);
        else if (v instanceof Boolean b) sb.append(b);
        else if (v instanceof Double d && (d.isNaN() || d.isInfinite())) sb.append("null");
        else if (v instanceof Number n) sb.append(n);
        else if (v instanceof Enum<?> e) quote(sb, e.name());
        else if (v instanceof TemporalAccessor t) quote(sb, t.toString());
        else if (v instanceof Map<?, ?> m) {
            sb.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(','); first = false;
                quote(sb, String.valueOf(e.getKey())); sb.append(':'); write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('['); boolean first = true;
            for (Object o : c) { if (!first) sb.append(','); first = false; write(sb, o); }
            sb.append(']');
        } else quote(sb, v.toString());
    }

    static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<', '>', '&', '\u2028', '\u2029' -> sb.append(String.format("\\u%04x", (int) c));
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        sb.append('"');
    }
}
