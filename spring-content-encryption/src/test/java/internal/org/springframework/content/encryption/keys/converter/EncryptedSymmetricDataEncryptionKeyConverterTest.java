package internal.org.springframework.content.encryption.keys.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import jakarta.xml.bind.DatatypeConverter;
import org.junit.Test;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;

public class EncryptedSymmetricDataEncryptionKeyConverterTest {
    @Test
    public void encodesAndDecodes() {
        var encryptedKey = new EncryptedSymmetricDataEncryptionKey(
                "test",
                "123",
                "8",
                "ABC",
                new byte[5],
                new byte[4]
        );

        var encoded = EncryptedSymmetricDataEncryptionKeyConverter.convert(encryptedKey);

        var decoded = EncryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded, is(equalTo(encryptedKey)));
    }

    @Test
    public void doesNotDecodeDifferentType() {
        var key = new UnencryptedSymmetricDataEncryptionKey(
                "ABC",
                new byte[5],
                DatatypeConverter.parseHexBinary("ff504000f523659020")
        );

        var encoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var decoded = EncryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded, is(nullValue()));
    }

}