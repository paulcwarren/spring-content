package internal.org.springframework.content.s3.config;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.s3.config.ContentPropertyInfo;

public class ContentPropertyInfoImpl<S> implements ContentPropertyInfo<S> {

    private S entity;
    private ContentProperty contentProperty;

    public ContentPropertyInfoImpl(S entity, ContentProperty contentProperty) {
        this.entity = entity;
        this.contentProperty = contentProperty;
    }

    @Override
    public S entity() {
        return entity;
    }

    @Override
    public ContentProperty contentProperty() {
        return contentProperty;
    }
}
