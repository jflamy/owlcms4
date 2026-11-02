package app.owlcms.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The principal needs any of the listed permissions. {@code platformBound} means the page operates on one platform. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresPermission {
	Permission[] value();

	boolean platformBound() default false;
}
