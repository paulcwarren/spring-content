package org.springframework.content.encryption.keys;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Representation of the stored data encryption key
 */
public sealed interface StoredDataEncryptionKey {

    /**
     * An unencrypted symmetric data encryption key
     */
    @RequiredArgsConstructor
    @Getter
    final class UnencryptedSymmetricDataEncryptionKey implements StoredDataEncryptionKey {

        /**
         * The encryption algorithm used for data encryption
         */
        private final String algorithm;

        /**
         * The symmetric key for data encryption
         */
        private final byte[] keyData;

        /**
         * The IV for the encryption algorithm
         */
        private final byte[] initializationVector;
    }
}
