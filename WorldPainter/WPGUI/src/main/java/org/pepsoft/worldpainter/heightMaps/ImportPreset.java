package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.Platform;

public abstract class ImportPreset {
    private ImportPreset(String description) {
        this.description = description;
    }

    public final String getDescription() {
        return description;
    }

    public boolean isValid(double imageMinHeight, double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int minHeight, int maxHeight) {
        final Mapping mapping = getMapping(imageMaxHeight, imageLow, imageHigh, platform, maxHeight);
        final int lowWorldValue = getWorldValue(imageLow, mapping), highWorldValue = getWorldValue(imageHigh, mapping);
        return (mapping.imageLow >= imageMinHeight) && (mapping.imageHigh < imageMaxHeight)
                && (mapping.worldLow >= minHeight) && (mapping.worldHigh < maxHeight)
                && (lowWorldValue >= minHeight) && (highWorldValue < maxHeight)
                && (highWorldValue != lowWorldValue);
    }

    /**
     * Map the specified inputs to image and world values according to this preset.
     *
     * @param imageMaxHeight The theoretical maximum value of the input image.
     * @param imageLow       The lowest value in the input image.
     * @param imageHigh      The highest value in the input image.
     * @param platform       The platform for which the world is being created.
     * @param maxHeight      The selected {@code maxHeight} for the world being created.
     * @return The mapping of image low and high world low and high values.
     */
    public abstract Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight);

    private int getWorldValue(double imageValue, Mapping mapping) {
        return (int) Math.round((imageValue - mapping.imageLow) * mapping.levelScale + mapping.worldLow);
    }

    private final String description;

    public static final ImportPreset FULL_RANGE_0_BASED = new ImportPreset("Full range; from 0") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new Mapping (
                        imageLow, imageHigh,
                        0, maxHeight - 1
                );
            } else {
                return new Mapping (
                        0, imageHigh,
                        0, maxHeight - 1
                );
            }
        }
    };

    public static final ImportPreset FULL_RANGE_MINUS_64_BASED = new ImportPreset("Full range; from -64") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new Mapping (
                        imageLow, imageHigh,
                        -64, maxHeight - 1
                );
            } else {
                return new Mapping (
                        0, imageHigh,
                        -64, maxHeight - 1
                );
            }
        }
    };
    public static final ImportPreset WORLDPAINTER_LOW_RES_0_BASED = new ImportPreset("One to one (e.g. low res WorldPainter export); from 0") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new Mapping (
                        imageLow, Math.min(imageMaxHeight, maxHeight) - 1 + imageLow,
                        0, Math.min((int) Math.round(imageMaxHeight), maxHeight) - 1
                );
            } else {
                return new Mapping (
                        0, Math.min(imageMaxHeight, maxHeight) - 1,
                        0, Math.min((int) Math.round(imageMaxHeight), maxHeight) - 1
                );
            }
        }
    };

    public static final ImportPreset WORLDPAINTER_HIGH_RES_0_BASED = new ImportPreset("256 to one (e.g. high res WorldPainter export); from 0") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                final long worldHighOut = Math.round(imageHigh - imageLow) >> 8;
                long imageHighOut = worldHighOut << 8;
                while ((imageHighOut < (imageMaxHeight - 256)) && ((Math.round(imageHighOut + imageLow) >> 8) < (maxHeight - 1))){
                    imageHighOut += 256;
                }
                return new Mapping (
                        imageLow, imageHighOut + imageLow,
                        0, (int) (imageHighOut >> 8)
                );
            } else {
                long imageHighOut = (long) (maxHeight - 1) << 8;
                while (imageHighOut >= imageMaxHeight) {
                    imageHighOut -= 256;
                }
                return new Mapping (
                        0, imageHighOut,
                        0, (int) (imageHighOut >> 8)
                );
            }
        }
    };

    public static final ImportPreset WORLDPAINTER_LOW_RES_MINUS_64_BASED = new ImportPreset("One to one (e.g. low res WorldPainter export); from -64") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                return new Mapping (
                        imageLow, Math.min(imageMaxHeight, maxHeight) - 1 + imageLow,
                        -64, Math.min((int) Math.round(imageMaxHeight), maxHeight - 64) - 1
                );
            } else {
                return new Mapping (
                        0, Math.min(imageMaxHeight, maxHeight) - 1,
                        -64, Math.min((int) Math.round(imageMaxHeight), maxHeight - 64) - 1
                );
            }
        }
    };

    public static final ImportPreset WORLDPAINTER_HIGH_RES_MINUS_64_BASED = new ImportPreset("256 to one (e.g. high res WorldPainter export); from -64") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            if (imageLow < 0) {
                final long worldHighOut = (Math.round(imageHigh - imageLow) >> 8) - 64;
                long imageHighOut = worldHighOut << 8;
                while ((imageHighOut < (imageMaxHeight - 256)) && ((Math.round(imageHighOut + imageLow) >> 8) < (maxHeight + 63))){
                    imageHighOut += 256;
                }
                return new Mapping (
                        imageLow, imageHighOut + imageLow,
                        -64, (int) ((imageHighOut >> 8) - 64)
                );
            } else {
                long imageHighOut = (long) (maxHeight + 63) << 8;
                while (imageHighOut >= imageMaxHeight) {
                    imageHighOut -= 256;
                }
                return new Mapping (
                        0, imageHighOut,
                        -64, (int) ((imageHighOut >> 8) - 64)
                );
            }
        }
    };

    public static final ImportPreset FLOATING_POINT_MINUS_64_BASED = new ImportPreset("Normalised floating point range; from -64") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            return new Mapping(0.0, 1.0, -64, maxHeight - 1);
        }

        @Override
        public boolean isValid(double imageMinHeight, double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int minHeight, int maxHeight) {
            return (imageLow >= 0.0) && (imageHigh <= 1.0) && super.isValid(imageMinHeight, imageMaxHeight, imageLow, imageHigh, platform, minHeight, maxHeight);
        }
    };

    public static final ImportPreset FLOATING_POINT_0_BASED = new ImportPreset("Normalised floating point range; from 0") {
        @Override
        public Mapping getMapping(double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int maxHeight) {
            return new Mapping(0.0, 1.0, 0, maxHeight - 1);
        }

        @Override
        public boolean isValid(double imageMinHeight, double imageMaxHeight, double imageLow, double imageHigh, Platform platform, int minHeight, int maxHeight) {
            return (imageLow >= 0.0) && (imageHigh <= 1.0) && super.isValid(imageMinHeight, imageMaxHeight, imageLow, imageHigh, platform, minHeight, maxHeight);
        }
    };

    public static final ImportPreset[] PRESETS = { FLOATING_POINT_0_BASED, FLOATING_POINT_MINUS_64_BASED,
            FULL_RANGE_0_BASED, FULL_RANGE_MINUS_64_BASED, WORLDPAINTER_HIGH_RES_0_BASED, WORLDPAINTER_LOW_RES_0_BASED,
            WORLDPAINTER_HIGH_RES_MINUS_64_BASED, WORLDPAINTER_LOW_RES_MINUS_64_BASED };
    
    public static class Mapping {
        public Mapping(double imageLow, double imageHigh, int worldLow, int worldHigh) {
            this.imageLow = imageLow;
            this.imageHigh = imageHigh;
            this.worldLow = worldLow;
            this.worldHigh = worldHigh;
            levelScale = (worldHigh - worldLow) / (imageHigh - imageLow);
        }

        public final double imageLow, imageHigh, levelScale;
        public final int worldLow, worldHigh;
    }
}