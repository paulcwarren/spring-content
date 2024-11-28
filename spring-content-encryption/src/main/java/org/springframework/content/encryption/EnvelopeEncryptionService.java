package org.springframework.content.encryption;

import java.math.BigInteger;
import org.springframework.data.util.Pair;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class EnvelopeEncryptionService {

    private static KeyGenerator KEY_GENERATOR;

    private static String transformation = "AES/CTR/NoPadding";
    private static final String AES = "AES";

    static {
        // Create an encryption key.
        try {
            KEY_GENERATOR = KeyGenerator.getInstance(AES);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        KEY_GENERATOR.init(256, new SecureRandom());
    }

    //
    private final VaultOperations vaultOperations;

    private final SecureRandom secureRandom = new SecureRandom();

    public EnvelopeEncryptionService(VaultOperations vaultOperations) {
        this.vaultOperations = vaultOperations;
    }

    private CipherInputStream encryptMessage(InputStream is, final SecretKey dataKey, final byte[] nonce) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(transformation);
        SecretKeySpec key = new SecretKeySpec(dataKey.getEncoded(), AES);

        byte[] iv = new byte[128 / 8];
        System.arraycopy(nonce, 0, iv, 0, nonce.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        CipherInputStream cis = new CipherInputStream(is, cipher);
        return cis;
    }

    public Pair<CipherInputStream, byte[]> encrypt(InputStream is, String keyName) {
        try {
            SecretKey key = generateDataKey();

            byte[] nonce = new byte[96 / 8];
            secureRandom.nextBytes(nonce);

            // use vault to get ciphertext
            VaultTransitOperations transit = vaultOperations.opsForTransit();
            String base64Encoded = Base64.getEncoder().encodeToString(key.getEncoded());
            transit.createKey(keyName);
            String ciphertext = transit.encrypt(keyName, base64Encoded);

            byte[] encryptionContext = new byte[117];
            System.arraycopy(ciphertext.getBytes("UTF-8"), 0, encryptionContext, 0, 105);
            System.arraycopy(nonce, 0, encryptionContext, 105, 12);

            return Pair.of(encryptMessage(is, key, nonce), encryptionContext);
        } catch (Exception e) {
            throw new RuntimeException("unable to encrypt", e);
        }
    }

    private SecretKey generateDataKey() {
        return KEY_GENERATOR.generateKey();
    }

    private InputStream decryptInputStream(final SecretKeySpec secretKeySpec, byte[] nonce, int offset, InputStream is) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(transformation);

        byte[] iv = new byte[128 / 8];
        System.arraycopy(nonce, 0, iv, 0, nonce.length);

        int AES_BLOCK_SIZE = 16;
        long blockOffset = offset - (offset % AES_BLOCK_SIZE);
        final BigInteger ivBI = new BigInteger(1, iv);
        final BigInteger ivForOffsetBI = ivBI.add(BigInteger.valueOf(blockOffset / AES_BLOCK_SIZE));
        final byte[] ivForOffsetBA = ivForOffsetBI.toByteArray();
        final IvParameterSpec ivForOffset;
        if (ivForOffsetBA.length >= AES_BLOCK_SIZE) {
            ivForOffset = new IvParameterSpec(ivForOffsetBA, ivForOffsetBA.length - AES_BLOCK_SIZE, AES_BLOCK_SIZE);
        } else {
            final byte[] ivForOffsetBASized = new byte[AES_BLOCK_SIZE];
            System.arraycopy(ivForOffsetBA, 0, ivForOffsetBASized, AES_BLOCK_SIZE - ivForOffsetBA.length, ivForOffsetBA.length);
            ivForOffset = new IvParameterSpec(ivForOffsetBASized);
        }

        // Skip the blocks that we are not going to decrypt.
        // We advanced the IV manually to compensate for these skipped blocks,
        // and the stream will be zero-prefixed to compensate on the other side as well.
        // This saves encryption processing for all blocks that would be discarded anyways
        is.skipNBytes(blockOffset);

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivForOffset);

        return new OffsetInputStream(new SkippingInputStream(new CipherInputStream(is, cipher)), blockOffset);
    }

    private SecretKeySpec decryptKey(byte[] encryptedKey, String keyName) {
        VaultTransitOperations transit = vaultOperations.opsForTransit();
        String decryptedBase64Key = transit.decrypt(keyName, new String(encryptedKey));
        byte[] keyBytes = Base64.getDecoder().decode(decryptedBase64Key);

        SecretKeySpec key = new SecretKeySpec(keyBytes, AES);
        return key;
    }

    public InputStream decrypt(byte[] ecryptedContext, InputStream is, int offset, String keyName) {

        byte[] key = new byte[105];
        System.arraycopy(ecryptedContext, 0, key, 0, 105);
        byte[] nonce = new byte[12];
        System.arraycopy(ecryptedContext, 105, nonce, 0, 12);

        try {
            SecretKeySpec keySpec = decryptKey(key, keyName);
            return decryptInputStream(keySpec, nonce, offset, is);
        } catch (Exception e) {
            throw new RuntimeException("unable to decrypt", e);
        }
    }

    public void rotate(String keyName) {
        VaultTransitOperations transit = vaultOperations.opsForTransit();
        transit.rotate(keyName);
    }

    // CipherInputStream skip does not work.  This wraps a cipherinputstream purely to override the skip with a
    // working version.
    private static class SkippingInputStream extends FilterInputStream
    {
        private static final int MAX_SKIP_BUFFER_SIZE = 2048;

        protected SkippingInputStream(InputStream in)
        {
            super(in);
        }

        public long skip(long n)
                throws IOException
        {
            long remaining = n;
            int nr;

            if (n <= 0) {
                return 0;
            }

            int size = (int)Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
            byte[] skipBuffer = new byte[size];
            while (remaining > 0) {
                nr = in.read(skipBuffer, 0, (int)Math.min(size, remaining));
                if (nr < 0) {
                    break;
                }
                remaining -= nr;
            }

            return n - remaining;
        }
    }

    /**
     * Adds a fixed amount of 0-bytes in front of the delegate {@link InputStream}
     * <p>
     *
     * */
    private static class OffsetInputStream extends InputStream {
        private InputStream delegate;
        private long offsetBytes;

        public OffsetInputStream(InputStream delegate, long offsetBytes) {
            this.delegate = delegate;
            this.offsetBytes = offsetBytes;
        }

        @Override
        public long skip(long n) throws IOException {
            if(n <= 0) {
                return 0;
            }
            if(n <= offsetBytes) {
                offsetBytes -= n;
                return n;
            }
            if(offsetBytes > 0) {
                n = n - offsetBytes; // Still skipping so many bytes from the offset
                try {
                    return offsetBytes + delegate.skip(n);
                } finally {
                    offsetBytes = 0; // Now the whole offset is consumed; skip to the delegate
                }
            }

            return delegate.skip(n);
        }

        @Override
        public int read() throws IOException {
            if(offsetBytes > 0) {
                offsetBytes--;
                return 0;
            }
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if(offsetBytes > 0) {
                return super.read(b, off, len);
            }
            return delegate.read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            if(offsetBytes > 0) {
                return (int)Math.max(offsetBytes, Integer.MAX_VALUE);
            }
            return delegate.available();
        }
    }
}
