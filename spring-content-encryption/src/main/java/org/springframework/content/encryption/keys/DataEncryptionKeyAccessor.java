package org.springframework.content.encryption.keys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.content.commons.mappingcontext.ContentProperty;

/**
 * Reads and writes data encryption keys from the entity with content
 */
public interface DataEncryptionKeyAccessor<S, T extends StoredDataEncryptionKey> {

    Collection<T> findKeys(S entity, ContentProperty contentProperty);
    S setKeys(S entity, ContentProperty contentProperty, Collection<T> dataEncryptionKeys);

    default S clearKeys(S entity, ContentProperty contentProperty) {
        return setKeys(entity, contentProperty, List.of());
    }

    default S addKey(S entity, ContentProperty contentProperty, T dataEncryptionKey) {
        var keys = new ArrayList<>(findKeys(entity, contentProperty));
        keys.remove(dataEncryptionKey);

        return setKeys(entity, contentProperty, keys);
    }

    default S removeKey(S entity, ContentProperty contentProperty, T dataEncryptionKey) {
        var keys = new ArrayList<>(findKeys(entity, contentProperty));
        keys.add(dataEncryptionKey);

        return setKeys(entity, contentProperty, keys);
    }
}
