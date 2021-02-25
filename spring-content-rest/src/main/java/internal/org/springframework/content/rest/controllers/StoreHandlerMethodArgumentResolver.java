package internal.org.springframework.content.rest.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
import internal.org.springframework.content.rest.utils.RepositoryUtils;
import internal.org.springframework.content.rest.utils.StoreUtils;

public abstract class StoreHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private UriTemplate entityUriTemplate = new UriTemplate("/{repository}/{id}");
    private UriTemplate entityPropertyUriTemplate = new UriTemplate("/{repository}/{id}/{property}");
    private UriTemplate entityPropertyWithIdUriTemplate = new UriTemplate("/{repository}/{id}/{property}/{contentId}");
    private UriTemplate revisionPropertyUriTemplate = new UriTemplate("/{repository}/{id}/revisions/{revisionId}/{property}");
    private UriTemplate revisionPropertyWithIdUriTemplate = new UriTemplate("/{repository}/{id}/revisions/{revisionId}/{property}/{contentId}");

    private final RestConfiguration config;
    private final Repositories repositories;
    private final RepositoryInvokerFactory repoInvokerFactory;
    private final Stores stores;
    private RootResourceInformationHandlerMethodArgumentResolver rootResourceInfoResolver;
    private BaseUri baseUri;

    public StoreHandlerMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores, RootResourceInformationHandlerMethodArgumentResolver rootResourceInfoResolver, BaseUri baseUri) {
        this.config = config;
        this.repositories = repositories;
        this.repoInvokerFactory = repoInvokerFactory;
        this.stores = stores;
        this.rootResourceInfoResolver = rootResourceInfoResolver;
        this.baseUri = baseUri;
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

            RootResourceInformation rri = resolveRootResourceInformation(info, pathSegments, mavContainer, binderFactory);

            if (entityUriTemplate.matches(pathInfo)) {
                Object entity = new EntityResolver(rri.getInvoker())
                        .resolve(entityUriTemplate.match(pathInfo));

                return this.resolveAssociativeStoreEntityArgument(info, entity);

            }
            else if (entityPropertyUriTemplate.matches(pathInfo)) {

                Map<String,String> variables = entityPropertyUriTemplate.match(pathInfo);

                Object domainObj = new EntityResolver(rri.getInvoker())
                        .resolve(variables);

                HttpMethod method = HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod());
                PropertySpec spec = new PropertyResolver(method, this.getRepositories(), this.getStores(), info)
                        .resolve(domainObj, variables);

                return this.resolveAssociativeStorePropertyArgument(spec.getStoreInfo(),
                        spec.getDomainObj(), spec.getPropertyVal(), spec.isEmbeddedProperty());
            }
            else if (entityPropertyWithIdUriTemplate.matches(pathInfo)) {

                Map<String,String> variables = entityPropertyWithIdUriTemplate.match(pathInfo);

                Object domainObj = new EntityResolver(rri.getInvoker())
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

    private RootResourceInformation resolveRootResourceInformation(StoreInfo info, String[] pathSegments, ModelAndViewContainer mavContainer, WebDataBinderFactory binderFactory)
            throws Exception {

        Method m = ReflectionUtils.findMethod(RepositoryEntityControllerFacade.class, "getItemResource", RootResourceInformation.class);
        MethodParameter repoRequestMethodParameter = new MethodParameter(m, 0);

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, info.getDomainObjectClass());

        // this above lookup may fail when the path for a content store for a child entity is mapped to the same path as
        // repository path for the parent entity
        // this should probably not be allowed
        // when this is the case we perform an additional lookup using the repository variable from the URI
        if (ri == null) {
            ri = RepositoryUtils.findRepositoryInformation(repositories, pathSegments[1]);
        }

        if (ri == null) {
            throw new IllegalStateException(String.format("Unable to resolve root resource information for ", String.join("/", pathSegments)));
        }

        String repo = RepositoryUtils.repositoryPath(ri);
        String id = pathSegments[2];

        String repoUri = String.format("/%s/%s", repo, id);
        if (baseUri.equals(BaseUri.NONE) == false) {
            repoUri = String.format("%s%s", baseUri.getUri().toString(), repoUri);
        }

        NativeWebRequest repoRequestFacade = nativeWebRequestForGetItemResource(repoUri);
        RootResourceInformation rri = rootResourceInfoResolver.resolveArgument(repoRequestMethodParameter, mavContainer, repoRequestFacade, binderFactory);
        return rri;
    }

    private NativeWebRequest nativeWebRequestForGetItemResource(String pathInfo) {
        return new GetItemResourceNativeWebRequest(pathInfo);
    }

    public class GetItemResourceNativeWebRequest implements NativeWebRequest {

        private String pathInfo;

        public GetItemResourceNativeWebRequest(String pathInfo) {
            this.pathInfo = pathInfo;
        }

        @Override
        public String getHeader(String headerName) {
            return null;
        }

        @Override
        public String[] getHeaderValues(String headerName) {
            return null;
        }

        @Override
        public Iterator<String> getHeaderNames() {
            return null;
        }

        @Override
        public String getParameter(String paramName) {
            return null;
        }

        @Override
        public String[] getParameterValues(String paramName) {
            return null;
        }

        @Override
        public Iterator<String> getParameterNames() {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public String getContextPath() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public boolean checkNotModified(long lastModifiedTimestamp) {
            return false;
        }

        @Override
        public boolean checkNotModified(String etag) {
            return false;
        }

        @Override
        public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
            return false;
        }

        @Override
        public String getDescription(boolean includeClientInfo) {
            return null;
        }

        @Override
        public Object getAttribute(String name, int scope) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
        }

        @Override
        public void removeAttribute(String name, int scope) {
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return null;
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {
        }

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public Object getSessionMutex() {
            return null;
        }

        @Override
        public Object getNativeRequest() {
            return null;
        }

        @Override
        public Object getNativeResponse() {
            return null;
        }

        @Override
        public <T> T getNativeRequest(Class<T> requiredType) {
            return (T) new GetItemResourceHttpServletRequest(pathInfo);
        }

        @Override
        public <T> T getNativeResponse(Class<T> requiredType) {
            return null;
        }
    }

    public class GetItemResourceHttpServletRequest implements HttpServletRequest {

        private String pathInfo;

        public GetItemResourceHttpServletRequest(String pathInfo) {
            this.pathInfo = pathInfo;
        }

        @Override
        public Object getAttribute(String name) {

            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {

            return null;
        }

        @Override
        public String getCharacterEncoding() {

            return null;
        }

        @Override
        public void setCharacterEncoding(String env)
                throws UnsupportedEncodingException {


        }

        @Override
        public int getContentLength() {

            return 0;
        }

        @Override
        public long getContentLengthLong() {

            return 0;
        }

        @Override
        public String getContentType() {

            return null;
        }

        @Override
        public ServletInputStream getInputStream()
                throws IOException {

            return null;
        }

        @Override
        public String getParameter(String name) {

            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {

            return null;
        }

        @Override
        public String[] getParameterValues(String name) {

            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {

            return null;
        }

        @Override
        public String getProtocol() {

            return null;
        }

        @Override
        public String getScheme() {

            return null;
        }

        @Override
        public String getServerName() {

            return null;
        }

        @Override
        public int getServerPort() {

            return 0;
        }

        @Override
        public BufferedReader getReader()
                throws IOException {

            return null;
        }

        @Override
        public String getRemoteAddr() {

            return null;
        }

        @Override
        public String getRemoteHost() {

            return null;
        }

        @Override
        public void setAttribute(String name, Object o) {


        }

        @Override
        public void removeAttribute(String name) {


        }

        @Override
        public Locale getLocale() {

            return null;
        }

        @Override
        public Enumeration<Locale> getLocales() {

            return null;
        }

        @Override
        public boolean isSecure() {

            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {

            return null;
        }

        @Override
        public String getRealPath(String path) {

            return null;
        }

        @Override
        public int getRemotePort() {

            return 0;
        }

        @Override
        public String getLocalName() {

            return null;
        }

        @Override
        public String getLocalAddr() {

            return null;
        }

        @Override
        public int getLocalPort() {

            return 0;
        }

        @Override
        public ServletContext getServletContext() {

            return null;
        }

        @Override
        public AsyncContext startAsync()
                throws IllegalStateException {

            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IllegalStateException {

            return null;
        }

        @Override
        public boolean isAsyncStarted() {

            return false;
        }

        @Override
        public boolean isAsyncSupported() {

            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {

            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {

            return null;
        }

        @Override
        public String getAuthType() {

            return null;
        }

        @Override
        public Cookie[] getCookies() {

            return null;
        }

        @Override
        public long getDateHeader(String name) {

            return 0;
        }

        @Override
        public String getHeader(String name) {

            return null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {

            return null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {

            return null;
        }

        @Override
        public int getIntHeader(String name) {

            return 0;
        }

        @Override
        public String getMethod() {

            return null;
        }

        @Override
        public String getPathInfo() {

            return null;
        }

        @Override
        public String getPathTranslated() {

            return null;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        @Override
        public String getQueryString() {

            return null;
        }

        @Override
        public String getRemoteUser() {

            return null;
        }

        @Override
        public boolean isUserInRole(String role) {

            return false;
        }

        @Override
        public Principal getUserPrincipal() {

            return null;
        }

        @Override
        public String getRequestedSessionId() {

            return null;
        }

        @Override
        public String getRequestURI() {
            return pathInfo;
        }

        @Override
        public StringBuffer getRequestURL() {

            return null;
        }

        @Override
        public String getServletPath() {
            return "";
        }

        @Override
        public HttpSession getSession(boolean create) {

            return null;
        }

        @Override
        public HttpSession getSession() {

            return null;
        }

        @Override
        public String changeSessionId() {

            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {

            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {

            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {

            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {

            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse response)
                throws IOException,
                ServletException {

            return false;
        }

        @Override
        public void login(String username, String password)
                throws ServletException {


        }

        @Override
        public void logout()
                throws ServletException {


        }

        @Override
        public Collection<Part> getParts()
                throws IOException,
                ServletException {

            return null;
        }

        @Override
        public Part getPart(String name)
                throws IOException,
                ServletException {

            return null;
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
                throws IOException,
                ServletException {

            return null;
        }
    }

    public static class RepositoryEntityControllerFacade {

        @RequestMapping(value = "/{repository}/{id}", method = RequestMethod.GET)
        public ResponseEntity<EntityModel<?>> getItemResource(@QuerydslPredicate RootResourceInformation resourceInformation) {
            return null;
        };
    }

    protected abstract Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info);

    protected abstract Object resolveAssociativeStoreEntityArgument(StoreInfo info, Object entity);

    protected abstract Object resolveAssociativeStorePropertyArgument(StoreInfo storeInfo, Object domainObj, Object propertyVal, boolean embeddedProperty);
}
