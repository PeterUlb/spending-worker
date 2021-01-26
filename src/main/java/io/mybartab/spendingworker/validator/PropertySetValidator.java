package io.mybartab.spendingworker.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertySetValidator implements ConstraintValidator<PropertySet, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        if (value instanceof CharSequence) {
            if (((CharSequence) value).isEmpty()) {
                return false;
            }

            Pattern pattern = Pattern.compile("\\$\\{.*}");
            Matcher matcher = pattern.matcher((CharSequence) value);
            try {
                return !matcher.find();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }
}
