package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;

public interface ClassVisitor {

    boolean visitClass(String path, Class<?> klazz);
    boolean visitField(String path, Class<?> klazz, Field f);
    boolean visitClassEnd(String path, Class<?> klazz);
}
