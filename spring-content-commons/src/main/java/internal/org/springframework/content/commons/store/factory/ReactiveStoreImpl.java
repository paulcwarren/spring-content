package internal.org.springframework.content.commons.store.factory;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.context.ApplicationEventPublisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveStoreImpl implements ReactiveContentStore<Object, Serializable> {

    private static Log logger = LogFactory.getLog(ReactiveStoreImpl.class);

    private final ReactiveContentStore<Object, Serializable> delegate;
    private final ApplicationEventPublisher publisher;

    public ReactiveStoreImpl(ReactiveContentStore<Object, Serializable> delegate, ApplicationEventPublisher publisher) {
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public Mono<Object> setContent(Object entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer) {
        try {
            Mono<Object> result = delegate.setContent(entity, path, contentLen, buffer);
            return result;
        }
        catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Flux<ByteBuffer> getContent(Object entity, PropertyPath path) {
        return delegate.getContent(entity, path);
    }

    @Override
    public Mono<Object> unsetContent(Object entity, PropertyPath propertyPath) {
        return delegate.unsetContent(entity, propertyPath);
    }
}
