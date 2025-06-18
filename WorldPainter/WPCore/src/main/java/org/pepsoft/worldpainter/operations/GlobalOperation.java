package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.WorldPainterView;

/**
 * A WorldPainter {@link Operation operation} which is not activated and deactivated but invoked instantly. Meant for
 * global operations that do not require the user to interact with the map while the operation is active.
 *
 * <p>Operations implementing this interface will receive a regular, momentary button on the Tools panel, rather than a
 * toggle button.
 *
 * <p>The {@link #isActive()}, {@link #setActive(boolean)}, {@link #getOptionsPanel()} and {@link #interrupt()} methods
 * will never be invoked for operations which implement this interface. Instead, the {@link #invoke()} method is invoked
 * once when the button is pushed.
 */
public interface GlobalOperation extends Operation {
    /**
     * Invoked when the user presses the tool button for this operation. Will be invoked on the event thread. It may
     * block, as long as it keeps processing events, e.g. by showing a dialog window.
     *
     * <p>To obtain a window to use as parent for such a dialog, the implementation may use the view that was set
     * earlier by invoking {@link #setView(WorldPainterView)}:
     *
     * <pre>Window window = SwingUtilities.getWindowAncestor(view)</pre>
     *
     * <p>To obtain the dimension that is currently being edited, the implementation may also use the view:
     *
     * <pre>Dimension dimension = view.getDimension()</pre>
     *
     * <strong>Note</strong>: it is <em>strongly</em> recommended that if the operation opens windows, that it be
     * application modal windows so that the user cannot perform any other actions while it is open. Otherwise, the
     * potential for confusion and problems by multiple windows being open, or windows remaining open containing state
     * belonging to dimensions or worlds which have already been closed, is great.
     */
    void invoke();
}