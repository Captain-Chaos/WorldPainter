/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class River extends Layer {
    private River() {
        super(River.class.getName(), "River", "Generates a river complete with riverbed and sloping edges", DataSize.BIT, false, 25);
    }
    
    public static final River INSTANCE = new River();

    private static final long serialVersionUID = 1L;
}