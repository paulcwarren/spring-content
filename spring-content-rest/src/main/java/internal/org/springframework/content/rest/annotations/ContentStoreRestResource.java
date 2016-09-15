package internal.org.springframework.content.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ContentStoreRestResource {

	/**
	 * The path segment under which this resource is to be exported.
	 * 
	 * @return A valid path segment.
	 */
	String path() default "";

}
