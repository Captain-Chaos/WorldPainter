/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.HeightMap;

import java.awt.*;
import java.io.Serializable;

/**
 * An abstract base class for height maps which provides default implementations of most methods. To implement you must
 * at the very least:
 *
 * <ul>
 *     <li>Override one or both of {@link #getHeight(int, int)} and {@link #getHeight(float, float)}.
 *     <li>Implement {@link #getRange()}
 *     <li>Implement {@link #getIcon()}
 * </ul>
 *
 * @author pepijn
 */
public abstract class AbstractHeightMap implements HeightMap, Cloneable, Serializable {
    public AbstractHeightMap() {
        name = null;
    }
    
    public AbstractHeightMap(String name) {
        this.name = name;
    }

    public DelegatingHeightMap getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public AbstractHeightMap clone() {
        try {
            return (AbstractHeightMap) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Rectangle getExtent() {
        return null;
    }

    @Override
    public int getColour(int x, int y) {
        int value = (int) MathUtils.clamp(0L, Math.round(getHeight(x, y)), 255L);
        return (value << 16) | (value << 8) | value;
    }

    @Override
    public double getHeight(float x, float y) {
        return getHeight(Math.round(x), Math.round(y));
    }

    @Override
    public double getHeight(int x, int y) {
        return getHeight((float) x, (float) y);
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public double getConstantValue() {
        throw new UnsupportedOperationException("Not a constant height map");
    }

    @Override
    public double getBaseHeight() {
        return getRange()[0];
    }

    protected String name;
    protected long seed;
    DelegatingHeightMap parent;
    
    private static final long serialVersionUID = 1L;
}