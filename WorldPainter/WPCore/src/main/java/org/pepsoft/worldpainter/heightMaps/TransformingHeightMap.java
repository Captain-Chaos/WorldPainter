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

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * A height map which scales and/or translates another height map
 *
 * @author pepijn
 */
public class TransformingHeightMap extends DelegatingHeightMap {
    public TransformingHeightMap(String name, HeightMap baseHeightMap, int scaleX, int scaleY, int offsetX, int offsetY, int rotation) {
        super("baseHeightMap");
        setName(name);
        setHeightMap(0, baseHeightMap);
        this.scaleX = scaleX / 100.0f;
        this.scaleY = scaleY / 100.0f;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotation = rotation;
        recalculate();
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
        recalculate();
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
        recalculate();
    }

    public int getScaleX() {
        return (int) (scaleX * 100 + 0.5f);
    }

    public void setScaleX(int scaleX) {
        this.scaleX = scaleX / 100.0f;
        recalculate();
    }

    public int getScaleY() {
        return (int) (scaleY * 100 + 0.5f);
    }

    public void setScaleY(int scaleY) {
        this.scaleY = scaleY / 100.0f;
        recalculate();
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        recalculate();
    }

    @Override
    public float doGetHeight(int x, int y) {
        if (translateOnly) {
            return children[0].getHeight(x - offsetX, y - offsetY);
        } else {
            Point2D.Float coords = new Point2D.Float(x, y);
            transform.transform(coords, coords);
            return children[0].getHeight(coords.x, coords.y);
        }
    }

    @Override
    public float doGetHeight(float x, float y) {
        if (translateOnly) {
            return children[0].getHeight(x - offsetX, y - offsetY);
        } else {
            Point2D.Float coords = new Point2D.Float(x, y);
            transform.transform(coords, coords);
            return children[0].getHeight(coords.x, coords.y);
        }
    }

    @Override
    public int doGetColour(int x, int y) {
        if (translateOnly) {
            return children[0].getColour(x - offsetX, y - offsetY);
        } else {
            Point2D.Float coords = new Point2D.Float(x, y);
            transform.transform(coords, coords);
            return children[0].getColour((int) (coords.x + 0.5f), (int) (coords.y + 0.5f));
        }
    }
    
    @Override
    public Rectangle getExtent() {
        Rectangle extent = children[0].getExtent();
        if (extent != null) {
            if (translateOnly) {
                return new Rectangle(extent.x + offsetX, extent.y + offsetY, extent.width, extent.height);
            } else {
                Point2D p1 = new Point2D.Double(extent.getMinX(), extent.getMinY());
                Point2D p2 = new Point2D.Double(extent.getMinX(), extent.getMaxY());
                Point2D p3 = new Point2D.Double(extent.getMaxX(), extent.getMinY());
                Point2D p4 = new Point2D.Double(extent.getMaxX(), extent.getMaxY());
                try {
                    transform.inverseTransform(p1, p1);
                    transform.inverseTransform(p2, p2);
                    transform.inverseTransform(p3, p3);
                    transform.inverseTransform(p4, p4);
                } catch (NoninvertibleTransformException e) {
                    throw new RuntimeException(e);
                }
                double minX = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
                double maxX = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
                double minY = Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()));
                double maxY = Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()));
                return new Rectangle((int) minX, (int) minY, (int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY));
            }
        } else {
            return null;
        }
    }

    @Override
    public Icon getIcon() {
        return ICON_TRANSFORMING_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        return children[0].getRange();
    }

    private void recalculate() {
        if ((scaleX == 1.0f) && (scaleY == 1.0f) && (rotation == 0)) {
            translateOnly = true;
            transform = null;
        } else {
            translateOnly = false;
            transform = new AffineTransform();
            if ((scaleX != 1.0f) || (scaleY != 1.0f)) {
                transform.scale(1 / scaleX, 1 / scaleY);
            }
            if ((offsetX != 0) || (offsetY != 0)) {
                transform.translate(-offsetX, -offsetY);
            }
            if (rotation != 0) {
                transform.rotate(-rotation / DOUBLE_PI);
            }
        }
    }

    public static TransformingHeightMapBuilder build() {
        return new TransformingHeightMapBuilder();
    }

    private int offsetX, offsetY, rotation;
    private float scaleX, scaleY;
    private AffineTransform transform;
    private boolean translateOnly;

    private static final long serialVersionUID = 1L;
    private static final double DOUBLE_PI = Math.PI * 2;
    private static final Icon ICON_TRANSFORMING_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/transform.png");

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
            scaleX = scale;
            scaleY = scale;
            return this;
        }

        public TransformingHeightMapBuilder withScale(int scaleX, int scaleY) {
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            return this;
        }

        public TransformingHeightMapBuilder withRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }

        public TransformingHeightMap now() {
            return new TransformingHeightMap(name, baseHeightMap, scaleX, scaleY, offsetX, offsetY, rotation);
        }

        private String name;
        private HeightMap baseHeightMap;
        private int offsetX, offsetY;
        private int scaleX = 100, scaleY = 100;
        private int rotation;
    }
}