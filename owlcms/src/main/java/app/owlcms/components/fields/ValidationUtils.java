/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.SerializablePredicate;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class ValidationUtils {
	
	private final static Logger logger = (Logger) LoggerFactory.getLogger(ValidationUtils.class);

	/**
	 * Builds a validator out of a conditional function and an error message If the
	 * function returns true, the validator returns {@code Result.ok()}; if it
	 * returns false or throws an exception, {@code Result.error()} is returned with
	 * the message from the exception using getLocalizedMessage().
	 *
	 * @param <T>          the value type
	 * @param guard        the function used to validate, not null
	 * @param errorMessage an optional error message
	 * @return the new validator using the function
	 *
	 */
	public static <T> Validator<T> checkUsingException(SerializablePredicate<T> guard, String... errorMessage) {
		Objects.requireNonNull(guard, "guard cannot be null");
		return (value, context) -> {
			try {
				if (guard.test(value)) {
					return ValidationResult.ok();
				} else {
					logger.error("missing message {}", LoggerUtils.stackTrace());
					return ValidationResult.error(errorMessage.length > 0 ? errorMessage[0] : "Validation Error");
				}
			} catch (Exception e) {
				logger.error("check Using Exception {}",e.getLocalizedMessage());
				return ValidationResult.error(e.getLocalizedMessage());
			}
		};
	}
	
	public static <T> Validator<T> checkUsingException(SerializablePredicate<T> guard, Consumer<String> messageSetter, String... errorMessage) {
		Objects.requireNonNull(guard, "guard cannot be null");
		return (value, context) -> {
			try {
				if (guard.test(value)) {
					return ValidationResult.ok();
				} else {
					logger.error("missing message {}", LoggerUtils.stackTrace());
					String msg = errorMessage.length > 0 ? errorMessage[0] : "Validation Error";
					messageSetter.accept(msg);
					return ValidationResult.error(msg);
				}
			} catch (Exception e) {
				logger.error("check Using Exception {}",e.getLocalizedMessage());
				messageSetter.accept(e.getLocalizedMessage());
				return ValidationResult.error(e.getLocalizedMessage());
			}
		};
	}

}
