package internal.org.springframework.content.fragments;

public interface EncryptingContentStoreConfiguration {
    EncryptingContentStoreConfiguration encryptionKeyContentProperty(String encryptionKeyContentProperty);
    EncryptingContentStoreConfiguration keyring(String keyring);
}
