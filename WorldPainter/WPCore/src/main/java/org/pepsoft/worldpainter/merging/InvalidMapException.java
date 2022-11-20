package org.pepsoft.worldpainter.merging;

import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

public class InvalidMapException extends MDCCapturingRuntimeException {
    public InvalidMapException(String message) {
        super(message);
    }
}