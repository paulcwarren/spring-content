package org.springframework.content.commons.mappingcontext;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.TypeDescriptor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ContentProperty {

    private String contentPropertyPath;
    private String contentIdPropertyPath;
    private String contentLengthPropertyPath;
    private String mimeTypePropertyPath;
    private String originalFileNamePropertyPath;

    public Object getCustomProperty(Object entity, String propertyName) {
        String customContentPropertyPath = contentPropertyPath + StringUtils.capitalize(propertyName);

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper.getPropertyValue(customContentPropertyPath);
    }

    public void setCustomProperty(Object entity, String propertyName, Object value) {
        String customContentPropertyPath = contentPropertyPath + StringUtils.capitalize(propertyName);

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        wrapper.setPropertyValue(customContentPropertyPath, value);
    }

    public Object getContentId(Object entity) {
        if (contentLengthPropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper.getPropertyValue(contentIdPropertyPath);
    }

    public void setContentId(Object entity, Object value, Condition condition) {
        if (contentIdPropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);

        if (condition != null) {
            TypeDescriptor t = wrapper.getPropertyTypeDescriptor(contentIdPropertyPath);
            if (!condition.matches(t)) {
                return;
            }
        }

        wrapper.setPropertyValue(contentIdPropertyPath, value);
    }

    public TypeDescriptor getContentIdType(Object entity) {
        if (contentIdPropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper.getPropertyTypeDescriptor(contentIdPropertyPath);
    }

    public Object getContentLength(Object entity) {
        if (contentLengthPropertyPath == null) {
            return 0L;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper.getPropertyValue(contentLengthPropertyPath);
    }

    public void setContentLength(Object entity, Object value) {
        if (contentLengthPropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        wrapper.setPropertyValue(contentLengthPropertyPath, value);
    }

    public Object getMimeType(Object entity) {
        if (mimeTypePropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper.getPropertyValue(mimeTypePropertyPath);
    }

    public void setMimeType(Object entity, Object value) {
        if (mimeTypePropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        wrapper.setPropertyValue(mimeTypePropertyPath, value);
    }

    public void setOriginalFileName(Object entity, Object value) {
        if (originalFileNamePropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        wrapper.setPropertyValue(originalFileNamePropertyPath, value);
    }

    public Object getOriginalFileName(Object entity) {
        if (originalFileNamePropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper.getPropertyValue(originalFileNamePropertyPath);
    }
}
