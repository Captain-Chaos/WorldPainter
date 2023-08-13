/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pepijn Schmitz
 */
public class CustomItemsTreeModel implements TreeModel {
    public CustomItemsTreeModel(World2 world, ItemType itemType) {
        this.world = world;
        if ((itemType == ItemType.ALL) || (itemType == ItemType.TERRAIN)) {
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                MixedMaterial material = world.getMixedMaterial(i);
                if (material != null) {
                    customTerrains.add(material);
                }
            }
            if (! customTerrains.isEmpty()) {
                childrenOfRoot.add(TERRAINS);
            }
        }
        for (Dimension dim: world.getDimensions()) {
            processDimension(dim, itemType);
        }
        if (! customLayers.isEmpty()) {
            childrenOfRoot.add(LAYERS);
        }
        if (! customBiomes.isEmpty()) {
            childrenOfRoot.add(BIOMES);
        }
    }

    public List<Object> getSelectedItems(TreePath[] selectedTreePaths) {
        ArrayList<Object> selectedItems = new ArrayList<>();
        for (TreePath selectedPath: selectedTreePaths) {
            Object object = selectedPath.getLastPathComponent();
            if (object == ROOT) {
                selectedItems.addAll(customTerrains);
                selectedItems.addAll(customLayers);
                selectedItems.addAll(customBiomes);
            } else if (object == TERRAINS) {
                selectedItems.addAll(customTerrains);
            } else if (object == LAYERS) {
                selectedItems.addAll(customLayers);
            } else if (object == BIOMES) {
                selectedItems.addAll(customBiomes);
            } else if ((object instanceof CustomLayer) || (object instanceof MixedMaterial) || (object instanceof CustomBiome)) {
                selectedItems.add(object);
            } else {
                throw new InternalError("Unknown node type " + object.getClass() + " (\"" + object + "\") encountered");
            }
        }
        return selectedItems;
    }
    
    public static boolean hasCustomItems(World2 world, ItemType itemType) {
        if ((itemType == ItemType.ALL) || (itemType == ItemType.TERRAIN)) {
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                MixedMaterial material = world.getMixedMaterial(i);
                if (material != null) {
                    return true;
                }
            }
        }
        for (Dimension dim: world.getDimensions()) {
            if (((itemType == ItemType.ALL) || (itemType == ItemType.LAYER)) && (! dim.getCustomLayers().isEmpty()) && dim.getCustomLayers().stream().anyMatch(Layer::isExportableToFile)) {
                return true;
            }
            if (((itemType == ItemType.ALL) || (itemType == ItemType.BIOME)) && ((dim.getCustomBiomes() != null) && (! dim.getCustomBiomes().isEmpty()))) {
                return true;
            }
        }
        return false;
    }
    
    // TreeModel
    
    @Override
    public Object getRoot() {
        return ROOT;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == ROOT) {
            return childrenOfRoot.get(index);
        } else if (parent == TERRAINS) {
            return customTerrains.get(index);
        } else if (parent == LAYERS) {
            return customLayers.get(index);
        } else if (parent == BIOMES) {
            return customBiomes.get(index);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == ROOT) {
            return childrenOfRoot.size();
        } else if (parent == TERRAINS) {
            return customTerrains.size();
        } else if (parent == LAYERS) {
            return customLayers.size();
        } else if (parent == BIOMES) {
            return customBiomes.size();
        } else {
            // JIDE's CheckBoxTree insists on calling us with nodes that didn't
            // come from us
            return 0;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return ! ((node == ROOT) || (node == TERRAINS) || (node == LAYERS) || (node == BIOMES));
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == ROOT) {
            return childrenOfRoot.indexOf(child);
        } else if (parent == TERRAINS) {
            return customTerrains.indexOf(child);
        } else if (parent == LAYERS) {
            return customLayers.indexOf(child);
        } else if (parent == BIOMES) {
            return customBiomes.indexOf(child);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // Do nothing
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // Do nothing
    }
    
    private void processDimension(Dimension dim, ItemType itemType) {
        if ((itemType == ItemType.ALL) || (itemType == ItemType.LAYER)) {
            dim.getCustomLayers().stream().filter(Layer::isExportableToFile).forEach(customLayers::add);
        }
        if ((itemType == ItemType.ALL) || (itemType == ItemType.BIOME)) {
            if (dim.getCustomBiomes() != null) {
                customBiomes.addAll(dim.getCustomBiomes());
            }
        }
    }
    
    private final World2 world;
    private final List<CustomLayer> customLayers = new ArrayList<>();
    private final List<MixedMaterial> customTerrains = new ArrayList<>();
    private final List<MixedMaterial> customMaterials = new ArrayList<>();
    private final List<CustomBiome> customBiomes = new ArrayList<>();
    private final List<String> childrenOfRoot = new ArrayList<>();
    
    private static final String ROOT = "Custom Items", LAYERS = "Custom Layers", TERRAINS = "Custom Terrain", BIOMES = "Custom Biomes";

    public enum ItemType {ALL, LAYER, TERRAIN, BIOME}
}