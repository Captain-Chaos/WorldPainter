/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Coordinates loading, saving and editing of custom biomes.
 * 
 * @author pepijn
 */
public class CustomBiomeManager {
    public List<CustomBiome> getCustomBiomes() {
        return customBiomes;
    }

    public void setCustomBiomes(List<CustomBiome> customBiomes) {
        List<CustomBiome> oldCustomBiomes = this.customBiomes;
        this.customBiomes = customBiomes;
        if (oldCustomBiomes != null) {
            for (CustomBiome customBiome: oldCustomBiomes) {
                for (CustomBiomeListener listener: listeners) {
                    listener.customBiomeRemoved(customBiome);
                }
            }
        }
        if (customBiomes != null) {
            for (CustomBiome customBiome: customBiomes) {
                for (CustomBiomeListener listener: listeners) {
                    listener.customBiomeAdded(customBiome);
                }
            }
        }
    }

    public int getNextId() {
        if (customBiomes != null) {
            outer:
            for (int i = 40; i < 256; i++) {
                for (CustomBiome customBiome : customBiomes) {
                    if (customBiome.getId() == i) {
                        continue outer;
                    }
                }
                if (isBiomePresent(i)) {
                    continue;
                }
                return i;
            }
            return - 1;
        } else {
            return 40;
        }
    }

    public boolean addCustomBiome(Window parent, CustomBiome customBiome) {
        if (isBiomePresent(customBiome.getId())) {
            if (parent != null) {
                JOptionPane.showMessageDialog(parent, "The specified ID (" + customBiome.getId() + ") is already a regular biome (named " + Minecraft1_13Biomes.BIOME_NAMES[customBiome.getId()] + ")", "ID Already In Use", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
        if (customBiomes == null) {
            customBiomes = new ArrayList<>();
        }
        for (CustomBiome existingCustomBiome: customBiomes) {
            if (existingCustomBiome.getId() == customBiome.getId()) {
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent, "You already configured a custom biome with that ID (named " + existingCustomBiome.getName() + ")", "ID Already In Use", JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        }
        customBiomes.add(customBiome);
        for (CustomBiomeListener listener: listeners) {
            listener.customBiomeAdded(customBiome);
        }
        return true;
    }
    
    /**
     * Indicates that some aspect of a custom biome (other than the ID, which is
     * not allowed to change) has changed.
     * 
     * @param customBiome The custom biome that has been modified.
     */
    public void editCustomBiome(CustomBiome customBiome) {
        for (CustomBiome existingCustomBiome: customBiomes) {
            if (existingCustomBiome.getId() == customBiome.getId()) {
                for (CustomBiomeListener listener: listeners) {
                    listener.customBiomeChanged(customBiome);
                }
                return;
            }
        }
        throw new IllegalArgumentException("There is no custom biome installed with ID " + customBiome.getId());
    }
    
    /**
     * Removes a custom biome.
     * 
     * @param customBiome The custom biome to remove.
     */
    public void removeCustomBiome(CustomBiome customBiome) {
        for (Iterator<CustomBiome> i = customBiomes.iterator(); i.hasNext(); ) {
            CustomBiome existingCustomBiome = i.next();
            if (existingCustomBiome.getId() == customBiome.getId()) {
                i.remove();
                for (CustomBiomeListener listener: listeners) {
                    listener.customBiomeRemoved(customBiome);
                }
                return;
            }
        }
        throw new IllegalArgumentException("There is no custom biome installed with ID " + customBiome.getId());
    }
    
    public void addListener(CustomBiomeListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(CustomBiomeListener listener) {
        listeners.remove(listener);
    }

    private static boolean isBiomePresent(int biome) {
        return (biome <= Minecraft1_13Biomes.HIGHEST_BIOME_ID) && (Minecraft1_13Biomes.BIOME_NAMES[biome] != null);
    }

    private final List<CustomBiomeListener> listeners = new ArrayList<>();
    private List<CustomBiome> customBiomes;
    
    public interface CustomBiomeListener {
        void customBiomeAdded(CustomBiome customBiome);
        void customBiomeChanged(CustomBiome customBiome);
        void customBiomeRemoved(CustomBiome customBiome);
    }
}