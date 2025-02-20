package it.jacopocarlini.fffp.exceptions;

public class FeatureFlagEvaluationException extends RuntimeException {
  public FeatureFlagEvaluationException(String message, Throwable cause) {
    super(message, cause);
  }
}
