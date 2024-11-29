package org.springframework.content.encryption.config;


public interface EncryptingContentStoreConfigurer<S> {
    void configure(EncryptingContentStoreConfiguration<S> config);
}
