/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import org.pepsoft.util.MathUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_13Biomes;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.importing.MaskImporter.InputType.SIXTEEN_BIT_GREY_SCALE;

/**
 *
 * @author pepijn
 */
public class MaskImporter {
    public MaskImporter(Dimension dimension, File imageFile, List<Layer> allLayers) throws IOException {
        this(dimension, imageFile, ImageIO.read(imageFile), allLayers);
    }

    public MaskImporter(Dimension dimension, File imageFile, BufferedImage image, List<Layer> allLayers) {
        this.dimension = dimension;
        this.imageFile = imageFile;
        this.image = image;
        this.allLayers = allLayers;
        int sampleSize = image.getSampleModel().getSampleSize(0);
        if (sampleSize == 1) {
            inputType = InputType.ONE_BIT_GRAY_SCALE;
        } else if (image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
            if (sampleSize == 8) {
                inputType = InputType.EIGHT_BIT_GREY_SCALE;
            } else if (sampleSize == 16) {
                inputType = InputType.SIXTEEN_BIT_GREY_SCALE;
            } else {
                inputType = InputType.UNSUPPORTED;
            }
        } else {
            inputType = InputType.COLOUR;
        }

        switch (inputType) {
            case ONE_BIT_GRAY_SCALE:
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
        if ((mapping == Mapping.THRESHOLD) && (threshold == -1)) {
            throw new IllegalStateException("Threshold not set");
        }
        final int maxValue;
        switch (inputType) {
            case ONE_BIT_GRAY_SCALE:
                maxValue = 1;
                break;
            case EIGHT_BIT_GREY_SCALE:
                maxValue = 255;
                break;
            case SIXTEEN_BIT_GREY_SCALE:
                maxValue = 65535;
                break;
            default:
                maxValue = 0;
                break;
        }
        final Random random = new Random(dimension.getSeed() + xOffset * 31 + yOffset);

        // Scale the mask, if necessary
        final BufferedImage scaledImage;
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
                if (mapping == Mapping.FULL_RANGE) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                }
                g2.drawImage(image, 0, 0, newWidth, newHeight, null);
            } finally {
                g2.dispose();
            }
        }
        image = null; // The original image is no longer necessary, so allow it to be garbage collected to make more space available for the import

        // Create the appropriate mapping logic
        abstract class Applicator {
            void setTile(Tile tile) {
                this.tile = tile;
            }

            abstract void apply(int x, int y, int value);

            Tile tile;
        }
        final Applicator applicator;
        final String aspect;
        switch (inputType) {
            case ONE_BIT_GRAY_SCALE:
                if (removeExistingLayer) {
                    applicator = new Applicator() {
                        @Override
                        void apply(int x, int y, int value) {
                            tile.setBitLayerValue(applyToLayer, x, y, value != 0);
                        }
                    };
                } else {
                    applicator = new Applicator() {
                        @Override
                        void apply(int x, int y, int value) {
                            if (value != 0) {
                                tile.setBitLayerValue(applyToLayer, x, y, true);
                            }
                        }
                    };
                }
                aspect = "layer " + applyToLayer.getName();
                break;
            case EIGHT_BIT_GREY_SCALE:
            case SIXTEEN_BIT_GREY_SCALE:
                switch (mapping) {
                    case ONE_TO_ONE:
                        if (applyToTerrain) {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    tile.setTerrain(x, y, Terrain.VALUES[value]);
                                }
                            };
                            aspect = "terrain";
                        } else {
                            final int defaultValue = applyToLayer.getDefaultValue();
                            if (removeExistingLayer) {
                                applicator = new Applicator() {
                                    @Override
                                    void apply(int x, int y, int value) {
                                        if ((value != defaultValue)
                                                || (tile.getLayerValue(applyToLayer, x, y) != defaultValue)) {
                                            tile.setLayerValue(applyToLayer, x, y, value);
                                        }
                                    }
                                };
                            } else {
                                applicator = new Applicator() {
                                    @Override
                                    void apply(int x, int y, int value) {
                                        if ((value != defaultValue) && (value > tile.getLayerValue(applyToLayer, x, y))) {
                                            tile.setLayerValue(applyToLayer, x, y, value);
                                        }
                                    }
                                };
                            }
                            aspect = "layer " + applyToLayer.getName();
                        }
                        break;
                    case DITHERING:
                        if (removeExistingLayer) {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    boolean layerValue = (value > 0) && (random.nextInt(limit) <= value);
                                    if (layerValue || (tile.getBitLayerValue(applyToLayer, x, y))) {
                                        tile.setBitLayerValue(applyToLayer, x, y, layerValue);
                                    }
                                }

                                private final int limit = maxValue + 1;
                            };
                        } else {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    if ((value > 0) && (random.nextInt(limit) <= value)) {
                                        tile.setBitLayerValue(applyToLayer, x, y, true);
                                    }
                                }

                                private final int limit = maxValue + 1;
                            };
                        }
                        aspect = "layer " + applyToLayer.getName();
                        break;
                    case THRESHOLD:
                        if (removeExistingLayer) {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    boolean layerValue = value >= threshold;
                                    if (layerValue || tile.getBitLayerValue(applyToLayer, x, y)) {
                                        tile.setBitLayerValue(applyToLayer, x, y, layerValue);
                                    }
                                }
                            };
                        } else {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    if (value >= threshold) {
                                        tile.setBitLayerValue(applyToLayer, x, y, true);
                                    }
                                }
                            };
                        }
                        aspect = "layer " + applyToLayer.getName();
                        break;
                    case FULL_RANGE:
                        final int layerLimit;
                        if (applyToLayer.getDataSize() == Layer.DataSize.NIBBLE) {
                            layerLimit = 16;
                        } else if (applyToLayer.getDataSize() == Layer.DataSize.BYTE) {
                            layerLimit = 256;
                        } else {
                            throw new IllegalArgumentException();
                        }
                        final int defaultValue = applyToLayer.getDefaultValue();
                        if (removeExistingLayer) {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    int layerValue = value * layerLimit / limit;
                                    if ((layerValue != defaultValue) || (tile.getLayerValue(applyToLayer, x, y) != defaultValue)) {
                                        tile.setLayerValue(applyToLayer, x, y, layerValue);
                                    }
                                }

                                private final int limit = maxValue + 1;
                            };
                        } else {
                            applicator = new Applicator() {
                                @Override
                                void apply(int x, int y, int value) {
                                    int layerValue = value * layerLimit / limit;
                                    if ((layerValue != defaultValue) && (layerValue > tile.getLayerValue(applyToLayer, x, y))) {
                                        tile.setLayerValue(applyToLayer, x, y, layerValue);
                                    }
                                }

                                private final int limit = maxValue + 1;
                            };
                        }
                        aspect = "layer " + applyToLayer.getName();
                        break;
                    default:
                        throw new IllegalArgumentException("Don't know how to apply this combo");
                }
                break;
            case COLOUR:
                if (removeExistingLayer) {
                    applicator = new Applicator() {
                        @Override
                        void apply(int x, int y, int value) {
                            if (((value >> 24) & 0xff) > 0x7f) {
                                tile.setLayerValue(Annotations.INSTANCE, x, y, COLOUR_ANNOTATION_MAPPING[((value >> 12) & 0xf00) | ((value >> 8) & 0xf0) | ((value >> 4) & 0xf)]);
                            } else if (tile.getLayerValue(Annotations.INSTANCE, x, y) != 0) {
                                tile.setLayerValue(Annotations.INSTANCE, x, y, 0);
                            }
                        }
                    };
                } else {
                    applicator = new Applicator() {
                        @Override
                        void apply(int x, int y, int value) {
                            if (((value >> 24) & 0xff) > 0x7f) {
                                tile.setLayerValue(Annotations.INSTANCE, x, y, COLOUR_ANNOTATION_MAPPING[((value >> 12) & 0xf00) | ((value >> 8) & 0xf0) | ((value >> 4) & 0xf)]);
                            }
                        }
                    };
                }
                aspect = "annotations";
                break;
            default:
                throw new IllegalArgumentException("Don't know how to apply this combo");
        }
        if (dimension.getWorld() != null) {
            dimension.getWorld().addHistoryEntry(HistoryEntry.WORLD_MASK_IMPORTED_TO_DIMENSION, dimension.getName(), imageFile, aspect);
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
                    final int tileOffsetX = (tileX << TILE_SIZE_BITS) - xOffset, tileOffsetY = (tileY << TILE_SIZE_BITS) - yOffset;
                    final Raster raster = scaledImage.getRaster();
                    applicator.setTile(tile);
                    if (inputType == InputType.COLOUR) {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    applicator.apply(xInTile, yInTile, scaledImage.getRGB(imageX, imageY));
                                }
                            }
                        }
                    } else {
                        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                final int imageX = tileOffsetX + xInTile, imageY = tileOffsetY + yInTile;
                                if ((imageX >= 0) && (imageX < width) && (imageY >= 0) && (imageY < height)) {
                                    applicator.apply(xInTile, yInTile, raster.getSample(imageX, imageY, 0));
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

    /**
     * @return Whether this image type can be mapped to the terrain.
     */
    public boolean isTerrainPossible() {
        // Only gray scale images with at least 8 bits can be mapped to the
        // terrain. Possible in the future we will support mapping colours to
        // the terrain, but not yet.
        return (inputType == InputType.EIGHT_BIT_GREY_SCALE || inputType == InputType.SIXTEEN_BIT_GREY_SCALE)
            && (imageHighValue < Terrain.VALUES.length);
    }

//    TODO: dithered en threshold ook ondersteunen voor terrain en continue layers - mappen op één waarde
//            of beter nog: generieke mapping mode bouwen

    /**
     * @return The list of layers to which this image type can be mapped. May be
     *     empty.
     */
    public List<Layer> getPossibleLayers() {
        List<Layer> possibleLayers = new ArrayList<>(allLayers.size());
        for (Layer layer: allLayers) {
            if (layer.equals(Annotations.INSTANCE)) {
                // Annotations are a special case; since they are coloured we
                // support importing a colour image as annotations
                if (((inputType == InputType.EIGHT_BIT_GREY_SCALE || inputType == InputType.SIXTEEN_BIT_GREY_SCALE) && (imageHighValue < 16)) || inputType == InputType.COLOUR) {
                    possibleLayers.add(layer);
                }
            } else if (layer.equals(Biome.INSTANCE)) {
                // Biomes are a discrete layer which can only be mapped one on one
                if ((inputType == InputType.EIGHT_BIT_GREY_SCALE || inputType == InputType.SIXTEEN_BIT_GREY_SCALE) && (imageHighValue <= Minecraft1_13Biomes.HIGHEST_BIOME_ID )) {
                    possibleLayers.add(layer);
                }
            } else if (layer.getDataSize() == Layer.DataSize.BIT || layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
                // 8 or 16 bit masks can be applied by either dithering or applying a threshold
                if (inputType == InputType.ONE_BIT_GRAY_SCALE || inputType == InputType.EIGHT_BIT_GREY_SCALE || inputType == InputType.SIXTEEN_BIT_GREY_SCALE) {
                    possibleLayers.add(layer);
                }
            } else {
                // Continuous layers need a gray scale mask
                if (inputType == InputType.EIGHT_BIT_GREY_SCALE || inputType == InputType.SIXTEEN_BIT_GREY_SCALE) {
                    possibleLayers.add(layer);
                }
            }
        }
        return possibleLayers;
    }

    public Layer getApplyToLayer() {
        return applyToLayer;
    }

    public void setApplyToLayer(Layer applyToLayer) {
        if ((applyToLayer != null) && (! getPossibleLayers().contains(applyToLayer))) {
            throw new IllegalArgumentException("This image type cannot be applied to the specified layer");
        }
        this.applyToLayer = applyToLayer;
        if (applyToLayer != null) {
            applyToTerrain = false;
        }
    }

    public boolean isApplyToTerrain() {
        return applyToTerrain;
    }

    public void setApplyToTerrain(boolean applyToTerrain) {
        if (applyToTerrain && (! isTerrainPossible())) {
            throw new IllegalArgumentException("This image type cannot be applied to the terrain");
        }
        this.applyToTerrain = applyToTerrain;
        if (applyToTerrain) {
            applyToLayer = null;
        }
    }

    /**
     * @return The types of mapping possible for the specified target (terrain
     *     or layer).
     */
    public Set<Mapping> getPossibleMappings() {
        if (applyToTerrain) {
            return EnumSet.of(Mapping.ONE_TO_ONE);
        } else if (applyToLayer.equals(Annotations.INSTANCE)) {
            return EnumSet.of(Mapping.ONE_TO_ONE);
        } else if (applyToLayer.equals(Biome.INSTANCE)) {
            return EnumSet.of(Mapping.ONE_TO_ONE);
        } else if (applyToLayer.getDataSize() == Layer.DataSize.BIT || applyToLayer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
            switch (inputType) {
                case ONE_BIT_GRAY_SCALE:
                    return EnumSet.of(Mapping.ONE_TO_ONE);
                case EIGHT_BIT_GREY_SCALE:
                case SIXTEEN_BIT_GREY_SCALE:
                    return EnumSet.of(Mapping.DITHERING, Mapping.THRESHOLD);
            }
        } else {
            return EnumSet.of(Mapping.FULL_RANGE);
        }
        throw new IllegalStateException();
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(Mapping mapping) {
        if (! getPossibleMappings().contains(mapping)) {
            throw new IllegalArgumentException();
        }
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

    private static int findNearestAnnotationValue(int colour) {
        final int red = (colour & 0xff0000) >> 16;
        final int green = (colour & 0xff00) >> 8;
        final int blue = colour & 0xff;
        float minDistance = Float.MAX_VALUE;
        int minDistanceIndex = -1;
        for (int i = 0; i < ANNOTATIONS_PALETTE.length; i++) {
            final float distance = MathUtils.getDistance(red - ANNOTATIONS_PALETTE[i][0], green - ANNOTATIONS_PALETTE[i][1], blue - ANNOTATIONS_PALETTE[i][2]);
            if (distance < minDistance) {
                minDistance = distance;
                minDistanceIndex = i;
            }
        }
        return minDistanceIndex + 1;
    }

    private final Dimension dimension;
    private final List<Layer> allLayers;
    private final InputType inputType;
    private final int imageLowValue, imageHighValue;
    private final File imageFile;
    private BufferedImage image;
    private boolean applyToTerrain, removeExistingLayer;
    private Layer applyToLayer;
    private Mapping mapping;
    private int scale, xOffset, yOffset, threshold = -1;

    public enum InputType {UNSUPPORTED, ONE_BIT_GRAY_SCALE, EIGHT_BIT_GREY_SCALE, SIXTEEN_BIT_GREY_SCALE, COLOUR}
    public enum Mapping {ONE_TO_ONE, DITHERING, THRESHOLD, FULL_RANGE}

    private static int[][] ANNOTATIONS_PALETTE = {
        {0xdd, 0xdd, 0xdd},
        {0xdb, 0x7d, 0x3e},
        {0xb3, 0x50, 0xbc},
        {0x6a, 0x8a, 0xc9},
        {0xb1, 0xa6, 0x27},
        {0x41, 0xae, 0x38},
        {0xd0, 0x84, 0x99},
        {0x9a, 0xa1, 0xa1},
        {0x2e, 0x6e, 0x89},
        {0x7e, 0x3d, 0xb5},
        {0x2e, 0x38, 0x8d},
        {0x4f, 0x32, 0x1f},
        {0x35, 0x46, 0x1b},
        {0x96, 0x34, 0x30},
        {0x19, 0x16, 0x16}
    };

    /**
     * A table which maps colours to annotation layer values. The colours have
     * their last four bits stripped to keep the table small.
     */
    private static final int[] COLOUR_ANNOTATION_MAPPING = new int[4096];

    static {
        for (int i = 0; i < 4096; i++) {
            COLOUR_ANNOTATION_MAPPING[i] = findNearestAnnotationValue(((i & 0xf00) << 12) | ((i & 0xf0) << 8) | ((i & 0xf) << 4));
        }
    }
}