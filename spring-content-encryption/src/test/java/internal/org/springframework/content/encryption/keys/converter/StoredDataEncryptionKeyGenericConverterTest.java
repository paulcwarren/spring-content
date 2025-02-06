package internal.org.springframework.content.encryption.keys.converter;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

public class StoredDataEncryptionKeyGenericConverterTest {
    private static final ConfigurableConversionService conversionService = new DefaultConversionService();

    static {
        conversionService.addConverter(new StoredDataEncryptionKeyGenericConverter(conversionService));

        conversionService.addConverter(UnencryptedSymmetricDataEncryptionKey.class, byte[].class, UnencryptedSymmetricDataEncryptionKeyConverter::convert);
        conversionService.addConverter(byte[].class, UnencryptedSymmetricDataEncryptionKey.class, UnencryptedSymmetricDataEncryptionKeyConverter::convert);

        conversionService.addConverter(EncryptedSymmetricDataEncryptionKey.class, byte[].class, EncryptedSymmetricDataEncryptionKeyConverter::convert);
        conversionService.addConverter(byte[].class, EncryptedSymmetricDataEncryptionKey.class, EncryptedSymmetricDataEncryptionKeyConverter::convert);
    }

    @Test
    public void convertsBasedOnSourceType() {
        var key = new UnencryptedSymmetricDataEncryptionKey("Test", new byte[0], new byte[0]);

        var conversionResult = conversionService.convert(key, TypeDescriptor.valueOf(StoredDataEncryptionKey.class), TypeDescriptor.valueOf(byte[].class));

        var expectedResult = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        assertThat(conversionResult, is(equalTo(expectedResult)));
    }

    @Test
    public void convertsBasedOnTargetType_unencryptedKey() {
        var key = new UnencryptedSymmetricDataEncryptionKey("Test", new byte[0], new byte[0]);
        var encoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var conversionResult = conversionService.convert(encoded, StoredDataEncryptionKey.class);

        assertThat(conversionResult, is(equalTo(key)));
    }

    @Test
    public void convertsBasedOnTargetType_encryptedKey() {
        var key = new EncryptedSymmetricDataEncryptionKey("test", "1", "1", "Test", new byte[0], new byte[0]);
        var encoded = EncryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var conversionResult = conversionService.convert(encoded, StoredDataEncryptionKey.class);

        assertThat(conversionResult, is(equalTo(key)));
    }

}