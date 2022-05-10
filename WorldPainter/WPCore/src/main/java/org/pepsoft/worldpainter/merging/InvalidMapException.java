package org.pepsoft.worldpainter.merging;

import org.pepsoft.worldpainter.exception.WPRuntimeException;

public class InvalidMapException extends WPRuntimeException {
    public InvalidMapException(String message) {
        super(message);
    }
}