package org.springframework.content.commons.property;

import lombok.Value;

@Value(staticConstructor="from")
public class PropertyPath {

    private String name;

}
