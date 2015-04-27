/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class SwampLandRenderer extends ColouredPatternRenderer {
    public SwampLandRenderer() {
        super(0x002000, new boolean[][] {
            {false, false, false, false, false, false, false, false},
            {false, false, false, true,  true,  false, false, false},
            {true,  true,  false, true,  true,  false, true,  true},
            {false, true,  true,  true,  true,  true,  true,  false},
            {false, false, true,  true,  true,  true,  false, false},
            {false, false, false, false, false, false, false, false},
            {true,  true,  true,  true,  true,  true,  true,  false},
            {true,  true,  true,  true,  true,  true,  true,  false},
        });
    }
}