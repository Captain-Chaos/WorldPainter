/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import java.util.logging.LogManager;

/**
 *
 * @author pepijn
 */
public class WPLogManager extends LogManager {
    @Override
    public void reset() throws SecurityException {
        // Do nothing
    }
    
    public void realReset() {
        super.reset();
    }
}