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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.DeletableResource;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import internal.org.springframework.content.rest.io.RenderableResource;
import internal.org.springframework.content.rest.io.RenderedResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

public class ContentServiceHandlerMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    private static final Logger logger = LoggerFactory.getLogger(ContentServiceHandlerMethodArgumentResolver.class);

    private static final Map<Class<?>, StoreExportedMethodsMap> storeExportedMethods = new HashMap<>();

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

        return super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }


    @Override
    protected Object resolveStoreArgument(NativeWebRequest nativeWebRequest, StoreInfo info) {

        return new StoreContentService(info.getImplementation(Store.class), byteRangeRestRequestHandler);
    }

    @Override
    protected Object resolveAssociativeStoreEntityArgument(StoreInfo info, Object domainObj) {

      if (ContentStore.class.isAssignableFrom(info.getInterface())) {
          return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(domainObj.getClass()), domainObj, byteRangeRestRequestHandler, context);
      }
      throw new UnsupportedOperationException(format("No content service for store %s", info.getInterface()));
    }

    @Override
    protected Object resolveAssociativeStorePropertyArgument(StoreInfo info, Object domainObj, Object propertyVal, boolean embeddedProperty) {

      if (ContentStore.class.isAssignableFrom(info.getInterface())) {
          if (embeddedProperty) {
              return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(domainObj.getClass()), domainObj, propertyVal, byteRangeRestRequestHandler);
          } else {
              return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(propertyVal.getClass()), propertyVal, byteRangeRestRequestHandler, context);
          }
      }
      throw new UnsupportedOperationException(format("ContentService for interface '%s' not implemented", info.getInterface()));
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

        public ContentStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, ApplicationContext context) {
            this.config = config;
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = null;
            this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
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

            Method[] methodsToUse = getExportedMethodsFor(store.getInterface()).getContentMethods();

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

                        if (acceptedMimeType.includes(resourceType) && matchParameters(acceptedMimeType, resourceType)) {

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

            Method[] methodsToUse = getExportedMethodsFor(store.getInterface()).setContentMethods();

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

            Method[] methodsToUse = getExportedMethodsFor(store.getInterface()).unsetContentMethods();

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

        private Method[] filterMethods(Method[] methods, Resolver<Method, HttpHeaders> resolver, HttpHeaders headers) {

            List<Method> resolved = new ArrayList<>();
            for (Method method : methods) {
                if (resolver.resolve(method, headers)) {
                    resolved.add(method);
                }
            }

            return resolved.toArray(new Method[]{});
        }

        private boolean matchParameters(MediaType acceptedMediaType, MediaType producableMediaType) {
            for (String name : producableMediaType.getParameters().keySet()) {
                String s1 = producableMediaType.getParameter(name);
                String s2 = acceptedMediaType.getParameter(name);
                if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static StoreExportedMethodsMap getExportedMethodsFor(Class<?> storeInterfaceClass) {

        StoreExportedMethodsMap exportMap = storeExportedMethods.get(storeInterfaceClass);
        if (exportMap == null) {
            storeExportedMethods.put(storeInterfaceClass, new StoreExportedMethodsMap(storeInterfaceClass));
            exportMap = storeExportedMethods.get(storeInterfaceClass);
        }

        return exportMap;
    }

    public static class StoreExportedMethodsMap {

        private static Method[] SETCONTENT_METHODS = null;
        private static Method[] UNSETCONTENT_METHODS = null;
        private static Method[] GETCONTENT_METHODS = null;

        static {
            SETCONTENT_METHODS = new Method[] {
                ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class),
                ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class),
            };

            UNSETCONTENT_METHODS = new Method[] {
                ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class),
            };

            GETCONTENT_METHODS = new Method[] {
                ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class),
            };
        }

        private Class<?> storeInterface;
        private Method[] getContentMethods;
        private Method[] setContentMethods;
        private Method[] unsetContentMethods;

        public StoreExportedMethodsMap(Class<?> storeInterface) {
            this.storeInterface = storeInterface;
            this.getContentMethods = calculateExports(GETCONTENT_METHODS);
            this.setContentMethods = calculateExports(SETCONTENT_METHODS);
            this.unsetContentMethods = calculateExports(UNSETCONTENT_METHODS);
        }

        public Method[] getContentMethods() {
            return this.getContentMethods;
        }

        public Method[] setContentMethods() {
            return this.setContentMethods;
        }

        public Method[] unsetContentMethods() {
            return this.unsetContentMethods;
        }

        private Method[] calculateExports(Method[] storeMethods) {

            List<Method> exportedMethods = new ArrayList<>();
            exportedMethods.addAll(Arrays.asList(storeMethods));

            List<Method> unexportedMethods = new ArrayList<>();

            for (Method m : exportedMethods) {
                for (Method dm : storeInterface.getDeclaredMethods()) {
                    if (!dm.isBridge()) {
                        if (dm.getName().equals(m.getName())) {
                            if (argsMatch(dm, m)) {

                                RestResource r = dm.getAnnotation(RestResource.class);
                                if (r != null && r.exported() == false) {

                                    unexportedMethods.add(m);
                                }
                            }
                        }
                    }
                }
            }

            for (Method unexportedMethod : unexportedMethods) {

                exportedMethods.remove(unexportedMethod);
            }

            return exportedMethods.toArray(new Method[]{});
        }

        private boolean argsMatch(Method dm, Method m) {

            for (int i=0; i < m.getParameterTypes().length; i++) {

                if (!m.getParameterTypes()[i].isAssignableFrom(dm.getParameterTypes()[i])) {

                    return false;
                }
            }

            return true;
        }
    }

    private static boolean isClientAbortException(Exception e) {
        if (e.getClass().getSimpleName().equals("ClientAbortException")) {
            return true;
        }
        return false;
    }
}
