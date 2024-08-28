package com.ericsson.streamAdapter.output;

/**
 * This class is an exception class for Output Thread.
 *
 * @author eeimho
 */

public class OutputThreadException extends Exception{
    private static final long serialVersionUID = -8761115996938064966L;

    public OutputThreadException(String message) {
        super(message);
    }

    public OutputThreadException(String message, Exception e) {
        super(message, e);
    }
}
