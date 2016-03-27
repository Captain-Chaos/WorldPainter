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

import org.pepsoft.worldpainter.HeightMap;

import java.awt.*;

/**
 * A height map which scales and/or translates another height map
 *
 * @author pepijn
 */
public class TransformingHeightMap extends DelegatingHeightMap {
    public TransformingHeightMap(String name, HeightMap baseHeightMap, int scale, int offsetX, int offsetY) {
        super("baseHeightMap");
        setName(name);
        setHeightMap(0, baseHeightMap);
        this.scale = scale / 100.0f;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public HeightMap getBaseHeightMap() {
        return children[0];
    }

    public void setBaseHeightMap(HeightMap baseHeightMap) {
        replace(0, baseHeightMap);
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public int getScale() {
        return (int) (scale * 100 + 0.5f);
    }

    public void setScale(int scale) {
        this.scale = scale / 100.0f;
    }

    @Override
    public float getHeight(float x, float y) {
        if (scale == 1.0) {
            return children[0].getHeight(x - offsetX, y - offsetY);
        } else {
            return children[0].getHeight((x - offsetX) / scale, (y - offsetY) / scale);
        }
    }

    @Override
    public int getColour(int x, int y) {
        if (scale == 1.0) {
            return children[0].getColour(x - offsetX, y - offsetY);
        } else {
            return children[0].getColour((int) ((x - offsetX) / scale + 0.5), (int) ((y - offsetY) / scale + 0.5));
        }
    }
    
    @Override
    public Rectangle getExtent() {
        Rectangle extent = children[0].getExtent();
        if (extent != null) {
            extent = (Rectangle) extent.clone();
            if ((offsetX != 0) || (offsetY != 0)) {
                extent.translate(offsetX, offsetY);
            }
            if (scale != 1.0) {
                extent.width = (int) (extent.width * scale + 0.5);
                extent.height = (int) (extent.height * scale + 0.5);
            }
            return extent;
        } else {
            return null;
        }
    }

    public static TransformingHeightMapBuilder build() {
        return new TransformingHeightMapBuilder();
    }

    private int offsetX, offsetY;
    private float scale;

    public static class TransformingHeightMapBuilder {
        public TransformingHeightMapBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public TransformingHeightMapBuilder withHeightMap(HeightMap baseHeightMap) {
            this.baseHeightMap = baseHeightMap;
            return this;
        }

        public TransformingHeightMapBuilder withOffset(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            return this;
        }

        public TransformingHeightMapBuilder withScale(int scale) {
            this.scale = scale;
            return this;
        }

        public TransformingHeightMap now() {
            return new TransformingHeightMap(name, baseHeightMap, scale, offsetX, offsetY);
        }

        private String name;
        private HeightMap baseHeightMap;
        private int offsetX, offsetY;
        private int scale = 100;
    }
}