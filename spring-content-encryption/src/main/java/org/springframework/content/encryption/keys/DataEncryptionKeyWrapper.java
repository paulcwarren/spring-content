package org.springframework.content.encryption.keys;

import org.springframework.content.encryption.engine.ContentEncryptionEngine.EncryptionParameters;

/**
 * Encryption and decryption of data encryption keys
 * @param <T> format of the encrypted data encryption key
 */
public interface DataEncryptionKeyWrapper<T extends StoredDataEncryptionKey> {

    /**
     * Checks if this encryptor can decrypt a certain encrypted key
     * @param storedDataEncryptionKey The key to check support for
     */
    boolean supports(StoredDataEncryptionKey storedDataEncryptionKey);

    /**
     * Encrypt the data encryption key
     * @param dataEncryptionParameters Unencrypted data encryption parameters
     * @return A representation of the encrypted data encryption key
     */
    T wrapEncryptionKey(EncryptionParameters dataEncryptionParameters);

    /**
     * Decrypt the encrypted data encryption key
     * @param encryptedDataEncryptionKey A representation of the encrypted data encryption key
     * @return Unencrypted data encryption parameters
     */
    EncryptionParameters unwrapEncryptionKey(T encryptedDataEncryptionKey);


}
