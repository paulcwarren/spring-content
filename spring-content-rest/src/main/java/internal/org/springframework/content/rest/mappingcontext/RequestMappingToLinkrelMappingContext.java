package internal.org.springframework.content.rest.mappingcontext;

import org.springframework.content.commons.mappingcontext.ClassWalker;

import java.util.HashMap;
import java.util.Map;

public class RequestMappingToLinkrelMappingContext {

    private Map<Class<?>, Map<String, String>> mappings = new HashMap<Class<?>, Map<String,String>>();

    public RequestMappingToLinkrelMappingContext() {}

    public Map<String, String> getMappings(Class<?> domainClass) {

        Map<String, String> mappings = this.mappings.get(domainClass);
        if (mappings == null) {
            mappings = resolveMappings(domainClass);
        }
        return mappings;
    }

    private Map<String,String> resolveMappings(Class<?> domainClass) {
        RequestMappingToLinkrelMappingBuilder visitor = new RequestMappingToLinkrelMappingBuilder();
        ClassWalker walker = new ClassWalker(visitor);
        walker.accept(domainClass);
        Map<String,String> mappings = visitor.getRequestMappings();
        this.mappings.put(domainClass, mappings);
        return mappings;
    }
}
