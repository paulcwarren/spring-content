package org.springframework.content.encryption.engine;

import java.io.InputStream;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Encrypts and decrypts content streams
 */
public interface ContentEncryptionEngine {

    /**
     * Creates a new set of encryption parameters for encrypting
     * @return A new set of encryption parameters
     */
    EncryptionParameters createNewParameters();

    /**
     * Encrypt a content stream
     *
     * @param plainText The unencrypted content stream
     * @param encryptionParameters Parameters for the encryption algorithm
     * @return A stream of encrypted content
     */
    InputStream encrypt(InputStream plainText, EncryptionParameters encryptionParameters);

    /**
     * Decrypt an encrypted content stream
     * @param cipherText Function to obtain (part of) the encrypted content stream
     * @param encryptionParameters Parameters for the encryption algorithm
     * @param requestParameters Parameters for working on a part of the content stream
     * @return A stream of unencrypted content
     */
    InputStream decrypt(
            Function<InputStreamRequestParameters, InputStream> cipherText,
            EncryptionParameters encryptionParameters,
            InputStreamRequestParameters requestParameters
    );

    /**
     * Content-specific parameters for the encryption algorithm
     */
    @Getter
    @RequiredArgsConstructor
    class EncryptionParameters {
        private final SecretKey secretKey;
        private final byte[] initializationVector;
    }

    /**
     * Parameters to handle manipulate the input stream
     * <p>
     * Currently, a portion of the input stream can be requested by using byte offsets
     */
    @RequiredArgsConstructor
    @Getter
    class InputStreamRequestParameters {

        private final long startByteOffset;
        private final Long endByteOffset; // can be null when we want to read the stream until the end

        public static InputStreamRequestParameters full() {
            return startingFrom(0);
        }

        public static InputStreamRequestParameters startingFrom(long startByteOffset) {
            return new InputStreamRequestParameters(startByteOffset, null);
        }

    }


}
