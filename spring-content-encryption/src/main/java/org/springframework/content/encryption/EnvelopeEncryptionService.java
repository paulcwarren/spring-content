package org.springframework.content.encryption;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.util.Pair;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;

public class EnvelopeEncryptionService implements InitializingBean {

    private static KeyGenerator KEY_GENERATOR;

    private static final String KEYRING_NAME = "shared-key";

    static {
        // Create an encryption key.
        try {
            KEY_GENERATOR = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        KEY_GENERATOR.init(256, new SecureRandom());
    }

    //
    private static final String AES = "AES";
    private final VaultOperations vaultOperations;

    public EnvelopeEncryptionService(VaultOperations vaultOperations) {
        this.vaultOperations = vaultOperations;
    }

    private CipherInputStream encryptMessage(InputStream is, final SecretKey dataKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(dataKey.getEncoded(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        CipherInputStream cis = new CipherInputStream(is, cipher);
        return cis;
    }

    public Pair<CipherInputStream, byte[]> encrypt(InputStream is, String keyName) {
        try {
            SecretKey key = generateDataKey();

            // use vault to get ciphertext
            VaultTransitOperations transit = vaultOperations.opsForTransit();
            String base64Encoded = Base64.getEncoder().encodeToString(key.getEncoded());
            transit.createKey(keyName);
            String ciphertext = transit.encrypt(keyName, base64Encoded);

            return Pair.of(encryptMessage(is, key), ciphertext.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("unable to encrypt", e);
        }
    }

    public Pair<CipherInputStream, byte[]> encrypt(InputStream is) {
        return this.encrypt(is, KEYRING_NAME);
    }

    private SecretKey generateDataKey() {
        return KEY_GENERATOR.generateKey();
    }

    private CipherInputStream decryptInputStream(final SecretKeySpec secretKeySpec, InputStream is) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        CipherInputStream cis = new CipherInputStream(is, cipher);
        return cis;
    }
    private SecretKeySpec decryptKey(byte[] encryptedKey, String keyName) {
        VaultTransitOperations transit = vaultOperations.opsForTransit();
        String decryptedBase64Key = transit.decrypt(keyName, new String(encryptedKey));
        byte[] keyBytes = Base64.getDecoder().decode(decryptedBase64Key);

        SecretKeySpec key = new SecretKeySpec(keyBytes, AES);
        return key;
    }

    private SecretKeySpec decryptKey(byte[] encryptedKey) {
        return this.decryptKey(encryptedKey, KEYRING_NAME);
    }

    public CipherInputStream decrypt(byte[] ecryptedKey, InputStream is, String keyName) {
        try {
            SecretKeySpec key = decryptKey(ecryptedKey, keyName);
            return decryptInputStream(key, is);
        } catch (Exception e) {
            throw new RuntimeException("unable to decrypt", e);
        }
    }

    public CipherInputStream decrypt(byte[] ecryptedKey, InputStream is) {
        try {
            SecretKeySpec key = decryptKey(ecryptedKey, KEYRING_NAME);
            return decryptInputStream(key, is);
        } catch (Exception e) {
            throw new RuntimeException("unable to decrypt", e);
        }
    }

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
}
