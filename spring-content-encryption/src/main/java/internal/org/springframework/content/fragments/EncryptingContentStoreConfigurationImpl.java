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
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.keys.DataEncryptionKeyEncryptor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;

class EncryptingContentStoreConfigurationImpl implements EncryptingContentStoreConfiguration {

    private String encryptionKeyContentProperty = "encryption";

    private final ConfigurableConversionService conversionService = new GenericConversionService();
    {
        conversionService.addConverter(new ByteArrayToListConverter(conversionService));
        conversionService.addConverter(new ListToByteArrayConverter(conversionService));
        conversionService.addConverter(new EncryptedDataEncryptionKeyGenericConverter(conversionService));
        conversionService.addConverter(byte[].class, UnencryptedSymmetricDataEncryptionKey.class, UnencryptedSymmetricDataEncryptionKeyConvertor::convert);
        conversionService.addConverter(UnencryptedSymmetricDataEncryptionKey.class, byte[].class, UnencryptedSymmetricDataEncryptionKeyConvertor::convert);
    }

    @Override
    public EncryptingContentStoreConfiguration encryptionKeyContentProperty(String encryptionKeyContentProperty) {
        this.encryptionKeyContentProperty = encryptionKeyContentProperty;
        return this;
    }

    @Override
    public EncryptingContentStoreConfiguration configureConvertors(Consumer<ConverterRegistry> converterConfigurer) {
        converterConfigurer.accept(conversionService);
        return this;
    }

    <S, DEK extends StoredDataEncryptionKey> ContentCryptoService<S, DEK> initializeCryptoService(MappingContext mappingContext) {
        return new ContentCryptoService<S, DEK>(
                mappingContext,
                new ContentPropertyDataEncryptionKeyAccessor<>(encryptionKeyContentProperty, conversionService),
                List.of(
                        (DataEncryptionKeyEncryptor<DEK>) new UnencryptedSymmetricDataEncryptionKeyEncryptor()
                ),
                new AesCtrEncryptionEngine(128)
        );
    }
}
