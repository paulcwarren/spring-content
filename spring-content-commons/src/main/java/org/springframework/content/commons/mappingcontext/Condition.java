package org.springframework.content.commons.mappingcontext;

import org.springframework.core.convert.TypeDescriptor;

public interface Condition {
    public boolean matches(TypeDescriptor descriptor);
}
