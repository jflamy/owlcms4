package app.owlcms.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A display output page that an account token, or the display PIN in PIN mode, may open. The page's roles still decide.
 * <p>
 * Not inherited: each page opts in explicitly, so a new page is not exposed to tokens by accident.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TokenAccessible {
}
