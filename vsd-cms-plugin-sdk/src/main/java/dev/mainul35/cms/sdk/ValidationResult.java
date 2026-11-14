package dev.mainul35.cms.sdk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a validation operation.
 * Contains validation status and any error messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Whether validation passed
     */
    private boolean valid;

    /**
     * List of validation error messages
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * List of validation warning messages
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    /**
     * Create a failed validation result with an error message
     */
    public static ValidationResult failure(String error) {
        return ValidationResult.builder()
                .valid(false)
                .errors(List.of(error))
                .build();
    }

    /**
     * Create a failed validation result with multiple error messages
     */
    public static ValidationResult failure(List<String> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .build();
    }

    /**
     * Add an error message to the result
     */
    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    /**
     * Add a warning message to the result
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Get combined error message
     */
    public String getErrorMessage() {
        return String.join("; ", errors);
    }

    /**
     * Get combined warning message
     */
    public String getWarningMessage() {
        return String.join("; ", warnings);
    }
}
