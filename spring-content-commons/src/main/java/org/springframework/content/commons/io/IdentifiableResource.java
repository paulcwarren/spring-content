package org.springframework.content.commons.io;

import java.io.Serializable;

import org.springframework.core.io.Resource;

public interface IdentifiableResource extends Resource {

    Serializable getId();

    void setId(Serializable id);
}
