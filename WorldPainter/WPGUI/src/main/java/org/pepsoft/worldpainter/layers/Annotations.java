/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

/**
 *
 * @author SchmitzP
 */
public class Annotations extends Layer {
    private Annotations() {
        super("org.pepsoft.Annotations", "Annotations", "Coloured annotations on the world, which can optionally be exported with it", DataSize.NIBBLE, 65);
    }
    
    public static final Annotations INSTANCE = new Annotations();
    
    private static final long serialVersionUID = 1L;
}