package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.Platform;

public abstract class ImportPreset {
    private ImportPreset(String description) {
        this.description = description;
    }

    public final String getDescription() {
        return description;
    }

    public boolean isValid(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
        final long[][] mapping = getMapping(bitDepth, imageLow, imageHigh, platform, maxHeight);
        return (mapping[0][0] >= 0) && (mapping[0][1] < 1L << bitDepth)
                && (mapping[1][0] >= platform.minZ) && (mapping[1][1] < maxHeight);
    }

    /**
     * Map the specified inputs to image and world values according to this preset.
     *
     * @param bitDepth  The bit depth of the input image.
     * @param imageLow  The lowest value in the input image.
     * @param imageHigh The highest value in the input image.
     * @param platform  The platform for which the world is being created.
     * @param maxHeight The selected {@code maxHeight} for the world being created.
     * @return An array containing the image low and high values in [0][0] and [0][1] respectively,
     */
    public abstract long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight);

    private final String description;

    public static final ImportPreset FULL_RANGE_0_BASED = new ImportPreset("Full range; from 0") {
        @Override
        public long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            return new long[][] {
                    {0, imageHigh},
                    {0, maxHeight - 1}
            };
        }
    };

    public static final ImportPreset FULL_RANGE_MINUS_64_BASED = new ImportPreset("Full range; from -64") {
        @Override
        public long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            return new long[][] {
                    {0, imageHigh},
                    {-64, maxHeight - 1}
            };
        }
    };
    public static final ImportPreset WORLDPAINTER_LOW_RES_0_BASED = new ImportPreset("(1:1)/low res WorldPainter export; from 0") {
        @Override
        public long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            final long imageMaxHeight = 1L << bitDepth;
            return new long[][] {
                    {0, Math.min(imageMaxHeight, maxHeight) - 1},
                    {0, Math.min(imageMaxHeight, maxHeight) - 1}
            };
        }
    };

    public static final ImportPreset WORLDPAINTER_HIGH_RES_0_BASED = new ImportPreset("(256:1)/high res WorldPainter export; from 0") {
        @Override
        public long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            final long imageMaxHeight = 1L << bitDepth;
            int imageHighOut = (maxHeight - 1) << 8;
            while (imageHighOut >= imageMaxHeight) {
                imageHighOut -= 256;
            }
            return new long[][] {
                    {0, imageHighOut},
                    {0, imageHighOut >> 8}
            };
        }
    };

    public static final ImportPreset WORLDPAINTER_LOW_RES_MINUS_64_BASED = new ImportPreset("(1:1)/low res WorldPainter export; from -64") {
        @Override
        public long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            final long imageMaxHeight = 1L << bitDepth;
            return new long[][] {
                    {0, Math.min(imageMaxHeight, maxHeight) - 1},
                    {-64, Math.min(imageMaxHeight, maxHeight - 64) - 1}
            };
        }
    };

    public static final ImportPreset WORLDPAINTER_HIGH_RES_MINUS_64_BASED = new ImportPreset("(256:1)/high res WorldPainter export; from -64") {
        @Override
        public long[][] getMapping(int bitDepth, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            final long imageMaxHeight = 1L << bitDepth;
            int imageHighOut = (maxHeight + 63) << 8;
            while (imageHighOut >= imageMaxHeight) {
                imageHighOut -= 256;
            }
            return new long[][] {
                    {0, imageHighOut},
                    {-64, (imageHighOut >> 8) - 64}
            };
        }
    };

    public static final ImportPreset[] PRESETS = { FULL_RANGE_0_BASED, FULL_RANGE_MINUS_64_BASED, WORLDPAINTER_LOW_RES_0_BASED, WORLDPAINTER_HIGH_RES_0_BASED, WORLDPAINTER_LOW_RES_MINUS_64_BASED, WORLDPAINTER_HIGH_RES_MINUS_64_BASED };
}