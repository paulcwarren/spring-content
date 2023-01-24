package internal.org.springframework.content.rest.mappingcontext;

import org.springframework.content.commons.mappingcontext.ClassWalker;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ContentPropertyToRequestMappingContext {

    private Map<Class<?>, Map<String, String>> mappings = new HashMap<Class<?>, Map<String,String>>();

    public ContentPropertyToRequestMappingContext() {}

    public Map<String, String> getMappings(Class<?> domainClass) {

        Map<String, String> mappings = this.mappings.get(domainClass);
        if (mappings == null) {
            mappings = resolveMappings(domainClass);
        }
        return mappings;
    }

    public boolean hasInverseMapping(Class<?> domainClass, String requestSubPath) {
        Map<String, String> mappings = this.mappings.get(domainClass);
        if (mappings == null) {
            mappings = resolveMappings(domainClass);
        }
        return mappings.containsValue(requestSubPath);
    }

    public Map<String, String> getInverseMappings(Class<?> domainClass) {

        Map<String, String> mappings = this.mappings.get(domainClass);
        if (mappings == null) {
            mappings = resolveMappings(domainClass);
        }
        return mappings.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    private Map<String,String> resolveMappings(Class<?> domainClass) {
        RestResourceMappingBuilder visitor = new RestResourceMappingBuilder((restResource -> restResource.path()));
        ClassWalker walker = new ClassWalker(visitor);
        walker.accept(domainClass);
        Map<String,String> mappings = visitor.getMappings();
        this.mappings.put(domainClass, mappings);
        return mappings;
    }

    public String resolveContentPropertyPath(Class<?> domainClass, String contentPropertyPath) {
        if (hasInverseMapping(domainClass, contentPropertyPath)) {
            return getInverseMappings(domainClass).get(contentPropertyPath);
        }
        return contentPropertyPath;
    }
}
