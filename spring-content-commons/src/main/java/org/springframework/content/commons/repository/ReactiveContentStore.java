package org.springframework.content.commons.repository;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.springframework.content.commons.property.PropertyPath;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveContentStore<S, SID extends Serializable> extends ContentRepository<S, SID> {

    Mono<S> setContent(S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer);

    Mono<Flux<ByteBuffer>> getContentAsFlux(S entity, PropertyPath path);
}
