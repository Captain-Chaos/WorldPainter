package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

import static org.pepsoft.minecraft.Constants.COLOUR_NAMES;
import static org.pepsoft.minecraft.Material.WOOLS;
import static org.pepsoft.util.IconUtils.createScaledColourIcon;
import static org.pepsoft.util.IconUtils.scaleIcon;

/**
 * A one-click operation that shows a list of paints where the user has clicked and allows the user to select one. Once
 * the selection is made, one of the methods on the configured {@link SelectionListener callback} is invoked to inform
 * the client of the selected paint.
 *
 * <p>A {@link #setCallback(SelectionListener) callback} <em>must</em> be configured or this operation will throw an
 * exception.
 */
public final class Eyedropper extends MouseOrTabletOperation {
    public Eyedropper(WorldPainterView view, ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        super("Eyedropper", "Select a paint from the map", view, "operation.eyedropper");
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
    }

    public SelectionListener getCallback() {
        return callback;
    }

    public void setCallback(SelectionListener callback) {
        this.callback = callback;
    }

    @Override
    protected void tick(int x, int y, boolean inverse, boolean first, float dynamicLevel) {
        if (! first) {
            throw new InternalError("Should never happen");
        }
        final Dimension dimension = getDimension();
        final Terrain terrain = dimension.getTerrainAt(x, y);
        final Map<Layer, Integer> layers = dimension.getLayersAt(x, y);
        if ((terrain == null) && (layers == null)) {
            DesktopUtils.beep();
        } else {
            // Conscious choice to always show the popup menu, even if there is only one choice, to always give feedback
            // to the user as to exactly what value was selected
            final JPopupMenu menu = new JPopupMenu();
            if (terrain != null) {
                menu.add(new AbstractAction(terrain.getName(), new ImageIcon(terrain.getScaledIcon(16, colourScheme))) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        callback.terrainSelected(terrain);
                    }
                });
            }
            if (layers != null) {
                final BiomeHelper biomeHelper = new BiomeHelper(colourScheme, customBiomeManager, dimension.getWorld().getPlatform());
                layers.forEach((layer, value) -> {
                    final String name;
                    final Icon icon;
                    if (layer instanceof Biome) {
                        name = biomeHelper.getBiomeName(value);
                        icon = biomeHelper.getBiomeIcon(value);
                    } else if (layer instanceof Annotations) {
                        final int colourIndex = value - ((value < 8) ? 1 : 0);
                        name = COLOUR_NAMES[colourIndex] + " Annotations";
                        icon = createScaledColourIcon(colourScheme.getColour(WOOLS[colourIndex]));
                    } else if (! layer.discrete) {
                        name = layer.getName();
                        icon = new ImageIcon(scaleIcon(layer.getIcon(), 16));
                    } else {
                        throw new UnsupportedOperationException("Discrete layer " + layer + " not supported");
                    }
                    menu.add(new AbstractAction(name, icon) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            callback.layerSelected(layer, value);
                        }
                    });
                });
            }
            final WorldPainterView view = getView();
            final Point menuCoords = view.worldToView(x, y);
            menu.show(view, menuCoords.x, menuCoords.y);
        }
    }

    private final ColourScheme colourScheme;
    private final CustomBiomeManager customBiomeManager;
    private SelectionListener callback;

    /**
     * A selection callback that will be notified of the selected value, or if the operation was cancelled.
     */
    public interface SelectionListener {
        /**
         * The user has selected the specified terrain type.
         */
        void terrainSelected(Terrain terrain);

        /**
         * The user has selected the specified layer, which had the specified value at the selected location.
         */
        void layerSelected(Layer layer, int value);

        /**
         * The selection operation has been cancelled.
         *
         * @param byUser Whether the selection was cancelled by an affirmative action by the user (such as pressing Esc)
         *               or as a secondary effect (such as selecting a different tool, loading a different dimension,
         *               etc.)
         */
        void selectionCancelled(boolean byUser);
    }
}