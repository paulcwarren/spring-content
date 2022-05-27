package org.springframework.content.s3.config;

import org.springframework.content.commons.mappingcontext.ContentProperty;

public interface ContentPropertyInfo<S> {

    S entity();
    ContentProperty contentProperty();
}
