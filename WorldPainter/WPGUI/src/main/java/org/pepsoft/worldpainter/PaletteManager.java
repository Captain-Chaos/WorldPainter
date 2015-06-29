/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Manages a set of layers, including managing palettes, layer buttons, etc.
 * 
 * @author SchmitzP
 */
public class PaletteManager {
    public PaletteManager(ButtonProvider buttonProvider) {
        this.buttonProvider = buttonProvider;
    }

    public List<Palette> getPalettes() {
        return Collections.unmodifiableList(paletteList);
    }

    public List<CustomLayer> getLayers() {
        List<CustomLayer> layers = new ArrayList<>();
        for (Palette palette: paletteList) {
            layers.addAll(palette.getLayers());
        }
        return layers;
    }

    public Map<String, Collection<CustomLayer>> getLayersByPalette() {
        Map<String, Collection<CustomLayer>> layers = new HashMap<>(paletteList.size());
        for (Palette palette: paletteList) {
            layers.put(palette.getName(), palette.getLayers());
        }
        return layers;
    }

    public boolean contains(Layer layer) {
        for (Palette palette: paletteList) {
            if (palette.contains(layer)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return paletteList.isEmpty();
    }

    public Palette getPalette(String name) {
        return palettesByName.get(name);
    }
    
    public Palette create(String name) {
        if (palettesByName.containsKey(name)) {
            throw new IllegalStateException("There is already a palette named \"" + name + "\"");
        }
        Palette palette = new Palette(name, buttonProvider.createPopupMenuButton(name));
        paletteList.add(palette);
        palettesByName.put(name, palette);
        return palette;
    }

    /**
     * Register a custom layer. It is added to the palette whose name is stored
     * in the layer. If there is no such palette, a new one is created and
     * returned.
     * 
     * @param layer The layer to register.
     */
    public Palette register(CustomLayer layer) {
        Palette palette = getPaletteContaining(layer);
        boolean paletteCreated = false;
        if (palette == null) {
            palette = new Palette(layer.getPalette(), buttonProvider.createPopupMenuButton(layer.getPalette()));
            paletteCreated = true;
            paletteList.add(palette);
            palettesByName.put(layer.getPalette(), palette);
        }
        palette.add(layer, buttonProvider.createCustomLayerButton(layer));
        return paletteCreated ? palette : null;
    }
    
    public Palette getPaletteContaining(CustomLayer layer) {
        return palettesByName.get(layer.getPalette());
    }

    public void activate(CustomLayer layer) {
        getPaletteContaining(layer).activate(layer);
    }
    
    public void deactivate(CustomLayer layer) {
        getPaletteContaining(layer).deactivate(layer);
    }
    
    public Palette move(CustomLayer layer, Palette destPalette) {
        Palette palette = getPaletteContaining(layer);
        List<Component> button = palette.remove(layer);
        destPalette.add(layer, button);
        layer.setPalette(destPalette.getName());
        return palette;
    }
    
    /**
     * Unregister a custom layer. It is removed from the palette it is currently
     * on.
     * 
     * @param layer The layer to remove.
     * @return The palette the layer was on.
     */
    public Palette unregister(CustomLayer layer) {
        Palette palette = getPaletteContaining(layer);
        palette.remove(layer);
        return palette;
    }

    public void delete(Palette palette) {
        paletteList.remove(palette);
        palettesByName.remove(palette.getName());
    }
    
    /**
     * Removes all palettes. Returns the list of removed palettes.
     * 
     * @return The removed palettes;
     */
    public List<Palette> clear() {
        List<Palette> oldPaletteList = new ArrayList<>(paletteList);
        paletteList.clear();
        palettesByName.clear();
        return oldPaletteList;
    }
    
    private final List<Palette> paletteList = new ArrayList<>();
    private final Map<String, Palette> palettesByName = new HashMap<>();
    private final ButtonProvider buttonProvider;

    public interface ButtonProvider {
        List<Component> createCustomLayerButton(CustomLayer layer);
        List<Component> createPopupMenuButton(String paletteName);
    }
}