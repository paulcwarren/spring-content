package internal.org.springframework.content.fragments;

import internal.org.springframework.content.encryption.engine.AesCtrEncryptionEngine;
import internal.org.springframework.content.encryption.keys.ContentPropertyDataEncryptionKeyAccessor;
import internal.org.springframework.content.encryption.keys.UnencryptedSymmetricDataEncryptionKeyWrapper;
import internal.org.springframework.content.encryption.keys.converter.ByteArrayToListConverter;
import internal.org.springframework.content.encryption.keys.converter.StoredDataEncryptionKeyGenericConverter;
import internal.org.springframework.content.encryption.keys.converter.EncryptedSymmetricDataEncryptionKeyConverter;
import internal.org.springframework.content.encryption.keys.converter.ListToByteArrayConverter;
import internal.org.springframework.content.encryption.keys.converter.UnencryptedSymmetricDataEncryptionKeyConverter;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.store.Store;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.engine.ContentEncryptionEngine;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.DataEncryptionKeyWrapper;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;

@Slf4j
class EncryptingContentStoreConfigurationImpl<S> implements EncryptingContentStoreConfiguration<S> {

    private DataEncryptionKeyAccessor<S, ? extends StoredDataEncryptionKey> dataEncryptionKeyAccessor;
    private List<DataEncryptionKeyWrapper<? extends StoredDataEncryptionKey>> dataEncryptionKeyWrappers;
    private ContentEncryptionEngine contentEncryptionEngine;

    private final ConfigurableConversionService conversionService = new GenericConversionService();
    {
        conversionService.addConverter(new ByteArrayToListConverter(conversionService));
        conversionService.addConverter(new ListToByteArrayConverter(conversionService));
        conversionService.addConverter(new StoredDataEncryptionKeyGenericConverter(conversionService));

        conversionService.addConverter(byte[].class, UnencryptedSymmetricDataEncryptionKey.class, UnencryptedSymmetricDataEncryptionKeyConverter::convert);
        conversionService.addConverter(UnencryptedSymmetricDataEncryptionKey.class, byte[].class, UnencryptedSymmetricDataEncryptionKeyConverter::convert);

        conversionService.addConverter(byte[].class, EncryptedSymmetricDataEncryptionKey.class, EncryptedSymmetricDataEncryptionKeyConverter::convert);
        conversionService.addConverter(EncryptedSymmetricDataEncryptionKey.class, byte[].class, EncryptedSymmetricDataEncryptionKeyConverter::convert);
    }

    @Override
    public EncryptingContentStoreConfiguration<S> dataEncryptionKeyAccessor(DataEncryptionKeyAccessor<S, ? extends StoredDataEncryptionKey> accessor) {
        this.dataEncryptionKeyAccessor = accessor;
        return this;
    }

    @Override
    public EncryptingContentStoreConfiguration<S> dataEncryptionKeyWrappers(
            List<DataEncryptionKeyWrapper<? extends StoredDataEncryptionKey>> wrappers
    ) {
        this.dataEncryptionKeyWrappers = List.copyOf(wrappers);
        return this;
    }

    @Override
    public EncryptingContentStoreConfiguration<S> contentEncryptionEngine(ContentEncryptionEngine contentEncryptionEngine) {
        this.contentEncryptionEngine = contentEncryptionEngine;
        return this;
    }

    @Override
    public EncryptingContentStoreConfiguration<S> encryptionKeyContentProperty(String encryptionKeyContentProperty) {
        return dataEncryptionKeyAccessor(new ContentPropertyDataEncryptionKeyAccessor<>(encryptionKeyContentProperty, conversionService));
    }

    @Override
    public EncryptingContentStoreConfiguration<S> unencryptedDataEncryptionKeys() {
        return dataEncryptionKeyWrappers(List.of(new UnencryptedSymmetricDataEncryptionKeyWrapper()));
    }

    @Override
    public EncryptingContentStoreConfiguration<S> configureConverters(Consumer<ConverterRegistry> converterConfigurer) {
        converterConfigurer.accept(conversionService);
        return this;
    }

    @Override
    public EncryptingContentStoreConfiguration<S> contentEncryptionMethod(ContentEncryptionMethod contentEncryptionMethod) {
        return contentEncryptionEngine(switch(contentEncryptionMethod) {
            case AES_CTR_128 -> new AesCtrEncryptionEngine(128);
            case AES_CTR_192 -> new AesCtrEncryptionEngine(192);
            case AES_CTR_256 -> new AesCtrEncryptionEngine(256);
        });
    }

    ContentCryptoService<S, ?> initializeCryptoService(MappingContext mappingContext, Class<? extends Store<?>> storeClass) {
        if(dataEncryptionKeyAccessor == null) {
            encryptionKeyContentProperty("encryption");
        }
        if(dataEncryptionKeyWrappers == null) {
            log.warn("Data Encryption Keys are NOT encrypted: No DataEncryptionKeyWrapper configured on store {}", storeClass);
            unencryptedDataEncryptionKeys();
        }
        if(contentEncryptionEngine == null) {
            log.warn("Using AES-CTR-128 as default encryption for store {}", storeClass);
            contentEncryptionMethod(ContentEncryptionMethod.AES_CTR_128);
        }

        if(dataEncryptionKeyWrappers.isEmpty()) {
            throw new IllegalStateException("No DataEncryptionKeyWrappers configured on store %s. Refusing to start as encrypted content would be unrecoverable.".formatted(storeClass));
        }
        return new ContentCryptoService(
                mappingContext,
                dataEncryptionKeyAccessor,
                dataEncryptionKeyWrappers,
                contentEncryptionEngine
        );
    }
}
