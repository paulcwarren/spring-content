package internal.org.springframework.content.rest.controllers;

import javax.persistence.Version;
import javax.servlet.http.HttpServletRequest;

import internal.org.springframework.content.rest.utils.ContentStoreUtils;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ResourceETagMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ResourceETagMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, ContentStoreService stores) {
        super(config, repositories, repoInvokerFactory, stores);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return "resourceETag".equals(methodParameter.getParameterName());
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        String pathInfo = nativeWebRequest.getNativeRequest(HttpServletRequest.class).getPathInfo();
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
            // do associativestore resource resolution

            // entity content
            if (pathSegments.length == 3) {
                String id = pathSegments[2];

                Object domainObj = findOne(this.getRepoInvokerFactory(), this.getRepositories(), info.getDomainObjectClass(), id);

                Object version = BeanUtils.getFieldWithAnnotation(domainObj, Version.class);
                if (version == null) {
                    version = "";
                }
                return version;

            // property content
            } else {
                return resolveProperty(HttpMethod
                        .valueOf(nativeWebRequest.getNativeRequest(HttpServletRequest.class).getMethod()), this.getRepositories(), this.getStores(), pathSegments, (s, e, p, propertyIsEmbedded) -> {
                    Object version = BeanUtils.getFieldWithAnnotation(p, Version.class);
                    if (version == null) {
                        version = BeanUtils.getFieldWithAnnotation(e, Version.class);
                    }
                    if (version == null) {
                        version = "";
                    }
                    return version.toString();
                });
            }
        } else {
            // do store resource resolution
        }

        return "";
    }
}
