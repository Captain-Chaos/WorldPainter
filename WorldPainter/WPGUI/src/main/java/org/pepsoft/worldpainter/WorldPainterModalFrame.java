package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.GUIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A {@link JFrame} that acts as much as possible as a resizable {@link JDialog}, but with a minimise and maximise
 * button.
 *
 * <p>To show the frame, use the {@link #setVisible(Runnable)} method. The {@link #setVisible(boolean)} method throws
 * an {@code UnsupportedOperationException}.
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
     * Disable the parent and show the frame. This method returns immediately. The callback is <em>only</em> invoked
     * when the frame is closed by invoking {@link #ok()}.
     *
     * <p>This method also enforces that only one of these frames is open at the same time. If another frame is already
     * open, this method will sound a beep and surface that frame. It will <em>not</em> open this frame, or invoke the
     * callback.
     *
     * @param okCallback The callback to invoke when the frame is closed by invoking {@link #ok()}.
     * @return {@code true} if the frame was opened.
     */
    public boolean setVisible(Runnable okCallback) {
        if (openFrame == null) {
            openFrame = this;
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
                    openFrame = null;
                }
            });
            super.setVisible(true);
            owner.setEnabled(false);
            return true;
        } else {
            DesktopUtils.beep();
            openFrame.setState(NORMAL);
            openFrame.toFront();
            return false;
        }
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

    private static WorldPainterModalFrame openFrame;

    private static final long serialVersionUID = 1L;
}