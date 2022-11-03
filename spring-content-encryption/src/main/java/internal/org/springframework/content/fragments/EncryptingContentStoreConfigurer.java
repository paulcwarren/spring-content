package internal.org.springframework.content.fragments;

import org.springframework.content.encryption.EncryptingContentStore;

public interface EncryptingContentStoreConfigurer<S> {
    void configure(EncryptingContentStoreConfiguration config);
}
