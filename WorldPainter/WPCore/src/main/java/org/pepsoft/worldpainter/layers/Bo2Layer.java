/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.bo2.Bo2LayerExporter;
import org.pepsoft.worldpainter.layers.bo2.Bo2ObjectProvider;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author pepijn
 */
public class Bo2Layer extends CustomLayer {
    public Bo2Layer(Bo2ObjectProvider objectProvider, String description, int colour) {
        super(objectProvider.getName(), description, DataSize.NIBBLE, 50, colour);
        this.objectProvider = objectProvider;
    }

    public Bo2ObjectProvider getObjectProvider() {
        return objectProvider;
    }

    public void setObjectProvider(Bo2ObjectProvider objectProvider) {
        this.objectProvider = objectProvider;
        setName(objectProvider.getName());
        setDescription("Custom " + objectProvider.getName() + " objects");
        
        // Legacy
        files = Collections.emptyList();
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        return Bo2LayerExporter.class;
    }

    @Override
    public Bo2LayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new Bo2LayerExporter(dimension, platform, this);
    }

    public int getDensity() {
        return density;
    }

    public void setDensity(int density) {
        this.density = density;
    }

    @Override
    public String getType() {
        return "Custom Objects";
    }

    // Cloneable

    @Override
    public Bo2Layer clone() {
        Bo2Layer clone = (Bo2Layer) super.clone();
        clone.objectProvider = objectProvider.clone();
        return clone;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Legacy support
        if (colour != 0) {
            setColour(colour);
            colour = 0;
        }
        if (density == 0) {
            density = 20;
        }
    }
    
    private Bo2ObjectProvider objectProvider;
    @Deprecated
    private int colour;
    @Deprecated
    private List<File> files = Collections.emptyList();
    private int density = 20;

    private static final long serialVersionUID = 1L;
}