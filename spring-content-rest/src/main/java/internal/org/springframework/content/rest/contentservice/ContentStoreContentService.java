package internal.org.springframework.content.rest.contentservice;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.rest.config.RestConfiguration.Resolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import internal.org.springframework.content.rest.io.AssociatedStoreResource;
import internal.org.springframework.content.rest.io.RenderedResource;
import internal.org.springframework.content.rest.io.StoreResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

public class ContentStoreContentService implements ContentService {

    private static final Logger logger = LoggerFactory.getLogger(ContentStoreContentService.class);

    private static final Map<String, StoreExportedMethodsMap> storeExportedMethods = new HashMap<>();

    private final RestConfiguration config;
    private final RepositoryInvoker repoInvoker;
    private final MappingContext mappingContext;
    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;


    public ContentStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, MappingContext mappingContext, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
        this.config = config;
        this.repoInvoker = repoInvoker;
        this.mappingContext = mappingContext;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
    }

    @Override
    public void getContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource resource, MediaType resourceType)
            throws ResponseStatusException, MethodNotAllowedException {

        AssociatedStoreResource storeResource = (AssociatedStoreResource)resource;
        ContentProperty property = storeResource.getContentProperty();

        Method[] methodsToUse = getExportedMethodsFor(storeResource.getStoreInfo().getInterface(), PropertyPath.from(property.getContentPropertyPath())).getContentMethods();

        if (methodsToUse.length > 1) {
            throw new IllegalStateException("Too many getContent methods");
        }

        if (methodsToUse.length == 0) {
            throw new MethodNotAllowedException();
        }

        try {
            MediaType producedResourceType = null;
            Resource storedRenditionResource = null;
            List<MediaType> acceptedMimeTypes = headers.getAccept();
            if (acceptedMimeTypes.size() > 0) {

                MediaType.sortBySpecificityAndQuality(acceptedMimeTypes);
                for (MediaType acceptedMimeType : acceptedMimeTypes) {

                    if (acceptedMimeType.includes(resourceType) && matchParameters(acceptedMimeType, resourceType)) {

                        producedResourceType = resourceType;
                        break;
                    } else if ((storedRenditionResource = findStoredRendition(storeResource, acceptedMimeType)) != null) {

                         resource = storedRenditionResource;
                         producedResourceType = acceptedMimeType;
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

        AssociatedStoreResource storeResource = (AssociatedStoreResource)target;
        ContentProperty property = storeResource.getContentProperty();

        Method[] methodsToUse = getExportedMethodsFor(storeResource.getStoreInfo().getInterface(), PropertyPath.from(property.getContentPropertyPath())).setContentMethods();

        if (methodsToUse.length > 1) {
            RestConfiguration.DomainTypeConfig dtConfig = config.forDomainType(storeResource.getStoreInfo().getDomainObjectClass());
            methodsToUse = filterMethods(methodsToUse, dtConfig.getSetContentResolver(), headers);
        }

        if (methodsToUse.length > 1) {
            throw new UnsupportedOperationException(format("Too many setContent methods exported.  Expected 1.  Got %s", methodsToUse.length));
        }

        if (methodsToUse.length == 0) {
            throw new MethodNotAllowedException();
        }

        Method methodToUse = methodsToUse[0];
        Object contentArg = convertContentArg(source, methodToUse.getParameterTypes()[indexOfContentArg(methodToUse.getParameterTypes())]);

        try {
            Object targetObj = storeResource.getStoreInfo().getImplementation(ContentStore.class);

            ReflectionUtils.makeAccessible(methodToUse);

            Object updatedDomainObj = ReflectionUtils.invokeMethod(methodToUse, targetObj, storeResource.getAssociation(), PropertyPath.from(property.getContentPropertyPath()), contentArg);

            property.setMimeType(updatedDomainObj, sourceMimeType.toString());

            String originalFilename = source.getFilename();
            if (source.getFilename() != null && StringUtils.hasText(originalFilename)) {
                property.setOriginalFileName(updatedDomainObj, source.getFilename());
            }

            repoInvoker.invokeSave(updatedDomainObj);
        } finally {
            cleanup(contentArg);
        }
    }

    @Override
    public void unsetContent(Resource resource) throws MethodNotAllowedException {

        AssociatedStoreResource storeResource = (AssociatedStoreResource)resource;
        ContentProperty property = storeResource.getContentProperty();

        Method[] methodsToUse = getExportedMethodsFor(storeResource.getStoreInfo().getInterface(), PropertyPath.from(property.getContentPropertyPath())).unsetContentMethods();

        if (methodsToUse.length == 0) {
            throw new MethodNotAllowedException();
        }

        if (methodsToUse.length > 1) {
            throw new IllegalStateException("Too many unsetContent methods");
        }

        Object updateObject = storeResource.getAssociation();

        Object targetObj = storeResource.getStoreInfo().getImplementation(ContentStore.class);

        ReflectionUtils.makeAccessible(methodsToUse[0]);

        Object updatedDomainObj = ReflectionUtils.invokeMethod(methodsToUse[0], targetObj, updateObject, PropertyPath.from(property.getContentPropertyPath()));

        updateObject = updatedDomainObj;
        property.setMimeType(updateObject, null);
        property.setOriginalFileName(updateObject, null);

        repoInvoker.invokeSave(updateObject);
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

    private void configureResourceForByteRangeRequest(RangeableResource resource, HttpHeaders headers) {
        if (headers.containsKey(HttpHeaders.RANGE)) {
            resource.setRange(headers.getFirst(HttpHeaders.RANGE));
        }
    }

    private int indexOfContentArg(Class<?>[] paramTypes) {
        for (int i=0; i < paramTypes.length; i++) {
            if (InputStream.class.equals(paramTypes[i]) || Resource.class.equals(paramTypes[i])) {
                return i;
            }
        }

        return 0;
    }

    private Resource findStoredRendition(AssociatedStoreResource storeResource, MediaType acceptedMimeType) {

        Resource storedRenditionResource = null;

        Object entity = storeResource.getAssociation();

        for (ContentProperty contentProperty : this.mappingContext.getContentProperties(entity.getClass())) {
            Object candidateMimeType = contentProperty.getMimeType(entity);
            if (candidateMimeType != null) {
                String strCandidateMimeType = candidateMimeType.toString();
                try {
                    MediaType candidateType = MediaType.parseMediaType(strCandidateMimeType);
                    if (acceptedMimeType.includes(candidateType) && matchParameters(acceptedMimeType, candidateType)) {
                        ContentStore store = storeResource.getStoreInfo().getImplementation(ContentStore.class);
                        storedRenditionResource = new RenderedResource(store.getContent(entity, PropertyPath.from(contentProperty.getContentPropertyPath())), storeResource);
                        break;
                    }
                } catch (InvalidMediaTypeException imte) {}
            }
        }

        return storedRenditionResource;
    }

    public static StoreExportedMethodsMap getExportedMethodsFor(Class<?> storeInterfaceClass, PropertyPath path) {

        StoreExportedMethodsMap exportMap = storeExportedMethods.get(storeInterfaceClass.getCanonicalName()+"#"+path.toString());
        if (exportMap == null) {
            storeExportedMethods.put(storeInterfaceClass.getCanonicalName()+"#"+path.toString(), new StoreExportedMethodsMap(storeInterfaceClass, path));
            exportMap = storeExportedMethods.get(storeInterfaceClass.getCanonicalName()+"#"+path.toString());
        }

        return exportMap;
    }

    public static class StoreExportedMethodsMap {

        private static Method[] SETCONTENT_METHODS = null;
        private static Method[] UNSETCONTENT_METHODS = null;
        private static Method[] GETCONTENT_METHODS = null;

        static {
            SETCONTENT_METHODS = new Method[] {
                ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class),
                ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class),
            };

            UNSETCONTENT_METHODS = new Method[] {
                ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class, PropertyPath.class),
            };

            GETCONTENT_METHODS = new Method[] {
                ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class, PropertyPath.class),
            };
        }

        private Class<?> storeInterface;
        private PropertyPath path;
        private Method[] getContentMethods;
        private Method[] setContentMethods;
        private Method[] unsetContentMethods;

        public StoreExportedMethodsMap(Class<?> storeInterface, PropertyPath path) {
            this.storeInterface = storeInterface;
            this.getContentMethods = calculateExports(GETCONTENT_METHODS, path);
            this.setContentMethods = calculateExports(SETCONTENT_METHODS, path);
            this.unsetContentMethods = calculateExports(UNSETCONTENT_METHODS, path);
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

        private Method[] calculateExports(Method[] storeMethods, PropertyPath path) {

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
                                    List<String> paths = Arrays.asList(r.paths());
                                    if (paths.contains("*") || paths.contains(path.getName())) {
                                        unexportedMethods.add(m);
                                    }
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

            if (m.getParameterTypes().length != dm.getParameterTypes().length) {
                return false;
            }

            for (int i=0; i < m.getParameterTypes().length; i++) {

                if (!m.getParameterTypes()[i].isAssignableFrom(dm.getParameterTypes()[i])) {

                    return false;
                }
            }

            return true;
        }
    }

    public static boolean isClientAbortException(Exception e) {
        if (e.getClass().getSimpleName().equals("ClientAbortException")) {
            return true;
        }
        return false;
    }
}
