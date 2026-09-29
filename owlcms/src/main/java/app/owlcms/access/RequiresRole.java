package app.owlcms.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The principal needs any of the listed base roles. {@code platformBound} means the page operates on one platform, so
 * the platform scope and lock of the grant are checked.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresRole {
	Role[] value();

	boolean platformBound() default false;
}
