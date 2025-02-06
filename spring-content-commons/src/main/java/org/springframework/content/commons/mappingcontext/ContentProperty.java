package org.springframework.content.commons.mappingcontext;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.core.convert.TypeDescriptor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

@Getter
@Setter
@EqualsAndHashCode
public class ContentProperty {

    private String contentPropertyPath;
    private String contentIdPropertyPath;
    private TypeDescriptor contentIdType;
    private String contentLengthPropertyPath;
    private TypeDescriptor contentLengthType;
    private String mimeTypePropertyPath;
    private String originalFileNamePropertyPath;

    public Object getCustomProperty(Object entity, String propertyName) {
        String customContentPropertyPath = getCustomPropertyPropertyPath(propertyName);

        BeanWrapper wrapper = getBeanWrapperForRead(entity);
        try {
            return wrapper.getPropertyValue(customContentPropertyPath);
        } catch (NullValueInNestedPathException nvinpe) {
            return null;
        }
    }

    public void setCustomProperty(Object entity, String propertyName, Object value) {
        String customContentPropertyPath = getCustomPropertyPropertyPath(propertyName);

        BeanWrapper wrapper = getBeanWrapperForWrite(entity);
        wrapper.setPropertyValue(customContentPropertyPath, value);
    }

    public String getCustomPropertyPropertyPath(String propertyName) {
        return contentPropertyPath + StringUtils.capitalize(propertyName);
    }

    public Object getContentId(Object entity) {
        if (contentIdPropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = getBeanWrapperForRead(entity);
        try {
            return wrapper.getPropertyValue(contentIdPropertyPath);
        } catch (NullValueInNestedPathException nvinpe) {
            return null;
        }
    }

    public void setContentId(Object entity, Object value, Condition condition) {
        if (contentIdPropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = getBeanWrapperForWrite(entity);

        if (condition != null) {
            TypeDescriptor t = wrapper.getPropertyTypeDescriptor(contentIdPropertyPath);
            if (!condition.matches(t)) {
                return;
            }
        }

        wrapper.setPropertyValue(contentIdPropertyPath, value);
    }

    public TypeDescriptor getContentIdType(Object entity) {
        Assert.notNull(this.contentIdType, "content id property type must be set");
        return this.contentIdType;
    }

    public TypeDescriptor getContentIdType() {
        Assert.notNull(this.contentIdType, "content id property type must be set");
        return this.contentIdType;
    }

    public void setContentIdType(TypeDescriptor descriptor) {
        this.contentIdType = descriptor;
    }

    public Object getContentLength(Object entity) {
        if (contentLengthPropertyPath == null) {
            return 0L;
        }

        BeanWrapper wrapper = getBeanWrapperForRead(entity);
        try {
            return wrapper.getPropertyValue(contentLengthPropertyPath);
        } catch (NullValueInNestedPathException nvinpe) {
            return null;
        }
    }

    public void setContentLength(Object entity, Object value) {
        if (contentLengthPropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = getBeanWrapperForWrite(entity);
        wrapper.setPropertyValue(contentLengthPropertyPath, value);
    }

    public TypeDescriptor getContentLengthType() {
        Assert.notNull(this.contentLengthType, "content length property type must be set");
        return this.contentLengthType;
    }

    public void setContentLengthType(TypeDescriptor descriptor) {
        this.contentLengthType = descriptor;
    }

    public Object getMimeType(Object entity) {
        if (mimeTypePropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = getBeanWrapperForRead(entity);
        try {
            return wrapper.getPropertyValue(mimeTypePropertyPath);
        } catch (NullValueInNestedPathException nvinpe) {
            return null;
        }
    }

    public void setMimeType(Object entity, Object value) {
        if (mimeTypePropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = getBeanWrapperForWrite(entity);
        wrapper.setPropertyValue(mimeTypePropertyPath, value);
    }

    public void setOriginalFileName(Object entity, Object value) {
        if (originalFileNamePropertyPath == null) {
            return;
        }

        BeanWrapper wrapper = getBeanWrapperForWrite(entity);
        wrapper.setPropertyValue(originalFileNamePropertyPath, value);
    }

    public Object getOriginalFileName(Object entity) {
        if (originalFileNamePropertyPath == null) {
            return null;
        }

        BeanWrapper wrapper = getBeanWrapperForRead(entity);
        try {
            return wrapper.getPropertyValue(originalFileNamePropertyPath);
        } catch (NullValueInNestedPathException nvinpe) {
            return null;
        }
    }

    private BeanWrapper getBeanWrapperForRead(Object entity) {
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        return wrapper;
    }

    private BeanWrapper getBeanWrapperForWrite(Object entity) {
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        wrapper.setAutoGrowNestedPaths(true);
        return wrapper;
    }
}
