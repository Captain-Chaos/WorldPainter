/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.WPContext;

/**
 *
 * @author pepijn
 */
public interface ContextProvider extends Plugin {
    WPContext getWPContextInstance();
}
