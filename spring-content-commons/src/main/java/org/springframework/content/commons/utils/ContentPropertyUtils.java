package org.springframework.content.commons.utils;

import java.lang.reflect.Field;
import java.util.UUID;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.data.mongodb.core.mapping.DBRef;

public final class ContentPropertyUtils {

    private static boolean IS_JPA_PRESENT = false;
    private static boolean IS_MONGO_PRESENT = false;

    static {
        try {
            IS_JPA_PRESENT = Class.forName("javax.persistence.OneToOne") != null;
            IS_MONGO_PRESENT = Class.forName("org.springframework.data.mongodb.core.mapping.DBRef") != null;
        } catch (ClassNotFoundException e) {
        }
    }


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


    public static boolean isRelationshipField(Field f) {

        if (IS_JPA_PRESENT) {
            if (f.getAnnotation(OneToOne.class) != null ||
                f.getAnnotation(OneToMany.class) != null ||
                f.getAnnotation(ManyToOne.class) != null ||
                f.getAnnotation(ManyToMany.class) != null) {
                return true;
            }
        }
        if (IS_MONGO_PRESENT) {
            if (f.getAnnotation(DBRef.class) != null) {
                return true;
            }
        }
        return false;
    }
}
