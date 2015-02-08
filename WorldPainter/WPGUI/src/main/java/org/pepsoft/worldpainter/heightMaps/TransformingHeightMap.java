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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import org.pepsoft.worldpainter.HeightMap;

/**
 * A height map which scales and/or translates another height map
 *
 * @author pepijn
 */
public class TransformingHeightMap extends AbstractHeightMap {
    public TransformingHeightMap(HeightMap baseHeightMap) {
        this(null, baseHeightMap, 100, 0, 0, false);
    }
    
    public TransformingHeightMap(HeightMap baseHeightMap, int scale, boolean smoothScaling) {
        this(null, baseHeightMap, scale, 0, 0, smoothScaling);
    }

    public TransformingHeightMap(HeightMap baseHeightMap, int offsetX, int offsetY) {
        this(null, baseHeightMap, 100, offsetX, offsetY, false);
    }

    public TransformingHeightMap(HeightMap baseHeightMap, int scale, int offsetX, int offsetY, boolean smoothScaling) {
        this(null, baseHeightMap, scale, offsetX, offsetY, smoothScaling);
    }

    public TransformingHeightMap(String name, HeightMap baseHeightMap) {
        this(name, baseHeightMap, 100, 0, 0, false);
    }
    
    public TransformingHeightMap(String name, HeightMap baseHeightMap, int scale, boolean smoothScaling) {
        this(name, baseHeightMap, scale, 0, 0, smoothScaling);
    }

    public TransformingHeightMap(String name, HeightMap baseHeightMap, int offsetX, int offsetY) {
        this(name, baseHeightMap, 100, offsetX, offsetY, false);
    }

    public TransformingHeightMap(String name, HeightMap baseHeightMap, int scale, int offsetX, int offsetY, boolean smoothScaling) {
        super(name);
        this.scale = scale / 100.0;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        if (scale == 100) {
            this.baseHeightMap = baseHeightMap;
            baseHeightMapScaled = false;
        } else {
            if (baseHeightMap instanceof BitmapHeightMap) {
                BufferedImage image = ((BitmapHeightMap) baseHeightMap).getImage();
                if ((image.getType() == BufferedImage.TYPE_BYTE_BINARY) || (image.getType() == BufferedImage.TYPE_BYTE_GRAY)) {
                    final int widthInBlocks = image.getWidth() * scale / 100;
                    final int heightInBlocks = image.getHeight() * scale / 100;
                    final int bitDepth = image.getSampleModel().getSampleSize(0);
                    final boolean sixteenBit = bitDepth == 16;
                    final BufferedImage scaledImage = new BufferedImage(widthInBlocks, heightInBlocks, sixteenBit ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY);
                    final Graphics2D g2 = scaledImage.createGraphics();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2.drawImage(image, 0, 0, widthInBlocks, heightInBlocks, null);
                    } finally {
                        g2.dispose();
                    }
                    this.baseHeightMap = new BitmapHeightMap(baseHeightMap.getName() + " (scaled)", scaledImage);
                    baseHeightMapScaled = true;
                } else {
                    throw new UnsupportedOperationException("Scaling of colour height maps not supported yet");
                }
            } else if (! smoothScaling) {
                this.baseHeightMap = baseHeightMap;
                baseHeightMapScaled = false;
            } else {
                throw new UnsupportedOperationException("Smooth scaling of non-image height maps not supported yet");
            }
        }
    }
    
    @Override
    public float getHeight(int x, int y) {
        if (baseHeightMapScaled || (scale == 1.0)) {
            return baseHeightMap.getHeight(x - offsetX, y - offsetY);
        } else {
            return baseHeightMap.getHeight((int) ((x - offsetX) / scale + 0.5), (int) ((y - offsetY) / scale + 0.5));
        }
    }

    @Override
    public int getColour(int x, int y) {
        if (baseHeightMapScaled || (scale == 1.0)) {
            return baseHeightMap.getColour(x - offsetX, y - offsetY);
        } else {
            return baseHeightMap.getColour((int) ((x - offsetX) / scale + 0.5), (int) ((y - offsetY) / scale + 0.5));
        }
    }
    
    @Override
    public float getBaseHeight() {
        return baseHeightMap.getBaseHeight();
    }

    @Override
    public Rectangle getExtent() {
        Rectangle extent = baseHeightMap.getExtent();
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

    private final HeightMap baseHeightMap;
    private final int offsetX, offsetY;
    private final double scale;
    private final boolean baseHeightMapScaled;
}
