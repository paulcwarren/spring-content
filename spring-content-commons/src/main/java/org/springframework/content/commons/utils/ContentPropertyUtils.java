package org.springframework.content.commons.utils;

import java.util.UUID;

public final class ContentPropertyUtils {

    private ContentPropertyUtils() {}


    public static boolean isPrimitiveContentPropertyClass(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.equals(UUID.class) || clazz.equals(String.class);
    }
}
