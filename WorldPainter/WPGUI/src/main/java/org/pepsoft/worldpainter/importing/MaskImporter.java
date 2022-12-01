/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.importing.Mapping.*;
import static org.pepsoft.worldpainter.importing.MaskImporter.InputType.SIXTEEN_BIT_GREY_SCALE;
import static org.pepsoft.worldpainter.layers.Layer.DataSize.BIT;

/**
 *
 * @author pepijn
 */
public class MaskImporter {
    public MaskImporter(Dimension dimension, File imageFile) throws IOException {
        this(dimension, imageFile, ImageIO.read(imageFile));
    }

    public MaskImporter(Dimension dimension, File imageFile, BufferedImage image) {
        this.dimension = dimension;
        this.imageFile = imageFile;
        this.image = image;
        int sampleSize = image.getSampleModel().getSampleSize(0);
        if (sampleSize == 1) {
            inputType = InputType.ONE_BIT_GREY_SCALE;
            imageMaxValue = 1;
        } else if (image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
            if (sampleSize == 8) {
                inputType = InputType.EIGHT_BIT_GREY_SCALE;
                imageMaxValue = 255;
            } else if (sampleSize == 16) {
                inputType = InputType.SIXTEEN_BIT_GREY_SCALE;
                imageMaxValue = 65535;
            } else {
                inputType = InputType.UNSUPPORTED;
                imageMaxValue = -1;
            }
        } else {
            inputType = InputType.COLOUR;
            imageMaxValue = 0xffffffff;
        }

        switch (inputType) {
            case ONE_BIT_GREY_SCALE:
                imageLowValue = 0;
                imageHighValue = 1;
                break;
            case EIGHT_BIT_GREY_SCALE:
            case SIXTEEN_BIT_GREY_SCALE:
                final int width = image.getWidth(), height = image.getHeight();
                final Raster raster = image.getRaster();
                int imageLowValue = Integer.MAX_VALUE, imageHighValue = Integer.MIN_VALUE;
outer:          for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int value = raster.getSample(x, y, 0);
                        if (value < imageLowValue) {
                            imageLowValue = value;
                        }
                        if (value > imageHighValue) {
                            imageHighValue = value;
                        }
                        if ((imageLowValue == 0) && ((inputType == SIXTEEN_BIT_GREY_SCALE) ? (imageHighValue == 65535) : (imageHighValue == 255))) {
                            // Lowest and highest possible values found; no
                            // point in looking any further!
                            break outer;
                        }
                    }
                }
                this.imageLowValue = imageLowValue;
                this.imageHighValue = imageHighValue;
                break;
            default:
                this.imageLowValue = -1;
                this.imageHighValue = -1;
                break;
        }
    }

    public void doImport(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if ((! applyToTerrain) && (applyToLayer == null)) {
            throw new IllegalStateException("Target not set");
        }
        if (mapping == null) {
            throw new IllegalStateException("Mapping not set");
        }

        // Set up mapping
        mapping.setThreshold(threshold);
        mapping.setMaskLowValue(imageLowValue);
        mapping.setMaskHighValue(imageHighValue);
        mapping.setMaskMaxValue(imageMaxValue);

        // Scale the mask, if necessary
        BufferedImage scaledImage;
        if (scale == 100) {
            // No scaling necessary
            scaledImage = image;
        } else {
            final int newWidth = image.getWidth() * scale / 100, newHeight = image.getHeight() * scale / 100;
            if (image.getColorModel() instanceof IndexColorModel) {
                scaledImage = new BufferedImage(newWidth, newHeight, image.getType(), (IndexColorModel) image.getColorModel());
            } else {
                scaledImage = new BufferedImage(newWidth, newHeight, image.getType());
            }
            Graphics2D g2 = scaledImage.createGraphics();
            try {
                if (mapping instanceof RangedMapping) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                }
                g2.drawImage(image, 0, 0, newWidth, newHeight, null);
            } finally {
                g2.dispose();
            }
        }
        image = null; // The original image is no longer necessary, so allow it to be garbage collected to make more space available for the import
        // Dither the mask, if necessary
        if ((mapping instanceof ColourToAnnotationsMapping) && ((ColourToAnnotationsMapping) mapping).dithered) {
            scaledImage = Mapping.ditherMask(scaledImage);
        }

        if (dimension.getWorld() != null) {
            dimension.getWorld().addHistoryEntry(HistoryEntry.WORLD_MASK_IMPORTED_TO_DIMENSION, dimension.getName(), imageFile, mapping.getAspect());
        }

        // Apply the mask tile by tile
        final int width = scaledImage.getWidth(), height = scaledImage.getHeight();
        final int tileX1 = xOffset >> TILE_SIZE_BITS, tileX2 = (xOffset + width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = yOffset >> TILE_SIZE_BITS, tileY2 = (yOffset + height- 1) >> TILE_SIZE_BITS;
        final int noOfTiles = (tileX2 - tileX1 + 1) * (tileY2 - tileY1 + 1);
        int tileCount = 0;
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                final Tile tile = dimension.getTile(tileX, tileY);
                if (tile == null) {
                    tileCount++;
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) tileCount / noOfTiles);
                    }
                    continue;
                }
                tile.inhibitEvents();
                try {
                    // First remove the existing layer, if requested
                    final int tileOffsetX = (tileX << TILE_SIZE_BITS) - xOffset, tileOffsetY = (tileY << TILE_SIZE_BITS) - yOffset;
                    if ((applyToLayer != null) && removeExistingLayer) {
                        // Crude heuristic to decide whether a tile lies entirely inside the area covered by the mask:
                        if ((tileX > tileX1) && (tileX < tileX2) && (tileY > tileY1) && (tileY < tileY2)) {
                            tile.clearLayerData(applyToLayer);
                        } else {
                            if (applyToLayer.dataSize.maxValue == 1) {
                                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                                    for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                        final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                        if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                            tile.setBitLayerValue(applyToLayer, xInTile, yInTile, false);
                                        }
                                    }
                                }
                            } else {
                                final int defaultValue = applyToLayer.getDefaultValue();
                                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                                    for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                        final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                        if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                            tile.setLayerValue(applyToLayer, xInTile, yInTile, defaultValue);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    final Raster raster = scaledImage.getRaster();
                    mapping.setTile(tile);
                    if (inputType == InputType.COLOUR) {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    mapping.apply(xInTile, yInTile, scaledImage.getRGB(imageX, imageY));
                                }
                            }
                        }
                    } else {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    mapping.apply(xInTile, yInTile, raster.getSample(imageX, imageY, 0));
                                }
                            }
                        }
                    }
                } finally {
                    tile.releaseEvents();
                }
                tileCount++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) tileCount / noOfTiles);
                }
            }
        }
    }

    public InputType getInputType() {
        return inputType;
    }

    public boolean isSupported() {
        return inputType != InputType.UNSUPPORTED;
    }

    public Layer getApplyToLayer() {
        return applyToLayer;
    }

    public void setApplyToLayer(Layer applyToLayer) {
        this.applyToLayer = applyToLayer;
        if (applyToLayer != null) {
            applyToTerrain = false;
        }
    }

    public Integer getApplyToLayerValue() {
        return applyToLayerValue;
    }

    public void setApplyToLayerValue(Integer applyToLayerValue) {
        this.applyToLayerValue = applyToLayerValue;
    }

    public boolean isApplyToTerrain() {
        return applyToTerrain;
    }

    public void setApplyToTerrain(boolean applyToTerrain) {
        this.applyToTerrain = applyToTerrain;
        if (applyToTerrain) {
            applyToLayer = null;
        }
    }

    public static class PossibleMappingsResult {
        PossibleMappingsResult(Mapping... mappings) {
            if ((mappings == null) || (mappings.length == 0)) {
                throw new IllegalArgumentException("mappings");
            }
            this.mappings = ImmutableSet.copyOf(mappings);
            reason = null;
        }

        PossibleMappingsResult(String reason) {
            if ((reason == null) || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("reason");
            }
            this.reason = reason;
            mappings = emptySet();
        }

        public final Set<Mapping> mappings;
        public final String reason;
    }

    /**
     * @return The types of mapping possible for the specified target (terrain or layer).
     */
    public PossibleMappingsResult getPossibleMappings() {
        switch (inputType) {
            case ONE_BIT_GREY_SCALE:
                if (applyToTerrain) {
                    if (applyToTerrainType != null) {
                        // One-bit mask to one terrain type
                        return new PossibleMappingsResult(setTerrainValue(applyToTerrainType));
                    } else {
                        // One-bit mask to terrain
                        return new PossibleMappingsResult("Pick one terrain type to apply one-bit mask to");
                    }
                } else if (applyToLayer != null) {
                    if (applyToLayer.discrete) {
                        // One-bit mask to discrete layer
                        if (applyToLayerValue != null) {
                            // Value selected
                            return new PossibleMappingsResult(setLayerValue(applyToLayer, applyToLayerValue));
                        } else {
                            // No value selected
                            return new PossibleMappingsResult("Pick one discrete layer value to apply one-bit mask to");
                        }
                    } else if (applyToLayer.getDataSize().maxValue == 1) {
                        // One-bit mask to one-bit layer
                        return new PossibleMappingsResult(setLayerValue(applyToLayer, 1));
                    } else {
                        // One-bit mask to continuous layer
                        if (applyToLayerValue != null) {
                            // Value selected
                            return new PossibleMappingsResult(setLayerValue(applyToLayer, applyToLayerValue));
                        } else {
                            // No value selected
                            return new PossibleMappingsResult("Pick one layer intensity to apply one-bit mask to");
                        }
                    }
                }
                break;
            case EIGHT_BIT_GREY_SCALE:
            case SIXTEEN_BIT_GREY_SCALE:
                if (applyToTerrain) {
                    if (applyToTerrainType != null) {
                        // Continuous greyscale mask to one terrain type
                        return new PossibleMappingsResult(setTerrainValue(applyToTerrainType).ditheredActualRange(), setTerrainValue(applyToTerrainType).ditheredFullRange(), setTerrainValue(applyToTerrainType).threshold());
                    } else {
                        // Continuous greyscale mask to terrain
                        if (imageHighValue < Terrain.VALUES.length) {
                            return new PossibleMappingsResult(mapToTerrain());
                        } else {
                            return new PossibleMappingsResult("Mask contains values higher than the highest terrain type index (" + (Terrain.VALUES.length - 1) + ")");
                        }
                    }
                } else if (applyToLayer != null) {
                    if (applyToLayer.discrete) {
                        // Continuous greyscale mask to discrete layer
                        if (applyToLayerValue != null) {
                            // Value selected
                            return new PossibleMappingsResult(setLayerValue(applyToLayer, applyToLayerValue).ditheredActualRange(), setLayerValue(applyToLayer, applyToLayerValue).ditheredFullRange(), setLayerValue(applyToLayer, applyToLayerValue).threshold());
                        } else {
                            // No value selected
                            if (imageHighValue <= applyToLayer.dataSize.maxValue) {
                                return new PossibleMappingsResult(mapToLayer(applyToLayer));
                            } else {
                                return new PossibleMappingsResult("Mask contains values higher than the highest layer value (" + applyToLayer.dataSize.maxValue + ")");
                            }
                        }
                    } else if (applyToLayer.getDataSize().maxValue == 1) {
                        // Continuous greyscale mask to one-bit layer
                        if (applyToLayer.getDataSize() == BIT) {
                            return new PossibleMappingsResult(mapToLayer(applyToLayer).ditheredActualRange(), mapToLayer(applyToLayer).ditheredFullRange(), mapToLayer(applyToLayer).threshold());
                        } else {
                            return new PossibleMappingsResult(mapToLayer(applyToLayer).threshold());
                        }
                    } else {
                        // Continuous greyscale mask to continuous layer
                        if (applyToLayerValue != null) {
                            // Value selected
                            return new PossibleMappingsResult(setLayerValue(applyToLayer, applyToLayerValue).ditheredActualRange(), setLayerValue(applyToLayer, applyToLayerValue).ditheredFullRange(), setLayerValue(applyToLayer, applyToLayerValue).threshold());
                        } else {
                            // No value selected
                            if (imageHighValue <= applyToLayer.dataSize.maxValue) {
                                return new PossibleMappingsResult(mapActualRangeToLayer(applyToLayer), mapFullRangeToLayer(applyToLayer), mapToLayer(applyToLayer));
                            } else {
                                return new PossibleMappingsResult(mapActualRangeToLayer(applyToLayer), mapFullRangeToLayer(applyToLayer));
                            }
                        }
                    }
                }
                break;
            case COLOUR:
                if (Annotations.INSTANCE.equals(applyToLayer) && (applyToLayerValue == null)) {
                    return new PossibleMappingsResult(colourToAnnotations(), colourToAnnotations().ditheredActualRange());
                } else {
                    return new PossibleMappingsResult("Colour mask can only be applied to Annotations layer");
                }
            default:
                return new PossibleMappingsResult("Bit depth or format of mask not supported");
        }
        return new PossibleMappingsResult("No target selected");
    }

    public Terrain getApplyToTerrainType() {
        return applyToTerrainType;
    }

    public void setApplyToTerrainType(Terrain applyToTerrainType) {
        this.applyToTerrainType = applyToTerrainType;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(Mapping mapping) {
        this.mapping = mapping;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getxOffset() {
        return xOffset;
    }

    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException();
        }
        this.threshold = threshold;
    }

    public boolean isRemoveExistingLayer() {
        return removeExistingLayer;
    }

    public void setRemoveExistingLayer(boolean removeExistingLayer) {
        this.removeExistingLayer = removeExistingLayer;
    }

    public int getImageHighValue() {
        return imageHighValue;
    }

    public int getImageLowValue() {
        return imageLowValue;
    }

    public int getImageMaxValue() {
        return imageMaxValue;
    }

    private final Dimension dimension;
    private final InputType inputType;
    private final int imageLowValue, imageHighValue, imageMaxValue;
    private final File imageFile;
    private BufferedImage image;
    private boolean applyToTerrain, removeExistingLayer;
    private Layer applyToLayer;
    private Mapping mapping;
    private int scale, xOffset, yOffset, threshold = -1;
    private Integer applyToLayerValue;
    private Terrain applyToTerrainType;

    public enum InputType {UNSUPPORTED, ONE_BIT_GREY_SCALE, EIGHT_BIT_GREY_SCALE, SIXTEEN_BIT_GREY_SCALE, COLOUR}
}