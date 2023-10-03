package org.springframework.content.commons.store;

@FunctionalInterface
public interface StoreExceptionTranslator {
    StoreAccessException translate(RuntimeException re);
}
