/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import java.awt.Rectangle;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.HeightMap;

/**
 *
 * @author pepijn
 */
public abstract class AbstractHeightMap implements HeightMap, Cloneable {
    public AbstractHeightMap() {
        name = null;
    }
    
    public AbstractHeightMap(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
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
        int value = MathUtils.clamp(0, (int) (getHeight(x, y) + 0.5f), 255);
        return (value << 16) | (value << 8) | value;
    }
    
    protected final String name;
    protected long seed;
    
    private static final long serialVersionUID = 1L;
}