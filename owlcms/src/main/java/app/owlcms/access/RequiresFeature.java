package app.owlcms.access;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import app.owlcms.data.config.FeatureSwitch;

/** The page is not found unless the feature switch is on. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresFeature {
	FeatureSwitch value();
}
