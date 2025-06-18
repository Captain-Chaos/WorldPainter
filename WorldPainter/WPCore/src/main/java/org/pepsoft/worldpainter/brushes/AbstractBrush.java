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

    /**
     * Get the bounding box of the brush, relative to its center. This may be larger than its radius, for example for a
     * rotated square brush.
     *
     * <p>This default implementation returns a box based on sides {@code radius * 2 + 1}. Subclasses should override
     * this method if their bounding box is different.
     *
     * @return The bounding box of the brush.
     */
    @Override
    public Rectangle getBoundingBox() {
        final int radius = getRadius();
        if (radius != -boundingBox.getX()) {
            boundingBox.setBounds(-radius, -radius, (2 * radius) + 1, (2 * radius) + 1);
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