package io.mybartab.spendingworker.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This is needed until https://github.com/spring-projects/spring-boot/issues/18816 is solved
 * The validation is not capable to detect the attempt to store strings like ${MY_ENV} into non-string variables,
 * this will result in a "Failed to bind properties" exception by Spring
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PropertySetValidator.class)
@Documented
public @interface PropertySet {
    String message() default "Property is not set";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
