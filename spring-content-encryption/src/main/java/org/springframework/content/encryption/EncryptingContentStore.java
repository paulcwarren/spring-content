package org.springframework.content.encryption;

import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.repository.SetContentParams;
import org.springframework.content.commons.repository.UnsetContentParams;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;

public interface EncryptingContentStore<S, SID extends Serializable> extends ContentStoreAware {
    void associate(S o, PropertyPath propertyPath, SID serializable);
    void unassociate(S o, PropertyPath propertyPath);
    InputStream getContent(S o, PropertyPath propertyPath);
    S setContent(S o, PropertyPath propertyPath, InputStream inputStream);
    S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen);
    S setContent(S entity, PropertyPath propertyPath, InputStream content, SetContentParams params);
    S setContent(S entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.store.SetContentParams params);
    S setContent(S o, PropertyPath propertyPath, Resource resource);
    Resource getResource(S entity, PropertyPath propertyPath);
    Resource getResource(S entity, PropertyPath propertyPath, GetResourceParams params);
    Resource getResource(S entity, PropertyPath propertyPath, org.springframework.content.commons.store.GetResourceParams params);
    S unsetContent(S entity, PropertyPath propertyPath);
    S unsetContent(S entity, PropertyPath propertyPath, UnsetContentParams params);
    S unsetContent(S entity, PropertyPath propertyPath, org.springframework.content.commons.store.UnsetContentParams params);
}
