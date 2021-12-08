/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import java.io.Serializable;

/**
 * Implementations must provide a functional {@link #clone()} method, as well as {@code equals()} and {@code hashCode()}
 * methods that provide business equality.
 *
 * @author Pepijn
 */
public abstract class ExportSettings implements Serializable, Cloneable {
    /**
     * Must return a deep copy of the export settings.
     * 
     * @return A deep copy of the export settings.
     */
    @Override
    public ExportSettings clone() {
        try {
            return (ExportSettings) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static final long serialVersionUID = 1L;
}