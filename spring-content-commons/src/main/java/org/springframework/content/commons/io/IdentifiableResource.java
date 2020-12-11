package org.springframework.content.commons.io;

import java.io.Serializable;

public interface IdentifiableResource {

    Serializable getId();

    void setId(Serializable id);
}
