package org.springframework.content.encryption.config;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.content.encryption.engine.ContentEncryptionEngine;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.DataEncryptionKeyWrapper;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * Configuration for encrypting content store
 * @param <S> The type of the store
 */
public interface EncryptingContentStoreConfiguration<S> {

    /**
     * Configures the method used to retrieve data encryption keys from permanent storage
     *
     * @see #encryptionKeyContentProperty(String) for a simple configuration that stores encryption keys in a content property
     */
    EncryptingContentStoreConfiguration<S> dataEncryptionKeyAccessor(DataEncryptionKeyAccessor<S, ?> accessor);

    /**
     * Configures encryption used for encrypting data encryption keys before they are stored
     * <p>
     * Multiple encryption methods can be supplied.
     * All encryptors will be used for encrypting a data encryption key, only the first supporting encryptor will be used to decrypt a data encryption key.
     * <p>
     * The functionality of multiple data encryption key encryptors is intended to be able to recover the data encryption key in case the key encryption key used to encrypt a data encryption key is lost.
     *
     * @see #unencryptedDataEncryptionKeys() for disabling data encryption key encryption
     */
    EncryptingContentStoreConfiguration<S> dataEncryptionKeyWrappers(List<DataEncryptionKeyWrapper<?>> encryptors);

    /**
     * Configures the method used for encrypting the content before it is stored
     */
    EncryptingContentStoreConfiguration<S> contentEncryptionEngine(ContentEncryptionEngine contentEncryptionEngine);

    /**
     * Configure the content property where the data encryption key is stored
     *
     * @see #dataEncryptionKeyAccessor(DataEncryptionKeyAccessor) for configuring a general method of storing the data encryption key
     */
    EncryptingContentStoreConfiguration<S> encryptionKeyContentProperty(String encryptionKeyContentProperty);

    /**
     * Do not encrypt the data encryption keys when they are persisted to permanent storage
     *
     * @see #dataEncryptionKeyWrappers(List) for configuring a general methods to encrypt data encryption keys
     */
    EncryptingContentStoreConfiguration<S> unencryptedDataEncryptionKeys();

    /**
     * Configure converters to be used for converting between a {@link StoredDataEncryptionKey} and its representation in an entity
     * @param converterConfigurer Configure converters
     */
    EncryptingContentStoreConfiguration<S> configureConverters(Consumer<ConverterRegistry> converterConfigurer);

    /**
     * Configure encryption to be used for encrypting content before it is stored
     * @see #contentEncryptionEngine(ContentEncryptionEngine) for configuring a general method for encrypting content
     */
    EncryptingContentStoreConfiguration<S> contentEncryptionMethod(ContentEncryptionMethod contentEncryptionMethod);

    /**
     * Predefined content encryption modes
     */
    enum ContentEncryptionMethod {
        AES_CTR_128,
        AES_CTR_192,
        AES_CTR_256
    }
}
