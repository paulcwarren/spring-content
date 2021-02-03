package internal.org.springframework.content.rest.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.utils.StoreUtils;

public class StoreInfoHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final RestConfiguration config;
    private final Repositories repositories;
    private final RepositoryInvokerFactory repoInvokerFactory;
    private final Stores stores;

    public StoreInfoHandlerMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores) {
        this.config = config;
        this.repositories = repositories;
        this.repoInvokerFactory = repoInvokerFactory;
        this.stores = stores;
    }

    RestConfiguration getConfig() {
        return config;
    }

    protected Repositories getRepositories() {
        return repositories;
    }

    RepositoryInvokerFactory getRepoInvokerFactory() {
        return repoInvokerFactory;
    }

    protected Stores getStores() {
        return stores;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return StoreInfo.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        String pathInfo = webRequest.getNativeRequest(HttpServletRequest.class).getRequestURI();
        pathInfo = new UrlPathHelper().getPathWithinApplication(webRequest.getNativeRequest(HttpServletRequest.class));
        pathInfo = StoreUtils.storeLookupPath(pathInfo, this.getConfig().getBaseUri());

        String[] pathSegments = pathInfo.split("/");
        if (pathSegments.length < 2) {
            return null;
        }

        String store = pathSegments[1];

        StoreInfo info = this.getStores().getStore(Store.class, StoreUtils.withStorePath(store));
        if (info == null) {
            throw new IllegalArgumentException(String.format("Store for path %s not found", store));
        }

        return info;
    }
}
