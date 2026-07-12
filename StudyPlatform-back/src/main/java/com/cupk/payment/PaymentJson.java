package com.cupk.payment;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支付模块JSON工具类，提供轻量级JSON构建和解析功能。
 */
final class PaymentJson {
    private PaymentJson() {
    }

    static String object(Map<String, ?> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            appendValue(builder, value);
        }
        return builder.append('}').toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendValue(StringBuilder builder, Object value) {
        if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object childValue = entry.getValue();
                if (entry.getKey() == null || childValue == null) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                appendValue(builder, childValue);
            }
            builder.append('}');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    static String stringValue(String json, String key) {
        if (json == null || key == null || key.isBlank()) {
            return "";
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? unescape(matcher.group(1)) : "";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescape(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}
