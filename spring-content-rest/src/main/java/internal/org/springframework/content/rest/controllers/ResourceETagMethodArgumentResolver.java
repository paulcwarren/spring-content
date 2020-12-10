package internal.org.springframework.content.rest.controllers;

import javax.persistence.Version;

import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ResourceETagMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ResourceETagMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores) {
        super(config, repositories, repoInvokerFactory, stores);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return "resourceETag".equals(methodParameter.getParameterName());
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        return super.resolveArgument(methodParameter, modelAndViewContainer, nativeWebRequest, webDataBinderFactory);
    }

    @Override
    protected Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info) {

        // do store resource resolution
        return "";
    }

    @Override
    protected Object resolveAssociativeStoreEntityArgument(StoreInfo info, Object domainObj) {

      Object version = BeanUtils.getFieldWithAnnotation(domainObj, Version.class);
      if (version == null) {
          version = "";
      }
      return version;
    }

    @Override
    protected Object resolveAssociativeStorePropertyArgument(StoreInfo storeInfo, Object domainObj, Object propertyVal, boolean embeddedProperty) {

      Object version = BeanUtils.getFieldWithAnnotation(propertyVal, Version.class);
      if (version == null) {
          version = BeanUtils.getFieldWithAnnotation(domainObj, Version.class);
      }
      if (version == null) {
          version = "";
      }
      return version.toString();
    }
}
