package org.pepsoft.worldpainter;

import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

/**
 * Created by pepijn on 27-9-15.
 */
public class MissingCustomTerrainException extends MDCCapturingRuntimeException {
    public MissingCustomTerrainException(String message, int index) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    private final int index;

    private static final long serialVersionUID = 1L;
}