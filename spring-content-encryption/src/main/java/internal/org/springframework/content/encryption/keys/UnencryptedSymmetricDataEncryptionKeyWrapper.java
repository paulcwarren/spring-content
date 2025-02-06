package internal.org.springframework.content.encryption.keys;

import javax.crypto.spec.SecretKeySpec;
import org.springframework.content.encryption.engine.ContentEncryptionEngine.EncryptionParameters;
import org.springframework.content.encryption.keys.DataEncryptionKeyWrapper;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;

public class UnencryptedSymmetricDataEncryptionKeyWrapper implements
        DataEncryptionKeyWrapper<UnencryptedSymmetricDataEncryptionKey> {
    @Override
    public boolean supports(StoredDataEncryptionKey storedDataEncryptionKey) {
        return storedDataEncryptionKey instanceof UnencryptedSymmetricDataEncryptionKey;
    }

    @Override
    public EncryptionParameters unwrapEncryptionKey(UnencryptedSymmetricDataEncryptionKey encryptedDataEncryptionKey) {
        return new EncryptionParameters(
                new SecretKeySpec(encryptedDataEncryptionKey.getKeyData(), encryptedDataEncryptionKey.getAlgorithm()),
                encryptedDataEncryptionKey.getInitializationVector()
        );
    }

    @Override
    public UnencryptedSymmetricDataEncryptionKey wrapEncryptionKey(EncryptionParameters dataEncryptionParameters) {
        return new UnencryptedSymmetricDataEncryptionKey(dataEncryptionParameters.getSecretKey().getAlgorithm(),
                dataEncryptionParameters.getSecretKey().getEncoded(),
                dataEncryptionParameters.getInitializationVector());
    }

}
