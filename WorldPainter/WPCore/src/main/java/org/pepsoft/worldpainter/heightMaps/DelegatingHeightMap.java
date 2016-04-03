package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.HeightMap;

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
        return index;
    }

    // HeightMap

    @Override
    public float getBaseHeight() {
        return children[0].getBaseHeight();
    }

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

    protected final HeightMap[] children;
    private final String[] roles;
    private final Map<String, Integer> indices = new HashMap<>();

    private static final long serialVersionUID = 1L;
}