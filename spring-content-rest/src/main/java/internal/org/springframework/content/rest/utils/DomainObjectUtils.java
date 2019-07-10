package internal.org.springframework.content.rest.utils;

import org.springframework.content.commons.utils.BeanUtils;

public final class DomainObjectUtils {

    private DomainObjectUtils() {}

    public static final Object getId(Object entity) {

        if (BeanUtils.hasFieldWithAnnotation(entity, javax.persistence.Id.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, javax.persistence.Id.class);
        } else if (BeanUtils.hasFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class)) {
            return BeanUtils.getFieldWithAnnotation(entity, org.springframework.data.annotation.Id.class);
        }

        return null;
    }
}
