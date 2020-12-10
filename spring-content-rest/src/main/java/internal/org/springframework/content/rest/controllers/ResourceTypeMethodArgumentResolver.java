package internal.org.springframework.content.rest.controllers;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.utils.StoreUtils;

public class ResourceTypeMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ResourceTypeMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores) {
        super(config, repositories, repoInvokerFactory, stores);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return "resourceType".equals(methodParameter.getParameterName());
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        return super.resolveArgument(methodParameter, modelAndViewContainer, nativeWebRequest, webDataBinderFactory);
    }

    @Override
    protected Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info) {

      String path = new UrlPathHelper().getPathWithinApplication(nativeWebRequest.getNativeRequest(HttpServletRequest.class));
      String pathToUse = path.substring(StoreUtils.storePath(info).length() + 1);
      String mimeType = new MimetypesFileTypeMap().getContentType(pathToUse);
      return MediaType.valueOf(mimeType != null ? mimeType : "");
    }

    @Override
    protected Object resolveAssociativeStoreEntityArgument(StoreInfo info, Object domainObj) {

      Object mimeType = BeanUtils.getFieldWithAnnotation(domainObj, MimeType.class);
      return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
    }

    @Override
    protected Object resolveAssociativeStorePropertyArgument(StoreInfo storeInfo, Object domainObj, Object propertyVal, boolean embeddedProperty) {

      Object mimeType = BeanUtils.getFieldWithAnnotation(propertyVal, MimeType.class);
      return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
    }
}
