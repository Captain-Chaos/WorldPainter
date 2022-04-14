package org.pepsoft.worldpainter.exporting;

import com.google.common.collect.ImmutableList;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.awt.image.DataBuffer.TYPE_INT;
import static java.util.Collections.singletonList;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class HeightMapExporter {
    public HeightMapExporter(Dimension dimension, boolean highRes) {
        this.dimension = dimension;
        this.highRes = highRes;
        minHeight = dimension.getMinHeight();
        highestPoint = dimension.getHightestIntHeight();
        if (highRes) {
            bitsRequired = (int) Math.ceil(Math.log(((highestPoint - minHeight + 1) << 8) - 1) / Math.log(2));
        } else {
            bitsRequired = (int) Math.ceil(Math.log(highestPoint - minHeight) / Math.log(2));
        }
    }

    public List<String> getSupportedFileExtensions() {
        if (bitsRequired <= 16) {
            return ImmutableList.of("png", "tiff");
        } else {
            return singletonList("tiff");
        }
    }

    public boolean exportToFile(File file) {
        final String type = file.getName().substring(file.getName().lastIndexOf('.') + 1).toUpperCase();
        // Leave the progress receiver indeterminate, since
        // by *far* the most time goes into actually writing
        // the file, and we can't report progress for that
        try {
            final BufferedImage image;
            // TODO fail gracefully if the world is too large because the data buffer would overflow
            if (bitsRequired > 16) {
                ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifiers.createGrayscale(32, TYPE_INT);
                image = imageTypeSpecifier.createBufferedImage(dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE);
            } else {
                image = new BufferedImage(dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE, (bitsRequired <= 8) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_USHORT_GRAY);
            }
            final WritableRaster raster = image.getRaster();
            for (Tile tile: dimension.getTiles()) {
                final int tileOffsetX = (tile.getX() - dimension.getLowestX()) * TILE_SIZE;
                final int tileOffsetY = (tile.getY() - dimension.getLowestY()) * TILE_SIZE;
                if (highRes) {
                    for (int dx = 0; dx < TILE_SIZE; dx++) {
                        for (int dy = 0; dy < TILE_SIZE; dy++) {
                            raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, tile.getRawHeight(dx, dy));
                        }
                    }
                } else {
                    for (int dx = 0; dx < TILE_SIZE; dx++) {
                        for (int dy = 0; dy < TILE_SIZE; dy++) {
                            raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, tile.getIntHeight(dx, dy) - minHeight);
                        }
                    }
                }
            }
            return ImageIO.write(image, type, file);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while exporting image", e);
        }
    }

    private final Dimension dimension;
    private final boolean highRes;
    private final int minHeight, highestPoint, bitsRequired;
}