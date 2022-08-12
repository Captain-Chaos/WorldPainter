/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.TreesExporter;
import org.pepsoft.worldpainter.layers.trees.TreeType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

/**
 *
 * @author pepijn
 */
public abstract class TreeLayer extends Layer {
    protected TreeLayer(String treeName, String treeDescription, int priority, char mnemonic) {
        super(treeName, "Generate " + treeDescription, DataSize.NIBBLE, false, priority, mnemonic);
    }

    protected TreeLayer(String treeName, String treeDescription, int priority) {
        super(treeName, "Generate " + treeDescription, DataSize.NIBBLE, false, priority);
    }

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        return TreesExporter.class;
    }

    @Override
    public TreesExporter<? extends TreeLayer> getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new TreesExporter<>(dimension, platform, settings, this);
    }
    
    public int getDefaultMaxWaterDepth() {
        return 0;
    }

    public int getDefaultTreeChance() {
        return 1280;
    }

    public int getDefaultMushroomIncidence() {
        return MUSHROOM_INCIDENCE;
    }

    public float getDefaultMushroomChance() {
        return MUSHROOM_CHANCE;
    }
    
    public int getDefaultLayerStrengthCap() {
        return Integer.MAX_VALUE;
    }
        
    public abstract TreeType pickTree(Random random);
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
    
    private static final int MUSHROOM_INCIDENCE = 25;
    private static final float MUSHROOM_CHANCE = PerlinNoise.getLevelForPromillage(10);
    private static final long serialVersionUID = 2011032901L;
}