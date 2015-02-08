package org.pepsoft.worldpainter.util;

public class TristateButtonModel extends com.jidesoft.swing.TristateButtonModel {
    public void setTristateMode(boolean tristateMode) {
        this.tristateMode = tristateMode;
        if ((! tristateMode) && isMixed()) {
            setState(TristateCheckBox.STATE_UNSELECTED);
        }
    }
    
    public boolean isTristateMode() {
        return tristateMode;
    }
    
    @Override
    protected int getNextState(int current) {
        if ((! tristateMode) && isSelected()) {
            return TristateCheckBox.STATE_UNSELECTED;
        } else {
            return super.getNextState(current);
        }
    }
    
    private boolean tristateMode = true;

    private static final long serialVersionUID = 1L;
}