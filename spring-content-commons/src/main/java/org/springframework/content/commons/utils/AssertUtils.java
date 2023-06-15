package org.springframework.content.commons.utils;

public final class AssertUtils {
    private AssertUtils() {}

    public static void atLeastOneNotNull(Object[] objects, String message) {
        boolean isNull = true;
        for (Object o : objects) {
            isNull &= o == null;
        }
        if (isNull) {
            throw new IllegalArgumentException(message);
        }
    }
}
