/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

/**
 *
 * @author pepijn
 */
public final class DefaultFilter implements Filter {
    public DefaultFilter(Dimension dimension, boolean inSelection, boolean outsideSelection, int aboveLevel, int belowLevel, boolean feather, Object onlyOn, Object exceptOn, int aboveDegrees, boolean slopeIsAbove) {
        this.dimension = dimension;
        this.inSelection = inSelection;
        this.outsideSelection = outsideSelection;
        if (inSelection && outsideSelection) {
            throw new IllegalArgumentException("inSelection and outsideSelection are mutually exclusive");
        }
        this.aboveLevel = aboveLevel;
        this.belowLevel = belowLevel;
        if (aboveLevel != Integer.MIN_VALUE) {
            checkLevel = true;
            if (belowLevel != Integer.MIN_VALUE) {
                // Above and below are checked
                if (belowLevel >= aboveLevel) {
                    levelType = LevelType.BETWEEN;
                } else {
                    levelType = LevelType.OUTSIDE;
                }
            } else {
                // Only above checked
                levelType = LevelType.ABOVE;
            }
        } else if (belowLevel != Integer.MIN_VALUE) {
            // Only below checked
            checkLevel = true;
            levelType = LevelType.BELOW;
        } else {
            // Neither checked
            checkLevel = false;
            levelType = null;
        }
        this.feather = feather;
        if (onlyOn != null) {
            this.onlyOn = true;
            onlyOnFilter = new OnlyOnTerrainOrLayerFilter(dimension, onlyOn);
        } else {
            this.onlyOn = false;
            onlyOnFilter = null;
        }
        if (exceptOn != null) {
            this.exceptOn = true;
            exceptOnFilter = new ExceptOnTerrainOrLayerFilter(dimension, exceptOn);
        } else {
            this.exceptOn = false;
            exceptOnFilter = null;
        }
        this.degrees = aboveDegrees;
        checkSlope = aboveDegrees >= 0;
        if (checkSlope) {
            this.slope = (float) Math.tan(aboveDegrees / (180 / Math.PI));
    //        System.out.println(degrees + "° -> " + slope);
        } else {
            slope = 0.0f;
        }
        this.slopeIsAbove = slopeIsAbove;
    }

    public int getAboveLevel() {
        return aboveLevel;
    }

    public int getBelowLevel() {
        return belowLevel;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public boolean isInSelection() {
        return inSelection;
    }

    public Layer getOnlyOnLayer() {
        return (onlyOnFilter != null) ? onlyOnFilter.getLayer() : null;
    }

    public Layer getExceptOnLayer() {
        return (exceptOnFilter != null) ? exceptOnFilter.getLayer() : null;
    }

    public static Builder buildForDimension(Dimension dimension) {
        return new Builder(dimension);
    }

    // Filter
    
    @Override
    public float modifyStrength(int x, int y, float strength) {
        if (strength > 0.0f) {
            if (inSelection && (! dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y)) && (! dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y))) {
                return 0.0f;
            } else if (outsideSelection && (dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y) || dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y))) {
                return 0.0f;
            }
            if (exceptOn && (exceptOnFilter.modifyStrength(x, y, strength) <= 0.0f)) {
                return 0.0f;
            }
            if (onlyOn && (onlyOnFilter.modifyStrength(x, y, strength) <= 0.0f)) {
                return 0.0f;
            }
            if (checkLevel) {
                final int terrainLevel = dimension.getIntHeightAt(x, y);
                switch (levelType) {
                    case ABOVE:
                        if (terrainLevel < aboveLevel) {
                            return feather ? Math.max((1 - (aboveLevel - terrainLevel) / 4.0f) * strength, 0.0f) : 0.0f;
                        }
                        break;
                    case BELOW:
                        if (terrainLevel > belowLevel) {
                            return feather ? Math.max((1 - (terrainLevel - belowLevel) / 4.0f) * strength, 0.0f) : 0.0f;
                        }
                        break;
                    case BETWEEN:
                        if ((terrainLevel < aboveLevel) || (terrainLevel > belowLevel)) {
                            return feather ? Math.max(Math.min((1 - (aboveLevel - terrainLevel) / 4.0f), (1 - (terrainLevel - belowLevel) / 4.0f)) * strength, 0.0f) : 0.0f;
                        }
                        break;
                    case OUTSIDE:
                        if ((terrainLevel > belowLevel) && (terrainLevel < aboveLevel)) {
                            return feather ? Math.max(Math.max((1 - (terrainLevel - belowLevel) / 4.0f), (1 - (aboveLevel - terrainLevel) / 4.0f)) * strength, 0.0f) : 0.0f;
                        }
                        break;
                }
            }
            if (checkSlope) {
                float terrainSlope = dimension.getSlope(x, y);
                if (slopeIsAbove ? (terrainSlope < slope) : (terrainSlope > slope)) {
                    return 0.0f;
                }
            }
            return strength;
        } else {
            return 0.0f;
        }
    }

    // Object

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DefaultFilter{");
        if (inSelection) {
            sb.append("in selection");
        } else if (outsideSelection) {
            sb.append("outside selection");
        }
        if (onlyOn) {
            if (inSelection || outsideSelection) {
                sb.append(" and ");
            }
            sb.append("only on ");
            sb.append(onlyOnFilter);
        }
        if (exceptOn) {
            if (inSelection || outsideSelection || onlyOn) {
                sb.append(' ');
            }
            sb.append("except on ");
            sb.append(exceptOnFilter);
        }
        if (checkLevel) {
            if (inSelection || outsideSelection || onlyOn || exceptOn) {
                sb.append(" and ");
            }
            sb.append("height ");
            switch (levelType) {
                case ABOVE:
                    sb.append("at or above ").append(aboveLevel);
                    break;
                case BELOW:
                    sb.append("at or below ").append(belowLevel);
                    break;
                case BETWEEN:
                    sb.append("between ").append(aboveLevel).append(" and ").append(belowLevel);
                    break;
                case OUTSIDE:
                    sb.append("not between ").append(aboveLevel).append(" and ").append(belowLevel);
                    break;
            }
        }
        if (checkSlope) {
            if (inSelection || outsideSelection || onlyOn || exceptOn || checkLevel) {
                sb.append(" and ");
            }
            sb.append("gradient ").append(slopeIsAbove ? "above " : "below ").append(slope);
        }
        sb.append('}');
        return sb.toString();
    }


    final boolean checkLevel, onlyOn, exceptOn, feather, checkSlope,
            slopeIsAbove, inSelection, outsideSelection;
    final LevelType levelType;
    final int aboveLevel, belowLevel, degrees;
    final float slope;
    final OnlyOnTerrainOrLayerFilter onlyOnFilter;
    final ExceptOnTerrainOrLayerFilter exceptOnFilter;
    Dimension dimension;

    public enum LevelType {
        BETWEEN, OUTSIDE, ABOVE, BELOW
    }

    public enum Condition {
        EQUAL, LOWER_THAN_OR_EQUAL, HIGHER_THAN_OR_EQUAL
    }

    public static class LayerValue {
        public LayerValue(Layer layer) {
            this.layer = layer;
            value = -1;
            condition = null;
        }

        public LayerValue(Layer layer, int value) {
            this(layer, value, Condition.EQUAL);
        }

        public LayerValue(Layer layer, int value, Condition condition) {
            switch (layer.getDataSize()) {
                case BIT_PER_CHUNK:
                case BIT:
                    if ((value < -1) || (value > 1)) {
                        throw new IllegalArgumentException("value " + value);
                    }
                    break;
                case NIBBLE:
                    if ((value < -15) || (value > 15)) {
                        throw new IllegalArgumentException("value " + value);
                    }
                    break;
                case BYTE:
                    if ((value < -255) || (value > 255)) {
                        throw new IllegalArgumentException("value " + value);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Data size " + layer.getDataSize() + " not supported");
            }
            this.layer = layer;
            this.value = value;
            this.condition = condition;
        }

        public final Layer layer;
        public final int value;
        public final Condition condition;
    }

    public static class Builder {
        public Builder(Dimension dimension) {
            this.dimension = dimension;
        }

        public Builder inSelection() {
            inSelection = true;
            return this;
        }

        public Builder outsideSelection() {
            outsideSelection = true;
            return this;
        }

        public Builder aboveLevel(int level) {
            aboveLevel = level;
            return this;
        }

        public Builder belowLevel(int level) {
            belowLevel = level;
            return this;
        }

        public Builder betweenLevels(int aboveLevel, int belowLevel) {
            this.aboveLevel = aboveLevel;
            this.belowLevel = belowLevel;
            return this;
        }

        public Builder feather() {
            feather = true;
            return this;
        }

        public Builder onlyOn(Object item) {
            onlyOn = item;
            return this;
        }

        public Builder exceptOn(Object item) {
            exceptOn = item;
            return this;
        }

        public Builder slopeIsAbove(int degrees) {
            aboveDegrees = degrees;
            slopeIsAbove = true;
            return this;
        }

        public Builder slopeIsBelow(int degrees) {
            aboveDegrees = degrees;
            slopeIsAbove = false;
            return this;
        }

        public DefaultFilter build() {
            return new DefaultFilter(dimension, inSelection, outsideSelection, aboveLevel, belowLevel, feather, onlyOn, exceptOn, aboveDegrees, slopeIsAbove);
        }

        private final Dimension dimension;
        private boolean inSelection, outsideSelection, feather, slopeIsAbove;
        private int aboveLevel, belowLevel, aboveDegrees;
        private Object onlyOn, exceptOn;
    }
}