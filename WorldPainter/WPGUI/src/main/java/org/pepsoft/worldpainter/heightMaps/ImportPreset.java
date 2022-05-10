package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.Platform;

public abstract class ImportPreset {
    private ImportPreset(String description) {
        this.description = description;
    }

    public final String getDescription() {
        return description;
    }

    public boolean isValid(long imageMinHeight, long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
        final long[][] mapping = getMapping(imageMaxHeight, imageLow, imageHigh, platform, maxHeight);
        return (mapping[0][0] >= imageMinHeight) && (mapping[0][1] < imageMaxHeight)
                && (mapping[1][0] >= platform.minZ) && (mapping[1][1] < maxHeight)
                && (getWorldValue(imageHigh, mapping) < maxHeight);
    }

    /**
     * Map the specified inputs to image and world values according to this preset.
     *
     * @param imageMaxHeight The theoretical maximum value of the input image.
     * @param imageLow       The lowest value in the input image.
     * @param imageHigh      The highest value in the input image.
     * @param platform       The platform for which the world is being created.
     * @param maxHeight      The selected {@code maxHeight} for the world being created.
     * @return An array containing the image low and high values in [0][0] and [0][1] respectively, and the
     * corresponding world low and high values in [1][0] and [1][1] respectively.
     */
    public abstract long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight);

    private int getWorldValue(long imageValue, long[][] mapping) {
        final long imageLowLevel = mapping[0][0];
        final int worldLowLevel  = (int) mapping[1][0];
        final float levelScale   = (float) ((int) mapping[1][1] - worldLowLevel) / (mapping[0][1] - imageLowLevel);
        return (int) ((imageValue - imageLowLevel) * levelScale + worldLowLevel);
    }

    private final String description;

    public static final ImportPreset FULL_RANGE_0_BASED = new ImportPreset("Full range; from 0") {
        @Override
        public long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new long[][] {
                        {imageLow, imageHigh},
                        {0, maxHeight - 1}
                };
            } else {
                return new long[][] {
                        {0, imageHigh},
                        {0, maxHeight - 1}
                };
            }
        }
    };

    public static final ImportPreset FULL_RANGE_MINUS_64_BASED = new ImportPreset("Full range; from -64") {
        @Override
        public long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new long[][] {
                        {imageLow, imageHigh},
                        {-64, maxHeight - 1}
                };
            } else {
                return new long[][] {
                        {0, imageHigh},
                        {-64, maxHeight - 1}
                };
            }
        }
    };
    public static final ImportPreset WORLDPAINTER_LOW_RES_0_BASED = new ImportPreset("One to one (e.g. low res WorldPainter export); from 0") {
        @Override
        public long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new long[][] {
                        {imageLow, Math.min(imageMaxHeight, maxHeight) - 1 + imageLow},
                        {0, Math.min(imageMaxHeight, maxHeight) - 1}
                };
            } else {
                return new long[][] {
                        {0, Math.min(imageMaxHeight, maxHeight) - 1},
                        {0, Math.min(imageMaxHeight, maxHeight) - 1}
                };
            }
        }
    };

    public static final ImportPreset WORLDPAINTER_HIGH_RES_0_BASED = new ImportPreset("256 to one (e.g. high res WorldPainter export); from 0") {
        @Override
        public long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                final long worldHighOut = (imageHigh - imageLow) >> 8;
                long imageHighOut = worldHighOut << 8;
                while ((imageHighOut < (imageMaxHeight - 256)) && (((imageHighOut + imageLow) >> 8) < (maxHeight - 1))){
                    imageHighOut += 256;
                }
                return new long[][] {
                        {imageLow, imageHighOut + imageLow},
                        {0, imageHighOut >> 8}
                };
            } else {
                long imageHighOut = (long) (maxHeight - 1) << 8;
                while (imageHighOut >= imageMaxHeight) {
                    imageHighOut -= 256;
                }
                return new long[][] {
                        {0, imageHighOut},
                        {0, imageHighOut >> 8}
                };
            }
        }
    };

    public static final ImportPreset WORLDPAINTER_LOW_RES_MINUS_64_BASED = new ImportPreset("One to one (e.g. low res WorldPainter export); from -64") {
        @Override
        public long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new long[][] {
                        {imageLow, Math.min(imageMaxHeight, maxHeight) - 1 + imageLow},
                        {-64, Math.min(imageMaxHeight, maxHeight - 64) - 1}
                };
            } else {
                return new long[][] {
                        {0, Math.min(imageMaxHeight, maxHeight) - 1},
                        {-64, Math.min(imageMaxHeight, maxHeight - 64) - 1}
                };
            }
        }
    };

    public static final ImportPreset WORLDPAINTER_HIGH_RES_MINUS_64_BASED = new ImportPreset("256 to one (e.g. high res WorldPainter export); from -64") {
        @Override
        public long[][] getMapping(long imageMaxHeight, long imageLow, long imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                final long worldHighOut = ((imageHigh - imageLow) >> 8) - 64;
                long imageHighOut = worldHighOut << 8;
                while ((imageHighOut < (imageMaxHeight - 256)) && (((imageHighOut + imageLow) >> 8) < (maxHeight + 63))){
                    imageHighOut += 256;
                }
                return new long[][] {
                        {imageLow, imageHighOut + imageLow},
                        {-64, (imageHighOut >> 8) - 64}
                };
            } else {
                long imageHighOut = (long) (maxHeight + 63) << 8;
                while (imageHighOut >= imageMaxHeight) {
                    imageHighOut -= 256;
                }
                return new long[][] {
                        {0, imageHighOut},
                        {-64, (imageHighOut >> 8) - 64}
                };
            }
        }
    };

    public static final ImportPreset[] PRESETS = { FULL_RANGE_0_BASED, FULL_RANGE_MINUS_64_BASED, WORLDPAINTER_HIGH_RES_0_BASED, WORLDPAINTER_LOW_RES_0_BASED, WORLDPAINTER_HIGH_RES_MINUS_64_BASED, WORLDPAINTER_LOW_RES_MINUS_64_BASED };
}