package internal.org.springframework.content.rest.contentservice;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import internal.org.springframework.content.rest.io.AssociatedStoreResource;
import internal.org.springframework.content.rest.io.RenderedResource;
import internal.org.springframework.content.rest.io.StoreResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

public class AssociativeStoreContentService implements ContentService {

    private static final Logger logger = LoggerFactory.getLogger(AssociativeStoreContentService.class);

    private static final Map<Class<?>, StoreExportedMethodsMap> storeExportedMethods = new HashMap<>();

    private final RestConfiguration config;
    private final StoreInfo store;
    private final RepositoryInvoker repoInvoker;
    private final Object domainObj;
    private final Object embeddedProperty;
    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    public AssociativeStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
        this.config = config;
        this.store = store;
        this.repoInvoker = repoInvoker;
        this.domainObj = domainObj;
        this.embeddedProperty = null;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
    }

    public AssociativeStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, ApplicationContext context) {
        this.config = config;
        this.store = store;
        this.repoInvoker = repoInvoker;
        this.domainObj = domainObj;
        this.embeddedProperty = null;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
    }

    public AssociativeStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, Object embeddedProperty, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
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

        Method[] methodsToUse = getExportedMethodsFor(((StoreResource)resource).getStoreInfo().getInterface()).getResourceMethods();

        if (methodsToUse.length > 1) {
            throw new IllegalStateException("Too many getResource methods");
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

                    } else if (((StoreResource) resource).isRenderableAs(acceptedMimeType)) {

                        resource = new RenderedResource(((StoreResource) resource).renderAs(acceptedMimeType), resource);
                        producedResourceType = acceptedMimeType;
                        break;
                    }
                }

                if (producedResourceType == null) {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    return;
                }
            }

            if (resource instanceof RangeableResource) {
                this.configureResourceForByteRangeRequest((RangeableResource)resource, headers);
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
            if (ContentStoreContentService.isClientAbortException(e)) {
                // suppress
            } else {
                logger.error("Unable to handle request", e);

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, format("Failed to handle request for %s", resource.getDescription()), e);
            }
        }
    }

    @Override
    public void setContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource source, MediaType sourceMimeType, Resource target) throws IOException, MethodNotAllowedException {

        AssociatedStoreResource storeResource = (AssociatedStoreResource)target;

        ContentProperty property = storeResource.getContentProperty();

        Object updateObject = storeResource.getAssociation();
        property.setMimeType(updateObject, sourceMimeType.toString());

        String originalFilename = source.getFilename();
        if (source.getFilename() != null && StringUtils.hasText(originalFilename)) {
            property.setOriginalFileName(updateObject, source.getFilename());
        }

        Method[] methodsToUse = getExportedMethodsFor(((StoreResource)target).getStoreInfo().getInterface()).getResourceMethods();

        if (methodsToUse.length == 0) {
            throw new MethodNotAllowedException();
        }

        Method methodToUse = methodsToUse[0];

        try {
            long len = IOUtils.copyLarge(source.getInputStream(), ((WritableResource)target).getOutputStream());
            property.setContentLength(updateObject, len);

            repoInvoker.invokeSave(updateObject);
        } finally {
        }
    }

    @Override
    public void unsetContent(Resource resource) throws MethodNotAllowedException {

        AssociatedStoreResource storeResource = (AssociatedStoreResource)resource;

        ContentProperty property = storeResource.getContentProperty();

        Method[] methodsToUse = getExportedMethodsFor(((StoreResource)resource).getStoreInfo().getInterface()).getResourceMethods();

        if (methodsToUse.length == 0) {
            throw new MethodNotAllowedException();
        }

        if (methodsToUse.length > 1) {
            throw new IllegalStateException("Too many unsetContent methods");
        }

        Object updateObject = storeResource.getAssociation();

        AssociativeStore targetObj = storeResource.getStoreInfo().getImplementation(AssociativeStore.class);

        try {
            storeResource.delete();
            targetObj.unassociate(updateObject, PropertyPath.from(property.getContentPropertyPath()));
            property.setContentLength(updateObject, 0);

            property.setMimeType(updateObject, null);

            property.setOriginalFileName(updateObject, null);

            repoInvoker.invokeSave(updateObject);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    public static StoreExportedMethodsMap getExportedMethodsFor(Class<?> storeInterfaceClass) {

        StoreExportedMethodsMap exportMap = storeExportedMethods.get(storeInterfaceClass);
        if (exportMap == null) {
            storeExportedMethods.put(storeInterfaceClass, new StoreExportedMethodsMap(storeInterfaceClass));
            exportMap = storeExportedMethods.get(storeInterfaceClass);
        }

        return exportMap;
    }

    public static class StoreExportedMethodsMap {

        private static Method[] GETRESOURCE_METHODS = null;
        private static Method[] SETCONTENT_METHODS = null;
        private static Method[] UNSETCONTENT_METHODS = null;
        private static Method[] GETCONTENT_METHODS = null;

        static {
            GETRESOURCE_METHODS = new Method[] {
                    ReflectionUtils.findMethod(AssociativeStore.class, "getResource", Object.class, PropertyPath.class),
                };

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
        private Method[] getResourceMethods;
        private Method[] getContentMethods;
        private Method[] setContentMethods;
        private Method[] unsetContentMethods;

        public StoreExportedMethodsMap(Class<?> storeInterface) {
            this.storeInterface = storeInterface;
            this.getResourceMethods = calculateExports(GETRESOURCE_METHODS);
            this.getContentMethods = calculateExports(GETCONTENT_METHODS);
            this.setContentMethods = calculateExports(SETCONTENT_METHODS);
            this.unsetContentMethods = calculateExports(UNSETCONTENT_METHODS);
        }

        public Method[] getResourceMethods() {
            return this.getResourceMethods;
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

    private void configureResourceForByteRangeRequest(RangeableResource resource, HttpHeaders headers) {
        if (headers.containsKey(HttpHeaders.RANGE)) {
            resource.setRange(headers.getFirst(HttpHeaders.RANGE));
        }
    }
}
