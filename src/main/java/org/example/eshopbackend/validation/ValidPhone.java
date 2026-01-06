package org.example.eshopbackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface ValidPhone {
    String message() default "Neplatné telefonní číslo";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    /** Defaultní region pro parsování (např. "CZ"). */
    String defaultRegion() default "CZ";
}
