package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.NativeWebRequest;

import internal.org.springframework.content.rest.io.AssociatedStoreResourceImpl;

public class AssociativeStoreResourceResolver implements ResourceResolver {

    private MappingContext mappingContext;

    public AssociativeStoreResourceResolver(MappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    public String getMapping() {
        return "/{repository}/{id}/**";
    }

    @Override
    public Resource resolve(NativeWebRequest nativeWebRequest, StoreInfo info, Object domainObj, PropertyPath propertyPath) {
        GetResourceParams params = GetResourceParams.builder().range(nativeWebRequest.getHeader("Range")).build();
        Resource r = info.getImplementation(AssociativeStore.class).getResource(domainObj, propertyPath, params);
        return new AssociatedStoreResourceImpl(info, domainObj, propertyPath, mappingContext.getContentProperty(domainObj.getClass(), propertyPath.getName()), r);
    }
}