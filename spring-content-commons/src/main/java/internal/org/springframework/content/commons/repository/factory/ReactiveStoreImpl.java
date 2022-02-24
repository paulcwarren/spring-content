package internal.org.springframework.content.commons.repository.factory;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.ReactiveContentStore;
import org.springframework.context.ApplicationEventPublisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveStoreImpl implements ReactiveContentStore<Object, Serializable> {

    private static Log logger = LogFactory.getLog(ReactiveStoreImpl.class);

    private final ContentStore<Object, Serializable> delegate;
    private final ApplicationEventPublisher publisher;
    private final Path copyContentRootPath;

    public ReactiveStoreImpl(ContentStore<Object, Serializable> delegate, ApplicationEventPublisher publisher, Path copyContentRootPath) {
        this.delegate = delegate;
        this.publisher = publisher;
        this.copyContentRootPath = copyContentRootPath;
    }

    @Override
    public Mono<Object> setContent(Object entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer) {
        try {
            Mono<Object> result = ((ReactiveContentStore)delegate).setContent(entity, path, contentLen, buffer);
            return result;
        }
        catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Flux<ByteBuffer> getContentAsFlux(Object entity, PropertyPath path) {
        return ((ReactiveContentStore)delegate).getContentAsFlux(entity, path);
    }
}
