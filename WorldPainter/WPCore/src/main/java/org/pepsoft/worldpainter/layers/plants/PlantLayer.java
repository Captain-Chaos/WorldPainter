/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.bo2.Bo2ObjectProvider;
import org.pepsoft.worldpainter.objects.WPObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.layers.plants.Plants.ALL_PLANTS;

/**
 *
 * @author pepijn
 */
public class PlantLayer extends CustomLayer {
    public PlantLayer(String name, String description, int colour) {
        super(name, description, DataSize.BIT, 35, colour);
    }
    
    public PlantSettings getSettings(int plantIndex) {
        return settings[plantIndex];
    }
    
    public void setSettings(int plantIndex, PlantSettings settings) {
        this.settings[plantIndex] = settings;
    }

    public boolean isGenerateFarmland() {
        return generateTilledDirt;
    }

    public void setGenerateFarmland(boolean generateFarmland) {
        this.generateTilledDirt = generateFarmland;
    }

    public Bo2ObjectProvider getObjectProvider(Platform platform) {
        int total = 0;
        for (PlantSettings setting: settings) {
            if (setting != null) {
                total += setting.occurrence;
            }
        }
        final int[] pool = new int[total], growthOffset = new int[total], growthRange = new int[total];
        int index = 0;
        for (int i = 0; i < settings.length; i++) {
            if (settings[i] != null) {
                for (int j = 0; j < settings[i].occurrence; j++) {
                    growthOffset[index] = settings[i].growthFrom;
                    growthRange[index] = (byte) (settings[i].growthTo - settings[i].growthFrom);
                    pool[index++] = i;
                }
            }
        }
        return new Bo2ObjectProvider() {
            @Override
            public String getName() {
                return PlantLayer.this.getName();
            }

            @Override
            public WPObject getObject() {
                final int index = random.nextInt(pool.length);
                final Plant plant = ALL_PLANTS[pool[index]];
                if (growthRange[index] == 0) {
                    return plant.realise(growthOffset[index], platform);
                } else {
                    return plant.realise(growthOffset[index] + random.nextInt(growthRange[index] + 1), platform);
                }
            }

            @Override
            public List<WPObject> getAllObjects() {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public void setSeed(long seed) {
                random.setSeed(seed);
            }

            /**
             * This is an ephemeral implementation, not meant for storage, there
             * is no need for it to be cloneable.
             *
             * @throws UnsupportedOperationException Always.
             */
            @Override
            public Bo2ObjectProvider clone() {
                throw new UnsupportedOperationException("Not supported");
            }

            private final Random random = new Random();
        };
    }
    
    // Layer
    
    @Override
    public PlantLayerExporter getExporter() {
        return new PlantLayerExporter(this);
    }

    // Cloneable

    @Override
    public PlantLayer clone() {
        PlantLayer clone = (PlantLayer) super.clone();
        clone.settings = new PlantSettings[settings.length];
        for (int i = 0; i < settings.length; i++) {
            clone.settings[i] = settings[i].clone();
        }
        return clone;
    }

    @SuppressWarnings("deprecation") // Legacy migration
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Legacy
        if (settings.length < ALL_PLANTS.length) {
            // (A) new plant(s) has been added
            PlantSettings[] newSettings = new PlantSettings[ALL_PLANTS.length];
            System.arraycopy(settings, 0, newSettings, 0, settings.length);
            settings = newSettings;
        }

        if (version < 1) {
            for (int i = 0; i < settings.length; i++) {
                if (settings[i] != null) {
                    if (ALL_PLANTS[i].material.isNamedOneOf(MC_CARROTS, MC_POTATOES)) {
                        settings[i].growthFrom = (settings[i].dataValueFrom == 3) ? 8 : (settings[i].dataValueFrom * 2 + 1);
                        settings[i].growthTo = (settings[i].dataValueTo == 3) ? 8 : (settings[i].dataValueTo * 2 + 1);
                    } else if (ALL_PLANTS[i].material.isNamed(MC_NETHER_WART)) {
                        settings[i].growthFrom = (settings[i].dataValueFrom > 0) ? (settings[i].dataValueFrom + 2) : 1;
                        settings[i].growthTo = (settings[i].dataValueTo > 0) ? (settings[i].dataValueTo + 2) : 1;
                    } else {
                        settings[i].growthFrom = settings[i].dataValueFrom + 1;
                        settings[i].growthTo = settings[i].dataValueTo + 1;
                    }
                    settings[i].dataValueFrom = -1;
                    settings[i].dataValueTo = -1;
                }
            }
        }
        version = 1;
    }

    public static class PlantSettings implements Serializable, Cloneable {
        @Override
        public PlantSettings clone() {
            try {
                return (PlantSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        short occurrence;
        @Deprecated
        byte dataValueFrom = -1, dataValueTo = -1;
        int growthFrom, growthTo;
        
        private static final long serialVersionUID = 1L;
    }
    
    private PlantSettings[] settings = new PlantSettings[ALL_PLANTS.length];
    private boolean generateTilledDirt = true; // Now referred to as generateFarmland
    private int version = 1;

    private static final long serialVersionUID = -2758775044863488107L;
}