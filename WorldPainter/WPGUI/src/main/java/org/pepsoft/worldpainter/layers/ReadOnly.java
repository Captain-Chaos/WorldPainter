/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class ReadOnly extends Layer {
    public ReadOnly() {
        super("Read Only", "Marks chunks that will not be changed when merging", Layer.DataSize.BIT_PER_CHUNK, 90);
    }
    
    public static final ReadOnly INSTANCE = new ReadOnly();

    private static final long serialVersionUID = 2011042801L;
}