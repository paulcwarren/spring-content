package internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.utils.StoreUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.springframework.content.rest.controllers.ContentService;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ContentServiceHandlerMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ContentServiceHandlerMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores) {
        super(config, repositories, repoInvokerFactory, stores);
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
                    return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(domainObj.getClass()), domainObj);
                } else if (AssociativeStore.class.isAssignableFrom(info.getInterface())) {
                    throw new UnsupportedOperationException("AssociativeStoreContentService not implemented");
                }

                // property content
            } else {
                return this.resolveProperty(HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod()), this.getRepositories(), info, pathSegments, (i, e, p, propertyIsEmbedded) -> {

                    if (ContentStore.class.isAssignableFrom(info.getInterface())) {
                        if (propertyIsEmbedded) {
                            return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(e.getClass()), e, p);
                        } else {
                            return new ContentStoreContentService(getConfig(), info, this.getRepoInvokerFactory().getInvokerFor(p.getClass()), p);
                        }
                    }
                    throw new UnsupportedOperationException(format("ContentService for interface '%s' not implemented", info.getInterface()));
                });
            }

            // do store resource resolution
        } else if (Store.class.isAssignableFrom(info.getInterface())) {

            return new StoreContentService(info.getImplementation(Store.class));
        }

        throw new IllegalArgumentException();
    }

    public static class StoreContentService implements ContentService {

        private final Store store;

        public StoreContentService(Store store) {
            this.store = store;
        }

        @Override
        public void setContent(InputStream content, MediaType mimeType, String originalFilename, Resource target) throws IOException {

            InputStream in = content;
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

        public ContentStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj) {
            this.config = config;
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = null;
        }

        public ContentStoreContentService(RestConfiguration config, StoreInfo store, RepositoryInvoker repoInvoker, Object domainObj, Object embeddedProperty) {
            this.config = config;
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = embeddedProperty;
        }

        @Override
        public void setContent(InputStream content, MediaType mimeType, String originalFilename, Resource target) throws IOException {

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, MimeType.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, MimeType.class, mimeType.toString());
            }

            if (originalFilename != null && StringUtils.hasText(originalFilename)) {
                if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, OriginalFileName.class)) {
                    BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? domainObj : embeddedProperty, OriginalFileName.class, originalFilename);
                }
            }

            Method[] methodsToUse = filterMethods(store.getInterface().getMethods(), this::withSetContentName, this::isExported);

            // exported
            // 0: 405 Method Not Allowed
            // 1: convert arg if necessary and invoke
            // 2: call resolver.  default resolver prefers InputStream.  can be overridden via config.  needs to take headers

            if (methodsToUse.length > 1) {
                RestConfiguration.DomainTypeConfig dtConfig = config.forDomainType(store.getDomainObjectClass());
                methodsToUse = filterMethods(methodsToUse, dtConfig.getSetContentResolver());
            }

            if (methodsToUse.length > 1) {
                throw new UnsupportedOperationException(format("Too many setContent methods exported.  Expected 1.  Got %s", methodsToUse.length));
            }

            if (methodsToUse.length == 0) {
                // respond with 405 method not allowed
            }

            Method methodToUse = methodsToUse[0];
            Object contentArg = convertContentArg(content, methodToUse.getParameterTypes()[1]);

            try {
	            Object targetObj = store.getImplementation(ContentStore.class);
	            Object updatedDomainObj = ReflectionUtils.invokeMethod(methodToUse, targetObj, (embeddedProperty == null ? domainObj : embeddedProperty), contentArg);
	            repoInvoker.invokeSave(embeddedProperty == null ? updatedDomainObj : domainObj);
            } finally {
				cleanup(contentArg);
            }
        }

        private boolean withSetContentName(Method method) {
            return method.getName().equals("setContent");
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

        private Object convertContentArg(InputStream content, Class<?> parameterType) {

            if (InputStream.class.equals(parameterType)) {
                return content;
            } else if (Resource.class.equals(parameterType)) {
                try {
                    File f = Files.createTempFile("", "").toFile();
                    FileUtils.copyInputStreamToFile(content, f);
                    return new FileSystemResource(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                throw new IllegalArgumentException(format("Unsupported content type %s", parameterType.getCanonicalName()));
            }

            return null;
        }

        private Method[] filterMethods(Method[] methods, Predicate<Method>...filters) {

            return Stream.of(methods)
                    .filter(Arrays.stream(filters).reduce(Predicate::and).orElse(t->true))
                    .collect(Collectors.toList())
                    .toArray(new Method[]{});
        }

        @Override
        public void unsetContent(Resource resource) {
            Object updatedDomainObj = store.getImplementation(ContentStore.class).unsetContent(embeddedProperty == null ? domainObj : embeddedProperty);

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, MimeType.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, MimeType.class, null);
            }

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, OriginalFileName.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, OriginalFileName.class, null);
            }

            repoInvoker.invokeSave(embeddedProperty == null ? updatedDomainObj : domainObj);
        }
    }
}
