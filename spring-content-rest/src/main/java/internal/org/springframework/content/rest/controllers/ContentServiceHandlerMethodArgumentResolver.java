package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import internal.org.springframework.content.rest.utils.ContentStoreUtils;
import org.apache.commons.io.IOUtils;

import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.rest.controllers.ContentService;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static java.lang.String.format;

public class ContentServiceHandlerMethodArgumentResolver extends StoreHandlerMethodArgumentResolver {

    public ContentServiceHandlerMethodArgumentResolver(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, ContentStoreService stores) {
        super(config, repositories, repoInvokerFactory, stores);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ContentService.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        String pathInfo = webRequest.getNativeRequest(HttpServletRequest.class).getPathInfo();
        pathInfo = ContentStoreUtils.storeLookupPath(pathInfo, this.getConfig().getBaseUri());

        String[] pathSegments = pathInfo.split("/");
        if (pathSegments.length < 2) {
            return null;
        }

        String store = pathSegments[1];

        ContentStoreInfo info = ContentStoreUtils.findStore(this.getStores(), store);
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
                    return new ContentStoreContentService(info.getImplementation(ContentStore.class), this.getRepoInvokerFactory().getInvokerFor(domainObj.getClass()), domainObj);
                } else if (AssociativeStore.class.isAssignableFrom(info.getInterface())) {
                    throw new UnsupportedOperationException("AssociativeStoreContentService not implemented");
                }

                // property content
            } else {
                return this.resolveProperty(HttpMethod.valueOf(webRequest.getNativeRequest(HttpServletRequest.class).getMethod()), this.getRepositories(), this.getStores(), pathSegments, (i, e, p, propertyIsEmbedded) -> {

                    if (ContentStore.class.isAssignableFrom(info.getInterface())) {
                        if (propertyIsEmbedded) {
                            return new ContentStoreContentService(info.getImplementation(ContentStore.class), this.getRepoInvokerFactory().getInvokerFor(e.getClass()), e, p);
                        } else {
                            return new ContentStoreContentService(info.getImplementation(ContentStore.class), this.getRepoInvokerFactory().getInvokerFor(p.getClass()), p);
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

        private final ContentStore store;
        private final RepositoryInvoker repoInvoker;
        private final Object domainObj;
        private final Object embeddedProperty;

        public ContentStoreContentService(ContentStore store, RepositoryInvoker repoInvoker, Object domainObj) {
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = null;
        }

        public ContentStoreContentService(ContentStore store, RepositoryInvoker repoInvoker, Object domainObj, Object embeddedProperty) {
            this.store = store;
            this.repoInvoker = repoInvoker;
            this.domainObj = domainObj;
            this.embeddedProperty = embeddedProperty;
        }

        @Override
        public void setContent(InputStream content, MediaType mimeType, String originalFilename, Resource target) throws IOException {
            Object updatedDomainObj = store.setContent(embeddedProperty == null ? domainObj : embeddedProperty, content);

            if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, MimeType.class)) {
                BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, MimeType.class, mimeType.toString());
            }

            if (originalFilename != null && StringUtils.hasText(originalFilename)) {
                if (BeanUtils.hasFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, OriginalFileName.class)) {
                    BeanUtils.setFieldWithAnnotation(embeddedProperty == null ? updatedDomainObj : embeddedProperty, OriginalFileName.class, originalFilename);
                }
            }

            updatedDomainObj = repoInvoker.invokeSave(embeddedProperty == null ? updatedDomainObj : domainObj);
        }

        @Override
        public void unsetContent(Resource resource) {
            Object updatedDomainObj = store.unsetContent(embeddedProperty == null ? domainObj : embeddedProperty);

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
