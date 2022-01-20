package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.mappingcontext.ContentProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EntityResolution {
    private Object entity;
    private ContentProperty contentProperty;
}
