package internal.org.springframework.content.encryption.keys.converter;

import internal.org.springframework.content.encryption.keys.converter.ByteBufferCodec.Field;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey.UnencryptedSymmetricDataEncryptionKeyBuilder;

@UtilityClass
public class UnencryptedSymmetricDataEncryptionKeyConverter {
    private static final ByteBufferCodec<UnencryptedSymmetricDataEncryptionKey, UnencryptedSymmetricDataEncryptionKeyBuilder> CODEC = new ByteBufferCodec<>(
            'U',
            List.of(
                    new Field<>(
                            String.class,
                            UnencryptedSymmetricDataEncryptionKey::getAlgorithm,
                            UnencryptedSymmetricDataEncryptionKeyBuilder::algorithm
                    ),
                    new Field<>(
                            byte[].class,
                            UnencryptedSymmetricDataEncryptionKey::getKeyData,
                            UnencryptedSymmetricDataEncryptionKeyBuilder::keyData
                    ),
                    new Field<>(
                            byte[].class,
                            UnencryptedSymmetricDataEncryptionKey::getInitializationVector,
                            UnencryptedSymmetricDataEncryptionKeyBuilder::initializationVector
                    )
            ),
            UnencryptedSymmetricDataEncryptionKey::builder,
            UnencryptedSymmetricDataEncryptionKeyBuilder::build
    );

    public byte[] convert(UnencryptedSymmetricDataEncryptionKey source) {
        return CODEC.encode(source);
    }

    public UnencryptedSymmetricDataEncryptionKey convert(byte[] bytes) {
        return CODEC.decode(bytes);
    }
}

