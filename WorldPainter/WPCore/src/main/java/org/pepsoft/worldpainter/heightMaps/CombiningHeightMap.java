/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.pepsoft.worldpainter.HeightMap;

/**
 * An abstract base class for a height map which somehow combines two
 * subordinate height maps.
 * 
 * @author SchmitzP
 */
public abstract class CombiningHeightMap extends DelegatingHeightMap {
    public CombiningHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super("heightMap1", "heightMap2");
        setHeightMap(0, heightMap1);
        setHeightMap(1, heightMap2);
    }

    public CombiningHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super("heightMap1", "heightMap2");
        setName(name);
        setHeightMap(0, heightMap1);
        setHeightMap(1, heightMap2);
    }

    public final HeightMap getHeightMap1() {
        return children[0];
    }

    public final HeightMap getHeightMap2() {
        return children[1];
    }

    public void setHeightMap1(HeightMap heightMap1) {
        replace(0, heightMap1);
    }

    public void setHeightMap2(HeightMap heightMap2) {
        replace(1, heightMap2);
    }

    // HeightMap

    @Override
    public Rectangle getExtent() {
        Rectangle extent1 = children[0].getExtent();
        Rectangle extent2 = children[1].getExtent();
        return (extent1 != null)
            ? ((extent2 != null) ? extent1.union(extent2) : extent1)
            : extent2;
    }

    @Override
    public abstract CombiningHeightMap clone();

    private Object readResolve() throws ObjectStreamException {
        if (heightMap1 != null) {
            try {
                Constructor<? extends CombiningHeightMap> constructor = getClass().getConstructor(String.class, HeightMap.class, HeightMap.class);
                return constructor.newInstance(name, heightMap1, heightMap2);
            } catch (NoSuchMethodException e) {
                throw new InvalidClassException(getClass().getName(), "Missing (String, HeightMap, HeightMap) constructor");
            } catch (IllegalAccessException e) {
                throw new InvalidClassException(getClass().getName(), "(String, HeightMap, HeightMap) constructor not accessible");
            } catch (InstantiationException e) {
                throw new InvalidClassException(getClass().getName(), "Abstract class");
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            return this;
        }
    }

    @Deprecated
    protected HeightMap heightMap1, heightMap2;

    private static final long serialVersionUID = 1L;
}