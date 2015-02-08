package org.pepsoft.worldpainter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.pepsoft.minecraft.Material;

import static org.pepsoft.minecraft.Material.AIR;

/**
 * @author SchmitzP
 */
public class CustomTerrainHelper {
    public CustomTerrainHelper(int index) {
        this.index = index;
    }

    public Material getMaterial(long seed, int x, int y, float z, int height) {
        final float dz = z - height;
        if (dz >= 0.5f) {
            return AIR;
        } else {
            return Terrain.customMaterials[index].getMaterial(seed, x, y, z);
        }
    }
    
    public Material getMaterial(long seed, int x, int y, int z, int height) {
        final int dz = z - height;
        if (dz > 0) {
            return AIR;
        } else {
            return Terrain.customMaterials[index].getMaterial(seed, x, y, z);
        }
    }

    public BufferedImage getIcon(ColourScheme colourScheme) {
        MixedMaterial material = Terrain.customMaterials[index];
        return (material != null) ? material.getIcon(colourScheme) : UNKNOWN_ICON;
    }

    public int getDefaultBiome() {
        return Terrain.customMaterials[index].getBiome();
    }
    
    public boolean isConfigured() {
        return Terrain.customMaterials[index] != null;
    }

    public String getName() {
        MixedMaterial material = Terrain.customMaterials[index];
        return (material != null) ? material.getName() : "Custom " + (index + 1);
    }
    
    public int getCustomTerrainIndex() {
        return index;
    }

    public int getColour(long seed, int x, int y, float z, int height, ColourScheme colourScheme) {
        MixedMaterial material = Terrain.customMaterials[index];
        Integer colour = (material != null) ? material.getColour() : null;
        return (colour != null) ? colour : colourScheme.getColour(getMaterial(seed, x, y, z, height));
    }
    
    public int getColour(long seed, int x, int y, int z, int height, ColourScheme colourScheme) {
        MixedMaterial material = Terrain.customMaterials[index];
        Integer colour = (material != null) ? material.getColour() : null;
        return (colour != null) ? colour : colourScheme.getColour(getMaterial(seed, x, y, z, height));
    }
    
    private final int index;
    
    private static final BufferedImage UNKNOWN_ICON;
    
    static {
        try {
            // If we're being loaded for some kind of headless library the image
            // may not be there
            InputStream iconStream = CustomTerrainHelper.class.getResourceAsStream("/org/pepsoft/worldpainter/icons/unknown_pattern.png");
            UNKNOWN_ICON = (iconStream != null) ? ImageIO.read(iconStream) : null;
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading unknown_pattern.png from classpath", e);
        }
    }
}