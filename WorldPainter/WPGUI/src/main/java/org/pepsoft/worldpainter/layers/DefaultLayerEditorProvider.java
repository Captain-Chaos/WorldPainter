/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.layers.bo2.Bo2LayerEditor;
import org.pepsoft.worldpainter.layers.combined.CombinedLayerEditor;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayer;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayerEditor;
import org.pepsoft.worldpainter.layers.plants.PlantLayer;
import org.pepsoft.worldpainter.layers.plants.PlantLayerEditor;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.LayerEditorProvider;

/**
 *
 * @author Pepijn Schmitz
 */
public class DefaultLayerEditorProvider extends AbstractPlugin implements LayerEditorProvider {
    public DefaultLayerEditorProvider() {
        super("DefaultLayerEditorProvider", Version.VERSION);
    }

    @SuppressWarnings("unchecked") // Responsibility of implementations
    @Override
    public <L extends Layer> LayerEditor<L> createLayerEditor(Class<L> layerType) {
        if (Bo2Layer.class.isAssignableFrom(layerType)) {
            return (LayerEditor<L>) new Bo2LayerEditor();
        } else if (CombinedLayer.class.isAssignableFrom(layerType)) {
            return (LayerEditor<L>) new CombinedLayerEditor();
        } else if (GroundCoverLayer.class.isAssignableFrom(layerType)) {
            return (LayerEditor<L>) new GroundCoverLayerEditor();
        } else if (PlantLayer.class.isAssignableFrom(layerType)) {
            return (LayerEditor<L>) new PlantLayerEditor();
        } else {
            return null;
        }
    }
}