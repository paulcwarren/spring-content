package org.springframework.content.encryption.keys;

import lombok.Builder;
import lombok.EqualsAndHashCode;
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
    @Builder
    @EqualsAndHashCode
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

    /**
     * An encrypted symmetric data encryption key
     */
    @RequiredArgsConstructor
    @Getter
    @Builder
    @EqualsAndHashCode
    final class EncryptedSymmetricDataEncryptionKey implements StoredDataEncryptionKey {

        /**
         * The encryption algorithm used for key encryption
         */
        private final String wrappingAlgorithm;

        /**
         * The identifier for the wrapping key that was used for key encryption
         */
        private final String wrappingKeyId;

        /**
         * The version of the wrapping key that was used for key encryption
         */
        private final String wrappingKeyVersion;

        /**
         * The encryption algorithm used for data encryption
         */
        private final String dataEncryptionAlgorithm;

        /**
         * The encrypted data encryption key
         */
        private final byte[] encryptedKeyData;

        /**
         * The IV for the encryption algorithm
         */
        private final byte[] initializationVector;
    }
}
