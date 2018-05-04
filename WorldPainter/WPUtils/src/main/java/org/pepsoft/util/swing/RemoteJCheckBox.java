package org.pepsoft.util.swing;

import javax.swing.*;

/**
 * A checkbox which binds to the model of another checkbox so that their states
 * are synced.
 *
 * Created by Pepijn Schmitz on 19-01-17.
 */
public class RemoteJCheckBox extends JCheckBox {
    public RemoteJCheckBox(JCheckBox peer, String text) {
        setModel(peer.getModel());
        setText(text);
    }
}