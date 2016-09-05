package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.HeightMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pepijn on 26-3-16.
 */
public abstract class DelegatingHeightMap extends AbstractHeightMap {
    protected DelegatingHeightMap(String... roles) {
        this.roles = roles;
        children = new HeightMap[roles.length];
        for (int index = 0; index < roles.length; index++) {
            indices.put(roles[index], index);
        }
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

    public final int getHeightMapCount() {
        return children.length;
    }

    public final String getRole(int index) {
        return roles[index];
    }

    public final int getIndex(String role) {
        return indices.get(role);
    }

    public final int getIndex(HeightMap heightMap) {
        for (int index = 0; index < children.length; index++)  {
            if (children[index] == heightMap) {
                return index;
            }
        }
        throw new IllegalArgumentException();
    }

    public final HeightMap getHeightMap(int index) {
        return children[index];
    }

    public final HeightMap getHeightMap(String role) {
        return children[indices.get(role)];
    }

    public final void setHeightMap(int index, HeightMap child) {
        if (children[index] != null) {
            throw new IllegalStateException();
        }
        children[index] = child;
        if (child instanceof AbstractHeightMap) {
            ((AbstractHeightMap) child).parent = this;
        }
        childrenChanged();
    }

    public final void setHeightMap(String role, HeightMap child) {
        setHeightMap(getIndex(role), child);
    }

    public final HeightMap replace(int index, HeightMap newChild) {
        HeightMap oldChild = children[index];
        children[index] = newChild;
        if (oldChild instanceof AbstractHeightMap) {
            ((AbstractHeightMap) oldChild).parent = null;
        }
        if (newChild instanceof AbstractHeightMap) {
            ((AbstractHeightMap) newChild).parent = this;
        }
        childrenChanged();
        return oldChild;
    }

    public final int replace(HeightMap oldChild, HeightMap newChild) {
        int index = getIndex(oldChild);
        children[index] = newChild;
        if (oldChild instanceof AbstractHeightMap) {
            ((AbstractHeightMap) oldChild).parent = null;
        }
        if (newChild instanceof AbstractHeightMap) {
            ((AbstractHeightMap) newChild).parent = this;
        }
        childrenChanged();
        return index;
    }

    // HeightMap

    @Override
    public final long getSeed() {
        return children[0].getSeed();
    }

    @Override
    public final void setSeed(long seed) {
        for (HeightMap child: children) {
            child.setSeed(seed);
        }
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

    protected final HeightMap[] children;
    private final String[] roles;
    private final Map<String, Integer> indices = new HashMap<>();
    protected float constantValue;
    protected boolean constant;
    protected int constantColour;

    private static final long serialVersionUID = 1L;
}