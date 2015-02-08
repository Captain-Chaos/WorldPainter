/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 *
 * @author pepijn
 */
public abstract class BetterAction extends AbstractAction {
    public BetterAction(String name, Icon icon) {
        super(name, icon);
    }

    public BetterAction(String name) {
        super(name);
    }
    
    public final String getName() {
        return (String) getValue(NAME);
    }
    
    public final void setShortDescription(String shortDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append(shortDescription);
        KeyStroke accelerator = (KeyStroke) getValue(ACCELERATOR_KEY);
        if (accelerator != null) {
            sb.append(" (");
            if ((accelerator.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) {
                sb.append("Ctrl+");
            }
            if ((accelerator.getModifiers() & InputEvent.META_DOWN_MASK) != 0) {
                sb.append("⌘+");
            }
            if ((accelerator.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0) {
                sb.append("Alt+");
            }
            if ((accelerator.getModifiers() & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
                sb.append("AltGr+");
            }
            if ((accelerator.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                sb.append("Shift+");
            }
            int keyCode = accelerator.getKeyCode();
            if (keyCode == KeyEvent.VK_ADD) {
                sb.append('+');
            } else if (keyCode == KeyEvent.VK_SUBTRACT) {
                sb.append('-');
            } else {
                sb.append((char) keyCode);
            }
            sb.append(')');
        }
        putValue(SHORT_DESCRIPTION, sb.toString());
    }
    
    public final void setAcceleratorKey(KeyStroke acceleratorKey) {
        putValue(ACCELERATOR_KEY, acceleratorKey);
    }
    
    public final KeyStroke getAcceleratorKey() {
        return (KeyStroke) getValue(ACCELERATOR_KEY);
    }
    
    public final void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }
    
    public final boolean isSelected() {
        return Boolean.TRUE.equals(getValue(SELECTED_KEY));
    }
}