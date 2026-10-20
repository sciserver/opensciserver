package org.sciserver.springapp.sciquery.controller;

/**
 * Represents the result of a validation process.
 */
public class ValidationResult {
    private String message;
    private boolean isValid;

    public ValidationResult() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * Constructor for creating a ValidationResult object.
     *
     * @param message The validation message.
     * @param isValid The validity status.
     */
    public ValidationResult(String message, boolean isValid) {
        super();
        this.message = message;
        this.isValid = isValid;
    }

    /**
     * Constructor for creating a ValidationResult object with only a validity status.
     *
     * @param isValid The validity status.
     */
    public ValidationResult(boolean isValid) {
        super();
        this.isValid = isValid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }
}
