package org.springframework.content.encryption.config;

import java.util.function.Consumer;
import org.springframework.core.convert.converter.ConverterRegistry;

public interface EncryptingContentStoreConfiguration {
    EncryptingContentStoreConfiguration encryptionKeyContentProperty(String encryptionKeyContentProperty);
    EncryptingContentStoreConfiguration configureConvertors(Consumer<ConverterRegistry> converterConfigurer);
}
