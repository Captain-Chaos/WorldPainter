package org.pepsoft.worldpainter.util;

import javax.swing.ButtonModel;
import javax.swing.Icon;

public final class TristateCheckBox extends com.jidesoft.swing.TristateCheckBox {
    public TristateCheckBox() {
        // Do nothing
    }
    
    public TristateCheckBox(String text) {
        super(text);
    }

    public TristateCheckBox(String text, Icon icon) {
        super(text, icon);
    }

    public void setTristateMode(boolean tristateMode) {
        ((TristateButtonModel) getModel()).setTristateMode(tristateMode);
    }
    
    public boolean isTristateMode() {
        return ((TristateButtonModel) getModel()).isTristateMode();
    }

    @Override
    protected ButtonModel createButtonModel() {
        return new TristateButtonModel();
    }
    
    private static final long serialVersionUID = 1L;
}