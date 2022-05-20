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
import org.pepsoft.worldpainter.layers.trees.SwampLandExporter;
import org.pepsoft.worldpainter.layers.trees.SwampTree;
import org.pepsoft.worldpainter.layers.trees.TreeType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

/**
 *
 * @author pepijn
 */
public class SwampLand extends TreeLayer {
    private SwampLand() {
        super("Swamp", "swamp land", 42, 'w');
    }

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        return SwampLandExporter.class;
    }

    @Override
    public SwampLandExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new SwampLandExporter(dimension, platform, settings, this);
    }
    
    @Override
    public TreeType pickTree(Random random) {
        return SWAMP_TREE;
    }

    @Override
    public int getDefaultMaxWaterDepth() {
        return 1;
    }

    @Override
    public int getDefaultTreeChance() {
        return 7680;
    }

    @Override
    public int getDefaultMushroomIncidence() {
        return 10;
    }

    @Override
    public float getDefaultMushroomChance() {
        return PerlinNoise.getLevelForPromillage(200);
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
    
    public static final SwampLand INSTANCE = new SwampLand();
    
    private static final SwampTree SWAMP_TREE = new SwampTree();
    private static final long serialVersionUID = 1L;
}