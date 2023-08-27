package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;

public class OnlyOnTerrainOrLayerFilter extends TerrainOrLayerFilter {
    public OnlyOnTerrainOrLayerFilter(Dimension dimension, Object item) {
        super(dimension, item);
    }

    @Override
    public float modifyStrength(int x, int y, float strength) {
        switch (objectType) {
            case BIOME:
                if (dimension.getLayerValueAt(Biome.INSTANCE, x, y) != value) {
                    return 0.0f;
                }
                break;
            case AUTO_BIOME:
                if ((dimension.getLayerValueAt(Biome.INSTANCE, x, y) != 255) || (dimension.getAutoBiome(x, y) != value)) {
                    return 0.0f;
                }
                break;
            case BIT_LAYER:
                if (!dimension.getBitLayerValueAt(layer, x, y)) {
                    return 0.0f;
                }
                break;
            case INT_LAYER_ANY:
                if (dimension.getLayerValueAt(layer, x, y) == 0) {
                    return 0.0f;
                }
                break;
            case INT_LAYER_EQUAL:
                if (dimension.getLayerValueAt(layer, x, y) == value) {
                    return 0.0f;
                }
                break;
            case INT_LAYER_EQUAL_OR_HIGHER:
                if (dimension.getLayerValueAt(layer, x, y) >= value) {
                    return 0.0f;
                }
                break;
            case INT_LAYER_EQUAL_OR_LOWER:
                if (dimension.getLayerValueAt(layer, x, y) <= value) {
                    return 0.0f;
                }
                break;
            case TERRAIN:
                if (dimension.getTerrainAt(x, y) != terrain) {
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
                if (dimension.getLayerValueAt(Annotations.INSTANCE, x, y) != value) {
                    return 0.0f;
                }
                break;
        }
        return strength;
    }
}