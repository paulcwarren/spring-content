package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;

public interface ClassVisitor {

    boolean visitClass(String path, Class<?> klazz);

    default boolean visitFieldBefore(String path, Class<?> klazz, Field f) {
        return true;
    }
    boolean visitField(String path, Class<?> klazz, Field f);

    default boolean visitFieldAfter(String path, Class<?> klazz, Field f) {
        return true;
    }

    boolean visitClassEnd(String path, Class<?> klazz);
}
