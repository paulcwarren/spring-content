package internal.org.springframework.content.rest.controllers;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.rest.controllers.ContentService;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;

import internal.org.springframework.content.rest.controllers.ContentServiceHandlerMethodArgumentResolver.ContentStoreContentService;
import internal.org.springframework.content.rest.controllers.ContentServiceHandlerMethodArgumentResolver.StoreContentService;
import internal.org.springframework.content.rest.io.AssociatedResource;
import internal.org.springframework.content.rest.io.StoreResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

public class ContentServiceFactory {

    private final RestConfiguration config;
    private final Repositories repositories;
    private final RepositoryInvokerFactory repoInvokerFactory;
    private final Stores stores;
    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    public ContentServiceFactory(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
        this.config = config;
        this.repositories = repositories;
        this.repoInvokerFactory = repoInvokerFactory;
        this.stores = stores;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
    }

    public ContentService getContentService(StoreInfo storeInfo, StoreResource resource) {

        if (ContentStore.class.isAssignableFrom(storeInfo.getInterface())) {

            Object entity = ((AssociatedResource)resource).getAssociation();
            return new ContentStoreContentService(config, storeInfo, repoInvokerFactory.getInvokerFor(entity.getClass()), entity, byteRangeRestRequestHandler);
        } else if (AssociativeStore.class.isAssignableFrom(storeInfo.getInterface())) {

            throw new UnsupportedOperationException("AssociativeStore not supported");
        } else {

            return new StoreContentService(storeInfo.getImplementation(Store.class), byteRangeRestRequestHandler);
        }
    }
}
