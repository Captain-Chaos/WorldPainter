/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Frost;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.MC_SNOW;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.*;

/**
 *
 * @author pepijn
 */
public class FrostExporter extends AbstractLayerExporter<Frost> implements SecondPassLayerExporter {
    public FrostExporter() {
        super(Frost.INSTANCE, new FrostSettings());
    }
    
    @Override
    public List<Fixup> render(final Dimension dimension, final Rectangle area, final Rectangle exportedArea, final MinecraftWorld minecraftWorld, Platform platform) {
        final FrostSettings settings = (FrostSettings) getSettings();
        final boolean frostEverywhere = settings.isFrostEverywhere();
        final int mode = settings.getMode();
        final boolean snowUnderTrees = settings.isSnowUnderTrees();
        final int maxHeight = dimension.getMaxHeight();
        final Random random = new Random(); // Only used for random snow height, so it's not a big deal if it's different every time
        String customNoSnowOnIds = System.getProperty("org.pepsoft.worldpainter.noSnowOn");
        if ((customNoSnowOnIds != null) && (! customNoSnowOnIds.trim().isEmpty())) {
            throw new IllegalArgumentException("The org.pepsoft.worldpainter.noSnowOn property is no longer supported; please let the author know if you need it");
        }
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                if (frostEverywhere || dimension.getBitLayerValueAt(Frost.INSTANCE, x, y)) {
                    int highestNonAirBlock = minecraftWorld.getHighestNonAirBlock(x, y);
                    Material previousMaterial = minecraftWorld.getMaterialAt(x, y, Math.min(minecraftWorld.getHighestNonAirBlock(x, y) + 1, maxHeight - 2));
                    int leafBlocksEncountered = 0;
                    for (int height = Math.min(highestNonAirBlock, maxHeight - 2); height >= 0; height--) {
                        Material material = minecraftWorld.getMaterialAt(x, y, height);
                        if (material.name.endsWith("_leaves")
                                || (material.solid
                                    && material.opaque)) {
                            if (material.isNamed(MC_WATER)) {
                                minecraftWorld.setMaterialAt(x, y, height, ICE);
                                break;
                            } else if ((material.name.endsWith("_leaves"))
                                    || (material.name.endsWith("_log"))
                                    || (material.name.endsWith("_bark"))) {
                                if (previousMaterial == AIR) {
                                    minecraftWorld.setMaterialAt(x, y, height + 1, SNOW);
                                }
                                leafBlocksEncountered++;
                                if ((! snowUnderTrees) && (leafBlocksEncountered > 1)) {
                                    break;
                                }
                            } else {
                                // Obliterate tall grass, 'cause there is too
                                // much of it, and leaving it in would look
                                // strange. Also replace existing snow, as we
                                // might want to place thicker snow
                                if ((previousMaterial == AIR) || (previousMaterial == GRASS) || (previousMaterial == FERN) || (previousMaterial == SNOW)) {
                                    if ((mode == FrostSettings.MODE_SMOOTH_AT_ALL_ELEVATIONS)
                                            || (height == dimension.getIntHeightAt(x, y))) {
                                        // Only vary the snow thickness if we're
                                        // at surface height, otherwise it looks
                                        // odd
                                        switch (mode) {
                                            case FrostSettings.MODE_FLAT:
                                                placeSnow(minecraftWorld, x, y, height, 1);
                                                break;
                                            case FrostSettings.MODE_RANDOM:
                                                placeSnow(minecraftWorld, x, y, height, random.nextInt(3) + 1);
                                                break;
                                            case FrostSettings.MODE_SMOOTH:
                                            case FrostSettings.MODE_SMOOTH_AT_ALL_ELEVATIONS:
                                                int layers = (int) ((dimension.getHeightAt(x, y) + 0.5f - dimension.getIntHeightAt(x, y)) / 0.125f) + 1;
                                                if ((layers > 1) && (! frostEverywhere)) {
                                                    layers = Math.max(Math.min(layers, dimension.getBitLayerCount(Frost.INSTANCE, x, y, 1) - 1), 1);
                                                }
                                                placeSnow(minecraftWorld, x, y, height, layers);
                                                break;
                                        }
                                    } else {
                                        // At other elevations just place a
                                        // regular thin snow block
                                        placeSnow(minecraftWorld, x, y, height, 1);
                                    }
                                }
                                break;
                            }
                        } else {
                            previousMaterial = material;
                            continue;
                        }
                        previousMaterial = material;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Place a snow block with a specific thickness, but only if thicker snow is
     * not already present.
     */
    private void placeSnow(MinecraftWorld minecraftWorld, int x, int y, int height, int layers) {
        if ((layers < 1) || (layers > 8)) {
            throw new IllegalArgumentException("layers " + layers);
        }
        Material existingMaterial = minecraftWorld.getMaterialAt(x, y, height + 1);
        if (existingMaterial.isNamed(MC_SNOW)) {
            // If there is already snow there, don't lower it
            layers = Math.max(layers, existingMaterial.getProperty(LAYERS));
        }
        minecraftWorld.setMaterialAt(x, y, height + 1, SNOW.withProperty(LAYERS, layers));
    }

    public static class FrostSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return frostEverywhere;
        }

        @Override
        public Frost getLayer() {
            return Frost.INSTANCE;
        }

        public boolean isFrostEverywhere() {
            return frostEverywhere;
        }

        public void setFrostEverywhere(boolean frostEverywhere) {
            this.frostEverywhere = frostEverywhere;
        }

        public int getMode() {
            return mode;
        }

        public void setMode(int mode) {
            this.mode = mode;
        }

        public boolean isSnowUnderTrees() {
            return snowUnderTrees;
        }

        public void setSnowUnderTrees(boolean snowUnderTrees) {
            this.snowUnderTrees = snowUnderTrees;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FrostSettings other = (FrostSettings) obj;
            if (this.frostEverywhere != other.frostEverywhere) {
                return false;
            }
            if (this.mode != other.mode) {
                return false;
            }
            if (this.snowUnderTrees != other.snowUnderTrees) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + (this.frostEverywhere ? 1 : 0);
            hash = 23 * hash + mode;
            hash = 23 * hash + (this.snowUnderTrees ? 1 : 0);
            return hash;
        }

        @Override
        public FrostSettings clone() {
            try {
                return (FrostSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        
        private boolean frostEverywhere;
        private int mode = MODE_SMOOTH;
        private boolean snowUnderTrees = true;
        
        public static final int MODE_FLAT                     = 0; // Always place thin snow blocks
        public static final int MODE_RANDOM                   = 1; // Place random height snow blocks on the surface
        public static final int MODE_SMOOTH                   = 2; // Place smooth snow blocks on the surface
        public static final int MODE_SMOOTH_AT_ALL_ELEVATIONS = 3; // Place smooth snow blocks at any elevation
        
        private static final long serialVersionUID = 2011060801L;
    }
}
