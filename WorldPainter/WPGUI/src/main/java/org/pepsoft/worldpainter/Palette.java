/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.swing.JideLabel;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.LayoutUtils;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.*;

import static java.util.Arrays.asList;

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

        // Row: Show/Solo labels
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

        // Row: palette show/solo checkboxes
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        showCheckBox = new JCheckBox();
        showCheckBox.setToolTipText("Uncheck to hide all layer on this palette from view (they will still be exported)");
        showCheckBox.setSelected(true);
        showCheckBox.addActionListener(e -> setShow(showCheckBox.isSelected()));
        soloCheckBox = new JCheckBox();
        soloCheckBox.setToolTipText("<html>Check to show <em>only</em> the layers on this palette (the other layers are still exported)</html>");
        soloCheckBox.addActionListener(e -> setSolo(soloCheckBox.isSelected()));
        LayoutUtils.addRowOfComponents(panel, constraints, asList(showCheckBox, soloCheckBox, new JLabel("<html><i>all </i></html>")));

        // Row: components provided to constructor
        LayoutUtils.addRowOfComponents(panel, constraints, buttonComponents);

        dockableFrame = new App.DockableFrameBuilder(panel, name, DockContext.DOCK_SIDE_WEST, 3).withIcon(ICON_LAYERS).build();
        dockableFrame.setKey("customLayerPalette." + name);
    }

    String getName() {
        return name;
    }

    List<CustomLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    boolean isShow() {
        return show;
    }

    void setShow(boolean show) {
        this.show = show;
        showCheckBox.setSelected(show);
        propertyChangeSupport.firePropertyChange("show", ! showCheckBox.isSelected(), showCheckBox.isSelected());
        dockableFrame.setFrameIcon(solo ? ICON_SOLO : (show ? ICON_LAYERS : ICON_NOT_SHOWN));
    }

    boolean isSolo() {
        return solo;
    }

    void setSolo(boolean solo) {
        this.solo = solo;
        soloCheckBox.setSelected(solo);
        propertyChangeSupport.firePropertyChange("solo", ! soloCheckBox.isSelected(), soloCheckBox.isSelected());
        dockableFrame.setFrameIcon(solo ? ICON_SOLO : (show ? ICON_LAYERS : ICON_NOT_SHOWN));
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

    void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private final String name;
    private final JPanel panel;
    private final List<CustomLayer> layers = new ArrayList<>();
    private final Map<CustomLayer, List<Component>> layerButtonComponents = new HashMap<>();
    private final DockableFrame dockableFrame;
    private final JCheckBox showCheckBox, soloCheckBox;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private boolean show = true, solo;

    private static final Icon ICON_LAYERS    = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/layers.png");
    private static final Icon ICON_NOT_SHOWN = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/cross.png");
    private static final Icon ICON_SOLO      = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/tick.png");
}