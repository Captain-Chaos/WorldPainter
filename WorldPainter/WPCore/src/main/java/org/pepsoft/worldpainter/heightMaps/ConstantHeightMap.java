/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

/**
 *
 * @author pepijn
 */
public final class ConstantHeightMap extends AbstractHeightMap {
    public ConstantHeightMap(float height) {
        this.height = height;
    }

    public ConstantHeightMap(String name, float height) {
        super(name);
        this.height = height;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    // HeightMap

    @Override
    public float getHeight(int x, int y) {
        return height;
    }

    @Override
    public float getHeight(float x, float y) {
        return height;
    }

    @Override
    public float getBaseHeight() {
        return height;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public float getConstantValue() {
        return height;
    }

    private float height;
    
    private static final long serialVersionUID = 1L;
}