/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Layer.DataSize;
import org.pepsoft.worldpainter.operations.Filter;

/**
 *
 * @author pepijn
 */
public final class FilterImpl implements Filter {
    public FilterImpl(Dimension dimension, int aboveLevel, int belowLevel, boolean feather, Object onlyOn, Object exceptOn, int aboveDegrees, boolean slopeIsAbove) {
        this.dimension = dimension;
        this.aboveLevel = aboveLevel;
        this.belowLevel = belowLevel;
        if (aboveLevel >= 0) {
            checkLevel = true;
            if (belowLevel >= 0) {
                // Above and below are checked
                if (belowLevel >= aboveLevel) {
                    levelType = LevelType.BETWEEN;
                } else {
                    levelType = LevelType.OUTSIDE;
                }
            } else {
                // Only above checked
                levelType = LevelType.ABOVE;
            }
        } else if (belowLevel >= 0) {
            // Only below checked
            checkLevel = true;
            levelType = LevelType.BELOW;
        } else {
            // Neither checked
            checkLevel = false;
            levelType = null;
        }
        this.feather = feather;
        if (onlyOn instanceof Terrain) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.TERRAIN;
            onlyOnTerrain = (Terrain) onlyOn;
            onlyOnLayer = null;
            onlyOnBiome = -1;
        } else if (onlyOn instanceof Layer) {
            this.onlyOn = true;
            if ((((Layer) onlyOn).getDataSize() == DataSize.BIT) || (((Layer) onlyOn).getDataSize() == DataSize.BIT_PER_CHUNK)) {
                onlyOnObjectType = ObjectType.BIT_LAYER;
            } else {
                onlyOnObjectType = ObjectType.INT_LAYER;
            }
            onlyOnTerrain = null;
            onlyOnLayer = (Layer) onlyOn;
            onlyOnBiome = -1;
        } else if (onlyOn instanceof Integer) {
            this.onlyOn = true;
            if ((Integer) onlyOn < 0) {
                onlyOnObjectType = ObjectType.AUTO_BIOME;
                onlyOnTerrain = null;
                onlyOnLayer = null;
                onlyOnBiome = -(Integer) onlyOn;
            } else {
                onlyOnObjectType = ObjectType.BIOME;
                onlyOnTerrain = null;
                onlyOnLayer = null;
                onlyOnBiome = (Integer) onlyOn;
            }
        } else if (BrushOptions.WATER.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.WATER;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnBiome = -1;
        } else if (BrushOptions.LAVA.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.LAVA;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnBiome = -1;
        } else if (BrushOptions.LAND.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.LAND;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnBiome = -1;
        } else if (BrushOptions.AUTO_BIOMES.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.BIOME;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnBiome = 255;
        } else {
            this.onlyOn = false;
            onlyOnObjectType = null;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnBiome = -1;
        }
        if (exceptOn instanceof Terrain) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.TERRAIN;
            exceptOnTerrain = (Terrain) exceptOn;
            exceptOnLayer = null;
            exceptOnBiome = -1;
        } else if (exceptOn instanceof Layer) {
            this.exceptOn = true;
            if ((((Layer) exceptOn).getDataSize() == DataSize.BIT) || (((Layer) exceptOn).getDataSize() == DataSize.BIT_PER_CHUNK)) {
                exceptOnObjectType = ObjectType.BIT_LAYER;
            } else {
                exceptOnObjectType = ObjectType.INT_LAYER;
            }
            exceptOnTerrain = null;
            exceptOnLayer = (Layer) exceptOn;
            exceptOnBiome = -1;
        } else if (exceptOn instanceof Integer) {
            this.exceptOn = true;
            if ((Integer) exceptOn < 0) {
                exceptOnObjectType = ObjectType.AUTO_BIOME;
                exceptOnTerrain = null;
                exceptOnLayer = null;
                exceptOnBiome = -(Integer) exceptOn;
            } else {
                exceptOnObjectType = ObjectType.BIOME;
                exceptOnTerrain = null;
                exceptOnLayer = null;
                exceptOnBiome = (Integer) exceptOn;
            }
        } else if (BrushOptions.WATER.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.WATER;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnBiome = -1;
        } else if (BrushOptions.LAVA.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.LAVA;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnBiome = -1;
        } else if (BrushOptions.LAND.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.LAND;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnBiome = -1;
        } else if (BrushOptions.AUTO_BIOMES.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.BIOME;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnBiome = 255;
        } else {
            this.exceptOn = false;
            exceptOnObjectType = null;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnBiome = -1;
        }
        this.degrees = aboveDegrees;
        checkSlope = aboveDegrees >= 0;
        if (checkSlope) {
            this.slope = (float) Math.tan(aboveDegrees / (180 / Math.PI));
    //        System.out.println(degrees + "° -> " + slope);
        } else {
            slope = 0.0f;
        }
        this.slopeIsAbove = slopeIsAbove;
    }

    public int getAboveLevel() {
        return aboveLevel;
    }

    public int getBelowLevel() {
        return belowLevel;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    // Filter
    
    @Override
    public float modifyStrength(int x, int y, float strength) {
        if (strength > 0.0f) {
            if (exceptOn) {
                switch (exceptOnObjectType) {
                    case BIOME:
                        if (dimension.getLayerValueAt(Biome.INSTANCE, x, y) == exceptOnBiome) {
                            return 0.0f;
                        }
                        break;
                    case AUTO_BIOME:
                        if (dimension.getAutoBiome(x, y) == exceptOnBiome) {
                            return 0.0f;
                        }
                        break;
                    case BIT_LAYER:
                        if (dimension.getBitLayerValueAt(exceptOnLayer, x, y)) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER:
                        if (dimension.getLayerValueAt(exceptOnLayer, x, y) != 0) {
                            return 0.0f;
                        }
                        break;
                    case TERRAIN:
                        if (dimension.getTerrainAt(x, y) == exceptOnTerrain) {
                            return 0.0f;
                        }
                        break;
                    case WATER:
                        if ((dimension.getWaterLevelAt(x, y) > dimension.getIntHeightAt(x, y)) && (! dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y))) {
                            return 0.0f;
                        }
                        break;
                    case LAVA:
                        if ((dimension.getWaterLevelAt(x, y) > dimension.getIntHeightAt(x, y)) && dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y)) {
                            return 0.0f;
                        }
                        break;
                    case LAND:
                        if (dimension.getWaterLevelAt(x, y) <= dimension.getIntHeightAt(x, y)) {
                            return 0.0f;
                        }
                        break;
                }
            }
            if (onlyOn) {
                switch (onlyOnObjectType) {
                    case BIOME:
                        if (dimension.getLayerValueAt(Biome.INSTANCE, x, y) != onlyOnBiome) {
                            return 0.0f;
                        }
                        break;
                    case AUTO_BIOME:
                        if ((dimension.getLayerValueAt(Biome.INSTANCE, x, y) != 255) || (dimension.getAutoBiome(x, y) != onlyOnBiome)) {
                            return 0.0f;
                        }
                        break;
                    case BIT_LAYER:
                        if (!dimension.getBitLayerValueAt(onlyOnLayer, x, y)) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER:
                        if (dimension.getLayerValueAt(onlyOnLayer, x, y) == 0) {
                            return 0.0f;
                        }
                        break;
                    case TERRAIN:
                        if (dimension.getTerrainAt(x, y) != onlyOnTerrain) {
                            return 0.0f;
                        }
                        break;
                    case WATER:
                        if ((dimension.getWaterLevelAt(x, y) <= dimension.getIntHeightAt(x, y)) || dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y)) {
                            return 0.0f;
                        }
                        break;
                    case LAVA:
                        if ((dimension.getWaterLevelAt(x, y) <= dimension.getIntHeightAt(x, y)) || (! dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y))) {
                            return 0.0f;
                        }
                        break;
                    case LAND:
                        if (dimension.getWaterLevelAt(x, y) > dimension.getIntHeightAt(x, y)) {
                            return 0.0f;
                        }
                        break;
                }
            }
            if (checkLevel) {
                final int terrainLevel = dimension.getIntHeightAt(x, y);
                switch (levelType) {
                    case ABOVE:
                        if (terrainLevel < aboveLevel) {
                            return feather ? Math.max((1 - (aboveLevel - terrainLevel) / 4.0f) * strength, 0.0f) : 0.0f;
                        }
                        break;
                    case BELOW:
                        if (terrainLevel > belowLevel) {
                            return feather ? Math.max((1 - (terrainLevel - belowLevel) / 4.0f) * strength, 0.0f) : 0.0f;
                        }
                        break;
                    case BETWEEN:
                        if ((terrainLevel < aboveLevel) || (terrainLevel > belowLevel)) {
                            return feather ? Math.max(Math.min((1 - (aboveLevel - terrainLevel) / 4.0f), (1 - (terrainLevel - belowLevel) / 4.0f)) * strength, 0.0f) : 0.0f;
                        }
                        break;
                    case OUTSIDE:
                        if ((terrainLevel > belowLevel) && (terrainLevel < aboveLevel)) {
                            return feather ? Math.max(Math.max((1 - (terrainLevel - belowLevel) / 4.0f), (1 - (aboveLevel - terrainLevel) / 4.0f)) * strength, 0.0f) : 0.0f;
                        }
                        break;
                }
            }
            if (checkSlope) {
                float terrainSlope = dimension.getSlope(x, y);
                if (slopeIsAbove ? (terrainSlope < slope) : (terrainSlope > slope)) {
                    return 0.0f;
                }
            }
            return strength;
        } else {
            return 0.0f;
        }
    }
    
    final boolean checkLevel;
    final boolean onlyOn, exceptOn;
    final boolean feather;
    final LevelType levelType;
    final ObjectType onlyOnObjectType, exceptOnObjectType;
    final int aboveLevel;
    final int belowLevel;
    final int onlyOnBiome, exceptOnBiome;
    final Terrain onlyOnTerrain, exceptOnTerrain;
    final Layer onlyOnLayer, exceptOnLayer;
    final float slope;
    final boolean checkSlope;
    final boolean slopeIsAbove;
    final int degrees;
    Dimension dimension;

    public enum LevelType {
        BETWEEN, OUTSIDE, ABOVE, BELOW
    }

    public enum ObjectType {
        TERRAIN, BIT_LAYER, INT_LAYER, BIOME, WATER, LAND, LAVA, AUTO_BIOME
    }
}