package org.springframework.content.encryption.store;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;

public interface EncryptingContentStore<S, SID extends Serializable> {
    void associate(S o, PropertyPath propertyPath, SID serializable);
    void unassociate(S o, PropertyPath propertyPath);
    InputStream getContent(S o, PropertyPath propertyPath);
    S setContent(S o, PropertyPath propertyPath, InputStream inputStream);
    S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen);
    S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params);
    S setContent(S o, PropertyPath propertyPath, Resource resource);
    Resource getResource(S entity, PropertyPath propertyPath);
    Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params);
    S unsetContent(S entity, PropertyPath propertyPath);
    S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params);
}
