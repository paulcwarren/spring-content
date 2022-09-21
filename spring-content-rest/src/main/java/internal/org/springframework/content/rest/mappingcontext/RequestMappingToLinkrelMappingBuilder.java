package internal.org.springframework.content.rest.mappingcontext;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.mappingcontext.ClassVisitor;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.content.rest.RestResource;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class RequestMappingToLinkrelMappingBuilder implements ClassVisitor {

    private Stack<String> segments = new Stack<>();
    private Stack<String> uriSegments = new Stack<>();
    private Map<String, String> paths = new HashMap<>();

    private Map<String, Boolean> pushed = new HashMap<>();
    private Map<String, Boolean> looseModes = new HashMap<>();

    public RequestMappingToLinkrelMappingBuilder() {}

    @Override
    public boolean visitClass(String path, Class<?> klazz) {
        pushed.put(klazz + ":" + path(segments), false);
        int numContentIds = 0;
        for (Field field : klazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ContentId.class)) {
                numContentIds++;
            }
        }
        looseModes.put(klazz + ":" + path(segments), numContentIds == 1);
        return true;
    }

    @Override
    public boolean visitFieldBefore(String path, Class<?> klazz, Field f) {
        if (isObject(f) ||
                (f.isAnnotationPresent(ContentId.class) && looseModes.get(klazz + ":" + path(segments)) == Boolean.FALSE ||
                 f.isAnnotationPresent(ContentId.class) && looseModes.get(klazz + ":" + path(segments)) == Boolean.TRUE && segments.size() == 0)) {
            RestResource restResource = f.getAnnotation(RestResource.class);
            if (restResource != null) {
                String linkrel = restResource.linkRel();
                if (StringUtils.hasLength(linkrel)) {
                    segments.push(linkrel);
                } else {
                    if (f.isAnnotationPresent(ContentId.class)) {
                        segments.push(propertyName(f.getName()));
                    } else  {
                        segments.push(f.getName());
                    }
                }
            } else {
                if (f.isAnnotationPresent(ContentId.class)) {
                    segments.push(propertyName(f.getName()));
                } else  {
                    segments.push(f.getName());
                }
            }
            if (f.isAnnotationPresent(ContentId.class)) {
                uriSegments.push(propertyName(f.getName()));
            } else {
                uriSegments.push(f.getName());
            }
            pushed.put(klazz + ":" + path(segments), true);
        }
        return true;
    }

    @Override
    public boolean visitField(String path, Class<?> klazz, Field f) {
        if (f.isAnnotationPresent(ContentId.class)) {
            paths.put(path(uriSegments), path(segments));
        }

        return true;
    }

    @Override
    public boolean visitFieldAfter(String path, Class<?> klazz, Field f) {
        if (pushed.get(klazz + ":" + path(segments)) == true) {
            segments.pop();
            uriSegments.pop();
        }
        return true;
    }

    @Override
    public boolean visitClassEnd(String path, Class<?> klazz) {
        return true;
    }

    public Map<String, String> getRequestMappings() {
        return paths;
    }

    private boolean isObject(Field field) {
        return field.getType().isPrimitive() == false &&
            field.getType().equals(String.class) == false &&
            field.getType().equals(UUID.class) == false &&
            field.getType().isEnum() == false &&
            ContentPropertyUtils.isWrapperType(field.getType()) == false &&
            ContentPropertyUtils.isRelationshipField(field) == false;
    }

    private String path(Stack<String> segments) {
        String fqPath = "";
        for (int i=0; i < segments.size(); i++) {
            if (i > 0) {
                fqPath += "/";
            }
            fqPath += segments.get(i);
        }
        return fqPath;
    }

    protected String propertyName(String name) {
        if (!StringUtils.hasLength(name)) {
            return name;
        }
        String[] segments = split(name);
        return segments[0];
    }

    private static String[] split(String name) {
        if (!StringUtils.hasLength(name)) {
            return new String[]{};
        }

        return name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }
}
