package org.springframework.content.encryption.keys;

import java.util.Collection;
import java.util.List;
import org.springframework.content.commons.mappingcontext.ContentProperty;

/**
 * Reads and writes data encryption keys from the entity with content
 */
public interface DataEncryptionKeyAccessor<S, T extends StoredDataEncryptionKey> {

    Collection<T> findKeys(S entity, ContentProperty contentProperty);

    default S clearKeys(S entity, ContentProperty contentProperty) {
        return setKeys(entity, contentProperty, List.of());
    }

    S setKeys(S entity, ContentProperty contentProperty, Collection<T> dataEncryptionKeys);
    S addKey(S entity, ContentProperty contentProperty, T dataEncryptionKey);
    S removeKey(S entity, ContentProperty contentProperty, T dataEncryptionKey);
}
