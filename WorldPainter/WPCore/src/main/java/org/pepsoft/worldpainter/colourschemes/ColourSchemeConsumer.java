/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.colourschemes;

import org.pepsoft.worldpainter.ColourScheme;

/**
 * Some component which needs to be configured with a colour scheme.
 * 
 * @author Pepijn Schmitz
 */
public interface ColourSchemeConsumer {
    void setColourScheme(ColourScheme colourScheme);
}