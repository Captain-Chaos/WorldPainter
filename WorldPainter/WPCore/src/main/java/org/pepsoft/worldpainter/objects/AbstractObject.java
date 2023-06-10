/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import org.pepsoft.worldpainter.Dimension;

import javax.vecmath.Point3i;

/**
 * Abstract base class for {@link WPObject} implementations.
 *
 * @author pepijn
 */
public abstract class AbstractObject implements WPObject {
    @Override
    public void prepareForExport(Dimension dimension) {
        // Do nothing
    }

    @Override
    public AbstractObject clone() {
        try {
            return (AbstractObject) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final Point3i getOffset() {
        return getAttribute(ATTRIBUTE_OFFSET);
    }

    private static final long serialVersionUID = -5872104411389620683L;
}