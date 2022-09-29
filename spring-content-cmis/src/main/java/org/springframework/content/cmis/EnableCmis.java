package org.springframework.content.cmis;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.Date;

import internal.org.springframework.content.cmis.CmisRegistrar;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({ CmisConfiguration.class, CmisRegistrar.class })
public @interface EnableCmis {

	String[] basePackages();

	String id() default "";

	String name() default "";

	String description() default "";

	String vendorName() default "";

	String productName() default "";

	String productVersion() default "";

}
