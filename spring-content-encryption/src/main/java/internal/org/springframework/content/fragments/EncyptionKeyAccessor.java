package internal.org.springframework.content.fragments;

public interface EncyptionKeyAccessor<S> {
    void setEncryptionKey(S entity, byte[] encryptionKey);
    void getEncryptionKey(S entity);
}
