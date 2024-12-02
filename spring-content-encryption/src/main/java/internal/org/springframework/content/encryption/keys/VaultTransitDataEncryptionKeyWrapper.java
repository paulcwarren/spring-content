package internal.org.springframework.content.encryption.keys;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.content.encryption.engine.ContentEncryptionEngine.EncryptionParameters;
import org.springframework.content.encryption.keys.DataEncryptionKeyWrapper;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;

@RequiredArgsConstructor
public class VaultTransitDataEncryptionKeyWrapper implements DataEncryptionKeyWrapper<EncryptedSymmetricDataEncryptionKey> {
    private static final String WRAPPING_ALGORITHM = "vault-transit";

    private final VaultTransitOperations transitOperations;
    private final String wrappingKeyId;

    @Override
    public boolean supports(StoredDataEncryptionKey storedDataEncryptionKey) {
        if(storedDataEncryptionKey instanceof EncryptedSymmetricDataEncryptionKey dek) {
            return Objects.equals(dek.getWrappingAlgorithm(), WRAPPING_ALGORITHM) && Objects.equals(dek.getWrappingKeyId(), wrappingKeyId);
        }
        return false;
    }

    @Override
    public EncryptedSymmetricDataEncryptionKey wrapEncryptionKey(EncryptionParameters dataEncryptionParameters) {
        var plainText = Plaintext.of(dataEncryptionParameters.getSecretKey().getEncoded());
        var encryptedKey = transitOperations.encrypt(wrappingKeyId, plainText);
        return new EncryptedSymmetricDataEncryptionKey(
                WRAPPING_ALGORITHM,
                wrappingKeyId,
                Integer.toString(encryptedKey.getContext().getKeyVersion()),
                dataEncryptionParameters.getSecretKey().getAlgorithm(),
                encryptedKey.getCiphertext().getBytes(StandardCharsets.UTF_8),
                dataEncryptionParameters.getInitializationVector()
        );
    }

    @Override
    public EncryptionParameters unwrapEncryptionKey(EncryptedSymmetricDataEncryptionKey encryptedDataEncryptionKey) {
        var cipherText = Ciphertext.of(
                new String(encryptedDataEncryptionKey.getEncryptedKeyData(), StandardCharsets.UTF_8)
        );

        var decryptedKey = transitOperations.decrypt(wrappingKeyId, cipherText);

        return new EncryptionParameters(
                new SecretKeySpec(decryptedKey.getPlaintext(), encryptedDataEncryptionKey.getDataEncryptionAlgorithm()),
                encryptedDataEncryptionKey.getInitializationVector()
        );
    }
}
