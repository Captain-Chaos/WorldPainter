/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.io.IOException;

/**
 *
 * @author SchmitzP
 */
public class AccessDeniedException extends IOException {
    public AccessDeniedException() {
        // Do nothing
    }

    public AccessDeniedException(String string) {
        super(string);
    }

    public AccessDeniedException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public AccessDeniedException(Throwable thrwbl) {
        super(thrwbl);
    }
    
    private static final long serialVersionUID = 1L;
}