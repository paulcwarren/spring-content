package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.NativeWebRequest;

import internal.org.springframework.content.rest.io.AssociatedStoreResourceImpl;

public class StoreResourceResolver implements ResourceResolver {

    private MappingContext mappingContext;

    public StoreResourceResolver(MappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    public String getMapping() {
        return "/{repository}/{id}";
    }

    @Override
    public Resource resolve(NativeWebRequest nativeWebRequest, StoreInfo info, Object domainObj, PropertyPath property) {
        GetResourceParams params = GetResourceParams.builder().range(nativeWebRequest.getHeader("Range")).build();
        Resource r = info.getImplementation(AssociativeStore.class).getResource(domainObj, property, params);
        return new AssociatedStoreResourceImpl(info, domainObj, property, mappingContext.getContentProperty(domainObj.getClass(), property.getName()), r);
    }
}