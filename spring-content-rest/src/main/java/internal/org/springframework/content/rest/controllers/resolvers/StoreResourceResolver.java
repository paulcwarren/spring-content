package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.NativeWebRequest;

import internal.org.springframework.content.rest.io.AssociatedStorePropertyPathResourceImpl;

public class StoreResourceResolver implements ResourceResolver {

    @Override
    public String getMapping() {
        return "/{repository}/{id}";
    }

    @Override
    public Resource resolve(NativeWebRequest nativeWebRequest, StoreInfo info, Object domainObj, ContentProperty property) {
        Resource r = info.getImplementation(AssociativeStore.class).getResource(domainObj, PropertyPath.from(property.getContentPropertyPath()));
        return new AssociatedStorePropertyPathResourceImpl(info, property, domainObj, r);
    }
}