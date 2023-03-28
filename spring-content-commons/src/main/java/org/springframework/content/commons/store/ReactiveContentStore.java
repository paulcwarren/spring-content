package org.springframework.content.commons.store;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.nio.ByteBuffer;

public interface ReactiveContentStore<S, SID extends Serializable> {

    Mono<S> setContent(S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer);

    Flux<ByteBuffer> getContent(S entity, PropertyPath path);

    Mono<S> unsetContent(S entity, PropertyPath propertyPath);
}
