package org.rutz;

/**
 * Exception thrown when an attribute-level transformation fails.
 */
public class AttributeLevelTransformationException extends Exception {
    public AttributeLevelTransformationException(String message) {
        super(message);
    }

    public AttributeLevelTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
