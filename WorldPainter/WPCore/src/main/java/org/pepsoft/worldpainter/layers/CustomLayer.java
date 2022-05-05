/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.layers.renderers.TransparentColourRenderer;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;

/**
 *
 * @author pepijn
 */
public abstract class CustomLayer extends Layer implements Cloneable {
    public CustomLayer(String name, String description, DataSize dataSize, int priority, int colour) {
        super(createId(name), name, description, dataSize, priority);
        this.colour = colour;
        renderer = new TransparentColourRenderer(colour);
    }

    public CustomLayer(String name, String description, DataSize dataSize, int priority, char mnemonic, int colour) {
        super(createId(name), name, description, dataSize, priority, mnemonic);
        this.colour = colour;
        renderer = new TransparentColourRenderer(colour);
    }

    /**
     * Custom layers have names independent of their ID, so changing the name
     * after creation is not a problem.
     * 
     * @param name The new name of the layer.
     */
    @Override
    public void setName(String name) {
        super.setName(name);
    }
    
    public int getColour() {
        return colour;
    }
    
    public void setColour(int colour) {
        if (colour != this.colour) {
            this.colour = colour;
            icon = null;
            renderer = new TransparentColourRenderer(colour);
        }
    }

    public int getBiome() {
        return biome;
    }

    public void setBiome(int biome) {
        this.biome = biome;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public String getPalette() {
        return palette;
    }

    public void setPalette(String palette) {
        this.palette = palette;
    }

    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * This is invoked by WorldPainter when the dimension height is transformed (minHeight and/or maxHeight changes,
     * and/or a shift and/or scaling operation applied), so that the layer settings may be adjusted accordingly, if
     * applicable.
     *
     * <p>The default implementation does nothing.
     */
    public void setMinMaxHeight(int oldMinHeight, int newMinHeight, int oldMaxHeight, int newMaxHeight, HeightTransform transform) {
        // Do nothing
    }

    /**
     * Get a short, human-readable description of the layer type.
     */
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public BufferedImage getIcon() {
        if (icon == null) {
            icon = createIcon();
        }
        return icon;
    }
    
    @Override
    public LayerRenderer getRenderer() {
        return renderer;
    }
    
    /**
     * Get any actions (in addition to the standard add, edit and remove
     * actions) for this custom layer. They will be added to the popup menu of
     * the layer button.
     *
     * <p>The {@link #KEY_DIMENSION} value of the action will be set to the
     * current dimension, if any, before the action is invoked.
     * 
     * @return Any custom actions for this layer, or {@code null} if there
     * aren't any.
     */
    public List<Action> getActions() {
        return null;
    }

    // Comparable
    
    @Override
    public int compareTo(Layer layer) {
        if ((index != null) && (layer instanceof CustomLayer) && (((CustomLayer) layer).index != null)) {
            // TODO: is this stable?
            if (index < ((CustomLayer) layer).index) {
                return -1;
            } else if (index > ((CustomLayer) layer).index) {
                return 1;
            }
        }
        return super.compareTo(layer);
    }

    // Cloneable

    /**
     * Create a deep copy of the custom layer, with a different ID and
     * independent settings.
     *
     * @return A deep copy of the custom layer.
     */
    @Override
    public CustomLayer clone() {
        try {
            CustomLayer clone = (CustomLayer) super.clone();
            clone.id = createId(getName());
            clone.hide = false;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedImage createIcon() {
        BufferedImage iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 1; x < 15; x++) {
            for (int y = 1; y < 15; y++) {
                if ((x == 1) || (x == 14) || (y == 1) || (y == 14)) {
                    iconImage.setRGB(x, y, 0xFF000000);
                } else {
                    iconImage.setRGB(x, y, 0xFF000000 | colour);
                }
            }
        }
        return iconImage;
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        renderer = new TransparentColourRenderer(colour);
        if (version < 1) {
            biome = -1;
        }
        if (version < 2) {
            palette = "Custom Layers";
        }
        if (version < 3) {
            export = true;
        }
        version = CURRENT_VERSION;
    }
    
    private static String createId(String name) {
        return name + "." + Long.toHexString(ID_GENERATOR.nextLong());
    }
    
    private int colour, biome = -1, version = CURRENT_VERSION;
    private boolean hide, export = true;
    private String palette = "Custom Layers";
    private Integer index = null;
    private transient BufferedImage icon;
    private transient LayerRenderer renderer;

    public static final String KEY_DIMENSION = "org.pepsoft.worldpainter.dimension";
    
    private static final int CURRENT_VERSION = 3;
    private static final Random ID_GENERATOR = new Random();
    private static final long serialVersionUID = 1L;
}