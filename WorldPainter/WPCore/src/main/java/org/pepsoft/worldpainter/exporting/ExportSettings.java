/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import java.io.Serializable;

/**
 * Implementations must be {@link Serializable}, unmodifiable and provide functional {@code equals()} and
 * {@code hashCode()} methods that provide business equality.
 *
 * @author Pepijn
 */
public abstract class ExportSettings implements Serializable {
    private static final long serialVersionUID = 1L;
}