/*
 * Copyright (C) 2014 pepijn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.worldpainter.heightMaps;

import java.awt.Rectangle;
import org.pepsoft.worldpainter.HeightMap;

/**
 * A height map which calculates the slope of an underlying height map, in
 * degrees from 0 to 90.
 *
 * @author pepijn
 */
public class SlopeHeightMap extends AbstractHeightMap {
    public SlopeHeightMap(HeightMap baseHeightMap) {
        this(baseHeightMap, 1.0f);
    }

    public SlopeHeightMap(HeightMap baseHeightMap, String name) {
        this(baseHeightMap, 1.0f, name);
    }
    
    public SlopeHeightMap(HeightMap baseHeightMap, float verticalScaling) {
        super(baseHeightMap.getName() != null ? "Slope of " + baseHeightMap.getName() : null);
        this.baseHeightMap = baseHeightMap;
        this.verticalScaling = verticalScaling;
    }

    public SlopeHeightMap(HeightMap baseHeightMap, float verticalScaling, String name) {
        super(name);
        this.baseHeightMap = baseHeightMap;
        this.verticalScaling = verticalScaling;
    }
    
    @Override
    public float getHeight(int x, int y) {
        if (verticalScaling != 1.0f) {
            return (float) (Math.tan(Math.max(Math.max(Math.abs(baseHeightMap.getHeight(x + 1, y) / verticalScaling - baseHeightMap.getHeight(x - 1, y) / verticalScaling) / 2,
                Math.abs(baseHeightMap.getHeight(x + 1, y + 1) / verticalScaling - baseHeightMap.getHeight(x - 1, y - 1) / verticalScaling) / ROOT_EIGHT),
                Math.max(Math.abs(baseHeightMap.getHeight(x, y + 1) / verticalScaling - baseHeightMap.getHeight(x, y - 1) / verticalScaling) / 2,
                Math.abs(baseHeightMap.getHeight(x - 1, y + 1) / verticalScaling - baseHeightMap.getHeight(x + 1, y - 1) / verticalScaling) / ROOT_EIGHT))) * RADIANS_TO_DEGREES);
        } else {
            return (float) (Math.tan(Math.max(Math.max(Math.abs(baseHeightMap.getHeight(x + 1, y) / verticalScaling - baseHeightMap.getHeight(x - 1, y)) / 2,
                Math.abs(baseHeightMap.getHeight(x + 1, y + 1) - baseHeightMap.getHeight(x - 1, y - 1)) / ROOT_EIGHT),
                Math.max(Math.abs(baseHeightMap.getHeight(x, y + 1) - baseHeightMap.getHeight(x, y - 1)) / 2,
                Math.abs(baseHeightMap.getHeight(x - 1, y + 1) - baseHeightMap.getHeight(x + 1, y - 1)) / ROOT_EIGHT))) * RADIANS_TO_DEGREES);
        }
    }

    @Override
    public float getBaseHeight() {
        return baseHeightMap.getBaseHeight();
    }

    @Override
    public Rectangle getExtent() {
        return baseHeightMap.getExtent();
    }

    @Override
    public void setSeed(long seed) {
        baseHeightMap.setSeed(seed);
    }

    @Override
    public long getSeed() {
        return baseHeightMap.getSeed();
    }

    @Override
    public SlopeHeightMap clone() {
        return new SlopeHeightMap(baseHeightMap.clone(), name);
    }
    
    private final HeightMap baseHeightMap;
    private final float verticalScaling;

    private static final double ROOT_EIGHT = Math.sqrt(8.0);
    private static final double RADIANS_TO_DEGREES = 180 / Math.PI;
    private static final long serialVersionUID = 1739316597277213055L;
}