/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;
import java.util.List;

/**
 *
 * @author pepijn
 */
public class AnnotationsExporter extends AbstractLayerExporter<Annotations> implements SecondPassLayerExporter {
    public AnnotationsExporter() {
        super(Annotations.INSTANCE);
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        AnnotationsSettings settings = (AnnotationsSettings) getSettings();
        if (settings == null) {
            settings = new AnnotationsSettings();
        }
        if (! settings.isExport()) {
            return null;
        }
        final int maxHeight = minecraftWorld.getMaxHeight() - 1;
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                final int value = dimension.getLayerValueAt(Annotations.INSTANCE, x, y);
                if (value > 0) {
                    final int height = dimension.getIntHeightAt(x, y);
                    final Material existingMaterial = minecraftWorld.getMaterialAt(x, y, height + 1);
                    if ((height < maxHeight) && (existingMaterial.veryInsubstantial || existingMaterial == Material.ICE)) {
                        minecraftWorld.setMaterialAt(x, y, height + 1, Material.get(Constants.BLK_WOOL, value - ((value < 8) ? 1 : 0)));
                    }
                }
            }
        }
        return null;
    }
    
    public static class AnnotationsSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return false;
        }

        @Override
        public Annotations getLayer() {
            return Annotations.INSTANCE;
        }

        public boolean isExport() {
            return export;
        }

        public void setExport(boolean export) {
            this.export = export;
        }

        public String getDefaultFont() {
            return defaultFont;
        }

        public void setDefaultFont(String defaultFont) {
            this.defaultFont = defaultFont;
        }

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + (this.export ? 1 : 0);
            hash = 41 * hash + (this.defaultFont != null ? this.defaultFont.hashCode() : 0);
            hash = 41 * hash + this.defaultSize;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AnnotationsSettings other = (AnnotationsSettings) obj;
            if (this.export != other.export) {
                return false;
            }
            if ((this.defaultFont == null) ? (other.defaultFont != null) : !this.defaultFont.equals(other.defaultFont)) {
                return false;
            }
            if (this.defaultSize != other.defaultSize) {
                return false;
            }
            return true;
        }

        @Override
        public ExporterSettings clone() {
            try {
                return (ExporterSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError();
            }
        }
        
        private boolean export;
        private String defaultFont = "Lucida Sans";
        private int defaultSize = 18;
    }
}