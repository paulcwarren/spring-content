package internal.org.springframework.content.encryption.engine;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.Function;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import lombok.SneakyThrows;
import org.springframework.content.encryption.engine.ContentEncryptionEngine;

/**
 * Symmetric data encryption engine using AES-CTR encryption mode
 */
public class AesCtrEncryptionEngine implements ContentEncryptionEngine {
    private final KeyGenerator keyGenerator;
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final int AES_BLOCK_SIZE_BYTES = 16; // AES has a 128-bit block size
    private static final int IV_SIZE_BYTES = AES_BLOCK_SIZE_BYTES; // IV is the same size as a block

    @SneakyThrows({NoSuchAlgorithmException.class})
    public AesCtrEncryptionEngine(int keySizeBits) {
        keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySizeBits, secureRandom);
    }

    @Override
    public EncryptionParameters createNewParameters() {
        var secretKey = keyGenerator.generateKey();
        byte[] iv = new byte[IV_SIZE_BYTES];
        secureRandom.nextBytes(iv);
        return new EncryptionParameters(
                secretKey,
                iv
        );
    }

    @SneakyThrows
    private Cipher initializeCipher(EncryptionParameters parameters, boolean forEncryption) {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(
                forEncryption?Cipher.ENCRYPT_MODE:Cipher.DECRYPT_MODE,
                parameters.getSecretKey(),
                new IvParameterSpec(parameters.getInitializationVector())
        );

        return cipher;
    }

    @Override
    public InputStream encrypt(InputStream plainText, EncryptionParameters encryptionParameters) {
        return new CipherInputStream(plainText, initializeCipher(encryptionParameters, true));
    }

    @Override
    public InputStream decrypt(
            Function<InputStreamRequestParameters, InputStream> cipherTextStreamRequest,
            EncryptionParameters encryptionParameters,
            InputStreamRequestParameters requestParameters
    ) {
        var blockStartOffset = calculateBlockOffset(requestParameters.getStartByteOffset());

        var adjustedIv = adjustIvForOffset(encryptionParameters.getInitializationVector(), blockStartOffset);

        var adjustedParameters = new EncryptionParameters(
                encryptionParameters.getSecretKey(),
                adjustedIv
        );

        var byteStartOffset = blockStartOffset * AES_BLOCK_SIZE_BYTES;

        var cipherTextStream = cipherTextStreamRequest.apply(requestParameters);

        var cipher = initializeCipher(adjustedParameters, false);

        return new ZeroPrefixedInputStream(
                new EnsureSingleSkipInputStream(
                        new CipherInputStream(
                                new SkippingInputStream(
                                        cipherTextStream,
                                        byteStartOffset
                                ),
                                cipher
                        )
                ),
                byteStartOffset
        );
    }

    private static long calculateBlockOffset(long offsetBytes) {
        return (offsetBytes - (offsetBytes % AES_BLOCK_SIZE_BYTES)) / AES_BLOCK_SIZE_BYTES;
    }

    private byte[] adjustIvForOffset(byte[] iv, long offsetBlocks) {
        // AES-CTR works by having a separate IV for every block.
        // This block IV is built from the initial IV and the block counter.
        var initialIv = new BigInteger(1, iv);

        // Because we're using BigInteger for math here,
        // the resulting byte array may be longer (when overflowing the IV size)
        // or shorter (when our IV starts with a bunch of 0)
        // It needs to be the proper length, and aligned properly
        byte[] bigintBytes = initialIv.add(BigInteger.valueOf(offsetBlocks))
                .toByteArray();

        if(bigintBytes.length == AES_BLOCK_SIZE_BYTES) {
            return bigintBytes;
        } else if(bigintBytes.length > AES_BLOCK_SIZE_BYTES) {
            // Byte array is longer, we need to cut a part of the front
            return Arrays.copyOfRange(bigintBytes, bigintBytes.length-IV_SIZE_BYTES, bigintBytes.length);
        } else {
            // Byte array is sorter, we need to pad the front with 0 bytes
            // Note that a bytes array is initialized to be all-zero by default
            byte[] ivBytes = new byte[IV_SIZE_BYTES];
            System.arraycopy(bigintBytes, 0, ivBytes, IV_SIZE_BYTES-bigintBytes.length, bigintBytes.length);
            return ivBytes;
        }
    }

}
