package org.springframework.content.commons.repository;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.springframework.content.commons.property.PropertyPath;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.ReactiveContentStore} instead.
 */
public interface ReactiveContentStore<S, SID extends Serializable> extends ContentRepository<S, SID> {

    Mono<S> setContent(S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer);

    Flux<ByteBuffer> getContent(S entity, PropertyPath path);

    Mono<S> unsetContent(S entity, PropertyPath propertyPath);
}
