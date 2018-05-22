/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 * A WorldPainter dialog window. Provides the following features:
 *
 * <ul><li>Always application modal
 * <li>A {@link #cancel()} method which dismisses the dialog programmatically
 * <li>The dialog is also cancelled if the user presses the <code>Esc</code> key
 * <li>An {@link #isCancelled()} method which indicates whether the dialog has
 * been cancelled or closed in any other way besides invoking the {@link #ok()}
 * method
 * <li>An {@link #ok()} method which dismisses the dialog programmatically and
 * clears the <code>cancelled</code> property
 * <li>[SAFE MODE] automatically appended to title if WorldPainter is running in
 * safe mode</ul>
 *
 * @author pepijn
 */
public class WorldPainterDialog extends JDialog {
    public WorldPainterDialog(Window parent) {
        this(parent, true);
    }

    public WorldPainterDialog(Window parent, boolean enableHelpKey) {
        super(parent, ModalityType.APPLICATION_MODAL);

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
            getRootPane().putClientProperty(App.HELP_KEY_KEY, "Dialog/" + getClass().getSimpleName());
            actionMap.put("help", new AbstractAction("help") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    App.getInstance().showHelp(WorldPainterDialog.this);
                }

                private static final long serialVersionUID = 1L;
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help");
        }
    }

    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(Configuration.getInstance().isSafeMode() ? (title + " [SAFE MODE]") : title);
    }

    protected void ok() {
        cancelled = false;
        dispose();
    }
    
    protected void cancel() {
        dispose();
    }
    
    private boolean cancelled = true;
    
    private static final long serialVersionUID = 1L;
}