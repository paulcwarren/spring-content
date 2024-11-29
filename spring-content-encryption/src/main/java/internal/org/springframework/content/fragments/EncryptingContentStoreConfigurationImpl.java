package internal.org.springframework.content.fragments;

import internal.org.springframework.content.encryption.engine.AesCtrEncryptionEngine;
import internal.org.springframework.content.encryption.keys.ContentPropertyDataEncryptionKeyAccessor;
import internal.org.springframework.content.encryption.keys.UnencryptedSymmetricDataEncryptionKeyEncryptor;
import internal.org.springframework.content.encryption.keys.converter.ByteArrayToListConverter;
import internal.org.springframework.content.encryption.keys.converter.EncryptedDataEncryptionKeyGenericConverter;
import internal.org.springframework.content.encryption.keys.converter.ListToByteArrayConverter;
import internal.org.springframework.content.encryption.keys.converter.UnencryptedSymmetricDataEncryptionKeyConvertor;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.store.Store;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.engine.ContentEncryptionEngine;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.DataEncryptionKeyEncryptor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;

@Slf4j
class EncryptingContentStoreConfigurationImpl<S> implements EncryptingContentStoreConfiguration<S> {

    private DataEncryptionKeyAccessor<S, ? extends StoredDataEncryptionKey> dataEncryptionKeyAccessor;
    private List<DataEncryptionKeyEncryptor<? extends StoredDataEncryptionKey>> dataEncryptionKeyEncryptors;
    private ContentEncryptionEngine contentEncryptionEngine;

    private final ConfigurableConversionService conversionService = new GenericConversionService();
    {
        conversionService.addConverter(new ByteArrayToListConverter(conversionService));
        conversionService.addConverter(new ListToByteArrayConverter(conversionService));
        conversionService.addConverter(new EncryptedDataEncryptionKeyGenericConverter(conversionService));
        conversionService.addConverter(byte[].class, UnencryptedSymmetricDataEncryptionKey.class, UnencryptedSymmetricDataEncryptionKeyConvertor::convert);
        conversionService.addConverter(UnencryptedSymmetricDataEncryptionKey.class, byte[].class, UnencryptedSymmetricDataEncryptionKeyConvertor::convert);
    }

    @Override
    public EncryptingContentStoreConfiguration<S> dataEncryptionKeyAccessor(DataEncryptionKeyAccessor<S, ? extends StoredDataEncryptionKey> accessor) {
        this.dataEncryptionKeyAccessor = accessor;
        return this;
    }

    @Override
    public EncryptingContentStoreConfiguration<S> dataEncryptionKeyEncryptors(
            List<DataEncryptionKeyEncryptor<? extends StoredDataEncryptionKey>> encryptors) {
        this.dataEncryptionKeyEncryptors = List.copyOf(encryptors);
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
        return dataEncryptionKeyEncryptors(List.of(new UnencryptedSymmetricDataEncryptionKeyEncryptor()));
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
        if(dataEncryptionKeyEncryptors == null) {
            log.warn("Data Encryption Keys are NOT encrypted: No DataEncryptionKeyEncryptor configured on store {}", storeClass);
            unencryptedDataEncryptionKeys();
        }
        if(contentEncryptionEngine == null) {
            log.warn("Using AES-CTR-128 as default encryption for store {}", storeClass);
            contentEncryptionMethod(ContentEncryptionMethod.AES_CTR_128);
        }
        return new ContentCryptoService(
                mappingContext,
                dataEncryptionKeyAccessor,
                dataEncryptionKeyEncryptors,
                contentEncryptionEngine
        );
    }
}
