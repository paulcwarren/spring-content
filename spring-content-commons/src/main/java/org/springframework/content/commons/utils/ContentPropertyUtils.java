package org.springframework.content.commons.utils;

import java.util.UUID;

public final class ContentPropertyUtils {

    private ContentPropertyUtils() {}


    public static boolean isPrimitiveContentPropertyClass(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.equals(UUID.class) || clazz.equals(String.class);
    }

    public static boolean isWrapperType(Class<?> type) {

        if (Boolean.class.equals(type) ||
            Byte.class.equals(type) ||
            Character.class.equals(type) ||
            Double.class.equals(type) ||
            Float.class.equals(type) ||
            Integer.class.equals(type) ||
            Long.class.equals(type) ||
            Short.class.equals(type)) {
            return true;
        }

        return false;
    }
}
