package internal.org.springframework.content.rest.controllers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.controllers.resolvers.EntityResolver;
import internal.org.springframework.content.rest.controllers.resolvers.PropertyResolver;
import internal.org.springframework.content.rest.controllers.resolvers.PropertyResolver.PropertySpec;
import internal.org.springframework.content.rest.controllers.resolvers.RevisionEntityResolver;
import internal.org.springframework.content.rest.utils.StoreUtils;

public abstract class StoreHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

//    private static boolean ROOT_RESOURCE_INFORMATION_CLASS_PRESENT = false;
//
//    static {
//        try {
//            ROOT_RESOURCE_INFORMATION_CLASS_PRESENT = StoreHandlerMethodArgumentResolver.class.getClassLoader().loadClass("org.springframework.data.rest.webmvc.config.RootResourceInformation") != null;
//        } catch (ClassNotFoundException e) {}
//    }

    private UriTemplate entityUriTemplate = new UriTemplate("/{repository}/{id}");
    private UriTemplate entityPropertyUriTemplate = new UriTemplate("/{repository}/{id}/{property}");
    private UriTemplate entityPropertyWithIdUriTemplate = new UriTemplate("/{repository}/{id}/{property}/{contentId}");
    private UriTemplate revisionPropertyUriTemplate = new UriTemplate("/{repository}/{id}/revisions/{revisionId}/{property}");
    private UriTemplate revisionPropertyWithIdUriTemplate = new UriTemplate("/{repository}/{id}/revisions/{revisionId}/{property}/{contentId}");

    private ApplicationContext context;
    private final RestConfiguration config;
    private final Repositories repositories;
    private final RepositoryInvokerFactory repoInvokerFactory;
    private final Stores stores;

    public StoreHandlerMethodArgumentResolver(ApplicationContext context, RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores) {
        this.context = context;
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
        return Store.class.isAssignableFrom(parameter.getParameterType());
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

//            RootResourceInformation rri = resolveRootResourceInformation(info, pathSegments, mavContainer, binderFactory);

            if (entityUriTemplate.matches(pathInfo)) {
                Object entity = new EntityResolver(context, this.getRepoInvokerFactory(), this.getRepositories(), info, pathSegments)
                        .resolve(entityUriTemplate.match(pathInfo));

                return this.resolveAssociativeStoreEntityArgument(info, entity);

            }
            else if (entityPropertyUriTemplate.matches(pathInfo)) {

                Map<String,String> variables = entityPropertyUriTemplate.match(pathInfo);

                Object domainObj = new EntityResolver(context, this.getRepoInvokerFactory(), this.getRepositories(), info, pathSegments)
                        .resolve(variables);

                HttpMethod method = HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod());
                PropertySpec spec = new PropertyResolver(method, this.getRepositories(), this.getStores(), info)
                        .resolve(domainObj, variables);

                return this.resolveAssociativeStorePropertyArgument(spec.getStoreInfo(),
                        spec.getDomainObj(), spec.getPropertyVal(), spec.isEmbeddedProperty());
            }
            else if (entityPropertyWithIdUriTemplate.matches(pathInfo)) {

                Map<String,String> variables = entityPropertyWithIdUriTemplate.match(pathInfo);

                Object domainObj = new EntityResolver(context, this.getRepoInvokerFactory(), this.getRepositories(), info, pathSegments)
                        .resolve(variables);

                HttpMethod method = HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod());
                PropertySpec spec = new PropertyResolver(method, this.getRepositories(), this.getStores(), info)
                        .resolve(domainObj, variables);

                return this.resolveAssociativeStorePropertyArgument(spec.getStoreInfo(),
                        spec.getDomainObj(), spec.getPropertyVal(), spec.isEmbeddedProperty());
            }
            else if (revisionPropertyUriTemplate.matches(pathInfo)) {

                Map<String,String> variables = revisionPropertyUriTemplate.match(pathInfo);

                Object domainObj = new RevisionEntityResolver(this.getRepoInvokerFactory(), this.getRepositories(), this.getStores(), info)
                        .resolve(variables);

                HttpMethod method = HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod());
                PropertySpec spec = new PropertyResolver(method, this.getRepositories(), this.getStores(), info)
                        .resolve(domainObj, variables);

                return this.resolveAssociativeStorePropertyArgument(spec.getStoreInfo(),
                        spec.getDomainObj(), spec.getPropertyVal(), spec.isEmbeddedProperty());
            }
            else if (revisionPropertyWithIdUriTemplate.matches(pathInfo)) {


                Map<String,String> variables = revisionPropertyWithIdUriTemplate.match(pathInfo);

                Object domainObj = new RevisionEntityResolver(this.getRepoInvokerFactory(), this.getRepositories(), this.getStores(), info)
                        .resolve(variables);

                HttpMethod method = HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod());
                PropertySpec spec = new PropertyResolver(method, this.getRepositories(), this.getStores(), info)
                        .resolve(domainObj, variables);

                return this.resolveAssociativeStorePropertyArgument(spec.getStoreInfo(),
                        spec.getDomainObj(), spec.getPropertyVal(), spec.isEmbeddedProperty());
            }
        } else if (Store.class.isAssignableFrom(info.getInterface())) {

            return resolveStoreArgument(webRequest, info);
        }

        throw new IllegalArgumentException();
    }

//    private RootResourceInformation resolveRootResourceInformation(StoreInfo info, String[] pathSegments, ModelAndViewContainer mavContainer, WebDataBinderFactory binderFactory)
//            throws Exception {
//
//        Method m = ReflectionUtils.findMethod(RepositoryEntityControllerFacade.class, "getItemResource", RootResourceInformation.class);
//        MethodParameter repoRequestMethodParameter = new MethodParameter(m, 0);
//
//        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, info.getDomainObjectClass());
//
//        // this above lookup may fail when the path for a content store for a child entity is mapped to the same path as
//        // repository path for the parent entity
//        // this should probably not be allowed
//        // when this is the case we perform an additional lookup using the repository variable from the URI
//        if (ri == null) {
//            ri = RepositoryUtils.findRepositoryInformation(repositories, pathSegments[1]);
//        }
//
//        if (ri == null) {
//            throw new IllegalStateException(String.format("Unable to resolve root resource information for ", String.join("/", pathSegments)));
//        }
//
//        String repo = RepositoryUtils.repositoryPath(ri);
//        String id = pathSegments[2];
//
//        String repoUri = String.format("/%s/%s", repo, id);
//        if (baseUri.equals(BaseUri.NONE) == false) {
//            repoUri = String.format("%s%s", baseUri.getUri().toString(), repoUri);
//        }
//
//        NativeWebRequest repoRequestFacade = nativeWebRequestForGetItemResource(repoUri);
//        RootResourceInformation rri = rootResourceInfoResolver.resolveArgument(repoRequestMethodParameter, mavContainer, repoRequestFacade, binderFactory);
//        return rri;
//    }


    protected abstract Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info);

    protected abstract Object resolveAssociativeStoreEntityArgument(StoreInfo info, Object entity);

    protected abstract Object resolveAssociativeStorePropertyArgument(StoreInfo storeInfo, Object domainObj, Object propertyVal, boolean embeddedProperty);
}
