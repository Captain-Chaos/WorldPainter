/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import static org.pepsoft.worldpainter.layers.Layer.DataSize.*;

/**
 *
 * @author pepijn
 */
public final class DefaultFilter implements Filter {
    public DefaultFilter(Dimension dimension, boolean inSelection, boolean outsideSelection, int aboveLevel, int belowLevel, boolean feather, Object onlyOn, Object exceptOn, int aboveDegrees, boolean slopeIsAbove) {
        this.dimension = dimension;
        this.inSelection = inSelection;
        this.outsideSelection = outsideSelection;
        if (inSelection && outsideSelection) {
            throw new IllegalArgumentException("inSelection and outsideSelection are mutually exclusive");
        }
        this.aboveLevel = aboveLevel;
        this.belowLevel = belowLevel;
        if (aboveLevel != Integer.MIN_VALUE) {
            checkLevel = true;
            if (belowLevel != Integer.MIN_VALUE) {
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
        } else if (belowLevel != Integer.MIN_VALUE) {
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
            onlyOnValue = -1;
        } else if (onlyOn instanceof Layer) {
            this.onlyOn = true;
            if ((((Layer) onlyOn).getDataSize() == BIT) || (((Layer) onlyOn).getDataSize() == BIT_PER_CHUNK)) {
                onlyOnObjectType = ObjectType.BIT_LAYER;
            } else {
                onlyOnObjectType = ObjectType.INT_LAYER_ANY;
            }
            onlyOnTerrain = null;
            onlyOnLayer = (Layer) onlyOn;
            onlyOnValue = -1;
        } else if (onlyOn instanceof LayerValue) {
            this.onlyOn = true;
            LayerValue layerValue = (LayerValue) onlyOn;
            if (layerValue.layer instanceof Biome) {
                if (layerValue.value < 0) {
                    onlyOnObjectType = ObjectType.AUTO_BIOME;
                    onlyOnTerrain = null;
                    onlyOnLayer = null;
                    onlyOnValue = -layerValue.value;
                } else {
                    onlyOnObjectType = ObjectType.BIOME;
                    onlyOnTerrain = null;
                    onlyOnLayer = null;
                    onlyOnValue = layerValue.value;
                }
            } else if (layerValue.layer instanceof Annotations) {
                if (layerValue.condition == null) {
                    onlyOnObjectType = ObjectType.ANNOTATION_ANY;
                    onlyOnTerrain = null;
                    onlyOnLayer = null;
                    onlyOnValue = -1;
                } else {
                    onlyOnObjectType = ObjectType.ANNOTATION;
                    onlyOnTerrain = null;
                    onlyOnLayer = null;
                    onlyOnValue = layerValue.value;
                }
            } else if ((layerValue.layer.getDataSize() != BIT) && (layerValue.layer.getDataSize() != BIT_PER_CHUNK) && (layerValue.layer.getDataSize() != NONE)) {
                if (layerValue.condition == null) {
                    onlyOnObjectType = ObjectType.INT_LAYER_ANY;
                    onlyOnTerrain = null;
                    onlyOnLayer = null;
                    onlyOnValue = -1;
                } else {
                    switch (layerValue.condition) {
                        case EQUAL:
                            onlyOnObjectType = ObjectType.INT_LAYER_EQUAL;
                            break;
                        case HIGHER_THAN_OR_EQUAL:
                            onlyOnObjectType = ObjectType.INT_LAYER_EQUAL_OR_HIGHER;
                            break;
                        case LOWER_THAN_OR_EQUAL:
                            onlyOnObjectType = ObjectType.INT_LAYER_EQUAL_OR_LOWER;
                            break;
                        default:
                            throw new InternalError();
                    }
                    onlyOnTerrain = null;
                    onlyOnLayer = null;
                    onlyOnValue = layerValue.value;
                }
            } else {
                throw new IllegalArgumentException("Layer value of type " + layerValue.layer.getClass().getSimpleName() + " not supported");
            }
        } else if (WATER.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.WATER;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnValue = -1;
        } else if (LAVA.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.LAVA;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnValue = -1;
        } else if (LAND.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.LAND;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnValue = -1;
        } else if (AUTO_BIOMES.equals(onlyOn)) {
            this.onlyOn = true;
            onlyOnObjectType = ObjectType.BIOME;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnValue = 255;
        } else {
            this.onlyOn = false;
            onlyOnObjectType = null;
            onlyOnTerrain = null;
            onlyOnLayer = null;
            onlyOnValue = -1;
        }
        if (exceptOn instanceof Terrain) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.TERRAIN;
            exceptOnTerrain = (Terrain) exceptOn;
            exceptOnLayer = null;
            exceptOnValue = -1;
        } else if (exceptOn instanceof Layer) {
            this.exceptOn = true;
            if ((((Layer) exceptOn).getDataSize() == BIT) || (((Layer) exceptOn).getDataSize() == BIT_PER_CHUNK)) {
                exceptOnObjectType = ObjectType.BIT_LAYER;
            } else {
                exceptOnObjectType = ObjectType.INT_LAYER_ANY;
            }
            exceptOnTerrain = null;
            exceptOnLayer = (Layer) exceptOn;
            exceptOnValue = -1;
        } else if (exceptOn instanceof LayerValue) {
            this.exceptOn = true;
            LayerValue layerValue = (LayerValue) exceptOn;
            if (layerValue.layer instanceof Biome) {
                if (layerValue.value < 0) {
                    exceptOnObjectType = ObjectType.AUTO_BIOME;
                    exceptOnTerrain = null;
                    exceptOnLayer = null;
                    exceptOnValue = -layerValue.value;
                } else {
                    exceptOnObjectType = ObjectType.BIOME;
                    exceptOnTerrain = null;
                    exceptOnLayer = null;
                    exceptOnValue = layerValue.value;
                }
            } else if (layerValue.layer instanceof Annotations) {
                if (layerValue.condition == null) {
                    exceptOnObjectType = ObjectType.ANNOTATION_ANY;
                    exceptOnTerrain = null;
                    exceptOnLayer = null;
                    exceptOnValue = -1;
                } else {
                    exceptOnObjectType = ObjectType.ANNOTATION;
                    exceptOnTerrain = null;
                    exceptOnLayer = null;
                    exceptOnValue = layerValue.value;
                }
            } else if ((layerValue.layer.getDataSize() != BIT) && (layerValue.layer.getDataSize() != BIT_PER_CHUNK) && (layerValue.layer.getDataSize() != NONE)) {
                if (layerValue.condition == null) {
                    exceptOnObjectType = ObjectType.INT_LAYER_ANY;
                    exceptOnTerrain = null;
                    exceptOnLayer = null;
                    exceptOnValue = -1;
                } else {
                    switch (layerValue.condition) {
                        case EQUAL:
                            exceptOnObjectType = ObjectType.INT_LAYER_EQUAL;
                            break;
                        case HIGHER_THAN_OR_EQUAL:
                            exceptOnObjectType = ObjectType.INT_LAYER_EQUAL_OR_HIGHER;
                            break;
                        case LOWER_THAN_OR_EQUAL:
                            exceptOnObjectType = ObjectType.INT_LAYER_EQUAL_OR_LOWER;
                            break;
                        default:
                            throw new InternalError();
                    }
                    exceptOnTerrain = null;
                    exceptOnLayer = null;
                    exceptOnValue = layerValue.value;
                }
            } else {
                throw new IllegalArgumentException("Layer value of type " + layerValue.layer.getClass().getSimpleName() + " not supported");
            }
        } else if (WATER.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.WATER;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnValue = -1;
        } else if (LAVA.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.LAVA;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnValue = -1;
        } else if (LAND.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.LAND;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnValue = -1;
        } else if (AUTO_BIOMES.equals(exceptOn)) {
            this.exceptOn = true;
            exceptOnObjectType = ObjectType.BIOME;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnValue = 255;
        } else {
            this.exceptOn = false;
            exceptOnObjectType = null;
            exceptOnTerrain = null;
            exceptOnLayer = null;
            exceptOnValue = -1;
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

    public boolean isInSelection() {
        return inSelection;
    }

    public Layer getOnlyOnLayer() {
        return onlyOnLayer;
    }

    public Layer getExceptOnLayer() {
        return exceptOnLayer;
    }

    public static Builder buildForDimension(Dimension dimension) {
        return new Builder(dimension);
    }

    // Filter
    
    @Override
    public float modifyStrength(int x, int y, float strength) {
        if (strength > 0.0f) {
            if (inSelection && (! dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y)) && (! dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y))) {
                return 0.0f;
            } else if (outsideSelection && (dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y) || dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y))) {
                return 0.0f;
            }
            if (exceptOn) {
                switch (exceptOnObjectType) {
                    case BIOME:
                        if (dimension.getLayerValueAt(Biome.INSTANCE, x, y) == exceptOnValue) {
                            return 0.0f;
                        }
                        break;
                    case AUTO_BIOME:
                        if (dimension.getAutoBiome(x, y) == exceptOnValue) {
                            return 0.0f;
                        }
                        break;
                    case BIT_LAYER:
                        if (dimension.getBitLayerValueAt(exceptOnLayer, x, y)) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_ANY:
                        if (dimension.getLayerValueAt(exceptOnLayer, x, y) != 0) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_EQUAL:
                        if (dimension.getLayerValueAt(exceptOnLayer, x, y) != exceptOnValue) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_EQUAL_OR_HIGHER:
                        if (dimension.getLayerValueAt(exceptOnLayer, x, y) < exceptOnValue) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_EQUAL_OR_LOWER:
                        if (dimension.getLayerValueAt(exceptOnLayer, x, y) > exceptOnValue) {
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
                    case ANNOTATION_ANY:
                        if (dimension.getLayerValueAt(Annotations.INSTANCE, x, y) > 0) {
                            return 0.0f;
                        }
                        break;
                    case ANNOTATION:
                        if (dimension.getLayerValueAt(Annotations.INSTANCE, x, y) == exceptOnValue) {
                            return 0.0f;
                        }
                        break;
                }
            }
            if (onlyOn) {
                switch (onlyOnObjectType) {
                    case BIOME:
                        if (dimension.getLayerValueAt(Biome.INSTANCE, x, y) != onlyOnValue) {
                            return 0.0f;
                        }
                        break;
                    case AUTO_BIOME:
                        if ((dimension.getLayerValueAt(Biome.INSTANCE, x, y) != 255) || (dimension.getAutoBiome(x, y) != onlyOnValue)) {
                            return 0.0f;
                        }
                        break;
                    case BIT_LAYER:
                        if (!dimension.getBitLayerValueAt(onlyOnLayer, x, y)) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_ANY:
                        if (dimension.getLayerValueAt(onlyOnLayer, x, y) == 0) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_EQUAL:
                        if (dimension.getLayerValueAt(onlyOnLayer, x, y) == onlyOnValue) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_EQUAL_OR_HIGHER:
                        if (dimension.getLayerValueAt(onlyOnLayer, x, y) >= onlyOnValue) {
                            return 0.0f;
                        }
                        break;
                    case INT_LAYER_EQUAL_OR_LOWER:
                        if (dimension.getLayerValueAt(onlyOnLayer, x, y) <= onlyOnValue) {
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
                    case ANNOTATION_ANY:
                        if (dimension.getLayerValueAt(Annotations.INSTANCE, x, y) == 0) {
                            return 0.0f;
                        }
                        break;
                    case ANNOTATION:
                        if (dimension.getLayerValueAt(Annotations.INSTANCE, x, y) != onlyOnValue) {
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

    // Object

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DefaultFilter{");
        if (inSelection) {
            sb.append("in selection");
        } else if (outsideSelection) {
            sb.append("outside selection");
        }
        if (onlyOn) {
            if (inSelection || outsideSelection) {
                sb.append(" and ");
            }
            sb.append("only on ");
            append(sb, onlyOnObjectType, onlyOnValue, onlyOnLayer, onlyOnTerrain);
        }
        if (exceptOn) {
            if (inSelection || outsideSelection || onlyOn) {
                sb.append(' ');
            }
            sb.append("except on ");
            append(sb, exceptOnObjectType, exceptOnValue, exceptOnLayer, exceptOnTerrain);
        }
        if (checkLevel) {
            if (inSelection || outsideSelection || onlyOn || exceptOn) {
                sb.append(" and ");
            }
            sb.append("height ");
            switch (levelType) {
                case ABOVE:
                    sb.append("at or above ").append(aboveLevel);
                    break;
                case BELOW:
                    sb.append("at or below ").append(belowLevel);
                    break;
                case BETWEEN:
                    sb.append("between ").append(aboveLevel).append(" and ").append(belowLevel);
                    break;
                case OUTSIDE:
                    sb.append("not between ").append(aboveLevel).append(" and ").append(belowLevel);
                    break;
            }
        }
        if (checkSlope) {
            if (inSelection || outsideSelection || onlyOn || exceptOn || checkLevel) {
                sb.append(" and ");
            }
            sb.append("gradient ").append(slopeIsAbove ? "above " : "below ").append(slope);
        }
        sb.append('}');
        return sb.toString();
    }

    private void append(StringBuilder sb, ObjectType objectType, int value, Layer layer, Terrain terrain) {
        switch (objectType) {
            case BIOME:
                sb.append("biome ").append(value);
                break;
            case AUTO_BIOME:
                sb.append("auto biome ").append(value);
                break;
            case BIT_LAYER:
            case INT_LAYER_ANY:
                sb.append("layer ").append(layer.getName().toLowerCase());
                break;
            case INT_LAYER_EQUAL:
                sb.append("layer ").append(layer).append(" is ").append(value);
                break;
            case INT_LAYER_EQUAL_OR_HIGHER:
                sb.append("layer ").append(layer).append(" >= ").append(value);
                break;
            case INT_LAYER_EQUAL_OR_LOWER:
                sb.append("layer ").append(layer).append(" <= ").append(value);
                break;
            case TERRAIN:
                sb.append("terrain ").append(terrain.name().toLowerCase());
                break;
            case WATER:
                sb.append("water");
                break;
            case LAVA:
                sb.append("lava");
                break;
            case LAND:
                sb.append("land");
                break;
            case ANNOTATION_ANY:
                sb.append("annotations");
                break;
            case ANNOTATION:
                sb.append(Annotations.getColourName(value).toLowerCase()).append(" annotations");
                break;
        }
    }

    final boolean checkLevel, onlyOn, exceptOn, feather, checkSlope,
            slopeIsAbove, inSelection, outsideSelection;
    final LevelType levelType;
    final ObjectType onlyOnObjectType, exceptOnObjectType;
    final int aboveLevel, belowLevel, onlyOnValue, exceptOnValue, degrees;
    final Terrain onlyOnTerrain, exceptOnTerrain;
    final Layer onlyOnLayer, exceptOnLayer;
    final float slope;
    Dimension dimension;

    public static final String LAND = "Land";
    public static final String WATER = "Water";
    public static final String LAVA = "Lava";
    public static final String AUTO_BIOMES = "Automatic Biomes";

    public enum LevelType {
        BETWEEN, OUTSIDE, ABOVE, BELOW
    }

    public enum ObjectType {
        TERRAIN, BIT_LAYER, INT_LAYER_ANY, INT_LAYER_EQUAL, INT_LAYER_EQUAL_OR_HIGHER, INT_LAYER_EQUAL_OR_LOWER, BIOME, WATER, LAND, LAVA, AUTO_BIOME, ANNOTATION_ANY, ANNOTATION
    }

    public enum Condition {
        EQUAL, LOWER_THAN_OR_EQUAL, HIGHER_THAN_OR_EQUAL
    }

    public static class LayerValue {
        public LayerValue(Layer layer) {
            this.layer = layer;
            value = -1;
            condition = null;
        }

        public LayerValue(Layer layer, int value) {
            this(layer, value, Condition.EQUAL);
        }

        public LayerValue(Layer layer, int value, Condition condition) {
            switch (layer.getDataSize()) {
                case BIT_PER_CHUNK:
                case BIT:
                    if ((value < -1) || (value > 1)) {
                        throw new IllegalArgumentException("value " + value);
                    }
                    break;
                case NIBBLE:
                    if ((value < -15) || (value > 15)) {
                        throw new IllegalArgumentException("value " + value);
                    }
                    break;
                case BYTE:
                    if ((value < -255) || (value > 255)) {
                        throw new IllegalArgumentException("value " + value);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Data size " + layer.getDataSize() + " not supported");
            }
            this.layer = layer;
            this.value = value;
            this.condition = condition;
        }

        public final Layer layer;
        public final int value;
        public final Condition condition;
    }

    public static class Builder {
        public Builder(Dimension dimension) {
            this.dimension = dimension;
        }

        public Builder inSelection() {
            inSelection = true;
            return this;
        }

        public Builder outsideSelection() {
            outsideSelection = true;
            return this;
        }

        public Builder aboveLevel(int level) {
            aboveLevel = level;
            return this;
        }

        public Builder belowLevel(int level) {
            belowLevel = level;
            return this;
        }

        public Builder betweenLevels(int aboveLevel, int belowLevel) {
            this.aboveLevel = aboveLevel;
            this.belowLevel = belowLevel;
            return this;
        }

        public Builder feather() {
            feather = true;
            return this;
        }

        public Builder onlyOn(Object item) {
            onlyOn = item;
            return this;
        }

        public Builder exceptOn(Object item) {
            exceptOn = item;
            return this;
        }

        public Builder slopeIsAbove(int degrees) {
            aboveDegrees = degrees;
            slopeIsAbove = true;
            return this;
        }

        public Builder slopeIsBelow(int degrees) {
            aboveDegrees = degrees;
            slopeIsAbove = false;
            return this;
        }

        public DefaultFilter build() {
            return new DefaultFilter(dimension, inSelection, outsideSelection, aboveLevel, belowLevel, feather, onlyOn, exceptOn, aboveDegrees, slopeIsAbove);
        }

        private final Dimension dimension;
        private boolean inSelection, outsideSelection, feather, slopeIsAbove;
        private int aboveLevel, belowLevel, aboveDegrees;
        private Object onlyOn, exceptOn;
    }
}