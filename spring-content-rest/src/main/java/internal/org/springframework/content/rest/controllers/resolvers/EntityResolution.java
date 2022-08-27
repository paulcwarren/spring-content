package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.property.PropertyPath;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EntityResolution {
    private Object entity;
    private PropertyPath property;
}
