package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;

/**
 * Created by Pepijn Schmitz on 13-10-16.
 */
public class DifferenceHeightMap extends CombiningHeightMap {
    public DifferenceHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super(heightMap1, heightMap2);
    }

    public DifferenceHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super(name, heightMap1, heightMap2);
    }

    @Override
    protected double doGetHeight(int x, int y) {
        return children[0].getHeight(x, y) - children[1].getHeight(x, y);
    }

    @Override
    protected double doGetHeight(float x, float y) {
        return children[0].getHeight(x, y) - children[1].getHeight(x, y);
    }

    @Override
    public Icon getIcon() {
        return ICON_DIFFERENCE_HEIGHTMAP;
    }

    @Override
    public double[] getRange() {
        double[] range0 = children[0].getRange();
        double[] range1 = children[1].getRange();
        return new double[] {range0[0] - range1[0], range0[1] - range1[1]};
    }

    @Override
    public CombiningHeightMap clone() {
        DifferenceHeightMap clone = new DifferenceHeightMap(name, children[0].clone(), children[1].clone());
        clone.setSeed(seed);
        return clone;
    }

    private static final Icon ICON_DIFFERENCE_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/minus.png");
}