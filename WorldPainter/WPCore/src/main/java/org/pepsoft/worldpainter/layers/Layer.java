/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public abstract class Layer implements Serializable, Comparable<Layer> {
    @Deprecated
    protected Layer(String name, String description, DataSize dataSize, int priority) {
        this(name, name, description, dataSize, priority, '\0');
    }

    @Deprecated
    protected Layer(String name, String description, DataSize dataSize, int priority, char mnemonic) {
        this(name, name, description, dataSize, priority, mnemonic);
    }

    protected Layer(String id, String name, String description, DataSize dataSize, int priority) {
        this(id, name, description, dataSize, priority, '\0');
    }
    
    protected Layer(String id, String name, String description, DataSize dataSize, int priority, char mnemonic) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.dataSize = dataSize;
        this.priority = priority;
        this.mnemonic = mnemonic;
        init();
    }
    
    public final DataSize getDataSize() {
        return dataSize;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    /**
     * Create a new exporter for this layer.
     *
     * @return A new exporter for this layer.
     */
    public LayerExporter getExporter() {
        if (exporterClass == null) {
            // Layer has no default exporter
            return null;
        } else {
            try {
                //noinspection unchecked Responsibility of implementor
                return exporterClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("Instantiation exception while instantiating exporter for layer " + name, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied while instantiating exporter for layer " + name, e);
            }
        }
    }
    
    public LayerRenderer getRenderer() {
        return renderer;
    }
    
    public BufferedImage getIcon() {
        return icon;
    }
    
    public char getMnemonic() {
        return mnemonic;
    }

    public int getPriority() {
        return priority;
    }

    public String getId() {
        return id;
    }

    /**
     * The default value which will be returned wherever the layer has not been
     * set. By default this is zero (or false).
     * 
     * @return The default value which will be returned where the layer value is
     *     not set.
     */
    public int getDefaultValue() {
        return 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Layer)
            && id.equals(((Layer) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    // Comparable

    @Override
    public int compareTo(Layer layer) {
        if (priority < layer.priority) {
            return -1;
        } else if (priority > layer.priority) {
            return 1;
        } else {
            return id.compareTo(layer.id);
        }
    }

    @SuppressWarnings("unchecked")
    private void init() {
        Class<? extends Layer> clazz = getClass();
        ClassLoader pluginClassLoader = PluginManager.getPluginClassLoader();
        try {
            LayerRenderer myRenderer;
            try {
                myRenderer = (LayerRenderer) pluginClassLoader.loadClass(clazz.getPackage().getName() + ".renderers." + clazz.getSimpleName() + "Renderer").newInstance();
            } catch (ClassNotFoundException | InstantiationException e) {
                // This most likely means the class does not exist
                myRenderer = null;
            }
            renderer = myRenderer;
            Class<LayerExporter> myExporterClass;
            try {
                myExporterClass = (Class<LayerExporter>) pluginClassLoader.loadClass(clazz.getPackage().getName() + ".exporters." + clazz.getSimpleName() + "Exporter");
            } catch (ClassNotFoundException e) {
                myExporterClass = null;
            }
            exporterClass = myExporterClass;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access denied while creating renderer for layer " + name, e);
        }
        icon = IconUtils.loadScaledImage(clazz.getClassLoader(), "org/pepsoft/worldpainter/icons/" + getClass().getSimpleName().toLowerCase() + ".png");
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Legacy
        if (id == null) {
            id = name;
        }
        
        init();
    }

    private String name, description;
    public final DataSize dataSize;
    public final int priority;
    protected String id;
    private transient LayerRenderer renderer;
    private transient Class<LayerExporter> exporterClass;
    private transient BufferedImage icon;
    private transient char mnemonic;

    private static final long serialVersionUID = 2011032901L;

    public enum DataSize {BIT, NIBBLE, BYTE, BIT_PER_CHUNK, NONE}
}
