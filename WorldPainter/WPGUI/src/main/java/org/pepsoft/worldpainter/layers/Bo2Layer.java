/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import org.pepsoft.worldpainter.layers.bo2.Bo2ObjectProvider;
import org.pepsoft.worldpainter.layers.bo2.Bo2LayerExporter;

/**
 *
 * @author pepijn
 */
public class Bo2Layer extends CustomLayer {
    public Bo2Layer(Bo2ObjectProvider objectProvider, int colour) {
        super(objectProvider.getName(), "Custom " + objectProvider.getName() + " objects in bo2 format", DataSize.NIBBLE, 50, colour);
        this.objectProvider = objectProvider;
        exporter = new Bo2LayerExporter(this);
    }

    public Bo2ObjectProvider getObjectProvider() {
        return objectProvider;
    }

    public void setObjectProvider(Bo2ObjectProvider objectProvider) {
        this.objectProvider = objectProvider;
        setName(objectProvider.getName());
        setDescription("Custom " + objectProvider.getName() + " objects in bo2 format");
        
        // Legacy
        files = Collections.emptyList();;
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    public Bo2LayerExporter getExporter() {
        return exporter;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        exporter = new Bo2LayerExporter(this);
        
        // Legacy support
        if (colour != 0) {
            setColour(colour);
            colour = 0;
        }
    }
    
    private Bo2ObjectProvider objectProvider;
    @Deprecated
    private int colour;
    @Deprecated
    private List<File> files = Collections.emptyList();
    private transient Bo2LayerExporter exporter;
    
    private static final long serialVersionUID = 1L;
}