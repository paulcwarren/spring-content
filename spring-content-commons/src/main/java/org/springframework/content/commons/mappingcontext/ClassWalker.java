package org.springframework.content.commons.mappingcontext;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassWalker {

    private static final Log LOGGER = LogFactory.getLog(ClassWalker.class);

    private ClassVisitor visitor;

    public ClassWalker(ClassVisitor visitor) {
        this.visitor = visitor;
    }

    public static String propertyName(String name) {
        if (!StringUtils.hasLength(name)) {
            return name;
        }

        String propertyName = calculateName(name);
        if (propertyName != null) {
            return propertyName;
        }

        String[] segments = split(name);
        if (segments.length == 1) {
            return segments[0];
        }
        else {
            StringBuilder b = new StringBuilder();
            for (int i=0; i < segments.length - 1; i++) {
                b.append(segments[i]);
            }
            return b.toString();
        }
    }

    public static String calculateName(String name) {
        Pattern p = Pattern.compile("^(.+)(Id|Len|Length|MimeType|Mimetype|ContentType|(?<!Mime|Content)Type|(?<!Original)FileName|(?<!Original)Filename|OriginalFileName|OriginalFilename)$");
        Matcher m = p.matcher(name);
        if (m.matches() == false) {
            return null;
        }
        return m.group(1);
    }

    public static String[] split(String name) {
        if (!StringUtils.hasLength(name)) {
            return new String[]{};
        }

        return name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
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
            fContinue &= visitor.visitFieldBefore("", klazz, field);
            fContinue &= visitor.visitField("", klazz, field);
            if (isObject(field)) {
                if (!contains(classStack, field.getType())) {
                    classStack.push(new WalkContext(field.getName(), field.getType()));
                    this.accept(field.getType(), classStack);
                    classStack.pop();
                }
            }
            fContinue &= visitor.visitFieldAfter("", klazz, field);
        }
        if (!fContinue) {
            return;
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
            fContinue &= visitor.visitFieldBefore("", klazz, field);
            fContinue &= visitor.visitField(context.getPath(), klazz, field);
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
            fContinue &= visitor.visitFieldAfter("", klazz, field);
        }
        if (!fContinue) {
            return;
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
