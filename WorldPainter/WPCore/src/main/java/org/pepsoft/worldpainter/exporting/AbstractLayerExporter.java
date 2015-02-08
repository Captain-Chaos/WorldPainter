/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

/**
 *
 * @author pepijn
 */
public abstract class AbstractLayerExporter<L extends Layer> implements LayerExporter<L> {
    @SuppressWarnings("unchecked")
    public AbstractLayerExporter(L layer, ExporterSettings<L> defaultSettings) {
        this.layer = layer;
        this.defaultSettings = defaultSettings;
        settings = defaultSettings.clone();
    }

    public AbstractLayerExporter(L layer) {
        this.layer = layer;
        this.defaultSettings = null;
    }
    
    @Override
    public final L getLayer() {
        return layer;
    }
    
    public final ExporterSettings<L> getSettings() {
        return settings;
    }

    @Override @SuppressWarnings("unchecked")
    public void setSettings(ExporterSettings<L> settings) {
        if (settings != null) {
            this.settings = settings;
        } else {
            this.settings = (defaultSettings != null) ? defaultSettings.clone() : null;
        }
    }
    
    protected final L layer;
    private final ExporterSettings<L> defaultSettings;
    private ExporterSettings<L> settings;
}