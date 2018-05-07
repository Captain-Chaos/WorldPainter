package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.pepsoft.minecraft.Material.AIR;

/**
 * @author SchmitzP
 */
public class CustomTerrainHelper {
    public CustomTerrainHelper(int index) {
        this.index = index;
    }

    public Material getMaterial(long seed, int x, int y, float z, int height) {
        if ((z - height) >= 0.5f) {
            return AIR;
        } else {
            MixedMaterial material = Terrain.customMaterials[index];
            if (material != null) {
                return material.getMaterial(seed, x, y, z);
            } else {
                throw new MissingCustomTerrainException("Custom terrain " + index + " not configured", index);
            }
        }
    }
    
    public Material getMaterial(long seed, int x, int y, int z, int height) {
        if ((z - height) > 0) {
            return AIR;
        } else {
            MixedMaterial material = Terrain.customMaterials[index];
            if (material != null) {
                return material.getMaterial(seed, x, y, z);
            } else {
                throw new MissingCustomTerrainException("Custom terrain " + index + " not configured", index);
            }
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
        Integer colour = (material != null) ? material.getColour() : UNKNOWN_COLOUR;
        return (colour != null) ? colour : colourScheme.getColour(getMaterial(seed, x, y, z, height));
    }
    
    public int getColour(long seed, int x, int y, int z, int height, ColourScheme colourScheme) {
        MixedMaterial material = Terrain.customMaterials[index];
        Integer colour = (material != null) ? material.getColour() : UNKNOWN_COLOUR;
        return (colour != null) ? colour : colourScheme.getColour(getMaterial(seed, x, y, z, height));
    }
    
    private final int index;
    
    private static final BufferedImage UNKNOWN_ICON;
    private static final Integer UNKNOWN_COLOUR = 0xff00ff; // Magenta
    
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