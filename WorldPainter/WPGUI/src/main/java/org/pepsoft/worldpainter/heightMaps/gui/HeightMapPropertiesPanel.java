/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.NinePatchHeightMap;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;

/**
 *
 * @author pepijn
 */
public class HeightMapPropertiesPanel extends JPanel {
    public HeightMapPropertiesPanel() {
        setLayout(new GridBagLayout());
    }

    public HeightMap getHeightMap() {
        return heightMap;
    }

    public void setHeightMap(HeightMap heightMap) {
        this.heightMap = heightMap;
        removeAll();
        if (heightMap.getName() != null) {
            addRow("Name:", heightMap.getName());
        }
        if (heightMap instanceof ConstantHeightMap) {
            addRow("Constant height: ", Float.toString(heightMap.getBaseHeight()));
        } else if (heightMap instanceof NinePatchHeightMap) {
            addRow("Height:", Float.toString(((NinePatchHeightMap) heightMap).getHeight()));
            addRow("Inner size:", Integer.toString(((NinePatchHeightMap) heightMap).getInnerSize()));
            addRow("Border size:", Integer.toString(((NinePatchHeightMap) heightMap).getBorderSize()));
            addRow("Coast size:", Integer.toString(((NinePatchHeightMap) heightMap).getCoastSize()));
        } else if (heightMap instanceof NoiseHeightMap) {
            addRow("Range:", Float.toString(((NoiseHeightMap) heightMap).getRange()));
            addRow("Scale:", Double.toString(((NoiseHeightMap) heightMap).getScale()));
            addRow("Octaves:", Integer.toString(((NoiseHeightMap) heightMap).getOctaves()));
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weighty = 1.0;
        add(Box.createGlue(), constraints);
        validate();
        repaint();
    }
    
    private void addRow(String... values) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);
        for (int i = 0; i < values.length; i++) {
            if (i == values.length - 1) {
                constraints.gridwidth = GridBagConstraints.REMAINDER;
                constraints.weightx = 1.0;
            }
            add(new JLabel(values[i]), constraints);
            if (i == values.length - 1) {
                constraints.gridwidth = 1;
                constraints.weightx = 0.0;
            }
        }
    }
    
    private HeightMap heightMap;
}