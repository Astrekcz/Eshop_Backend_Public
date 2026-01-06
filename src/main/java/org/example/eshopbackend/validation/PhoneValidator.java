package org.example.eshopbackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.eshopbackend.util.PhoneUtil;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    private String region;

    @Override
    public void initialize(ValidPhone ann) {
        this.region = ann.defaultRegion();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // null/blank řeš @NotBlank/@NotNull zvlášť; tady jen kontrola formátu
        if (value == null || value.isBlank()) return true;
        return PhoneUtil.toE164OrNull(value, region) != null;
    }
}
