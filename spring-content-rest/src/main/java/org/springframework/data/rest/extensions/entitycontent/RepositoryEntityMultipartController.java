package org.springframework.data.rest.extensions.entitycontent;

import internal.org.springframework.content.rest.contentservice.ContentStoreContentService;
import internal.org.springframework.content.rest.controllers.BadRequestException;
import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import internal.org.springframework.content.rest.controllers.resolvers.AssociativeStoreResourceResolver;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ControllerUtils;
import internal.org.springframework.content.rest.utils.StoreUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceType;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.HttpHeadersPreparer;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.*;

@RepositoryRestController
public class RepositoryEntityMultipartController {

    private static final String ENTITY_POST_MAPPING = "/{repository}";

    private RestConfiguration restConfig;
    private RepositoryInvokerFactory repoInvokerFactory;
    private MappingContext mappingContext;
    private ContentPropertyToExportedContext exportedMappingContext;
    private StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;
    private Stores stores;
    private SelfLinkProvider selfLinkProvider;
    private HttpHeadersPreparer headersPreparer;

    @Autowired
    public RepositoryEntityMultipartController(RestConfiguration restConfig, RepositoryInvokerFactory repoInvokerFactory, SelfLinkProvider selfLinkProvider, Stores stores, MappingContext mappingContext, ContentPropertyToExportedContext exportedMappingContext, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, @Qualifier("entityMultipartHttpMessageConverterConfigurer") RepositoryRestConfigurer configurer, HttpHeadersPreparer headersPreparer) {
        this.restConfig = restConfig;
        this.repoInvokerFactory = repoInvokerFactory;
        this.selfLinkProvider = selfLinkProvider;
        this.stores = stores;
        this.mappingContext = mappingContext;
        this.exportedMappingContext = exportedMappingContext;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
        this.headersPreparer = headersPreparer;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @ResponseBody
    @PostMapping(value = ENTITY_POST_MAPPING, consumes = "multipart/form-data")
    public ResponseEntity<RepresentationModel<?>> createEntityAndContent(RootResourceInformation repoInfo, PersistentEntityResource payload, PersistentEntityResourceAssembler assembler, @PathVariable("repository") String repository, @RequestHeader HttpHeaders headers, MultipartHttpServletRequest req, HttpServletResponse resp)
            throws IOException, MethodNotAllowedException, HttpRequestMethodNotSupportedException {

        repoInfo.verifySupportedMethod(HttpMethod.POST, ResourceType.COLLECTION);

        Class<?> domainType = repoInfo.getDomainType();

        Object savedEntity = payload.getContent();

        String pathInfo = new UrlPathHelper().getPathWithinApplication(req);
        pathInfo = StoreUtils.storeLookupPath(pathInfo, restConfig.getBaseUri());

        String[] pathSegments = pathInfo.split("/");
        if (pathSegments.length < 2) {
            throw new BadRequestException();
        }

        String store = pathSegments[1];

        boolean entitySaved = false;

        StoreInfo info = this.stores.getStore(Store.class, StoreUtils.withStorePath(store));
        if (info != null) {
            ContentStoreContentService service = new ContentStoreContentService(restConfig, info, repoInvokerFactory.getInvokerFor(domainType), mappingContext, exportedMappingContext, byteRangeRestRequestHandler);
            MultiValueMap<String, MultipartFile> files = req.getMultiFileMap();
            for (String path : files.keySet()) {
                MultipartFile file = files.get(path).get(0);

                Resource storeResource = new AssociativeStoreResourceResolver(mappingContext).resolve(new InternalWebRequest(req, resp), info, savedEntity, PropertyPath.from(file.getName()));

                headers.setContentLength(file.getSize());
                service.setContent(req, resp, headers, new InputStreamResourceWithFilename(file.getInputStream(), file.getOriginalFilename()), MediaType.parseMediaType(file.getContentType()), storeResource);
                entitySaved = true;
            }
        }

        // if we didn't find store info, or there weren't any files in the request, the entity has not been saved yet
        if (!entitySaved) {
            repoInvokerFactory.getInvokerFor(domainType).invokeSave(savedEntity);
        }

        Optional<PersistentEntityResource> resource = Optional.ofNullable(assembler.toFullResource(savedEntity));
        headers.setContentType(new MediaType("application", "hal+json"));

        HttpHeaders respHeaders = headersPreparer.prepareHeaders(resource);
		// addLocationHeader(respHeaders, assembler, savedEntity);
        String selfLink = selfLinkProvider.createSelfLinkFor(savedEntity).withSelfRel().expand(new Object[0]).getHref();
        respHeaders.setLocation(UriTemplate.of(selfLink).expand());
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, respHeaders, resource);
    }

    private static class InternalWebRequest implements NativeWebRequest {

        private final MultipartHttpServletRequest req;
        private final HttpServletResponse resp;

        public InternalWebRequest(MultipartHttpServletRequest req, HttpServletResponse resp) {
            this.req = req;
            this.resp = resp;
        }

        @Override
        public Object getNativeRequest() {
            return req;
        }

        @Override
        public Object getNativeResponse() {
            return resp;
        }

        @Override
        public <T> T getNativeRequest(Class<T> requiredType) {
            return (T) req;
        }

        @Override
        public <T> T getNativeResponse(Class<T> requiredType) {
            return (T) resp;
        }

        @Override
        public String getHeader(String headerName) {
            return req.getHeader(headerName);
        }

        @Override
        public String[] getHeaderValues(String headerName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> getHeaderNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getParameter(String paramName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getParameterValues(String paramName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> getParameterNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locale getLocale() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContextPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteUser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUserInRole(String role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkNotModified(long lastModifiedTimestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkNotModified(String etag) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDescription(boolean includeClientInfo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String name, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(String name, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getAttributeNames(int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object resolveReference(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getSessionMutex() {
            throw new UnsupportedOperationException();
        }
    }

    private final class InputStreamResourceWithFilename extends InputStreamResource {

        private final String filename;

        public InputStreamResourceWithFilename(InputStream inputStream, String filename) {
            super(inputStream);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
