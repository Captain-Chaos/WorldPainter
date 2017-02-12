package org.pepsoft.worldpainter.operations;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Pepijn Schmitz on 18-01-17.
 */
public class TerrainShapingOptionsPanel extends JPanel {
    public TerrainShapingOptionsPanel(TerrainShapingOptions<?> options) {
        initComponents();
        setOptions(options);
    }

    public TerrainShapingOptions<?> getOptions() {
        return options;
    }

    public void setOptions(TerrainShapingOptions<?> options) {
        this.options = options;
        checkBoxApplyTheme.setSelected(options.isApplyTheme());
    }

    private void initComponents() {
        setLayout(new GridLayout(0, 1));
        checkBoxApplyTheme.addActionListener(e -> {
            options.setApplyTheme(checkBoxApplyTheme.isSelected());
            firePropertyChange("options", null, options);
        });
        add(checkBoxApplyTheme);
    }

    private final JCheckBox checkBoxApplyTheme = new JCheckBox("Apply theme");
    private TerrainShapingOptions<?> options;
}
