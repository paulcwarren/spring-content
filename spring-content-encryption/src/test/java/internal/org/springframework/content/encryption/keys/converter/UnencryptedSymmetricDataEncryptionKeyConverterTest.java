package internal.org.springframework.content.encryption.keys.converter;


import jakarta.xml.bind.DatatypeConverter;
import org.junit.Test;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class UnencryptedSymmetricDataEncryptionKeyConverterTest {
    @Test
    public void encodesAndDecodes() {
        var key = new UnencryptedSymmetricDataEncryptionKey(
                "ABC",
                new byte[5],
                DatatypeConverter.parseHexBinary("ff504000f523659020")
        );

        var encoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var decoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded, is(equalTo(key)));
    }

    @Test
    public void handlesEmptyObject() {
        var key = new UnencryptedSymmetricDataEncryptionKey(
                "",
                new byte[0],
                new byte[0]
        );

        var encoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var decoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded, is(equalTo(key)));
    }

    @Test
    public void doesNotDecodeDifferentType() {
        var encryptedKey = new EncryptedSymmetricDataEncryptionKey(
                "test",
                "123",
                "8",
                "ABC",
                new byte[5],
                new byte[4]
        );

        var encoded = EncryptedSymmetricDataEncryptionKeyConverter.convert(encryptedKey);

        var decoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded, is(nullValue()));
    }

}