/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

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
    public float getStrength(int centerX, int centerY, int x, int y) {
        return getStrength(x - centerX, y - centerY);
    }
    
    @Override
    public float getFullStrength(int centerX, int centerY, int x, int y) {
        return getFullStrength(x - centerX, y - centerY);
    }

    @Override
    public int getEffectiveRadius() {
        return getRadius();
    }

    @Override
    public AbstractBrush clone() {
        try {
            return (AbstractBrush) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private final String name;
}