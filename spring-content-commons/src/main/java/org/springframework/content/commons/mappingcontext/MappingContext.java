package org.springframework.content.commons.mappingcontext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;

public class MappingContext {

    private Map<Class<?>, Map<String, ContentProperty>> context = new HashMap<>();

    private CharSequence keySeparator = "/";
    private CharSequence contentPropertySeparator = ".";

    public MappingContext(CharSequence keySeparator, CharSequence contentPropertySeparator) {
        this.keySeparator = keySeparator;
        this.contentPropertySeparator = contentPropertySeparator;
    }

    public MappingContext(Stores stores) {
        for (StoreInfo info : stores.getStores(Stores.MATCH_ALL)) {
            if (info.getDomainObjectClass() != null) {
                context.put(info.getDomainObjectClass(), resolveProperties(info.getDomainObjectClass()));
            }
        }
    }

    public boolean hasMapping(Class<?> domainClass, String path) {
        Map<String, ContentProperty> properties = context.get(domainClass);
        if (properties == null) {
            properties = resolveProperties(domainClass);
        }
        return properties.get(path) != null;
    }

    public ContentProperty getContentProperty(Class<?> domainClass, String path) {
        Map<String, ContentProperty> properties = context.get(domainClass);
        if (properties == null) {
            properties = resolveProperties(domainClass);
        }
        return properties.get(path);
    }

    public Collection<ContentProperty> getContentProperties(Class<?> domainClass) {
        Map<String, ContentProperty> properties = context.get(domainClass);
        if (properties == null) {
            properties = resolveProperties(domainClass);
        }
        return properties.values();
    }

    private Map<String, ContentProperty> resolveProperties(Class<?> domainClass) {
        ContentPropertyBuilderVisitor visitor = new ContentPropertyBuilderVisitor(this.keySeparator, this.contentPropertySeparator, new ContentPropertyBuilderVisitor.CanonicalName());
        ClassWalker walker = new ClassWalker(domainClass);
        walker.accept(visitor);
        context.put(domainClass, visitor.getProperties());
        return visitor.getProperties();
    }
}
