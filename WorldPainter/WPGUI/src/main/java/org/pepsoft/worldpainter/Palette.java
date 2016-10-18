/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.swing.JideLabel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.LayoutUtils;

/**
 *
 * @author SchmitzP
 */
public class Palette {
    Palette(String name, List<Component> buttonComponents) {
        super();
        this.name = name;
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);
        JideLabel label = new JideLabel("Show");
        label.setOrientation(SwingConstants.VERTICAL);
        label.setClockwise(false);
        label.setMinimumSize(label.getPreferredSize());
        constraints.anchor = GridBagConstraints.SOUTH;
        panel.add(label, constraints);
        label = new JideLabel("Solo");
        label.setOrientation(SwingConstants.VERTICAL);
        label.setClockwise(false);
        label.setMinimumSize(label.getPreferredSize());
        panel.add(label, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        panel.add(new JLabel(), constraints);
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        LayoutUtils.addRowOfComponents(panel, constraints, buttonComponents);

        dockableFrame = new App.DockableFrameBuilder(panel, name, DockContext.DOCK_SIDE_WEST, 3).build();
        dockableFrame.setKey("customLayerPalette." + name);
    }

    String getName() {
        return name;
    }

    List<CustomLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    }
    
    @SuppressWarnings("element-type-mismatch")
    boolean contains(Layer layer) {
        return layers.contains(layer);
    }
    
    void add(CustomLayer layer, List<Component> buttonComponents) {
        layers.add(layer);
        layerButtonComponents.put(layer, buttonComponents);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(1, 1, 1, 1);
        LayoutUtils.insertRowOfComponents(panel, constraints, panel.getComponentCount() - 3, buttonComponents);
    }
    
    List<Component> remove(CustomLayer layer) {
        if (layerButtonComponents.containsKey(layer)) {
            layers.remove(layer);
            List<Component> buttonComponents = layerButtonComponents.remove(layer);
            buttonComponents.forEach(panel::remove);
            return buttonComponents;
        } else {
            return null;
        }
    }
    
    void activate(CustomLayer layer) {
        ((JToggleButton) layerButtonComponents.get(layer).get(2)).setSelected(true);
    }

    void deactivate(CustomLayer layer) {
        ((JToggleButton) layerButtonComponents.get(layer).get(2)).setSelected(false);
    }
    
    boolean isEmpty() {
        return layers.isEmpty();
    }
    
    DockableFrame getDockableFrame() {
        return dockableFrame;
    }
    
    private final String name;
    private final JPanel panel;
    private final List<CustomLayer> layers = new ArrayList<>();
    private final Map<CustomLayer, List<Component>> layerButtonComponents = new HashMap<>();
    private final DockableFrame dockableFrame;
}