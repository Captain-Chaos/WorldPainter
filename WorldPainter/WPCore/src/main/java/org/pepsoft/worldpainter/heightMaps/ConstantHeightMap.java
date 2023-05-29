/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author pepijn
 */
public final class ConstantHeightMap extends AbstractHeightMap {
    public ConstantHeightMap(double height) {
        this.dHeight = height;
    }

    public ConstantHeightMap(String name, double height) {
        super(name);
        this.dHeight = height;
    }

    public double getHeight() {
        return dHeight;
    }

    // HeightMap

    @Override
    public double getHeight(int x, int y) {
        return dHeight;
    }

    @Override
    public double getHeight(float x, float y) {
        return dHeight;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public double getConstantValue() {
        return dHeight;
    }

    @Override
    public Icon getIcon() {
        return ICON_CONSTANT_HEIGHTMAP;
    }

    @Override
    public double[] getRange() {
        return new double[] {dHeight, dHeight};
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (version == 0) {
            dHeight = height;
            height = 0.0f;
            version = 1;
        }
    }

    private float height;
    private double dHeight;
    private int version = 1;
    
    private static final long serialVersionUID = 1L;
    private static final Icon ICON_CONSTANT_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/x.png");
}