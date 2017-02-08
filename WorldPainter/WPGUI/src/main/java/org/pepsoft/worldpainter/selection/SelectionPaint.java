package org.pepsoft.worldpainter.selection;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.painting.AbstractPaint;

import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A type of paint which manipulates the {@link SelectionChunk} and
 * {@link SelectionBlock} layers.
 *
 * <p>Created by Pepijn Schmitz on 31-01-17.
 */
public class SelectionPaint extends AbstractPaint {
    @Override
    public void apply(Dimension dimension, int x, int y, float dynamicLevel) {
        if (dimension != this.dimension) {
            this.dimension = dimension;
            selectionHelper = new SelectionHelper(dimension);
        }
        try {
            selectionHelper.addToSelection(x, y, brush, filter, dynamicLevel, null);
        } catch (ProgressReceiver.OperationCancelled operationCancelled) {
            // Can't happen since we don't pass in a progress receiver
            throw new InternalError();
        }
    }

    @Override
    public void remove(Dimension dimension, int x, int y, float dynamicLevel) {
        if (dimension != this.dimension) {
            this.dimension = dimension;
            selectionHelper = new SelectionHelper(dimension);
        }
        try {
            selectionHelper.removeFromSelection(x, y, brush, filter, dynamicLevel, null);
        } catch (ProgressReceiver.OperationCancelled operationCancelled) {
            // Can't happen since we don't pass in a progress receiver
            throw new InternalError();
        }
    }

    @Override
    public void applyPixel(Dimension dimension, int x, int y) {
        if (dimension != this.dimension) {
            this.dimension = dimension;
            selectionHelper = new SelectionHelper(dimension);
        }
    }

    @Override
    public void removePixel(Dimension dimension, int x, int y) {
        if (dimension != this.dimension) {
            this.dimension = dimension;
            selectionHelper = new SelectionHelper(dimension);
        }
    }

    @Override
    public BufferedImage getIcon(ColourScheme colourScheme) {
        return ICON;
    }

    private Dimension dimension;
    private SelectionHelper selectionHelper;

    private static final BufferedImage ICON = IconUtils.loadScaledImage("org/pepsoft/worldpainter/icons/edit_selection.png");
}
