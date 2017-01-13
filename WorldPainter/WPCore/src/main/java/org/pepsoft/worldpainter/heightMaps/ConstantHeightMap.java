/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;

import javax.swing.*;

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
    public boolean isConstant() {
        return true;
    }

    @Override
    public float getConstantValue() {
        return height;
    }

    @Override
    public Icon getIcon() {
        return ICON_CONSTANT_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        return new float[] {height, height};
    }

    private float height;
    
    private static final long serialVersionUID = 1L;
    private static final Icon ICON_CONSTANT_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/x.png");
}