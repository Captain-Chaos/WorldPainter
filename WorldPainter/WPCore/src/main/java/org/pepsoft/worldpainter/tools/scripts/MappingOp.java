/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.heightMaps.TransformingHeightMap;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author SchmitzP
 */
@SuppressWarnings("unused") // Used from scripts
public class MappingOp extends AbstractOperation<Void> {
    public MappingOp(ScriptingContext context, HeightMap heightMap) throws ScriptException {
        super(context);
        if (heightMap == null) {
            throw new ScriptException("heightMap may not be null");
        }
        this.heightMap = heightMap;
        Arrays.fill(mapping, -1);
    }
    
    public MappingOp(ScriptingContext context, Layer layer) throws ScriptException {
        super(context);
        if (layer == null) {
            throw new ScriptException("layer may not be null");
        }
        this.layer = layer;
        switch (layer.dataSize) {
            case BIT:
            case BIT_PER_CHUNK:
                layerValue = 1;
                break;
            case NIBBLE:
                layerValue = 8;
                break;
            case BYTE:
                layerValue = 128;
                break;
            default:
                throw new ScriptException("Layer type " + layer.getClass().getSimpleName() + " not supported");
        }
        Arrays.fill(mapping, -1);
    }

    public MappingOp(ScriptingContext context, int terrainIndex) throws ScriptException {
        super(context);
        if ((terrainIndex < 0) || (terrainIndex >= Terrain.VALUES.length)) {
            throw new ScriptException("Invalid terrain index specified");
        }
        this.terrainIndex = terrainIndex;
        mode = Mode.SET_TERRAIN;
        Arrays.fill(mapping, -1);
    }

    public MappingOp applyToLayer(Layer layer) {
        this.layer = layer;
        if (mode == Mode.SET_TERRAIN) {
            mode = Mode.SET;
        }
        return this;
    }
    
    public MappingOp applyToTerrain() {
        layer = null;
        mode = Mode.SET_TERRAIN;
        return this;
    }
    
    public MappingOp toWorld(World2 world) {
        this.world = world;
        return this;
    }
    
    public MappingOp applyToSurface() {
        this.dimIndex = DIM_NORMAL;
        return this;
    }
    
    public MappingOp applyToNether() {
        this.dimIndex = DIM_NETHER;
        return this;
    }
    
    public MappingOp applyToEnd() {
        this.dimIndex = DIM_END;
        return this;
    }

    public MappingOp applyToSurfaceCeiling() {
        this.dimIndex = DIM_NORMAL_CEILING;
        return this;
    }

    public MappingOp applyToNetherCeiling() {
        this.dimIndex = DIM_NETHER_CEILING;
        return this;
    }

    public MappingOp applyToEndCeiling() {
        this.dimIndex = DIM_END_CEILING;
        return this;
    }

    public MappingOp fromLevel(int level) throws ScriptException {
        if (! colourMapping.isEmpty()) {
            throw new ScriptException("Cannot mix grey scale and colour mapping");
        }
        storedLowerFrom = level;
        storedUpperFrom = level;
        return this;
    }
    
    public MappingOp fromLevels(int lower, int upper) throws ScriptException {
        if (! colourMapping.isEmpty()) {
            throw new ScriptException("Cannot mix grey scale and colour mapping");
        }
        storedLowerFrom = lower;
        storedUpperFrom = upper;
        return this;
    }
    
    public MappingOp fromColour(int red, int green, int blue) throws ScriptException {
        validateRGB(red, green, blue);
        storedColour = 0xff000000L | (red << 16) | (green << 8) | blue;
        return this;
    }

    public MappingOp fromColour(int alpha, int red, int green, int blue) throws ScriptException {
        if ((alpha < 0) || (alpha > 255)) {
            throw new ScriptException("Invalid alpha value " + alpha + " specified");
        }
        validateRGB(red, green, blue);
        storedColour = ((long) alpha << 24) | (red << 16) | (green << 8) | blue;
        return this;
    }
    
    public MappingOp toLevel(int level) throws ScriptException {
        if ((level < 0) || (level > 255)) {
            throw new ScriptException("Illegal value for layer: " + level);
        }
        mapStoredValuesTo(level);
        layerValue = level;
        return this;
    }

    public MappingOp toTerrain(int terrain) throws ScriptException {
        if ((terrain < 0) || (terrain >= Terrain.VALUES.length)) {
            throw new ScriptException("Illegal value for terrain index: " + terrain);
        }
        mapStoredValuesTo(terrain);
        return this;
    }
    
    public MappingOp toLevels(int lower, int upper) {
        if (storedColour != -1) {
            throw new IllegalArgumentException("Cannot map a colour to a range");
        } else if (storedLowerFrom == storedUpperFrom) {
            if (lower == upper) {
                mapping[storedLowerFrom] = lower;
            } else {
                throw new IllegalArgumentException("Cannot map a single value to a range");
            }
        } else if (lower == upper) {
            for (int i = storedLowerFrom; i <= storedUpperFrom; i++) {
                mapping[i] = lower;
            }
        } else {
            float factor = (float) (upper - lower) / (storedUpperFrom - storedLowerFrom);
            for (int i = storedLowerFrom; i <= storedUpperFrom; i++) {
                mapping[i] = lower + (int) ((i - storedLowerFrom) * factor + 0.5f);
            }
        }
        return this;
    }
    
    public MappingOp setAlways() {
        mode = Mode.SET;
        return this;
    }
    
    public MappingOp setWhenLower() {
        mode = Mode.SET_WHEN_LOWER;
        return this;
    }
    
    public MappingOp setWhenHigher() {
        mode = Mode.SET_WHEN_HIGHER;
        return this;
    }
    
    public MappingOp scale(int scale) {
        this.scale = scale;
        return this;
    }

    public MappingOp shift(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }
    
    public MappingOp withFilter(Filter filter) {
        this.filter = filter;
        return this;
    }
    
    @Override
    public Void go() throws ScriptException {
        goCalled();

        // Check preconditions
        if ((heightMap == null) && (layer == null) && (terrainIndex == -1)) {
            throw new ScriptException("No data source (heightMap, layer or terrain) specified");
        }
        if ((mode != Mode.SET_TERRAIN) && (layer == null)) {
            throw new ScriptException("layer not specified");
        }
        if (world == null) {
            throw new ScriptException("world not specified");
        }
        boolean greyScaleMapPresent = false;
        for (int mappedValue: mapping) {
            if (mappedValue != -1) {
                greyScaleMapPresent = true;
                break;
            }
        }
        final boolean colourMapPresent = ! colourMapping.isEmpty();
        if ((greyScaleMapPresent || colourMapPresent) && (heightMap == null)) {
            throw new ScriptException("Mapping specified but no height map specified");
        } else if (heightMap != null) {
            if ((! greyScaleMapPresent) && (! colourMapPresent)) {
                throw new ScriptException("mapping not specified");
            }
            if (greyScaleMapPresent && colourMapPresent) {
                throw new ScriptException("Cannot mix grey scale and colour mapping");
            }
            if (layer != null) {
                if (layer.dataSize == Layer.DataSize.NONE) {
                    throw new ScriptException("Layer of unsupported type specified: " + layer);
                }
                int bits;
                switch (layer.dataSize) {
                    case BIT:
                    case BIT_PER_CHUNK:
                        bits = 1;
                        break;
                    case NIBBLE:
                        bits = 4;
                        break;
                    case BYTE:
                        bits = 8;
                        break;
                    default:
                        throw new InternalError();
                }
                int maxValue = (1 << bits) - 1;
                if (greyScaleMapPresent) {
                    for (int mappedValue: mapping) {
                        if ((mappedValue < -1) || (mappedValue > maxValue)) {
                            throw new ScriptException("Invalid destination level " + mappedValue + " specified for " + bits + "-bit layer " + layer);
                        }
                    }
                } else {
                    for (Map.Entry<Integer, Integer> entry: colourMapping.entrySet()) {
                        int mappedValue = entry.getValue();
                        if ((mappedValue < 0) || (mappedValue > maxValue)) {
                            throw new ScriptException("Invalid destination level " + mappedValue + " specified for " + bits + "-bit layer " + layer);
                        }
                    }
                }
            } else {
                if (greyScaleMapPresent) {
                    for (int mappedValue: mapping) {
                        if ((mappedValue < -1) || (mappedValue >= Terrain.VALUES.length)) {
                            throw new ScriptException("Invalid terrain index " + mappedValue + " specified");
                        }
                    }
                } else {
                    for (Map.Entry<Integer, Integer> entry: colourMapping.entrySet()) {
                        int mappedValue = entry.getValue();
                        if ((mappedValue < 0) || (mappedValue >= Terrain.VALUES.length)) {
                            throw new ScriptException("Invalid terrain index " + mappedValue + " specified");
                        }
                    }
                }
            }
        }
        final Dimension dimension = world.getDimension(dimIndex);
        if (dimension == null) {
            throw new ScriptException("Non existent dimension specified");
        }
        
        final HeightMap scaledHeightMap;
        Rectangle extent = new Rectangle(dimension.getLowestX() << TILE_SIZE_BITS, dimension.getLowestY() << TILE_SIZE_BITS, dimension.getWidth() << TILE_SIZE_BITS, dimension.getHeight() << TILE_SIZE_BITS);
        final boolean smoothScalingAllowed = greyScaleMapPresent
            && (mode != Mode.SET_TERRAIN)
            && (! Biome.INSTANCE.equals(layer))
            && (! Annotations.INSTANCE.equals(layer));
        if (heightMap != null) {
            if ((scale != 100) || (offsetX != 0) || (offsetY != 0)) {
                boolean smoothScaling = (scale != 100) && smoothScalingAllowed; // TODO ?
                scaledHeightMap = TransformingHeightMap.build().withHeightMap(heightMap).withScale(scale).withOffset(offsetX, offsetY).now();
            } else {
                scaledHeightMap = heightMap;
            }
            if (scaledHeightMap.getExtent() != null) {
                extent = extent.intersection(scaledHeightMap.getExtent());
            }
        } else {
            scaledHeightMap = null;
        }
        final int x1 = extent.x, y1 = extent.y;
        final int x2 = extent.x + extent.width, y2 = extent.y + extent.height;
        final boolean bitLayer = (layer != null) && ((layer.getDataSize() == Layer.DataSize.BIT) || (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK));
        if (filter instanceof DefaultFilter) {
            ((DefaultFilter) filter).setDimension(dimension);
        }
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                int valueOut;
                if (scaledHeightMap != null) {
                    if (colourMapPresent) {
                        int colour = scaledHeightMap.getColour(x, y);
                        if (colourMapping.containsKey(colour)) {
                            valueOut = colourMapping.get(colour);
                        } else {
                            continue;
                        }
                    } else {
                        int valueIn = (int) (scaledHeightMap.getHeight(x, y) + 0.5f);
                        if ((valueIn < 0) || (valueIn > 65535)) {
                            continue;
                        }
                        valueOut = mapping[valueIn];
                        if (valueOut == -1) {
                            continue;
                        }
                    }
                } else if (layer != null) {
                    valueOut = layerValue;
                } else {
                    valueOut = terrainIndex;
                }
                if (filter != null) {
                    float filterValue = filter.modifyStrength(x, y, 1.0f);
                    if (filterValue == 0.0f) {
                        continue;
                    } else if (smoothScalingAllowed && (filterValue != 1.0f)) {
                        valueOut = (int) (filterValue * valueOut + 0.5f);
                    }
                }
                switch (mode) {
                    case SET_TERRAIN:
                        dimension.setTerrainAt(x, y, Terrain.VALUES[valueOut]);
                        break;
                    case SET:
                        if (bitLayer) {
                            dimension.setBitLayerValueAt(layer, x, y, (valueOut != 0));
                        } else {
                            dimension.setLayerValueAt(layer, x, y, valueOut);
                        }
                        break;
                    case SET_WHEN_HIGHER:
                        if (bitLayer) {
                            if (valueOut != 0) {
                                dimension.setBitLayerValueAt(layer, x, y, true);
                            }
                        } else {
                            if (dimension.getLayerValueAt(layer, x, y) < valueOut) {
                                dimension.setLayerValueAt(layer, x, y, valueOut);
                            }
                        }
                        break;
                    case SET_WHEN_LOWER:
                        if (bitLayer) {
                            if (valueOut == 0) {
                                dimension.setBitLayerValueAt(layer, x, y, false);
                            }
                        } else {
                            if (dimension.getLayerValueAt(layer, x, y) > valueOut) {
                                dimension.setLayerValueAt(layer, x, y, valueOut);
                            }
                        }
                        break;
                }
            }
        }
        return null;
    }

    private void validateRGB(int red, int green, int blue) throws ScriptException {
        if ((red < 0) || (red > 255)) {
            throw new ScriptException("Invalid red value " + red + " specified");
        }
        if ((green < 0) || (green > 255)) {
            throw new ScriptException("Invalid green value " + green + " specified");
        }
        if ((blue < 0) || (blue > 255)) {
            throw new ScriptException("Invalid blue value " + blue + " specified");
        }
    }

    private void mapStoredValuesTo(int value) {
        if (storedColour >= 0) {
            colourMapping.put((int) storedColour, value);
            storedColour = -1L;
        } else {
            for (int i = storedLowerFrom; i <= storedUpperFrom; i++) {
                mapping[i] = value;
            }
        }
    }

    private final int[] mapping = new int[65536];
    private final Map<Integer, Integer> colourMapping = new HashMap<>();
    private HeightMap heightMap;
    private Layer layer;
    private World2 world;
    private int dimIndex, storedLowerFrom, storedUpperFrom, scale = 100, offsetX, offsetY, terrainIndex, layerValue;
    private long storedColour = -1L;
    private Mode mode = Mode.SET;
    private Filter filter;
   
    enum Mode {
        SET, SET_WHEN_LOWER, SET_WHEN_HIGHER, SET_TERRAIN
    }
}