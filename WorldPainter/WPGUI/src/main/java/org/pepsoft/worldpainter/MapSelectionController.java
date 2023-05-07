package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Operation;
import org.pepsoft.worldpainter.tools.Eyedropper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.beans.PropertyVetoException;

import static java.awt.Color.BLACK;

class MapSelectionController {
    MapSelectionController(App app, WorldPainter view) {
        this.app = app;
        this.view = view;
        eyedropper = new Eyedropper(view, app.getColourScheme(), app.getCustomBiomeManager());
    }

    /**
     * Temporarily selects the eyedropper tool to let the user select a paint from the map.
     *
     * @param paintSelectionListener The listener which will be invoked with the selected terrain or layer, if the user
     *                               does not press Esc or select a different tool first.
     */
    void selectPaintOnMap(Eyedropper.SelectionListener paintSelectionListener) {
        if (this.paintSelectionListener != null) {
            throw new IllegalStateException("Paint selection already in progress");
        }
        this.paintSelectionListener = paintSelectionListener;
        final JLabel label = new JLabel("<html><font size='+1'>Click on the map to select a paint.<br>Press Esc to cancel.</font></html>");
        label.setBorder(new CompoundBorder(new LineBorder(BLACK), new EmptyBorder(5, 5, 5, 5)));
        app.setGlassPaneComponent(label);
        final Operation activeOperation = app.getActiveOperation();
        if (activeOperation != null) {
            try {
                activeOperation.setActive(false);
            } catch (PropertyVetoException e) {
                logger.error("Property veto exception while deactivating operation " + activeOperation, e);
            }
        }
        paintSelectionDrawBrushWasActive = view.isDrawBrush();
        if (paintSelectionDrawBrushWasActive) {
            view.setDrawBrush(false);
        }
        eyedropper.setCallback(new Eyedropper.SelectionListener() {
            @Override
            public void terrainSelected(Terrain terrain) {
                cancelPaintSelection(false, false);
                paintSelectionListener.terrainSelected(terrain);
            }

            @Override
            public void layerSelected(Layer layer, int value) {
                cancelPaintSelection(false, false);
                paintSelectionListener.layerSelected(layer, value);
            }

            @Override public void selectionCancelled(boolean byUser) {}
        });
        try {
            eyedropper.setActive(true);
        } catch (PropertyVetoException e) {
            throw new RuntimeException("Property veto exception while activating eyedropper", e);
        }
    }

    void cancelPaintSelection(boolean notifyListener, boolean cancelledByUser) {
        if (paintSelectionListener != null) {
            Eyedropper.SelectionListener paintSelectionListener = this.paintSelectionListener;
            this.paintSelectionListener = null;
            app.removeGlassPaneComponent();
            eyedropper.setCallback(null);
            try {
                eyedropper.setActive(false);
            } catch (PropertyVetoException e) {
                logger.error("Property veto exception while deactivating eyedropper", e);
            }
            if (paintSelectionDrawBrushWasActive) {
                view.setDrawBrush(true);
            }
            final Operation activeOperation = app.getActiveOperation();
            if (activeOperation != null) {
                try {
                    activeOperation.setActive(true);
                } catch (PropertyVetoException e) {
                    throw new RuntimeException("Property veto exception while activating " + activeOperation, e);
                }
            }
            if (notifyListener) {
                paintSelectionListener.selectionCancelled(cancelledByUser);
            }
        }
    }

    boolean isSelectionActive() {
        return paintSelectionListener != null;
    }

    private final App app;
    private final WorldPainter view;
    private final Eyedropper eyedropper;
    private Eyedropper.SelectionListener paintSelectionListener;
    private boolean paintSelectionDrawBrushWasActive;

    private static final Logger logger = LoggerFactory.getLogger(MapSelectionController.class);
}