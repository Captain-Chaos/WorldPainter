package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;

import static java.util.Objects.requireNonNull;
import static org.pepsoft.worldpainter.layers.Layer.DataSize.*;
import static org.pepsoft.worldpainter.panels.DefaultFilter.LayerValue;

public abstract class TerrainOrLayerFilter implements Filter {
    protected TerrainOrLayerFilter(final Dimension dimension, final Object item) {
        requireNonNull(dimension, "dimension");
        requireNonNull(item, "onlyOn");
        this.dimension = dimension;
        if (item instanceof Terrain) {
            objectType = ObjectType.TERRAIN;
            terrain = (Terrain) item;
            layer = null;
            value = -1;
        } else if (item instanceof Layer) {
            if ((((Layer) item).getDataSize() == BIT) || (((Layer) item).getDataSize() == BIT_PER_CHUNK)) {
                objectType = ObjectType.BIT_LAYER;
            } else {
                objectType = ObjectType.INT_LAYER_ANY;
            }
            terrain = null;
            layer = (Layer) item;
            value = -1;
        } else if (item instanceof LayerValue) {
            LayerValue layerValue = (LayerValue) item;
            if (layerValue.layer instanceof Biome) {
                if (layerValue.value < 0) {
                    objectType = ObjectType.AUTO_BIOME;
                    terrain = null;
                    layer = null;
                    value = -layerValue.value;
                } else {
                    objectType = ObjectType.BIOME;
                    terrain = null;
                    layer = null;
                    value = layerValue.value;
                }
            } else if (layerValue.layer instanceof Annotations) {
                if (layerValue.condition == null) {
                    objectType = ObjectType.ANNOTATION_ANY;
                    terrain = null;
                    layer = null;
                    value = -1;
                } else {
                    objectType = ObjectType.ANNOTATION;
                    terrain = null;
                    layer = null;
                    value = layerValue.value;
                }
            } else if ((layerValue.layer.getDataSize() != BIT) && (layerValue.layer.getDataSize() != BIT_PER_CHUNK) && (layerValue.layer.getDataSize() != NONE)) {
                if (layerValue.condition == null) {
                    objectType = ObjectType.INT_LAYER_ANY;
                    terrain = null;
                    layer = layerValue.layer;
                    value = -1;
                } else {
                    switch (layerValue.condition) {
                        case EQUAL:
                            objectType = ObjectType.INT_LAYER_EQUAL;
                            break;
                        case HIGHER_THAN_OR_EQUAL:
                            objectType = ObjectType.INT_LAYER_EQUAL_OR_HIGHER;
                            break;
                        case LOWER_THAN_OR_EQUAL:
                            objectType = ObjectType.INT_LAYER_EQUAL_OR_LOWER;
                            break;
                        default:
                            throw new InternalError();
                    }
                    terrain = null;
                    layer = layerValue.layer;
                    value = layerValue.value;
                }
            } else {
                throw new IllegalArgumentException("Layer value of type " + layerValue.layer.getClass().getSimpleName() + " not supported");
            }
        } else if (WATER.equals(item)) {
            objectType = ObjectType.WATER;
            terrain = null;
            layer = null;
            value = -1;
        } else if (LAVA.equals(item)) {
            objectType = ObjectType.LAVA;
            terrain = null;
            layer = null;
            value = -1;
        } else if (LAND.equals(item)) {
            objectType = ObjectType.LAND;
            terrain = null;
            layer = null;
            value = -1;
        } else if (AUTO_BIOMES.equals(item)) {
            objectType = ObjectType.BIOME;
            terrain = null;
            layer = null;
            value = 255;
        } else {
            throw new IllegalArgumentException("Don't know how to filter on " + item + " (type: " + item.getClass() + ")");
        }
    }

    public Dimension getDimension() {
        return dimension;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public Layer getLayer() {
        return layer;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

    final Dimension dimension;
    final ObjectType objectType;
    final Terrain terrain;
    final Layer layer;
    final int value;

    public static final String LAND = "Land";
    public static final String WATER = "Water";
    public static final String LAVA = "Lava";
    public static final String AUTO_BIOMES = "Automatic Biomes";

    public enum ObjectType {
        TERRAIN, BIT_LAYER, INT_LAYER_ANY, INT_LAYER_EQUAL, INT_LAYER_EQUAL_OR_HIGHER, INT_LAYER_EQUAL_OR_LOWER, BIOME, WATER, LAND, LAVA, AUTO_BIOME, ANNOTATION_ANY, ANNOTATION
    }
}