package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.tools.Eyedropper;

import java.awt.*;
import java.util.Set;

/**
 * A special version of {@code WorldPainterDialog} which adds support for temporarily hiding it so that the user may
 * select a paint from the map.
 *
 * <p>Because this dialog can be hidden to allow paint selection from the editor view, which would otherwise be blocked,
 * its {@link #setVisible(boolean)} method may return early and should not be used. Instead the
 * {@link #setVisible(Runnable)} method should be used which allows a callback to be supplied which will be executed
 * when the dialog is properly closed.
 */
public class WPDialogWithPaintSelection extends WorldPainterDialog {
    public WPDialogWithPaintSelection(Window parent) {
        this(parent, true);
    }

    public WPDialogWithPaintSelection(Window parent, boolean enableHelpKey) {
        super(parent, enableHelpKey);
        if (parent instanceof App) {
            app = (App) parent;
        } else {
            app = null;
        }
    }

    /**
     * Show the dialog. This method may return early; a callback should be specified for any work that must be done
     * after the dialog is closed properly by using {@link #setVisible(Runnable)} instead.
     */
    public void setVisible() {
        setVisible(null);
    }

    /**
     * Show the dialog. This method may return early; a callback should be specified for any work that must be done
     * after the dialog is closed properly. The callback is <em>only</em> invoked when the dialog is closed by invoking
     * {@link #ok()}.
     *
     * @param okCallback The callback to invoke when the dialog is closed by invoking {@link #ok()}.
     */
    public void setVisible(Runnable okCallback) {
        this.okCallback = okCallback;
        super.setVisible(true);
        if (selectFromMapListener != null) {
            app.selectPaintOnMap(selectFromMapTypes, new Eyedropper.SelectionListener() {
                @Override
                public void terrainSelected(Terrain terrain) {
                    selectFromMapListener.terrainSelected(terrain);
                    showAgain();
                }

                @Override
                public void layerSelected(Layer layer, int value) {
                    selectFromMapListener.layerSelected(layer, value);
                    showAgain();
                }

                @Override
                public void selectionCancelled(boolean byUser) {
                    if (byUser) {
                        showAgain();
                    } else {
                        DesktopUtils.beep();
                        cancel();
                    }
                }

                private void showAgain() {
                    selectFromMapListener = null;
                    WPDialogWithPaintSelection.super.setVisible(true);
                }
            });
        }
    }

    /**
     * Not supported; always throws an {@code UnsupportedOperationException}. Use {@link #setVisible()} or
     * {@link #setVisible(Runnable)} instead.
     *
     * @param b Not used.
     * @throws UnsupportedOperationException Always.
     */
    @Override
    @Deprecated
    public void setVisible(boolean b) {
        throw new UnsupportedOperationException("Use setVisible(Runnable)");
    }

    @Override
    protected void ok() {
        super.ok();
        if (okCallback != null) {
            okCallback.run();
        }
    }

    /**
     * Temporarily hide the dialog and allow the user to select a paint from the map. If the user selects a paint, the
     * dialog is made visible again and the specified listener is invoked. If the user expressly cancels the selection,
     * the dialog is made visible again but the listener not invoked. If the selection is cancelled for any other
     * reason, the dialog is disposed of, as if {@link #cancel()} had been invoked.
     *
     * @param paintTypes        The type(s) of paint to select, or {@code null} to select from all paint types.
     * @param selectionListener The listener to invoke when the user has made a selection.
     */
    protected final void selectFromMap(Set<Eyedropper.PaintType> paintTypes, Eyedropper.SelectionListener selectionListener) {
        if (selectionListener == null) {
            throw new NullPointerException("selectionListener");
        } else if (selectFromMapListener != null) {
            throw new IllegalStateException("Map selection already in progress");
        } else if (app == null) {
            throw new IllegalArgumentException("Parent is not of type App");
        }
        selectFromMapListener = selectionListener;
        selectFromMapTypes = paintTypes;
        super.setVisible(false);
    }

    private Eyedropper.SelectionListener selectFromMapListener;
    private Set<Eyedropper.PaintType> selectFromMapTypes;
    private Runnable okCallback;

    protected final App app;
}