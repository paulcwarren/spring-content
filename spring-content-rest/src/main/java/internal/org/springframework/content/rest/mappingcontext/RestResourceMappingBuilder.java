package internal.org.springframework.content.rest.mappingcontext;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.mappingcontext.ClassVisitor;
import org.springframework.content.commons.mappingcontext.ClassWalker;
import org.springframework.content.commons.utils.ContentPropertyUtils;
import org.springframework.content.rest.RestResource;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RestResourceMappingBuilder implements ClassVisitor {

    private final Function<RestResource, String> segmentProvider;
    private Stack<String> segments = new Stack<>();
    private Stack<String> uriSegments = new Stack<>();
    private Map<String, String> mappings = new HashMap<>();

    private Map<String, Boolean> visited = new HashMap<>();
    private Map<String, Boolean> looseModes = new HashMap<>();

    public RestResourceMappingBuilder(Function<RestResource, String> segmentProvider) {
        this.segmentProvider = segmentProvider;
    }

    @Override
    public boolean visitClass(String path, Class<?> klazz) {
        visited.put(klazz + ":" + path(segments), false);
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
                String segment = segmentProvider.apply(restResource);
                if (StringUtils.hasLength(segment)) {
                    segments.push(segment);
                } else {
                    if (f.isAnnotationPresent(ContentId.class)) {
                        segments.push(ClassWalker.propertyName(f.getName()));
                    } else  {
                        segments.push(f.getName());
                    }
                }
            } else {
                if (f.isAnnotationPresent(ContentId.class)) {
                    segments.push(ClassWalker.propertyName(f.getName()));
                } else  {
                    segments.push(f.getName());
                }
            }
            if (f.isAnnotationPresent(ContentId.class)) {
                uriSegments.push(ClassWalker.propertyName(f.getName()));
            } else {
                uriSegments.push(f.getName());
            }
            visited.put(klazz + ":" + path(segments), true);
        }
        return true;
    }

    @Override
    public boolean visitField(String path, Class<?> klazz, Field f) {
        if (f.isAnnotationPresent(ContentId.class)) {
            mappings.put(path(uriSegments), path(segments));
        }

        return true;
    }

    @Override
    public boolean visitFieldAfter(String path, Class<?> klazz, Field f) {
        if (visited.get(klazz + ":" + path(segments)) == true) {
            segments.pop();
            uriSegments.pop();
        }
        return true;
    }

    @Override
    public boolean visitClassEnd(String path, Class<?> klazz) {
        return true;
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public Map<String, String> getInverseMappings() {
        return mappings.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
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
}
