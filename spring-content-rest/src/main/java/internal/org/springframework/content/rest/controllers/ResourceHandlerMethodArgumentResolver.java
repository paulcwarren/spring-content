package internal.org.springframework.content.rest.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.io.AssociatedResource;
import internal.org.springframework.content.rest.io.AssociatedResourceImpl;
import internal.org.springframework.content.rest.io.RenderableResourceImpl;
import internal.org.springframework.content.rest.utils.StoreUtils;

public class ResourceHandlerMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ResourceHandlerMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores) {
        super(config, repositories, repoInvokerFactory, stores);
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

      return info.getImplementation(Store.class).getResource(pathToUse);
    }

    @Override
    protected Object resolveAssociativeStoreEntityArgument(StoreInfo storeInfo, Object domainObj) {

        Resource r = storeInfo.getImplementation(AssociativeStore.class).getResource(domainObj);
        r = new AssociatedResourceImpl(domainObj, r);

        if (Renderable.class.isAssignableFrom(storeInfo.getInterface())) {
            r = new RenderableResourceImpl((Renderable)storeInfo.getImplementation(AssociativeStore.class), (AssociatedResource)r);
        }
        return r;
    }

    @Override
    protected Object resolveAssociativeStorePropertyArgument(StoreInfo storeInfo, Object domainObj, Object propertyVal, boolean embeddedProperty) {

        AssociativeStore s = storeInfo.getImplementation(AssociativeStore.class);
        Resource resource = s.getResource(propertyVal);
        resource = new AssociatedResourceImpl(propertyVal, resource);
        if (Renderable.class.isAssignableFrom(storeInfo.getInterface())) {
            resource = new RenderableResourceImpl((Renderable)storeInfo.getImplementation(AssociativeStore.class), (AssociatedResource)resource);
        }
        return resource;
    }
}
