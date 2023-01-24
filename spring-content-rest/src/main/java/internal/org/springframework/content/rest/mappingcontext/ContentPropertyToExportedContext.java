package internal.org.springframework.content.rest.mappingcontext;

import org.springframework.content.commons.mappingcontext.ClassWalker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ContentPropertyToExportedContext {

    private Map<Class<?>, Map<String, Boolean>> mappings = new HashMap<Class<?>, Map<String,Boolean>>();

    public Map<String, Boolean> getMappings(Class<?> domainClass) {

        Map<String, Boolean> mappings = this.mappings.get(domainClass);
        if (mappings == null) {
            mappings = resolveMappings(domainClass);
        }
        return mappings;
    }

    private Map<String,Boolean> resolveMappings(Class<?> domainClass) {
        RestResourceMappingBuilder visitor = new RestResourceMappingBuilder((restResource -> String.valueOf(restResource.exported())));
        ClassWalker walker = new ClassWalker(visitor);
        walker.accept(domainClass);
        Map<String,String> mappings = visitor.getMappings();
        this.mappings.put(domainClass, collapse(mappings));
        return this.mappings.get(domainClass);
    }

    private Map<String, Boolean> collapse(Map<String, String> mappings) {
        Map<String, Boolean> collapsedMappings = new HashMap<>();
        for (Map.Entry<String,String> entry : mappings.entrySet()) {
            Boolean exported = true;
            String[] segments = entry.getValue().split("/");
            for (int i=0; i < segments.length; i++) {
                exported = exported & valueOf(segments[i]);
            }
            collapsedMappings.put(entry.getKey(), exported);
        }
        return collapsedMappings;
    }

    private Boolean valueOf(String segment) {
        if (segment.toLowerCase(Locale.ROOT).equals("false")) {
            return false;
        }
        return true;
    }
}
