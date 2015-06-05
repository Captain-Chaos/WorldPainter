/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import java.io.IOException;

/**
 * This exception may be thrown instead of an {@link IOException} in circumstances where the likely cause is that the
 * file, or a file in the directory, is opened by another process.
 *
 * @author pepijn
 */
public class FileInUseException extends IOException {
    public FileInUseException() {
        // Do nothing
    }

    public FileInUseException(String message) {
        super(message);
    }

    public FileInUseException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileInUseException(Throwable cause) {
        super(cause);
    }
    
    private static final long serialVersionUID = 1L;
}