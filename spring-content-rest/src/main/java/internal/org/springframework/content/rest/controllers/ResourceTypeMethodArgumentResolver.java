package internal.org.springframework.content.rest.controllers;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import internal.org.springframework.content.rest.utils.ContentStoreUtils;

import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

public class ResourceTypeMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ResourceTypeMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, ContentStoreService stores) {
        super(config, repositories, repoInvokerFactory, stores);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return "resourceType".equals(methodParameter.getParameterName());
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        String pathInfo = nativeWebRequest.getNativeRequest(HttpServletRequest.class).getRequestURI();
        pathInfo = new UrlPathHelper().getPathWithinApplication(nativeWebRequest.getNativeRequest(HttpServletRequest.class));
        pathInfo = ContentStoreUtils.storeLookupPath(pathInfo, this.getConfig().getBaseUri());

        String[] pathSegments = pathInfo.split("/");
        if (pathSegments.length < 2) {
            return null;
        }

        String store = pathSegments[1];

        ContentStoreInfo info = ContentStoreUtils.findStore(this.getStores(), store);
        if (info == null) {
            throw new IllegalArgumentException(String.format("Store for path %s not found", store));
        }

        if (AssociativeStore.class.isAssignableFrom(info.getInterface())) {

            // entity content
            if (pathSegments.length == 3) {
                String id = pathSegments[2];

                Object domainObj = findOne(this.getRepoInvokerFactory(), this.getRepositories(), info.getDomainObjectClass(), id);

                Object mimeType = BeanUtils.getFieldWithAnnotation(domainObj, MimeType.class);
                return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);

            // property content
            } else {
                return resolveProperty(HttpMethod.valueOf(nativeWebRequest.getNativeRequest(HttpServletRequest.class).getMethod()), this.getRepositories(), this.getStores(), pathSegments, (s, e, p, propertyIsEmbedded) -> {
                    Object mimeType = BeanUtils.getFieldWithAnnotation(p, MimeType.class);
                    return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
                });
            }
        } else if (Store.class.isAssignableFrom(info.getInterface())) {

            String path = new UrlPathHelper().getPathWithinApplication(nativeWebRequest.getNativeRequest(HttpServletRequest.class));
            String pathToUse = path.substring(ContentStoreUtils.storePath(info).length() + 1);
            String mimeType = new MimetypesFileTypeMap().getContentType(pathToUse);
            return MediaType.valueOf(mimeType != null ? mimeType : "");
        }

        return null;
    }
}
