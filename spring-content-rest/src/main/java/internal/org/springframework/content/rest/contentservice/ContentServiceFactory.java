package internal.org.springframework.content.rest.contentservice;

import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;

import internal.org.springframework.content.rest.io.AssociatedStoreResource;
import internal.org.springframework.content.rest.io.StoreResource;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

public class ContentServiceFactory {

    private final RestConfiguration config;
    private final Repositories repositories;
    private final RepositoryInvokerFactory repoInvokerFactory;
    private final Stores stores;
    private MappingContext mappingContext;
    private final ContentPropertyToExportedContext exportContext;
    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    public ContentServiceFactory(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores, MappingContext mappingContext, ContentPropertyToExportedContext exportContext, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler) {
        this.config = config;
        this.repositories = repositories;
        this.repoInvokerFactory = repoInvokerFactory;
        this.stores = stores;
        this.mappingContext = mappingContext;
        this.exportContext = exportContext;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
    }

    public ContentService getContentService(StoreResource resource) {

        if (ContentStore.class.isAssignableFrom(resource.getStoreInfo().getInterface()) || org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(resource.getStoreInfo().getInterface())) {

            Object entity = ((AssociatedStoreResource)resource).getAssociation();

            return new ContentStoreContentService(config, null, repoInvokerFactory.getInvokerFor(entity.getClass()), mappingContext, exportContext, byteRangeRestRequestHandler);

        } else if (AssociativeStore.class.isAssignableFrom(resource.getStoreInfo().getInterface()) || org.springframework.content.commons.store.AssociativeStore.class.isAssignableFrom(resource.getStoreInfo().getInterface())) {

            Object entity = ((AssociatedStoreResource)resource).getAssociation();
            return new AssociativeStoreContentService(config, null, repoInvokerFactory.getInvokerFor(entity.getClass()), entity, byteRangeRestRequestHandler);

        } else {

            return new StoreContentService(byteRangeRestRequestHandler);
        }
    }
}
