/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author SchmitzP
 */
public class MixedMaterialChooser extends JLabel implements MouseListener {
    public MixedMaterialChooser() {
        setText("<html><u>click to select</u></html>");
        setForeground(Color.BLUE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(this);
    }

    public MixedMaterial getMaterial() {
        return selectedMaterial;
    }

    public void setMaterial(MixedMaterial material) {
        selectedMaterial = material;
        if (material != null) {
            setText("<html><u>" + material.getName() + "</u></html>");
        } else {
            setText("<html><u>click to select</u></html>");
        }
        // Don't specify previous material, otherwise the event will not be
        // fired if only the name or colour changed
        firePropertyChange("material", null, material);
    }

    public boolean isExtendedBlockIds() {
        return extendedBlockIds;
    }

    public void setExtendedBlockIds(boolean extendedBlockIds) {
        this.extendedBlockIds = extendedBlockIds;
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }
    
    //JComponent
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); //To change body of generated methods, choose Tools | Templates.
        if (enabled) {
            setForeground(Color.BLUE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setForeground(Color.GRAY);
            setCursor(null);
        }
    }

    // MouseListener
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (isEnabled()) {
            editMaterial();
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    
    private void editMaterial() {
        MixedMaterial material = selectedMaterial;
        if (material == null) {
            material = MixedMaterial.create(BLK_DIRT);
        }
        CustomMaterialDialog dialog = new CustomMaterialDialog(SwingUtilities.getWindowAncestor(this), material, extendedBlockIds, colourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            selectedMaterial = dialog.getMaterial();
            setText("<html><u>" + selectedMaterial.getName() + "</u></html>");
            // Don't specify previous material, otherwise the event will not be
            // fired if only the name or colour changed
            firePropertyChange("material", null, selectedMaterial);
        }
    }
    
    private MixedMaterial selectedMaterial;
    private boolean extendedBlockIds;
    private ColourScheme colourScheme;
}