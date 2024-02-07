package org.springframework.data.rest.extensions.entitycontent;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class SpringDataRestPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            context.getClassLoader().loadClass("org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer");
            return true; // Spring Data REST is present
        } catch (ClassNotFoundException e) {
            return false; // Spring Data REST is not present
        }
    }
}
