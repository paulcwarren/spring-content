package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.NativeWebRequest;

public interface ResourceResolver {
    public String getMapping();
    public Resource resolve(NativeWebRequest nativeWebRequest, StoreInfo info, Object domainObj, ContentProperty property);
}