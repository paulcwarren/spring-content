package internal.org.springframework.content.encryption.keys.converter;

import internal.org.springframework.content.encryption.keys.converter.ByteBufferCodec.Field;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey.EncryptedSymmetricDataEncryptionKeyBuilder;

@UtilityClass
public class EncryptedSymmetricDataEncryptionKeyConverter {
    private static final ByteBufferCodec<EncryptedSymmetricDataEncryptionKey, EncryptedSymmetricDataEncryptionKeyBuilder> CODEC = new ByteBufferCodec<>(
            'E',
            List.of(
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::getWrappingAlgorithm,
                            EncryptedSymmetricDataEncryptionKeyBuilder::wrappingAlgorithm
                    ),
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::getWrappingKeyId,
                            EncryptedSymmetricDataEncryptionKeyBuilder::wrappingKeyId
                    ),
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::getWrappingKeyVersion,
                            EncryptedSymmetricDataEncryptionKeyBuilder::wrappingKeyVersion
                    ),
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::getDataEncryptionAlgorithm,
                            EncryptedSymmetricDataEncryptionKeyBuilder::dataEncryptionAlgorithm
                    ),
                    new Field<>(
                            byte[].class,
                            EncryptedSymmetricDataEncryptionKey::getEncryptedKeyData,
                            EncryptedSymmetricDataEncryptionKeyBuilder::encryptedKeyData
                    ),
                    new Field<>(
                            byte[].class,
                            EncryptedSymmetricDataEncryptionKey::getInitializationVector,
                            EncryptedSymmetricDataEncryptionKeyBuilder::initializationVector
                    )
            ),
            EncryptedSymmetricDataEncryptionKey::builder,
            EncryptedSymmetricDataEncryptionKeyBuilder::build
    );

    public byte[] convert(EncryptedSymmetricDataEncryptionKey source) {
        return CODEC.encode(source);
    }

    public EncryptedSymmetricDataEncryptionKey convert(byte[] bytes) {
        return CODEC.decode(bytes);
    }
}

