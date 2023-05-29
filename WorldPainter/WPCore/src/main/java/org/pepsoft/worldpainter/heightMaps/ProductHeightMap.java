/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;

/**
 * A height map which is the product of two other height maps.
 * 
 * @author pepijn
 */
public class ProductHeightMap extends CombiningHeightMap {
    public ProductHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super(heightMap1, heightMap2);
    }

    public ProductHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super(name, heightMap1, heightMap2);
    }

    @Override
    public double doGetHeight(int x, int y) {
        double height1 = children[0].getHeight(x, y);
        return (height1 == 0.0) ? 0.0 : height1 * children[1].getHeight(x, y);
    }

    @Override
    public double doGetHeight(float x, float y) {
        double height1 = children[0].getHeight(x, y);
        return (height1 == 0.0) ? 0.0 : height1 * children[1].getHeight(x, y);
    }

    @Override
    public ProductHeightMap clone() {
        ProductHeightMap clone = new ProductHeightMap(name, children[0].clone(), children[1].clone());
        clone.setSeed(getSeed());
        return clone;
    }

    @Override
    public Icon getIcon() {
        return ICON_PRODUCT_HEIGHTMAP;
    }

    @Override
    public double[] getRange() {
        double[] range0 = children[0].getRange();
        double[] range1 = children[1].getRange();
        return new double[] {range0[0] * range1[0], range0[1] * range1[1]};
    }

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_PRODUCT_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/times.png");
}