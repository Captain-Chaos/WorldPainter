/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import org.pepsoft.minecraft.Material;

/**
 *
 * @author pepijn
 */
public class Theme implements Serializable {
    public Theme(Material floorMaterial, Material beamMaterial, Material wallMaterial, Material roofMaterial, Material windowMaterial, Material interiorWallMaterial) {
        this.floorMaterial = floorMaterial;
        this.beamMaterial = beamMaterial;
        this.wallMaterial = wallMaterial;
        this.roofMaterial = roofMaterial;
        this.windowMaterial = windowMaterial;
        this.interiorWallMaterial = interiorWallMaterial;
    }
    
    public BufferedImage getPreview(BufferedImage texturePack) {
        if (preview == null) {
            constructPreview(texturePack);
        }
        return preview;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Theme other = (Theme) obj;
        if (this.floorMaterial != other.floorMaterial && (this.floorMaterial == null || !this.floorMaterial.equals(other.floorMaterial))) {
            return false;
        }
        if (this.beamMaterial != other.beamMaterial && (this.beamMaterial == null || !this.beamMaterial.equals(other.beamMaterial))) {
            return false;
        }
        if (this.wallMaterial != other.wallMaterial && (this.wallMaterial == null || !this.wallMaterial.equals(other.wallMaterial))) {
            return false;
        }
        if (this.roofMaterial != other.roofMaterial && (this.roofMaterial == null || !this.roofMaterial.equals(other.roofMaterial))) {
            return false;
        }
        if (this.windowMaterial != other.windowMaterial && (this.windowMaterial == null || !this.windowMaterial.equals(other.windowMaterial))) {
            return false;
        }
        if (this.interiorWallMaterial != other.interiorWallMaterial && (this.interiorWallMaterial == null || !this.interiorWallMaterial.equals(other.interiorWallMaterial))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + (this.floorMaterial != null ? this.floorMaterial.hashCode() : 0);
        hash = 61 * hash + (this.beamMaterial != null ? this.beamMaterial.hashCode() : 0);
        hash = 61 * hash + (this.wallMaterial != null ? this.wallMaterial.hashCode() : 0);
        hash = 61 * hash + (this.roofMaterial != null ? this.roofMaterial.hashCode() : 0);
        hash = 61 * hash + (this.windowMaterial != null ? this.windowMaterial.hashCode() : 0);
        hash = 61 * hash + (this.interiorWallMaterial != null ? this.interiorWallMaterial.hashCode() : 0);
        return hash;
    }
    
    private void constructPreview(BufferedImage texturePack) {
        BufferedImage tmp = new BufferedImage(9 * 8, 5 * 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        g2.scale(0.5, 0.5);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        try {
            // Floor
            for (int x = 1; x < 8; x++) {
                floorMaterial.paintImage(g2, x * 16, 4 * 16, texturePack);
            }
            
            // Beams
            for (int y = 1; y < 5; y++) {
                beamMaterial.paintImage(g2, 0, y * 16, texturePack);
                beamMaterial.paintImage(g2, 8 * 16, y * 16, texturePack);
            }
            
            // Interior wall
            for (int y = 1; y < 4; y++) {
                interiorWallMaterial.paintImage(g2, 4 * 16, y * 16, texturePack);
            }
            
            // Walls and windows
            for (int dx = 0; dx < 3; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    if ((dx == 1) && (dy == 1)) {
                        // Window
                        windowMaterial.paintImage(g2, (1 + dx) * 16, (1 + dy) * 16, texturePack);
                        windowMaterial.paintImage(g2, (5 + dx) * 16, (1 + dy) * 16, texturePack);
                    } else {
                        // Wall
                        wallMaterial.paintImage(g2, (1 + dx) * 16, (1 + dy) * 16, texturePack);
                        wallMaterial.paintImage(g2, (5 + dx) * 16, (1 + dy) * 16, texturePack);
                    }
                }
            }
            
            // Roof
            for (int x = 0; x < 9; x++) {
                roofMaterial.paintImage(g2, x * 16, 0, texturePack);
            }
        } finally {
            g2.dispose();
        }
        preview = tmp;
    }
    
    public final Material floorMaterial, beamMaterial, wallMaterial, roofMaterial, windowMaterial, interiorWallMaterial;
    
    private transient BufferedImage preview;
    
    private static final long serialVersionUID = 1L;
}
