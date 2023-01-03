package org.pepsoft.worldpainter;

import org.pepsoft.util.GUIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A WorldPainter frame window that acts as much as possible as a resizable dialog window, but with a minimise and
 * maximise button. Provides the following features:
 *
 * <ul><li>Emulates being application modal by disabling the owner window
 * <li>A {@link #cancel()} method which dismisses the frame programmatically
 * <li>The frame is also cancelled if the user presses the {@code Esc} key
 * <li>An {@link #isCancelled()} method which indicates whether the frame has been cancelled or closed in any other way
 * besides invoking the {@link #ok()} method
 * <li>An {@link #ok()} method which dismisses the frame programmatically and clears the {@code cancelled} property
 * <li>[SAFE MODE] automatically appended to title if WorldPainter is running in safe mode</ul>
 *
 * <p>This class is largely compatible with {@link WorldPainterDialog}, except that to show the frame, you use the
 * {@link #setVisible(Runnable)} method. The {@link #setVisible(boolean)} method throws an
 * {@code UnsupportedOperationException}.
 *
 * @author pepijn
 */
public class WorldPainterModalFrame extends JFrame {
    public WorldPainterModalFrame(Window owner) {
        this(owner, true);
    }

    public WorldPainterModalFrame(Window owner, boolean enableHelpKey) {
        this.owner = owner;

        setIconImage(App.ICON);

        if (owner instanceof App) {
            ((App) owner).pauseAutosave();
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    ((App) owner).resumeAutosave();
                }
            });
        }

        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }

            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

        if (enableHelpKey) {
            getRootPane().putClientProperty(App.KEY_HELP_KEY, "Dialog/" + getClass().getSimpleName());
            actionMap.put("help", new AbstractAction("help") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    App.getInstance().showHelp(WorldPainterModalFrame.this);
                }

                private static final long serialVersionUID = 1L;
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help");
        }
    }

    /**
     * Disable the owner and show the frame. This method returns immediately. The callback is <em>only</em> invoked
     * when the frame is closed by invoking {@link #ok()}.
     *
     * @param okCallback The callback to invoke when the frame is closed by invoking {@link #ok()}.
     */
    public void setVisible(Runnable okCallback) {
        if (open) {
            throw new IllegalStateException("Window is already open");
        }
        open = true;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                owner.setEnabled(true);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                // This should already have happened. If we really do it here for the first time, some Java or Swing
                // bug causes the owner to be hidden. This is a fallback:
                owner.setEnabled(true);
                // TODO this should not be necessary, but some Java or Swing bug sometimes causes the owner to be
                //  hidden even if it is already enabled by this point:
                owner.toFront();
                if (! cancelled) {
                    okCallback.run();
                }
                open = false;
            }
        });
        super.setVisible(true);
        owner.setEnabled(false);
    }

    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public Window getOwner() {
        return owner;
    }

    @Override
    public void setVisible(boolean b) {
        throw new UnsupportedOperationException("Use setVisible(Runnable)");
    }

    @Override
    public void setTitle(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        if (Version.isSnapshot()) {
            sb.append(" [SNAPSHOT]");
        }
        if (Configuration.getInstance().isSafeMode()) {
            sb.append(" [SAFE MODE]");
        }
        super.setTitle(sb.toString());
    }

    protected void ok() {
        cancelled = false;
        owner.setEnabled(true);
        dispose();
    }

    protected void cancel() {
        owner.setEnabled(true);
        dispose();
    }

    /**
     * Goes through all child components and applies any additional UI scaling
     * that is necessary because it is not possible via the UIManager defaults.
     *
     * <p>Should be called by subclasses <em>after</em> they have added all
     * their components.
     */
    protected final void scaleToUI() {
        GUIUtils.scaleToUI(this);
    }

    /**
     * Adjusts the size of the window to the UI scale. The window will not be scaled down, nor be made larger than the
     * available space on the screen.
     *
     * <p>Should be called by subclasses <em>after</em> they have added and scaled all their components.
     */
    protected final void scaleWindowToUI() {
        GUIUtils.scaleWindow(this);
    }

    private final Window owner;
    private boolean cancelled = true;
    private volatile boolean open;

    private static final long serialVersionUID = 1L;
}