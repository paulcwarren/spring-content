package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ClassWalker {

    private static final Log LOGGER = LogFactory.getLog(ClassWalker.class);

    private ClassVisitor visitor;

    public ClassWalker(ClassVisitor visitor) {
        this.visitor = visitor;
    }

    public void accept(Class<?> klazz) {

        boolean fContinue = true;
        Stack<WalkContext> classStack = new Stack<>();

        fContinue &= visitor.visitClass("", klazz);
        if (!fContinue) {
            return;
        }

        List<Field>fields = new ArrayList<>();
        fields = getAllFields(fields, klazz);

        for (Field field : fields) {
            if (!isObject(field)) {
                fContinue &= visitor.visitField("", klazz, field);
            }
        }
        if (!fContinue) {
            return;
        }

        for (Field field : fields) {
            if (isObject(field)) {
                if (!contains(classStack, field.getType())) {
                    classStack.push(new WalkContext(field.getName(), field.getType()));
                    this.accept(field.getType(), classStack);
                    classStack.pop();
                }
            }
        }
        if (!fContinue) {
            return;
        }

        visitor.visitClassEnd("", klazz);
    }

    public void accept(Class<?> klazz, Stack<WalkContext> classStack) {

        boolean fContinue = true;

        WalkContext context = classStack.peek();

        fContinue &= visitor.visitClass(context.getPath(), klazz);
        if (!fContinue) {
            return;
        }

        List<Field>fields = new ArrayList<>();
        fields = getAllFields(fields, context.getClazz());

        for (Field field : fields) {
            if (!isObject(field)) {
                fContinue &= visitor.visitField(context.getPath(), klazz, field);
            }
        }
        if (!fContinue) {
            return;
        }

        for (Field field : fields) {
            if (isObject(field)) {
                if (!contains(classStack, field.getType())) {
                    String path = field.getName();
                    if (StringUtils.hasLength(context.getPath())) {
                        path = String.format("%s/%s", context.getPath(), path);
                    }
                    classStack.push(new WalkContext(path, field.getType()));
                    this.accept(field.getType(), classStack);
                    classStack.pop();
                }
            }
        }
        if (!fContinue) {
            return;
        }

        visitor.visitClassEnd(context.getPath(), klazz);
    }

    private boolean contains(Stack<WalkContext> classStack, Class<?> type) {

        for (WalkContext context : classStack) {
            if (context.getClazz().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isObject(Field field) {
        return field.getType().isPrimitive() == false &&
            field.getType().equals(String.class) == false &&
            field.getType().equals(UUID.class) == false &&
            field.getType().isEnum() == false &&
            ContentPropertyUtils.isWrapperType(field.getType()) == false &&
            ContentPropertyUtils.isRelationshipField(field) == false;
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    @Getter
    @AllArgsConstructor
    public static class WalkContext {

        private String path;
        private Class<?> clazz;
    }
}
