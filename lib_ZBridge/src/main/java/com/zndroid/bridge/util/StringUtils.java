package com.zndroid.bridge.util;

public class StringUtils {

    private static final String EMPTY = "";

    public static String connect(String... elements) {
        return join(EMPTY, elements);
    }

    public static String join(String delimiter, String... elements) {
        if (elements == null) {
            return EMPTY;
        }
        int length = elements.length;
        if (length == 0) {
            return EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(elements[i]);
            if (i != length - 1) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }
}
