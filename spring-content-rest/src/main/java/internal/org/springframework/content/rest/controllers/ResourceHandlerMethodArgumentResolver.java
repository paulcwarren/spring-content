package internal.org.springframework.content.rest.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import internal.org.springframework.content.rest.mappingcontext.ContentPropertyRequest;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.controllers.resolvers.AssociativeStoreResourceResolver;
import internal.org.springframework.content.rest.controllers.resolvers.EntityResolution;
import internal.org.springframework.content.rest.controllers.resolvers.EntityResolvers;
import internal.org.springframework.content.rest.controllers.resolvers.ResourceResolver;
import internal.org.springframework.content.rest.controllers.resolvers.StoreResourceResolver;
import internal.org.springframework.content.rest.io.StoreResourceImpl;
import internal.org.springframework.content.rest.utils.StoreUtils;

import static org.apache.commons.lang.StringUtils.join;

public class ResourceHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private UriTemplate entityUriTemplate = new UriTemplate("/{repository}/{id}");
    private UriTemplate entityPropertyUriTemplate = new UriTemplate("/{repository}/{id}/{property}");
    private UriTemplate entityPropertyWithIdUriTemplate = new UriTemplate("/{repository}/{id}/{property}/**");
    private UriTemplate revisionPropertyUriTemplate = new UriTemplate("/{repository}/{id}/revisions/{revisionId}/{property}");
    private UriTemplate revisionPropertyWithIdUriTemplate = new UriTemplate("/{repository}/{id}/revisions/{revisionId}/{property}/{contentId}");

    private EntityResolvers entityResolvers;
    private List<ResourceResolver> resolvers = new ArrayList<>();

    private ApplicationContext context;
    private final RestConfiguration config;
    private final Repositories repositories;
    private final Stores stores;
    private final ContentPropertyToRequestMappingContext requestMappingContext;
    private final MappingContext mappingContext;

    public ResourceHandlerMethodArgumentResolver(ApplicationContext context, RestConfiguration config, Repositories repositories, Stores stores, ContentPropertyToRequestMappingContext requestMappingContext, MappingContext mappingContext, EntityResolvers entityResolvers) {
        this.context = context;
        this.config = config;
        this.repositories = repositories;
        this.stores = stores;
        this.requestMappingContext = requestMappingContext;
        this.mappingContext = mappingContext;

        this.entityResolvers = entityResolvers;

        resolvers.add(new StoreResourceResolver(this.mappingContext));
        resolvers.add(new AssociativeStoreResourceResolver(this.mappingContext));
    }

    RestConfiguration getConfig() {
        return config;
    }

    protected Repositories getRepositories() {
        return repositories;
    }

    protected Stores getStores() {
        return stores;
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return Resource.class.isAssignableFrom(methodParameter.getParameterType());
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

        if (AssociativeStore.class.isAssignableFrom(info.getInterface())) {

            String resolvedContentPropertyPath = requestMappingContext.resolveContentPropertyPath(info.getDomainObjectClass(), ContentPropertyRequest.from(pathInfo).getContentPropertyPath());
            String resolvedStoreLookupPath = ContentPropertyRequest.from(pathSegments[1], pathSegments[2], resolvedContentPropertyPath).getRequestURI();

            EntityResolution result = this.entityResolvers.resolve(resolvedStoreLookupPath);

            AntPathMatcher matcher = new AntPathMatcher();
            Comparator<String> patternComparator = matcher.getPatternComparator(resolvedStoreLookupPath);

            List<String> uriTemplates = new ArrayList<>();
            for (ResourceResolver resolver : resolvers) {
                if (matcher.match(resolver.getMapping(), resolvedStoreLookupPath)) {
                    uriTemplates.add(resolver.getMapping());
                }
            }

            String bestMatch = null;
            if (uriTemplates.size() > 1) {
                uriTemplates.sort(patternComparator);
            }

            bestMatch = uriTemplates.get(0);

            ResourceResolver matchedResolver = null;
            for (ResourceResolver resolver : resolvers) {
                if (bestMatch.equals(resolver.getMapping())) {
                    matchedResolver = resolver;
                }
            }

            return matchedResolver.resolve(webRequest, info, result.getEntity(), result.getProperty());

        } else if (Store.class.isAssignableFrom(info.getInterface())) {

            return resolveStoreArgument(webRequest, info);
        }

        throw new IllegalArgumentException();
    }

    protected Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info) {
        String path = new UrlPathHelper().getPathWithinApplication(nativeWebRequest.getNativeRequest(HttpServletRequest.class));
        String pathToUse = path.substring(StoreUtils.storePath(info).length() + 1);

        return new StoreResourceImpl(info, info.getImplementation(Store.class).getResource(pathToUse));
      }
}
