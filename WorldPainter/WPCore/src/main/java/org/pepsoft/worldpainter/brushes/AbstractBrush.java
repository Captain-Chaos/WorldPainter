/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

import java.awt.*;

/**
 *
 * @author pepijn
 */
public abstract class AbstractBrush implements Brush, Cloneable {
    public AbstractBrush(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public int getEffectiveRadius() {
        return getRadius();
    }

    @Override
    public Rectangle getBoundingBox() {
        final int effectiveRadius = getEffectiveRadius();
        if (effectiveRadius != -boundingBox.getX()) {
            boundingBox.setBounds(-effectiveRadius, -effectiveRadius, (2 * effectiveRadius) + 1, (2 * effectiveRadius) + 1);
        }
        return boundingBox;
    }

    @Override
    public AbstractBrush clone() {
        try {
            final AbstractBrush clone = (AbstractBrush) super.clone();
            clone.boundingBox = (Rectangle) boundingBox.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private final String name;
    private Rectangle boundingBox = new Rectangle();
}