package dev.mainul35.cms.sdk.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of component property validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Whether the validation passed
     */
    private boolean isValid;

    /**
     * List of validation errors (empty if valid)
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * List of validation warnings (non-blocking)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .isValid(true)
                .build();
    }

    /**
     * Create a failed validation result with errors
     */
    public static ValidationResult failure(List<String> errors) {
        return ValidationResult.builder()
                .isValid(false)
                .errors(errors)
                .build();
    }

    /**
     * Create a failed validation result with a single error
     */
    public static ValidationResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return failure(errors);
    }
}
