package internal.org.springframework.content.rest.mappingcontext;

import org.springframework.content.commons.mappingcontext.ClassWalker;

import java.util.HashMap;
import java.util.Map;

public class ContentPropertyToLinkrelMappingContext {

    private Map<Class<?>, Map<String, String>> mappings = new HashMap<Class<?>, Map<String,String>>();

    public ContentPropertyToLinkrelMappingContext() {}

    public Map<String, String> getMappings(Class<?> domainClass) {

        Map<String, String> mappings = this.mappings.get(domainClass);
        if (mappings == null) {
            mappings = resolveMappings(domainClass);
        }
        return mappings;
    }

    private Map<String,String> resolveMappings(Class<?> domainClass) {
        RestResourceMappingBuilder visitor = new RestResourceMappingBuilder((rr)->rr.linkRel());
        ClassWalker walker = new ClassWalker(visitor);
        walker.accept(domainClass);
        Map<String,String> mappings = visitor.getMappings();
        this.mappings.put(domainClass, mappings);
        return mappings;
    }
}
