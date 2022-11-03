package org.springframework.content.encryption;

import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;

public interface EncryptingContentStore<S, SID extends Serializable> extends ContentStoreAware {
    void associate(S o, PropertyPath propertyPath, SID serializable);
    void unassociate(S o, PropertyPath propertyPath);
    InputStream getContent(S o, PropertyPath propertyPath);
    S setContent(S o, PropertyPath propertyPath, InputStream inputStream);
    S setContent(S entity, PropertyPath propertyPath, InputStream content, long contentLen);
    S setContent(S o, PropertyPath propertyPath, Resource resource);
    Resource getResource(S entity, PropertyPath propertyPath);
    S unsetContent(S entity, PropertyPath propertyPath);
}
