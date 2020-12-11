package org.springframework.content.commons.annotations;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.content.commons.store.ValueGenerator;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface GenericGenerator {

    /**
     * Generator strategy either a predefined strategy or a fully qualified class name implementing ContentIdGenerator interface.
     */
    Class<? extends ValueGenerator<? extends Object, ? extends Serializable>> strategy();

}
