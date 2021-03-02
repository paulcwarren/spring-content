package internal.org.springframework.content.rest.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.io.AssociatedStorePropertyResourceImpl;
import internal.org.springframework.content.rest.io.AssociatedStoreResourceImpl;
import internal.org.springframework.content.rest.io.StoreResourceImpl;
import internal.org.springframework.content.rest.utils.StoreUtils;

public class ResourceHandlerMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ResourceHandlerMethodArgumentResolver(ApplicationContext context, RestConfiguration config, Repositories repositories, Stores stores) {
        super(context, config, repositories, stores);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return Resource.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        return super.resolveArgument(methodParameter, modelAndViewContainer, nativeWebRequest, webDataBinderFactory);
    }

    @Override
    protected Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info) {
      String path = new UrlPathHelper().getPathWithinApplication(nativeWebRequest.getNativeRequest(HttpServletRequest.class));
      String pathToUse = path.substring(StoreUtils.storePath(info).length() + 1);

      return new StoreResourceImpl(info, info.getImplementation(Store.class).getResource(pathToUse));
    }

    @Override
    protected Object resolveAssociativeStoreEntityArgument(StoreInfo storeInfo, Object domainObj) {

        Resource r = storeInfo.getImplementation(AssociativeStore.class).getResource(domainObj);
        r = new AssociatedStoreResourceImpl(storeInfo, domainObj, r);
        return r;
    }

    @Override
    protected Object resolveAssociativeStorePropertyArgument(StoreInfo storeInfo, Object domainObj, Object propertyVal, boolean embeddedProperty) {

        AssociativeStore s = storeInfo.getImplementation(AssociativeStore.class);
        Resource resource = s.getResource(propertyVal);
        resource = new AssociatedStorePropertyResourceImpl(storeInfo, propertyVal, embeddedProperty, domainObj, resource);
        return resource;
    }
}
