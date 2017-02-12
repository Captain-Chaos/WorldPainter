package org.pepsoft.util.swing;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Created by Pepijn Schmitz on 19-01-17.
 */
public class RemoteJCheckBox extends JCheckBox implements ChangeListener {
    public RemoteJCheckBox(JCheckBox peer, String text) {
        if (peer == null) {
            throw new NullPointerException("peer");
        }
        this.peer = peer;
        setText(text);
        setSelected(peer.isSelected());
        peer.addChangeListener(this);
        addChangeListener(this);
    }

    // ChangeListener

    @Override
    public void stateChanged(ChangeEvent e) {
        if (programmaticChange) {
            return;
        }
        if (e.getSource() == this) {
            // We have changed; change the peer, without falling into an endless
            // loop
            programmaticChange = true;
            try {
                peer.setSelected(isSelected());
            } finally {
                programmaticChange = false;
            }
        } else if (e.getSource() == peer) {
            // Our peer has changed; reflect the change, without falling into an
            // endless loop
            programmaticChange = true;
            try {
                setSelected(peer.isSelected());
            } finally {
                programmaticChange = false;
            }
        }
    }

    private final JCheckBox peer;
    private boolean programmaticChange;
}