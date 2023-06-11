package org.pepsoft.worldpainter.exporting;

import com.google.common.collect.ImmutableList;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static java.awt.image.DataBuffer.TYPE_FLOAT;
import static java.awt.image.DataBuffer.TYPE_INT;
import static java.util.Collections.singletonList;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.exporting.HeightMapExporter.Format.FLOAT_NORMALISED;
import static org.pepsoft.worldpainter.exporting.HeightMapExporter.Format.INTEGER_LOW_RESOLUTION;

public class HeightMapExporter {
    public HeightMapExporter(Dimension dimension, Format format) {
        this.dimension = dimension;
        this.format = format;
        minHeight = dimension.getMinHeight();
        switch (format) {
            case INTEGER_HIGH_RESOLUTION:
                intHighestHeight = dimension.getHighestIntHeight();
                bitsRequired = (int) Math.ceil(Math.log(((intHighestHeight - minHeight + 1) << 8) - 1) / Math.log(2));
                floatLowestHeight = floatHighestHeight = -Float.MAX_VALUE;
                break;
            case INTEGER_LOW_RESOLUTION:
                intHighestHeight = dimension.getHighestIntHeight();
                bitsRequired = (int) Math.ceil(Math.log(intHighestHeight - minHeight) / Math.log(2));
                floatLowestHeight = floatHighestHeight = -Float.MAX_VALUE;
                break;
            case FLOAT_NORMALISED:
            case FLOAT_ONE_TO_ONE:
                final float[] range = dimension.getHeightRange();
                floatLowestHeight = range[0];
                floatHighestHeight = range[1];
                bitsRequired = 32;
                intHighestHeight = Integer.MIN_VALUE;
                break;
            default:
                throw new InternalError();
        }
    }

    public List<String> getSupportedFileExtensions() {
        if (bitsRequired <= 16) {
            return ImmutableList.of("png", "tiff");
        } else {
            return singletonList("tiff");
        }
    }

    public String getDefaultFilename() {
        final String defaultExtension = getSupportedFileExtensions().get(0);
        final StringBuilder sb = new StringBuilder();
        sb.append(dimension.getWorld().getName().replaceAll("\\s", "").toLowerCase());
        if (dimension.getAnchor().dim != DIM_NORMAL) {
            sb.append('_');
            sb.append(dimension.getName().replaceAll("\\s", "").toLowerCase());
        }
        switch (format) {
            case FLOAT_NORMALISED:
                sb.append("_normalised-");
                break;
            case INTEGER_HIGH_RESOLUTION:
                sb.append("_high-res-");
                break;
            default:
                sb.append('_');
                break;
        }
        sb.append("heightmap.");
        sb.append(defaultExtension);
        return sb.toString();
    }

    public boolean exportToFile(File file) {
        final String type = file.getName().substring(file.getName().lastIndexOf('.') + 1).toUpperCase();
        try {
            final BufferedImage image;
            final ImageWriter writer;
            final ImageWriteParam params;
            final float scale, offset;
            final double formatMax, dimensionMax;
            // TODO fail gracefully if the world is too large because the data buffer would overflow
            switch (format) {
                case INTEGER_LOW_RESOLUTION:
                case INTEGER_HIGH_RESOLUTION:
                    if (bitsRequired > 16) {
                        final ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifiers.createGrayscale(32, TYPE_INT);
                        image = imageTypeSpecifier.createBufferedImage(dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE);
                        final Iterator<ImageWriter> writers = ImageIO.getImageWriters(imageTypeSpecifier, type);
                        if (writers.hasNext()) {
                            writer = writers.next();
                            params = writer.getDefaultWriteParam();
                            params.setCompressionMode(MODE_EXPLICIT);
                            params.setCompressionType("LZW");
                            params.setCompressionQuality(0f);
                            formatDescription = "in 32-bit unsigned integer grayscale compressed " + type + " format.";
                            formatMax = Math.pow(2.0, 32.0);
                            dimensionMax = (format == INTEGER_LOW_RESOLUTION) ? (intHighestHeight - minHeight) : ((intHighestHeight - minHeight) << 8);
                        } else {
                            return false;
                        }
                    } else {
                        image = new BufferedImage(dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE, (bitsRequired <= 8) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_USHORT_GRAY);
                        final Iterator<ImageWriter> writers = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(image), type);
                        if (writers.hasNext()) {
                            writer = writers.next();
                            params = writer.getDefaultWriteParam();
                            formatDescription = ((bitsRequired <= 8) ? "in 8-bit" : "in 16-bit") + " unsigned integer grayscale " + type + " format.";
                            formatMax = Math.pow(2.0, (bitsRequired <= 8) ? 8.0 : 16.0);
                            dimensionMax = (format == INTEGER_LOW_RESOLUTION) ? (intHighestHeight - minHeight) : ((intHighestHeight - minHeight) << 8);
                        } else {
                            return false;
                        }
                    }
                    scale = offset = -Float.MAX_VALUE;
                    break;
                case FLOAT_NORMALISED:
                case FLOAT_ONE_TO_ONE:
                    final ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifiers.createGrayscale(32, TYPE_FLOAT);
                    image = imageTypeSpecifier.createBufferedImage(dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE);
                    final Iterator<ImageWriter> writers = ImageIO.getImageWriters(imageTypeSpecifier, type);
                    if (writers.hasNext()) {
                        writer = writers.next();
                        params = writer.getDefaultWriteParam();
                        params.setCompressionMode(MODE_EXPLICIT);
                        params.setCompressionType("LZW");
                        params.setCompressionQuality(0f);
                        formatDescription = "in " + ((format == FLOAT_NORMALISED) ? "normalised " : "") + " floating point grayscale compressed " + type + " format.";
                    } else {
                        return false;
                    }
                    scale = floatHighestHeight - floatLowestHeight;
                    offset = floatLowestHeight;
                    formatMax = (format == FLOAT_NORMALISED) ? 1.0 : floatHighestHeight;
                    dimensionMax = (format == FLOAT_NORMALISED) ? 1.0 : floatHighestHeight;
                    break;
                default:
                    throw new InternalError();
            }
            if (dimensionMax / formatMax < 0.2) {
                formatDescription += "\n\n" +
                        "PLEASE NOTE: this height map will appear very dark when displayed as\n" +
                        "an image, because the exported values are very small compared to the\n" +
                        "theoretical range of the image format.";
            }
            final WritableRaster raster = image.getRaster();
            for (Tile tile: dimension.getTiles()) {
                final int tileOffsetX = (tile.getX() - dimension.getLowestX()) * TILE_SIZE;
                final int tileOffsetY = (tile.getY() - dimension.getLowestY()) * TILE_SIZE;
                switch (format) {
                    case INTEGER_HIGH_RESOLUTION:
                        for (int dx = 0; dx < TILE_SIZE; dx++) {
                            for (int dy = 0; dy < TILE_SIZE; dy++) {
                                raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, tile.getRawHeight(dx, dy));
                            }
                        }
                        break;
                    case INTEGER_LOW_RESOLUTION:
                        for (int dx = 0; dx < TILE_SIZE; dx++) {
                            for (int dy = 0; dy < TILE_SIZE; dy++) {
                                raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, tile.getIntHeight(dx, dy) - minHeight);
                            }
                        }
                        break;
                    case FLOAT_ONE_TO_ONE:
                        for (int dx = 0; dx < TILE_SIZE; dx++) {
                            for (int dy = 0; dy < TILE_SIZE; dy++) {
                                raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, tile.getHeight(dx, dy));
                            }
                        }
                        break;
                    case FLOAT_NORMALISED:
                        for (int dx = 0; dx < TILE_SIZE; dx++) {
                            for (int dy = 0; dy < TILE_SIZE; dy++) {
                                raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, (tile.getHeight(dx, dy) - offset) / scale);
                            }
                        }
                        break;
                }
            }
            try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(image, null, null), params);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while exporting image", e);
        }
    }

    public String getFormatDescription() {
        return formatDescription;
    }

    private final Dimension dimension;
    private final Format format;
    private final int minHeight, intHighestHeight, bitsRequired;
    private final float floatLowestHeight, floatHighestHeight;
    private String formatDescription;

    public enum Format { INTEGER_LOW_RESOLUTION, INTEGER_HIGH_RESOLUTION, FLOAT_NORMALISED, FLOAT_ONE_TO_ONE}
}