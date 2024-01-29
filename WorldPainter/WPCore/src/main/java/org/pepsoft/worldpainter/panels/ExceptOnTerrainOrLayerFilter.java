package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.operations.Filter;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class ExceptOnTerrainOrLayerFilter extends TerrainOrLayerFilter {
    private ExceptOnTerrainOrLayerFilter(final Dimension dimension, final Object item) {
        super(dimension, item);
    }

    @Override
    public float modifyStrength(int x, int y, float strength) {
        switch (objectType) {
            case BIOME:
                if (dimension.getLayerValueAt(Biome.INSTANCE, x, y) == value) {
                    return 0.0f;
                }
                break;
            case AUTO_BIOME:
                if (dimension.getAutoBiome(x, y) == value) {
                    return 0.0f;
                }
                break;
            case BIT_LAYER:
                if (dimension.getBitLayerValueAt(layer, x, y)) {
                    return 0.0f;
                }
                break;
            case INT_LAYER_ANY:
                if (dimension.getLayerValueAt(layer, x, y) != 0) {
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
                if (dimension.getTerrainAt(x, y) == terrain) {
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
                if (dimension.getLayerValueAt(Annotations.INSTANCE, x, y) == value) {
                    return 0.0f;
                }
                break;
        }
        return strength;
    }

    @SuppressWarnings("unchecked") // Guaranteed by code
    public static Filter create(Dimension dimension, Object item) {
        if (item instanceof List) {
            return new CombinedFilter(((List<Object>) item).stream()
                    .map(object -> ExceptOnTerrainOrLayerFilter.create(dimension, object))
                    .collect(toList()));
        } else {
            return new ExceptOnTerrainOrLayerFilter(dimension, item);
        }
    }
}