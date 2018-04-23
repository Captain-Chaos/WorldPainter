/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.bo2.Bo2ObjectProvider;
import org.pepsoft.worldpainter.objects.WPObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

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

    public boolean isGenerateTilledDirt() {
        return generateTilledDirt;
    }

    public void setGenerateTilledDirt(boolean generateTilledDirt) {
        this.generateTilledDirt = generateTilledDirt;
    }

    public Bo2ObjectProvider getObjectProvider() {
        int total = 0;
        for (PlantSettings setting: settings) {
            if (setting != null) {
                total += setting.occurrence;
            }
        }
        final byte[] pool = new byte[total], growthOffset = new byte[total], growthRange = new byte[total];
        int index = 0;
        for (byte i = 0; i < settings.length; i++) {
            if (settings[i] != null) {
                for (short j = 0; j < settings[i].occurrence; j++) {
                    growthOffset[index] = settings[i].dataValueFrom;
                    growthRange[index] = (byte) (settings[i].dataValueTo - settings[i].dataValueFrom);
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
                final Plant plant = Plant.ALL_PLANTS[pool[index]];
                if (growthRange[index] == 0) {
                    return plant.withGrowth(growthOffset[index]);
                } else {
                    return plant.withGrowth(growthOffset[index] + random.nextInt(growthRange[index] + 1));
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Legacy
        if (settings.length < Plant.ALL_PLANTS.length) {
            // (A) new plant(s) has been added
            PlantSettings[] newSettings = new PlantSettings[Plant.ALL_PLANTS.length];
            System.arraycopy(settings, 0, newSettings, 0, settings.length);
            settings = newSettings;
        }
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
        byte dataValueFrom, dataValueTo;
        
        private static final long serialVersionUID = 1L;
    }
    
    private PlantSettings[] settings = new PlantSettings[Plant.ALL_PLANTS.length];
    private boolean generateTilledDirt = true;

    private static final long serialVersionUID = -2758775044863488107L;
}