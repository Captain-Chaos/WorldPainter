/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;
import javax.swing.Action;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.layers.renderers.TransparentColourRenderer;

/**
 *
 * @author pepijn
 */
public abstract class CustomLayer extends Layer {
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
     * @return Any custom actions for this layer, or <code>null</code> if there
     * aren't any.
     */
    public List<Action> getActions() {
        return null;
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
        version = CURRENT_VERSION;
    }
    
    private static String createId(String name) {
        return name + "." + Long.toHexString(ID_GENERATOR.nextLong());
    }
    
    private int colour, biome = -1, version = CURRENT_VERSION;
    private boolean hide;
    private String palette = "Custom Layers";
    private transient BufferedImage icon;
    private transient LayerRenderer renderer;

    public static final String KEY_DIMENSION = "org.pepsoft.worldpainter.dimension";
    
    private static final int CURRENT_VERSION = 2;
    private static final Random ID_GENERATOR = new Random();
    private static final long serialVersionUID = 1L;
}