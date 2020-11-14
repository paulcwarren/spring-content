package internal.org.springframework.content.rest.controllers;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.rest.config.RestConfiguration.Resolver;
import org.springframework.content.rest.controllers.ContentService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UrlPathHelper;

import internal.org.springframework.content.rest.io.RenderableResource;
import internal.org.springframework.content.rest.io.RenderedResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.StoreUtils;

public class ContentServiceHandlerMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    private static final Logger logger = LoggerFactory.getLogger(ContentServiceHandlerMethodArgumentResolver.class);

    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    private ApplicationContext context;

    public ContentServiceHandlerMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, ApplicationContext context) {
        super(config, repositories, repoInvokerFactory, stores);
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
        this.context = context;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ContentService.class.isAssignableFrom(parameter.getParameterType());
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
            throw new IllegalArgumentException(format("Store for path %s not found", store));
        }

        if (AssociativeStore.class.isAssignableFrom(info.getInterface())) {
            // do associativestore resource resolution

            // entity content
            if (pathSegments.length == 3) {
                String id = pathSegments[2];

                Object domainObj = findOne(this.getRepoInvokerFactory(), this.getRepositories(), info.getDomainObjectClass(), id);

                if (ContentStore.class.isAssignableFrom(info.getInterface())) {
                    return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(domainObj.getClass()), domainObj, byteRangeRestRequestHandler, context);
                } else if (AssociativeStore.class.isAssignableFrom(info.getInterface())) {
                    throw new UnsupportedOperationException("AssociativeStoreContentService not implemented");
                }

                // property content
            } else {
                return this.resolveProperty(HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod()), this.getRepositories(), info, pathSegments, (i, e, p, propertyIsEmbedded) -> {

                    if (ContentStore.class.isAssignableFrom(info.getInterface())) {
                        if (propertyIsEmbedded) {
                            return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(e.getClass()), e, p, byteRangeRestRequestHandler);
                        } else {
                            return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(p.getClass()), p, byteRangeRestRequestHandler, context);
                        }
                    }
                    throw new UnsupportedOperationException(format("ContentService for interface '%s' not implemented", info.getInterface()));
                });
            }

            // do store resource resolution
        } else if (Store.class.isAssignableFrom(info.getInterface())) {

            return new StoreContentService(info.getImplementation(Store.class), byteRangeRestRequestHandler);
        }

        throw new IllegalArgumentException();
    }

    public static class StoreContentService implements ContentService {

        private final Store store;
        private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

        public StoreContentService(Store store, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
            this.store = store;
            this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
        }

        @Override
        public void getContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource resource, MediaType resourceType)
                throws ResponseStatusException {
            try {
                MediaType producedResourceType = null;
                List<MediaType> acceptedMimeTypes = headers.getAccept();
                if (acceptedMimeTypes.size() > 0) {

                    MediaType.sortBySpecificityAndQuality(acceptedMimeTypes);
                    for (MediaType acceptedMimeType : acceptedMimeTypes) {
                        if (resource instanceof RenderableResource && ((RenderableResource) resource)
                                .isRenderableAs(acceptedMimeType)) {
                            resource = new RenderedResource(((RenderableResource) resource)
                                    .renderAs(acceptedMimeType), resource);
                            producedResourceType = acceptedMimeType;
                            break;
                        }
                        else if (acceptedMimeType.includes(resourceType)) {
                            producedResourceType = resourceType;
                            break;
                        }
                    }

                    if (producedResourceType == null) {
                        response.setStatus(HttpStatus.NOT_FOUND.value());
                        return;
                    }
                }

                request.setAttribute("SPRING_CONTENT_RESOURCE", resource);
                request.setAttribute("SPRING_CONTENT_CONTENTTYPE", producedResourceType);
            } catch (Exception e) {

                logger.error("Unable to retrieve content", e);

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
            }

            try {
                byteRangeRestRequestHandler.handleRequest(request, response);
            }
            catch (Exception e) {

                if (isClientAbortException(e)) {
                    // suppress
                } else {
                    logger.error("Unable to handle request", e);

                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
                }
            }
        }

        @Override
        public void setContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource source, MediaType sourceMimeType, Resource target)
                throws IOException, MethodNotAllowedException {

            InputStream in = source.getInputStream();
            OutputStream out = ((WritableResource) target).getOutputStream();
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);

        }

        @Override
        public void unsetContent(Resource resource) {

            Assert.notNull(resource);
            if (resource instanceof DeletableResource) {
                try {
                    ((DeletableResource) resource).delete();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class ContentStoreContentService implements ContentService {

        private final RestConfiguration config;
        private final StoreInfo store;
        private final RepositoryInvoker repoInvoker;
        private final Object domainObj;
        private final Object embeddedProperty;
        private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;
        private ApplicationContext context;

        public ContentStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, ApplicationContext context) {
            this.config = config;
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = null;
            this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
            this.context = context;
        }

        public ContentStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, Object embeddedProperty, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
            this.config = config;
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = embeddedProperty;
            this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
        }

        @Override
        public void getContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource resource, MediaType resourceType)
                throws ResponseStatusException, MethodNotAllowedException {

            Method[] methodsToUse = filterMethods(store.getInterface().getMethods(), this::withGetContentName, this::isOveridden, this::isExported);

            if (methodsToUse.length > 1) {
                throw new IllegalStateException("Too many getContent methods");
            }

            if (methodsToUse.length == 0) {
                throw new MethodNotAllowedException();
            }

            try {
                MediaType producedResourceType = null;
                List<MediaType> acceptedMimeTypes = headers.getAccept();
                if (acceptedMimeTypes.size() > 0) {

                    MediaType.sortBySpecificityAndQuality(acceptedMimeTypes);
                    for (MediaType acceptedMimeType : acceptedMimeTypes) {

                        if (acceptedMimeType.includes(resourceType)) {

                            producedResourceType = resourceType;
                            break;

                        } else if (resource instanceof RenderableResource &&
                                ((RenderableResource) resource).isRenderableAs(acceptedMimeType)) {

                            resource = new RenderedResource(((RenderableResource) resource).renderAs(acceptedMimeType), resource);
                            producedResourceType = acceptedMimeType;
                            break;
                        }
                    }

                    if (producedResourceType == null) {
                        response.setStatus(HttpStatus.NOT_FOUND.value());
                        return;
                    }
                }

                request.setAttribute("SPRING_CONTENT_RESOURCE", resource);
                request.setAttribute("SPRING_CONTENT_CONTENTTYPE", producedResourceType);
            } catch (Exception e) {

                logger.error("Unable to retrieve content", e);

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
            }

            try {
                byteRangeRestRequestHandler.handleRequest(request, response);
            }
            catch (Exception e) {
                if (isClientAbortException(e)) {
                    // suppress
                } else {
                    logger.error("Unable to handle request", e);

                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
                }
            }
        }

        @Override
        public void setContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource source, MediaType sourceMimeType, Resource target) throws IOException, MethodNotAllowedException {

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, MimeType.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, MimeType.class, sourceMimeType.toString());
            }

            String originalFilename = source.getFilename();
            if (source.getFilename() != null && StringUtils.hasText(originalFilename)) {
                if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, OriginalFileName.class)) {
                    BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, OriginalFileName.class, originalFilename);
                }
            }

            Method[] methodsToUse = filterMethods(store.getInterface().getMethods(), this::withSetContentName, this::isOveridden, this::isExported);

            if (methodsToUse.length > 1) {
                RestConfiguration.DomainTypeConfig dtConfig = config.forDomainType(store.getDomainObjectClass());
                methodsToUse = filterMethods(methodsToUse, dtConfig.getSetContentResolver(), headers);
            }

            if (methodsToUse.length > 1) {
                throw new UnsupportedOperationException(format("Too many setContent methods exported.  Expected 1.  Got %s", methodsToUse.length));
            }

            if (methodsToUse.length == 0) {
                throw new MethodNotAllowedException();
            }

            Method methodToUse = methodsToUse[0];
            Object contentArg = convertContentArg(source, methodToUse.getParameterTypes()[1]);

            try {
                Object targetObj = store.getImplementation(ContentStore.class);

                ReflectionUtils.makeAccessible(methodToUse);

                Object updatedDomainObj = ReflectionUtils.invokeMethod(methodToUse, targetObj, (embeddedProperty == null ? domainObj : embeddedProperty), contentArg);
                repoInvoker.invokeSave(embeddedProperty == null ? updatedDomainObj : domainObj);
            } finally {
                cleanup(contentArg);
            }
        }


        @Override
        public void unsetContent(Resource resource) throws MethodNotAllowedException {

            Method[] methodsToUse = filterMethods(store.getInterface().getMethods(), this::withUnsetContentName, this::isOveridden, this::isExported);

            if (methodsToUse.length == 0) {
                throw new MethodNotAllowedException();
            }

            if (methodsToUse.length > 1) {
                throw new IllegalStateException("Too many unsetContent methods");
            }

            Object targetObj = store.getImplementation(ContentStore.class);

            ReflectionUtils.makeAccessible(methodsToUse[0]);

            Object updatedDomainObj = ReflectionUtils.invokeMethod(methodsToUse[0], targetObj, (embeddedProperty == null ? domainObj : embeddedProperty));

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, MimeType.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, MimeType.class, null);
            }

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, OriginalFileName.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, OriginalFileName.class, null);
            }

            repoInvoker.invokeSave(embeddedProperty == null ? updatedDomainObj : domainObj);
        }

        private boolean withGetContentName(Method method) {
            return method.getName().equals("getContent");
        }

        private boolean withSetContentName(Method method) {
            return method.getName().equals("setContent");
        }

        private boolean withUnsetContentName(Method method) {
            return method.getName().equals("unsetContent");
        }

        private boolean isOveridden(Method method) {
            return !method.isBridge();
        }

        private boolean isExported(Method method) {
            RestResource restResource = method.getAnnotation(RestResource.class);
            if (restResource == null || restResource.exported()) {
                return true;
            }
            return false;
        }

        private void cleanup(Object contentArg) {

            if (contentArg == null) {
                return;
            }

            if (FileSystemResource.class.isAssignableFrom(contentArg.getClass())) {
                ((FileSystemResource)contentArg).getFile().delete();
            }
        }

        private Object convertContentArg(Resource resource, Class<?> parameterType) {

            if (InputStream.class.equals(parameterType)) {
                try {
                    return resource.getInputStream();
                } catch (IOException e) {
                    throw new IllegalArgumentException(format("Unable to get inputstream from resource %s", resource.getFilename()));
                }
            } else if (Resource.class.equals(parameterType)) {
                try {
                    File f = Files.createTempFile("", "").toFile();
                    FileUtils.copyInputStreamToFile(resource.getInputStream(), f);
                    return new FileSystemResource(f);
                } catch (IOException e) {
                    throw new IllegalArgumentException(format("Unable to re-purpose resource %s", resource.getFilename()));
                }
            } else {
                throw new IllegalArgumentException(format("Unsupported content type %s", parameterType.getCanonicalName()));
            }
        }

        private Method[] filterMethods(Method[] methods, Predicate<Method>...filters) {

            return Stream.of(methods)
                    .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(t->true))
                    .collect(Collectors.toList())
                    .toArray(new Method[]{});
        }

        private Method[] filterMethods(Method[] methods, Resolver<Method, HttpHeaders> resolver, HttpHeaders headers) {

            List<Method> resolved = new ArrayList<>();
            for (Method method : methods) {
                if (resolver.resolve(method, headers)) {
                    resolved.add(method);
                }
            }

            return resolved.toArray(new Method[]{});
        }
    }

    private static boolean isClientAbortException(Exception e) {
        if (e.getClass().getSimpleName().equals("ClientAbortException")) {
            return true;
        }
        return false;
    }
}
