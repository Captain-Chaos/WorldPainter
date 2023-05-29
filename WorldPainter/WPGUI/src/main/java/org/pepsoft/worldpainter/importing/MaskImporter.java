/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.heightMaps.BitmapHeightMap;
import org.pepsoft.worldpainter.heightMaps.TransformingHeightMap;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.util.Set;

import static java.awt.image.BufferedImage.*;
import static java.awt.image.DataBuffer.TYPE_DOUBLE;
import static java.awt.image.DataBuffer.TYPE_FLOAT;
import static java.util.Collections.emptySet;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.importing.Mapping.*;
import static org.pepsoft.worldpainter.importing.MaskImporter.InputType.*;
import static org.pepsoft.worldpainter.layers.Layer.DataSize.BIT;

/**
 *
 * @author pepijn
 */
public class MaskImporter {
    public MaskImporter(Dimension dimension, File imageFile, BufferedImage image) {
        this.dimension = dimension;
        this.imageFile = imageFile;
        this.image = image;
        int sampleSize = image.getSampleModel().getSampleSize(0);
        if (sampleSize == 1) {
            inputType = ONE_BIT_GREY_SCALE;
            imageMaxValue = 1;
            unsupportedReason = null;
        } else if (image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
            switch (image.getRaster().getTransferType()) {
                case TYPE_FLOAT:
                    inputType = FLOAT_GREY_SCALE;
                    imageMaxValue = Float.MAX_VALUE;
                    unsupportedReason = null;
                    break;
                case TYPE_DOUBLE:
                    inputType = DOUBLE_GREY_SCALE;
                    imageMaxValue = Double.MAX_VALUE;
                    unsupportedReason = null;
                    break;
                default:
                    switch (sampleSize) {
                        case 8:
                            inputType = EIGHT_BIT_GREY_SCALE;
                            imageMaxValue = 255.0;
                            unsupportedReason = null;
                            break;
                        case 16:
                            inputType = SIXTEEN_BIT_GREY_SCALE;
                            imageMaxValue = 65535.0;
                            unsupportedReason = null;
                            break;
                        case 32:
                            inputType = THIRTY_TWO_BIT_GREY_SCALE;
                            imageMaxValue = 4294967295.0;
                            unsupportedReason = null;
                            break;
                        default:
                            inputType = UNSUPPORTED;
                            imageMaxValue = -1;
                            unsupportedReason = "Grey scale images of " + sampleSize + " bits not yet supported";
                            break;
                    }
                    break;
            }
        } else {
            inputType = InputType.COLOUR;
            imageMaxValue = 0xffffffff;
            unsupportedReason = null;
        }

        final int width = image.getWidth(), height = image.getHeight();
        final Raster raster = image.getRaster();
        switch (inputType) {
            case ONE_BIT_GREY_SCALE:
                imageLowValue = 0;
                imageHighValue = 1;
                break;
            case EIGHT_BIT_GREY_SCALE:
            case SIXTEEN_BIT_GREY_SCALE:
            case THIRTY_TWO_BIT_GREY_SCALE:
                final long lImageMaxValue = (inputType == EIGHT_BIT_GREY_SCALE) ? 255L : ((inputType == SIXTEEN_BIT_GREY_SCALE) ? 65535L : 4294967295L);
                long lImageLowValue = Integer.MAX_VALUE, lImageHighValue = Integer.MIN_VALUE;
outer:          for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        final long value = raster.getSample(x, y, 0) & 0xffffffffL;
                        if (value < lImageLowValue) {
                            lImageLowValue = value;
                        }
                        if (value > lImageHighValue) {
                            lImageHighValue = value;
                        }
                        if ((lImageLowValue == 0) && (lImageHighValue == lImageMaxValue)) {
                            // Lowest and highest possible values found; no point in looking any further!
                            break outer;
                        }
                    }
                }
                this.imageLowValue = lImageLowValue;
                this.imageHighValue = lImageHighValue;
                break;
            case FLOAT_GREY_SCALE:
            case DOUBLE_GREY_SCALE:
                final double dImageMaxValue = Double.MAX_VALUE;
                double dImageLowValue = Double.MAX_VALUE, dImageHighValue = -Double.MAX_VALUE;
outer:          for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        final double value = raster.getSampleDouble(x, y, 0);
                        if (value < dImageLowValue) {
                            dImageLowValue = value;
                        }
                        if (value > dImageHighValue) {
                            dImageHighValue = value;
                        }
                        if ((dImageLowValue <= 0.0) && (dImageHighValue >= dImageMaxValue)) {
                            // Lowest and highest possible values found; no point in looking any further!
                            break outer;
                        }
                    }
                }
                this.imageLowValue = dImageLowValue;
                this.imageHighValue = dImageHighValue;
                break;
            default:
                this.imageLowValue = -1.0;
                this.imageHighValue = -1.0;
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
        final boolean colour = inputType == COLOUR;
        final boolean bitmask = inputType == ONE_BIT_GREY_SCALE;
        final boolean discrete = (applyToTerrain && (applyToTerrainType == null)) || ((applyToLayer != null) && applyToLayer.discrete && (applyToLayerValue == null));

        // Set up mapping
        mapping.setThreshold(threshold);
        mapping.setMaskLowValue(imageLowValue);
        mapping.setMaskHighValue(imageHighValue);
        mapping.setMaskMaxValue(imageMaxValue);

        // Scale the mask, if necessary
        BufferedImage scaledImage;
        final HeightMap scaledHeightMap;
        final int oldWidth = image.getWidth(), oldHeight = image.getHeight();
        final int width = Math.round(oldWidth * scale), height = Math.round(oldHeight * scale);
        if ((width == oldWidth) && (height == oldHeight)) {
            // No scaling necessary
            scaledImage = image;
            scaledHeightMap = BitmapHeightMap.build().withImage(image).now();
        } else if (colour) {
            // We are mapping a colour image. Colour images might need dithering, which we have to do via the image. For
            // now, colour images can be smoothed. TODO: that might change if we introduce mapping discrete colours to
            //  terrain or layer values, which is not yet possible
            // Scale to full RGB in order to be able to scale smoothly (for now)
            scaledImage = new BufferedImage(width, height, TYPE_INT_ARGB);
            Graphics2D g2 = scaledImage.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(image, 0, 0, width, height, null);
            } finally {
                g2.dispose();
            }
            if ((mapping instanceof ColourToAnnotationsMapping) && ((ColourToAnnotationsMapping) mapping).dithered) {
                scaledImage = Mapping.ditherMask(scaledImage);
            }
            scaledHeightMap = null;
        } else {
            // Do the scaling via the height map, as it works with more image types
            final BitmapHeightMap.BitmapHeightMapBuilder builder = BitmapHeightMap.build().withImage(image);
            if ((! bitmask) && (! discrete)) {
                builder.withSmoothScaling(true);
            }
            scaledHeightMap = TransformingHeightMap.build().withHeightMap(builder.now()).withScale(scale).now();
            scaledImage = null;
        }
        image = null; // The original image is no longer necessary, so allow it to be garbage collected to make more space available for the import

        if (dimension.getWorld() != null) {
            dimension.getWorld().addHistoryEntry(HistoryEntry.WORLD_MASK_IMPORTED_TO_DIMENSION, dimension.getName(), imageFile, mapping.getAspect());
        }

        // Apply the mask tile by tile
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

                    mapping.setTile(tile);
                    if (colour) {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    mapping.applyColour(xInTile, yInTile, scaledImage.getRGB(imageX, imageY));
                                }
                            }
                        }
                    } else if (discrete) {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    // This is warranted because doubles can still precisely store integers up to around
                                    // 2⁵³ and the expected values for discrete layers will always be much smaller than
                                    // that
                                    mapping.applyDiscrete(xInTile, yInTile, (int) scaledHeightMap.getHeight(imageX, imageY));
                                }
                            }
                        }
                    } else {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    mapping.applyGreyScale(xInTile, yInTile, scaledHeightMap.getHeight(imageX, imageY));
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
        return inputType != UNSUPPORTED;
    }

    public String getScalingNotSupportedReason() {
        final int imageType = image.getType();
        if ((image.getColorModel() instanceof IndexColorModel) && (imageType != TYPE_BYTE_BINARY) && (imageType != TYPE_BYTE_INDEXED)) {
            return "Scaling not supported for indexed images of type " + imageType;
        }
        return null;
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
            case THIRTY_TWO_BIT_GREY_SCALE:
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
            case FLOAT_GREY_SCALE:
            case DOUBLE_GREY_SCALE:
                if (applyToTerrain) {
                    if (applyToTerrainType != null) {
                        // Continuous greyscale mask to one terrain type
                        return new PossibleMappingsResult(setTerrainValue(applyToTerrainType).ditheredActualRange(), setTerrainValue(applyToTerrainType).ditheredFullRange(), setTerrainValue(applyToTerrainType).threshold());
                    } else {
                        return new PossibleMappingsResult("Pick one terrain type to apply floating point mask to");
                    }
                } else if (applyToLayer != null) {
                    if (applyToLayer.discrete) {
                        // Continuous greyscale mask to discrete layer
                        if (applyToLayerValue != null) {
                            // Value selected
                            return new PossibleMappingsResult(setLayerValue(applyToLayer, applyToLayerValue).ditheredActualRange(), setLayerValue(applyToLayer, applyToLayerValue).ditheredFullRange(), setLayerValue(applyToLayer, applyToLayerValue).threshold());
                        } else {
                            // No value selected
                            return new PossibleMappingsResult("Pick one discrete layer value type to apply floating point mask to");
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

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
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

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
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

    public double getImageHighValue() {
        return imageHighValue;
    }

    public double getImageLowValue() {
        return imageLowValue;
    }

    public double getImageMaxValue() {
        return imageMaxValue;
    }

    public String getUnsupportedReason() {
        return unsupportedReason;
    }

    private final Dimension dimension;
    private final InputType inputType;
    private final double imageLowValue, imageHighValue, imageMaxValue;
    private final File imageFile;
    private final String unsupportedReason;
    private BufferedImage image;
    private boolean applyToTerrain, removeExistingLayer;
    private Layer applyToLayer;
    private Mapping mapping;
    private float scale;
    private int xOffset, yOffset;
    private double threshold = -1.0;
    private Integer applyToLayerValue;
    private Terrain applyToTerrainType;

    public enum InputType { UNSUPPORTED, ONE_BIT_GREY_SCALE, EIGHT_BIT_GREY_SCALE, SIXTEEN_BIT_GREY_SCALE, THIRTY_TWO_BIT_GREY_SCALE, COLOUR, FLOAT_GREY_SCALE, DOUBLE_GREY_SCALE }
}