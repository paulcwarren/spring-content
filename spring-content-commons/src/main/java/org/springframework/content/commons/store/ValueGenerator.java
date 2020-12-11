package org.springframework.content.commons.store;

import java.io.Serializable;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.utils.BeanUtils;

public interface ValueGenerator<S, SID> {

    default boolean regenerate(S entity) {
        Serializable id = (Serializable) BeanUtils.getFieldWithAnnotation(entity, ContentId.class);
        if (id == null) {
            return true;
        }
        return false;
    }

    SID generate(S entity);
}
