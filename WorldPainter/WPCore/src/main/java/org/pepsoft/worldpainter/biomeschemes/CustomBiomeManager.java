/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
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
                if (autoBiomeScheme.isBiomePresent(i)) {
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
        if (autoBiomeScheme.isBiomePresent(customBiome.getId())) {
            JOptionPane.showMessageDialog(parent, "The specified ID (" + customBiome.getId() + ") is already a regular biome (named " + autoBiomeScheme.getBiomeName(customBiome.getId()) + ")", "ID Already In Use", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (customBiomes == null) {
            customBiomes = new ArrayList<CustomBiome>();
        }
        for (CustomBiome existingCustomBiome: customBiomes) {
            if (existingCustomBiome.getId() == customBiome.getId()) {
                JOptionPane.showMessageDialog(parent, "You already configured a custom biome with that ID (named " + existingCustomBiome.getName() + ")", "ID Already In Use", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        customBiomes.add(customBiome);
        for (CustomBiomeListener listener: listeners) {
            listener.customBiomeAdded(customBiome);
        }
        return true;
    }
    
    public void editCustomBiome(CustomBiome customBiome) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public void removeCustomBiome() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public void addListener(CustomBiomeListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(CustomBiomeListener listener) {
        listeners.remove(listener);
    }
    
    private final AutoBiomeScheme autoBiomeScheme = new AutoBiomeScheme(null);
    private List<CustomBiome> customBiomes;
    private List<CustomBiomeListener> listeners = new ArrayList<CustomBiomeListener>();
    
    public interface CustomBiomeListener {
        void customBiomeAdded(CustomBiome customBiome);
        void customBiomeChanged(CustomBiome customBiome);
        void customBiomeRemoved(CustomBiome customBiome);
    }
}