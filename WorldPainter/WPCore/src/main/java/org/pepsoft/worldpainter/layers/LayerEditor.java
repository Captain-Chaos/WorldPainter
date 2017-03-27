/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import java.util.List;
import javax.swing.JComponent;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

/**
 * An editor of {@link Layer} settings.
 * 
 * @author Pepijn Schmitz
 * @param <L> The layer for which this is the editor.
 */
public interface LayerEditor<L extends Layer> {
    /**
     * Get the actual layer editor component. Must be a singleton which is
     * backed by this <code>LayerEditor</code>, in other words it should be
     * possible to reuse the instance by invoking the other methods on the
     * <code>LayerEditor</code> to set a layer, etc.
     * 
     * @return The actual layer editor component.
     */
    JComponent getComponent();
    
    /**
     * Create a new layer with default settings.
     * 
     * @return A new instance of this editors layer type, with default settings.
     */
    L createLayer();
    
    /**
     * Get the layer currently being edited.
     * 
     * @return The layer currently being edited.
     */
    L getLayer();
 
    /**
     * Set the layer to be edited. The layer's settings should be loaded into
     * any previously returned editor component from {@link #getComponent()}.
     * 
     * @param layer The layer to be edited.
     */
    void setLayer(L layer);
    
    /**
     * Save the current settings from the editor component to the layer.
     * <em>Until</em> this is called no changes must be made to the layer, even
     * if the user changes settings on the editor component!
     * 
     * @throws IllegalStateException If invoked when the settings are incomplete
     *     or invalid, as indicated by {@link #isCommitAvailable()}.
     */
    void commit();
    
    /**
     * Discard the current settings from the editor component and reload the
     * settings from the layer being edited.
     */
    void reset();
    
    /**
     * Get a copy of the current settings from the editor component
     * <em>without</em> saving them or applying them to the layer.
     * 
     * @return A copy of the current settings from the editor component.
     * @throws IllegalStateException If invoked when the settings are incomplete
     *     or invalid, as indicated by {@link #isCommitAvailable()}.
     */
    ExporterSettings getSettings();
    
    /**
     * Indicates whether the current settings are valid and complete and can
     * therefore be committed. If not, a {@link #commit()} and
     * {@link #getSettings()} will result in an {@link IllegalStateException}.
     * 
     * @return <code>true</code> if the current settings are valid and complete
     *     and can be committed.
     */
    boolean isCommitAvailable();
    
    /**
     * Set the context from which the layer editor must obtains its context
     * information and to which it must report events. This will be invoked with
     * a suitable context before any of the other methods are invoked.
     *
     * @param context The context to set.
     */
    void setContext(LayerEditorContext context);

    /**
     * A context from which the layer editor may obtain context information and
     * to which it may report events.
     */
    interface LayerEditorContext {
        /**
         * Get the current colour scheme.
         * 
         * @return The current colour scheme.
         */
        ColourScheme getColourScheme();

        /**
         * Indicates whether to support extended (12-bit) block IDs. If the
         * layer editor supports selecting block IDs anywhere and this returns
         * <code>true</code> then it should allow selection of IDs up to and
         * including 4095.
         * 
         * @return <code>true</code> if extended block IDs must be supported.
         */
        boolean isExtendedBlockIds();
        
        /**
         * Get the current custom biome manager.
         * 
         * @return The current custom biome manager.
         */
        CustomBiomeManager getCustomBiomeManager();
        
        /**
         * Get a list with all currently available layers.
         * 
         * @return A list with all currently available layers.
         */
        List<Layer> getAllLayers();
        
        /**
         * The layer editor must invoke this whenever the settings on the layer
         * editor component have changed, for instance because a new layer has
         * been loaded, the user has made a change, or the settings have been
         * reset.
         */
        void settingsChanged();

        /**
         * Get the dimension currently loaded in the editor.
         *
         * @return The dimension currently loaded in the editor.
         */
        Dimension getDimension();
    }
}