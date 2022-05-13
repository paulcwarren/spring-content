package internal.org.springframework.content.rest.contentservice;

import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
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
    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;
    private final ApplicationEventPublisher publisher;

    public ContentServiceFactory(RestConfiguration config, Repositories repositories, RepositoryInvokerFactory repoInvokerFactory, Stores stores, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, ApplicationEventPublisher publisher) {
        this.config = config;
        this.repositories = repositories;
        this.repoInvokerFactory = repoInvokerFactory;
        this.stores = stores;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
        this.publisher = publisher;
    }

    public ContentService getContentService(StoreResource resource) {

        if (ContentStore.class.isAssignableFrom(resource.getStoreInfo().getInterface())) {

            Object entity = ((AssociatedStoreResource)resource).getAssociation();

            return new EventingContentService(publisher, new ContentStoreContentService(config, null, repoInvokerFactory.getInvokerFor(entity.getClass()), entity, byteRangeRestRequestHandler));

        } else if (AssociativeStore.class.isAssignableFrom(resource.getStoreInfo().getInterface())) {

            Object entity = ((AssociatedStoreResource)resource).getAssociation();
            return new AssociativeStoreContentService(config, null, repoInvokerFactory.getInvokerFor(entity.getClass()), entity, byteRangeRestRequestHandler);

        } else {

            return new StoreContentService(byteRangeRestRequestHandler);
        }
    }
}
