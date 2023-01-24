package org.springframework.content.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RestResource {

    /**
     * Flag indicating whether this resource is exported or not.
     *
     * @return {@literal true} if the resource is to be exported, {@literal false} otherwise.
     */
    boolean exported() default true;

    /**
     * The set of paths that are governed by this export when exported=false
     *
     * @return  an array of paths
     * @deprecated
     */
    String[] paths() default {"*"};

    /**
     * The name of the path element to be exported
     *
     * @return  the path element
     */
    String path() default "";

    /**
     * The name of the linkrel element to be exported
     *
     * @return  the linkrel element
     */
    String linkRel() default "";
}
