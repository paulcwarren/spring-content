package org.springframework.content.encryption;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.util.Pair;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class EnvelopeEncryptionServiceCTR implements InitializingBean {

    private static KeyGenerator KEY_GENERATOR;

    private static String transformation = "AES/CTR/NoPadding";
    private static final String AES = "AES";

    private static final String KEYRING_NAME = "shared-key";

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

    public EnvelopeEncryptionServiceCTR(VaultOperations vaultOperations) {
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

//    public Pair<CipherInputStream, byte[]> encrypt(InputStream is) {
//        return this.encrypt(is, KEYRING_NAME);
//    }

    private SecretKey generateDataKey() {
        return KEY_GENERATOR.generateKey();
    }

    private InputStream decryptInputStream(final SecretKeySpec secretKeySpec, byte[] nonce, int offset, InputStream is) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(transformation);

        byte[] iv = new byte[128 / 8];
        System.arraycopy(nonce, 0, iv, 0, nonce.length);
//        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        int AES_BLOCK_SIZE = 16;
//        int offset = 48;
        int blockOffset = offset - (offset % AES_BLOCK_SIZE);
        final BigInteger ivBI = new BigInteger(1, iv);
        final BigInteger ivForOffsetBI = ivBI.add(BigInteger.valueOf(blockOffset / AES_BLOCK_SIZE));
        final byte[] ivForOffsetBA = ivForOffsetBI.toByteArray();
        final IvParameterSpec ivForOffset;
        if (ivForOffsetBA.length >= AES_BLOCK_SIZE) {
            ivForOffset = new IvParameterSpec(ivForOffsetBA, ivForOffsetBA.length - AES_BLOCK_SIZE,
                    AES_BLOCK_SIZE);
        } else {
            final byte[] ivForOffsetBASized = new byte[AES_BLOCK_SIZE];
            System.arraycopy(ivForOffsetBA, 0, ivForOffsetBASized, AES_BLOCK_SIZE
                    - ivForOffsetBA.length, ivForOffsetBA.length);
            ivForOffset = new IvParameterSpec(ivForOffsetBASized);
        }                // increment counter

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivForOffset);

//        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        CipherInputStream cis = new CipherInputStream(is, cipher);

        InputStream inputStreamToReturn = cis;
        if (offset == 0) {
            inputStreamToReturn = new ZeroOffsetSkipInputStream(cis);
        } else if (offset > 0) {
            inputStreamToReturn = new OffsetSkipInputStream(cis, offset % AES_BLOCK_SIZE);
        }

        return inputStreamToReturn;
    }
    private SecretKeySpec decryptKey(byte[] encryptedKey, String keyName) {
        VaultTransitOperations transit = vaultOperations.opsForTransit();
        String decryptedBase64Key = transit.decrypt(keyName, new String(encryptedKey));
        byte[] keyBytes = Base64.getDecoder().decode(decryptedBase64Key);

        SecretKeySpec key = new SecretKeySpec(keyBytes, AES);
        return key;
    }

//    private SecretKeySpec decryptKey(byte[] encryptedKey) {
//        return this.decryptKey(encryptedKey, KEYRING_NAME);
//    }

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

//    public CipherInputStream decrypt(byte[] ecryptedKey, InputStream is) {
//        try {
//            SecretKeySpec key = decryptKey(ecryptedKey, KEYRING_NAME);
//            return decryptInputStream(key, is);
//        } catch (Exception e) {
//            throw new RuntimeException("unable to decrypt", e);
//        }
//    }

    public void rotate(String keyName) {
        VaultTransitOperations transit = vaultOperations.opsForTransit();
        transit.rotate(keyName);
    }

    public void rotate() {
        this.rotate(KEYRING_NAME);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        vaultOperations.opsForTransit().createKey(KEYRING_NAME);
    }

    // CipherInputStream skip does not work.  This wraps a cipherinputstream purely to override the skip with a
    // working version
    public class ZeroOffsetSkipInputStream extends FilterInputStream
    {
        private static final int MAX_SKIP_BUFFER_SIZE = 2048;

        protected ZeroOffsetSkipInputStream(InputStream in)
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

    // CipherInputStream skip does not work.  This wraps a cipherinputstream purely to override the skip with a
    // working version
    public class OffsetSkipInputStream extends FilterInputStream
    {
        private static final int MAX_SKIP_BUFFER_SIZE = 2048;
        private final int offset;

        protected OffsetSkipInputStream(InputStream in, int offset)
        {
            super(in);
            this.offset = offset;
        }

        public long skip(long n)
                throws IOException
        {
            long remaining = offset;
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
}
