package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Looks values up in JSON by a dotted path such as {@code items[0].name}. Never throws: a path that does
 * not lead anywhere yields {@code null}.
 */
public final class JsonPaths {

    private JsonPaths() {
    }

    /** The node at {@code path}, or null if there is none. An empty path returns {@code root}. */
    public static JsonNode get(JsonNode root, String path) {
        if (root == null) {
            return null;
        }
        if (path == null || path.isBlank()) {
            return root;
        }
        JsonNode cur = root;
        int i = 0;
        int n = path.length();
        while (i < n) {
            char c = path.charAt(i);
            if (c == '.') {
                i++;
            } else if (c == '[') {
                int end = path.indexOf(']', i);
                if (end < 0) {
                    return null;
                }
                int idx;
                try {
                    idx = Integer.parseInt(path.substring(i + 1, end).trim());
                } catch (NumberFormatException e) {
                    return null;
                }
                if (!cur.isArray() || idx < 0 || idx >= cur.size()) {
                    return null;
                }
                cur = cur.get(idx);
                i = end + 1;
            } else {
                int j = i;
                while (j < n && path.charAt(j) != '.' && path.charAt(j) != '[') {
                    j++;
                }
                String name = path.substring(i, j);
                if (!cur.isObject() || !cur.has(name)) {
                    return null;
                }
                cur = cur.get(name);
                i = j;
            }
        }
        return cur;
    }

    /** The first segment of a path: {@code items[0].x} gives {@code items}. */
    public static String firstSegment(String path) {
        int i = 0;
        while (i < path.length() && path.charAt(i) != '.' && path.charAt(i) != '[') {
            i++;
        }
        return path.substring(0, i);
    }

    /** Everything after the first segment, without a leading dot: {@code a.b[0]} gives {@code b[0]}. */
    public static String rest(String path) {
        int i = firstSegment(path).length();
        if (i < path.length() && path.charAt(i) == '.') {
            i++;
        }
        return path.substring(i);
    }
}
