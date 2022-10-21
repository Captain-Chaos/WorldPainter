package org.pepsoft.worldpainter.operations;

import org.intellij.lang.annotations.Language;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Pepijn Schmitz on 18-01-17.
 */
public class TerrainShapingOptionsPanel extends StandardOptionsPanel {
    public TerrainShapingOptionsPanel(String name, @Language("HTML") String description, TerrainShapingOptions<?> options) {
        super(name, description);
        setOptions(options);
    }

    public TerrainShapingOptions<?> getOptions() {
        return options;
    }

    public void setOptions(TerrainShapingOptions<?> options) {
        this.options = options;
        checkBoxApplyTheme.setSelected(options.isApplyTheme());
    }

    @Override
    protected void addAdditionalComponents(GridBagConstraints constraints) {
        checkBoxApplyTheme = new JCheckBox("Apply theme");
        checkBoxApplyTheme.addActionListener(e -> {
            options.setApplyTheme(checkBoxApplyTheme.isSelected());
            firePropertyChange("options", null, options);
        });
        add(checkBoxApplyTheme, constraints);
    }

    private JCheckBox checkBoxApplyTheme;
    private TerrainShapingOptions<?> options;
}
