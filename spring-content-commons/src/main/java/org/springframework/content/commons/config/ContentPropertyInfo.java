package org.springframework.content.commons.config;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;

import java.io.Serializable;

@RequiredArgsConstructor(staticName="of")
@EqualsAndHashCode
public class ContentPropertyInfo<S, SID extends Serializable> {
    private final S entity;
    private final SID contentId;

    private final PropertyPath propertyPath;
    private final ContentProperty contentProperty;

    public S entity() {
        return entity;
    }

    public SID contentId() {
        return contentId;
    }

    public PropertyPath propertyPath() {
        return propertyPath;
    }

    public ContentProperty contentProperty() {
        return contentProperty;
    }
}
