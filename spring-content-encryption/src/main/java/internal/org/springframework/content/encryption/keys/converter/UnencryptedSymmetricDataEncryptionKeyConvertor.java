package internal.org.springframework.content.encryption.keys.converter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;

@UtilityClass
public class UnencryptedSymmetricDataEncryptionKeyConvertor {

    public byte[] convert(UnencryptedSymmetricDataEncryptionKey source) {
        var algorithm = source.getAlgorithm().getBytes(StandardCharsets.UTF_8);
        var algorithmSize = algorithm.length;
        var keyDataSize = source.getKeyData().length;
        var ivSize = source.getInitializationVector().length;
        var sizeBytes = 3*Integer.BYTES;

        var buffer = ByteBuffer.allocate(Character.BYTES + sizeBytes + algorithmSize + keyDataSize + ivSize);
        buffer.putChar('U'); // Type marker
        buffer.putInt(algorithmSize);
        buffer.putInt(keyDataSize);
        buffer.putInt(ivSize);
        buffer.put(algorithm);
        buffer.put(source.getKeyData());
        buffer.put(source.getInitializationVector());
        return buffer.array();
    }

    public UnencryptedSymmetricDataEncryptionKey convert(byte[] bytes) {
        var buffer = ByteBuffer.wrap(bytes);
        if (buffer.getChar() != 'U') {
            return null;
        }

        var algorithmSize = buffer.getInt();
        var keyDataSize = buffer.getInt();
        var ivSize = buffer.getInt();

        var algorithm = new byte[algorithmSize];
        var keyData = new byte[keyDataSize];
        var iv = new byte[ivSize];

        buffer.get(algorithm);
        buffer.get(keyData);
        buffer.get(iv);

        return new UnencryptedSymmetricDataEncryptionKey(
                new String(algorithm, StandardCharsets.UTF_8),
                keyData,
                iv
        );
    }
}

