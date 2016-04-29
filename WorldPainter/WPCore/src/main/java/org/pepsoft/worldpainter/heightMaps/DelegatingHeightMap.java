package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.HeightMap;

import java.util.Arrays;

/**
 * Created by Pepijn Schmitz on 01-04-16.
 */
public abstract class DelegatingHeightMap extends AbstractHeightMap {
    protected DelegatingHeightMap(String... roleNames) {
        this.roleNames = roleNames;
        children = new HeightMap[roleNames.length];
    }

    /**
     * Recursively recalculate the constant value, if any, of the tree of height
     * maps.
     */
    public void recalculateConstantValue() {
        for (HeightMap child: children) {
            if (child instanceof DelegatingHeightMap) {
                ((DelegatingHeightMap) child).recalculateConstantValue();
            }
        }
        determineConstant();
    }

    @Override
    public long getSeed() {
        return children[0].getSeed();
    }

    @Override
    public void setSeed(long seed) {
        for (HeightMap child: children) {
            child.setSeed(seed);
        }
    }

    @Override
    public float getBaseHeight() {
        return baseHeight;
    }

    @Override
    public boolean isConstant() {
        return constant;
    }

    @Override
    public float getConstantValue() {
        return constantValue;
    }

    @Override
    public final float getHeight(float x, float y) {
        if (constant) {
            return constantValue;
        } else {
            return doGetHeight(x, y);
        }
    }

    @Override
    public final float getHeight(int x, int y) {
        if (constant) {
            return constantValue;
        } else {
            return doGetHeight(x, y);
        }
    }

    @Override
    public final int getColour(int x, int y) {
        if (constant) {
            return constantColour;
        } else {
            return doGetColour(x, y);
        }
    }

    public int getIndex(HeightMap heightMap) {
        for (int index = 0; index < children.length; index++) {
            if (heightMap.equals(children[index])) {
                return index;
            }
        }
        throw new IllegalArgumentException();
    }

    public HeightMap getHeightMap(int index) {
        return children[index];
    }

    public String getRole(int index) {
        return roleNames[index];
    }

    public int getHeightMapCount() {
        return children.length;
    }

    public void replace(HeightMap oldHeightMap, HeightMap newHeightMap) {
        children[getIndex(oldHeightMap)] = newHeightMap;
        if (newHeightMap instanceof AbstractHeightMap) {
            ((AbstractHeightMap) newHeightMap).parent = this;
        }
        childrenChanged();
    }

    protected void setHeightMap(int index, HeightMap heightMap) {
        children[index] = heightMap;
        if (heightMap instanceof AbstractHeightMap) {
            ((AbstractHeightMap) heightMap).parent = this;
        }
        childrenChanged();
    }

    protected void replace(int index, HeightMap heightMap) {
        children[index] = heightMap;
        if (heightMap instanceof AbstractHeightMap) {
            ((AbstractHeightMap) heightMap).parent = this;
        }
        childrenChanged();
    }


    protected void childrenChanged() {
        determineConstant();
    }

    /**
     * Determines whether this height map can be constant by checking that all
     * its children are constant height maps. Invoked automatically whenever the
     * children of this height map change; should also be invoked by subclasses
     * whenever some property which affects the calculated value of the height
     * map changes.
     */
    protected void determineConstant() {
        if (Arrays.stream(children).allMatch(m -> (m != null) && m.isConstant())) {
            constant = true;
            constantValue = doGetHeight(0, 0);
            constantColour = doGetColour(0, 0);
        } else {
            constant = false;
        }
    }

    protected int doGetColour(int x, int y) {
        int value = MathUtils.clamp(0, (int) (doGetHeight(x, y) + 0.5f), 255);
        return (value << 16) | (value << 8) | value;
    }

    protected float doGetHeight(float x, float y) {
        return doGetHeight((int) (x + 0.5f), (int) (y + 0.5f));
    }

    protected float doGetHeight(int x, int y) {
        return doGetHeight((float) x, (float) y);
    }

    protected final String[] roleNames;
    protected final HeightMap[] children;
    protected float baseHeight, constantValue;
    protected boolean constant;
    protected int constantColour;
}