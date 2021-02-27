package internal.org.springframework.content.rest.controllers.resolvers;

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
import java.util.Optional;

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

import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;
import internal.org.springframework.content.rest.utils.RepositoryUtils;

public class EntityResolver {

    private static boolean ROOT_RESOURCE_INFORMATION_CLASS_PRESENT = false;

    static {
        try {
            ROOT_RESOURCE_INFORMATION_CLASS_PRESENT = EntityResolver.class.getClassLoader().loadClass("org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver") != null;
        } catch (ClassNotFoundException e) {}
    }

    private ApplicationContext context;
    private RepositoryInvokerFactory factory;
    private Repositories repositories;
    private StoreInfo storeInfo;
    private RepositoryInvoker invoker;
    private String[] pathSegments;

    public EntityResolver(ApplicationContext context, RepositoryInvokerFactory factory, Repositories repositories, StoreInfo storeInfo, String[] pathSegments) {
        this.context = context;
        this.factory = factory;
        this.repositories = repositories;
        this.storeInfo = storeInfo;
        this.pathSegments = pathSegments;
        //this.invoker = invoker;
    }

    public Object resolve(Map<String,String> variables) {
        String repository = variables.get("repository");
        String id = variables.get("id");

        Object domainObj = null;
        try {
            try {
                domainObj = findOne(factory, repositories, storeInfo.getDomainObjectClass(), id);
            } catch (IllegalArgumentException iae) {
                domainObj = findOne(factory, repositories, repository, id);
            }
        }
        catch (HttpRequestMethodNotSupportedException e) {
            throw new ResourceNotFoundException();
        }

        return domainObj;
    }

    public Object findOne(RepositoryInvokerFactory repoInvokerFactory, Repositories repositories, String repository, String id)
            throws HttpRequestMethodNotSupportedException {

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, repository);

        if (ri == null) {
            throw new ResourceNotFoundException();
        }

        Class<?> domainObjClazz = ri.getDomainType();

        return findOne(repoInvokerFactory, repositories, domainObjClazz, id);
    }

    public Object findOne(RepositoryInvokerFactory repoInvokerFactory, Repositories repositories, Class<?> domainObjClass, String id)
            throws HttpRequestMethodNotSupportedException {

        Optional<Object> domainObj = null;

        if (ROOT_RESOURCE_INFORMATION_CLASS_PRESENT) {

            RepositoryInvoker invoker;
            try {
                invoker = resolveRootResourceInformation(storeInfo, pathSegments, new ModelAndViewContainer(), new FakeWebBinderFactory());
                if (invoker != null) {
                    domainObj = invoker.invokeFindById(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, domainObjClass);

            if (ri == null) {
                throw new ResourceNotFoundException();
            }

            Class<?> domainObjClazz = ri.getDomainType();
            Class<?> idClazz = ri.getIdType();

            Optional<Method> findOneMethod = ri.getCrudMethods().getFindOneMethod();
            if (!findOneMethod.isPresent()) {
                throw new HttpRequestMethodNotSupportedException("fineOne");
            }

            Object oid = new DefaultConversionService().convert(id, idClazz);
            domainObj = (Optional<Object>) ReflectionUtils.invokeMethod(findOneMethod.get(),
                    repositories.getRepositoryFor(domainObjClazz).get(),
                    oid);
        }
        return domainObj.orElseThrow(ResourceNotFoundException::new);
    }

    private RepositoryInvoker resolveRootResourceInformation(StoreInfo info, String[] pathSegments, ModelAndViewContainer mavContainer, WebDataBinderFactory binderFactory)
            throws Exception {

        Method m = ReflectionUtils.findMethod(RepositoryEntityControllerFacade.class, "getItemResource", org.springframework.data.rest.webmvc.RootResourceInformation.class);
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
        org.springframework.data.rest.webmvc.BaseUri baseUri = context.getBean(org.springframework.data.rest.webmvc.BaseUri.class);
        if (baseUri.equals(org.springframework.data.rest.webmvc.BaseUri.NONE) == false) {
            repoUri = String.format("%s%s", baseUri.getUri().toString(), repoUri);
        }

        NativeWebRequest repoRequestFacade = nativeWebRequestForGetItemResource(repoUri);
        org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver rootResourceInfoResolver = context.getBean(org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver.class);
        org.springframework.data.rest.webmvc.RootResourceInformation rri = rootResourceInfoResolver.resolveArgument(repoRequestMethodParameter, mavContainer, repoRequestFacade, binderFactory);
        return rri.getInvoker();
    }

    public static class FakeWebBinderFactory implements WebDataBinderFactory {

        @Override
        public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
                throws Exception {
            return null;
        }
    }

    public static NativeWebRequest nativeWebRequestForGetItemResource(String pathInfo) {
        return new GetItemResourceNativeWebRequest(pathInfo);
    }

    public static class GetItemResourceNativeWebRequest implements NativeWebRequest {

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

    public static class GetItemResourceHttpServletRequest implements HttpServletRequest {

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
        public ResponseEntity<EntityModel<?>> getItemResource(@QuerydslPredicate org.springframework.data.rest.webmvc.RootResourceInformation resourceInformation) {
            return null;
        };
    }

}
