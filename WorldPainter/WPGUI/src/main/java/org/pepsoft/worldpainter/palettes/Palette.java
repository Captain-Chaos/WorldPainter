/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.palettes;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.swing.JideLabel;
import org.jetbrains.annotations.NotNull;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.LayoutUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.*;

import static java.util.Arrays.asList;
import static javax.swing.SwingUtilities.getWindowAncestor;

/**
 *
 * @author SchmitzP
 */
public class Palette {
    Palette(String name, List<Component> buttonComponents, PaletteManager paletteManager) {
        super();
        this.name = name;
        this.paletteManager = paletteManager;
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
        constraints.anchor = GridBagConstraints.NORTHEAST;
        final JButton editButton = new JButton(ICON_EDIT);
        editButton.setMargin(new Insets(2, 2, 2, 2));
        editButton.setToolTipText("Edit palette name and layer order");
        editButton.addActionListener(this::editPalette);
        panel.add(editButton, constraints);

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

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
        for (CustomLayer layer: layers) {
            layer.setPalette(name);
        }
        dockableFrame.setTitle(name);
        dockableFrame.setTabTitle(name);
        dockableFrame.setKey("customLayerPalette." + name);
    }

    public List<CustomLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
        showCheckBox.setSelected(show);
        propertyChangeSupport.firePropertyChange("show", ! showCheckBox.isSelected(), showCheckBox.isSelected());
        dockableFrame.setFrameIcon(solo ? ICON_SOLO : (show ? ICON_LAYERS : ICON_NOT_SHOWN));
    }

    public boolean isSolo() {
        return solo;
    }

    public void setSolo(boolean solo) {
        this.solo = solo;
        soloCheckBox.setSelected(solo);
        propertyChangeSupport.firePropertyChange("solo", ! soloCheckBox.isSelected(), soloCheckBox.isSelected());
        dockableFrame.setFrameIcon(solo ? ICON_SOLO : (show ? ICON_LAYERS : ICON_NOT_SHOWN));
    }

    @SuppressWarnings("element-type-mismatch")
    public boolean contains(Layer layer) {
        return layers.contains(layer);
    }

    public DockableFrame getDockableFrame() {
        return dockableFrame;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    void add(CustomLayer layer, List<Component> buttonComponents) {
        final int index;
        if (layer.getPaletteIndex() == null) {
            layers.add(layer);
            index = layers.size() - 1;
            layer.setPaletteIndex(index);
        } else {
            index = Math.min(layer.getPaletteIndex(), layers.size());
            layers.add(index, layer);
        }
        layerButtonComponents.put(layer, buttonComponents);
        LayoutUtils.insertRowOfComponents(panel, createConstraints(), componentIndex(index), buttonComponents);
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
    
    public boolean isEmpty() {
        return layers.isEmpty();
    }
    
    private void editPalette(ActionEvent event) {
        final EditPaletteDialog dialog = new EditPaletteDialog(getWindowAncestor(dockableFrame), paletteManager, this);
        dialog.setVisible(true);
    }

    @NotNull
    private static GridBagConstraints createConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(1, 1, 1, 1);
        return constraints;
    }

    private static int componentIndex(int index) {
        return 6 + (index * 3);
    }

    private final JPanel panel;
    private final List<CustomLayer> layers = new ArrayList<>();
    private final Map<CustomLayer, List<Component>> layerButtonComponents = new HashMap<>();
    private final DockableFrame dockableFrame;
    private final JCheckBox showCheckBox, soloCheckBox;
    private final PaletteManager paletteManager;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private String name;
    private boolean show = true, solo;

    private static final Icon ICON_LAYERS    = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/layers.png");
    private static final Icon ICON_NOT_SHOWN = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/cross.png");
    private static final Icon ICON_SOLO      = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/tick.png");
    private static final Icon ICON_EDIT      = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/cog.png");
}