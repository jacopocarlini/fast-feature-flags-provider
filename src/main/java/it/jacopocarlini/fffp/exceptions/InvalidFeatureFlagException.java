package it.jacopocarlini.fffp.exceptions;

public class InvalidFeatureFlagException extends RuntimeException {
    public InvalidFeatureFlagException(String message) {
        super(message);
    }
}
