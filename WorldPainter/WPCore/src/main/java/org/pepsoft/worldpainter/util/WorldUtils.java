package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.SuperflatGenerator;
import org.pepsoft.minecraft.SuperflatPreset;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.DIM_END;
import static org.pepsoft.worldpainter.Constants.DIM_NETHER;

/**
 * Utility methods for working with WorldPainter worlds.
 */
public final class WorldUtils {
    private WorldUtils() {
        // Prevent instantiation
    }

    /**
     * Change the build limits of a {@link World2 WorldPainter world}.
     */
    public static void resizeWorld(final World2 world, final HeightTransform transform, final int newMinHeight, final int newMaxHeight, final boolean transformLayers, final ProgressReceiver progressReceiver) throws OperationCancelled {
        world.setMaxHeight(newMaxHeight);
        final Set<Dimension> dimensions = world.getDimensions();
        final int total = dimensions.size();
        int count = 0;
        for (Dimension dimension: dimensions) {
            resizeDimension(dimension, newMinHeight, newMaxHeight, transform, transformLayers, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) count++ / total, 1f / total) : null);
        }
    }

    /**
     * Change the build limits of a {@link Dimension}.
     */
    public static void resizeDimension(final Dimension dim, final int newMinHeight, final int newMaxHeight, final HeightTransform transform, final boolean transformLayers, final ProgressReceiver progressReceiver) throws OperationCancelled {
        final int oldMinHeight = dim.getMinHeight(), oldMaxHeight = dim.getMaxHeight();
        final int dimNewMinHeight, dimNewMaxHeight;
        switch (dim.getAnchor().dim) {
            case DIM_NETHER:
                dimNewMinHeight = Math.max(newMinHeight, 0);
                dimNewMaxHeight = Math.min(newMaxHeight, DEFAULT_MAX_HEIGHT_NETHER);
                break;
            case DIM_END:
                dimNewMinHeight = Math.max(newMinHeight, 0);
                dimNewMaxHeight = Math.min(newMaxHeight, DEFAULT_MAX_HEIGHT_END);
                break;
            default:
                dimNewMinHeight = newMinHeight;
                dimNewMaxHeight = newMaxHeight;
                break;
        }
        if ((dimNewMinHeight == oldMinHeight) && (dimNewMaxHeight == oldMaxHeight) && transform.isIdentity()) {
            // Dimension heights don't need to change
            return;
        }
        dim.clearUndo();
        dim.getTiles().forEach(Tile::inhibitEvents);
        try {
            final int tileCount = dim.getTileCount();
            int tileNo = 0;
            for (Tile tile: dim.getTiles()) {
                tile.setMinMaxHeight(dimNewMinHeight, dimNewMaxHeight, transform);
                tileNo++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) tileNo / tileCount);
                }
            }
            dim.setMinHeight(dimNewMinHeight);
            dim.setMaxHeight(dimNewMaxHeight);
            dim.getTileFactory().setMinMaxHeight(dimNewMinHeight, dimNewMaxHeight, transform);
            if (dim.getAnchor().invert) {
                // Adjust ceiling height if necessary
                final int minHeightDelta = dimNewMinHeight - oldMinHeight;
                dim.setCeilingHeight(dim.getCeilingHeight() - minHeightDelta);
            } else {
                dim.setCeilingHeight(dimNewMaxHeight);
            }
            if (transformLayers) {
                for (ExporterSettings exporterSettings: dim.getAllLayerSettings().values()) {
                    exporterSettings.setMinMaxHeight(oldMinHeight, dimNewMinHeight, oldMaxHeight, dimNewMaxHeight, transform);
                }
                for (CustomLayer customLayer: dim.getCustomLayers()) {
                    customLayer.setMinMaxHeight(oldMinHeight, dimNewMinHeight, oldMaxHeight, dimNewMaxHeight, transform);
                }
                if (dim.getGenerator() instanceof SuperflatGenerator) {
                    if (dimNewMinHeight != oldMinHeight) {
                        final int raiseBy = oldMinHeight - dimNewMinHeight;
                        final SuperflatPreset settings = ((SuperflatGenerator) dim.getGenerator()).getSettings();
                        // Not sure how this can be null, but it has been observed in the wild:
                        // TODO: find out how this can happen and fix it properly
                        if (settings != null) {
                            final List<SuperflatPreset.Layer> layers = new ArrayList<>(settings.getLayers());
                            if (raiseBy > 0) {
                                // Insert deepslate to raise the Superflat terrain up. Skip the lowest layer
                                // if that is bedrock, so that bedrock remains at the bottom. If the lowest
                                // or second-lowest layer is already deepslate: expand that
                                if (layers.get(0).getMaterialName().equals(MC_BEDROCK)) {
                                    if (layers.get(1).getMaterialName().equals(MC_DEEPSLATE)) {
                                        layers.get(1).setThickness(layers.get(1).getThickness() + raiseBy);
                                    } else {
                                        layers.add(1, new SuperflatPreset.Layer(MC_DEEPSLATE, raiseBy));
                                    }
                                } else if (layers.get(0).getMaterialName().equals(MC_DEEPSLATE)) {
                                    layers.get(0).setThickness(layers.get(0).getThickness() + raiseBy);
                                } else {
                                    layers.add(0, new SuperflatPreset.Layer(MC_DEEPSLATE, raiseBy));
                                }
                            } else {
                                int lowerBy = -raiseBy;
                                // Keep reducing the thickness of layers, starting with the bottom one, until we
                                // have shaved off enough, or we run out of layers
                                for (SuperflatPreset.Layer layer: layers) {
                                    final int amount = Math.min(lowerBy, layer.getThickness() - 1);
                                    if (amount > 0) {
                                        layer.setThickness(layer.getThickness() - amount);
                                        lowerBy -= amount;
                                        if (lowerBy == 0) {
                                            break;
                                        }
                                    }
                                }
                            }
                            settings.setLayers(layers);
                        }
                    }
                }
            }
            dim.clearUndo();
            dim.armSavePoint();
            if (progressReceiver != null) {
                progressReceiver.setProgress(1f);
            }
        } finally {
            dim.getTiles().forEach(Tile::releaseEvents);
        }
    }
}