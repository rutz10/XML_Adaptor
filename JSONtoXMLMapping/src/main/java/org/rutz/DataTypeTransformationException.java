package org.rutz;

/**
 * Exception thrown when data type conversion fails.
 */
public class DataTypeTransformationException extends Exception {
    public DataTypeTransformationException(String message) {
        super(message);
    }

    public DataTypeTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
