/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps.gui;

import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

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
        addField("Name:", heightMap, "name");
        if (heightMap instanceof ConstantHeightMap) {
            addField("Constant height: ", heightMap, "baseHeight");
        } else if (heightMap instanceof NinePatchHeightMap) {
            addField("Height:", heightMap, "height");
            addField("Inner size:", heightMap, "innerSize", 0, null);
            addField("Border size:", heightMap, "borderSize", 0, null);
            addField("Coast size:", heightMap, "coastSize", 0, null);
        } else if (heightMap instanceof NoiseHeightMap) {
            addField("Range:", heightMap, "range", 0f, null);
            addField("Scale:", heightMap, "scale", 0.0, null);
            addField("Octaves:", heightMap, "octaves", 1, 8);
        } else if (heightMap instanceof TransformingHeightMap) {
            addField("Scale: ", heightMap, "scale", 0, null);
            addField("X offset: ", heightMap, "offsetX");
            addField("Y offset: ", heightMap, "offsetY");
        } else if (heightMap instanceof BitmapHeightMap) {
            BufferedImage image = ((BitmapHeightMap) heightMap).getImage();
            int noOfChannels = image.getColorModel().getNumComponents();
            addField("Channel:", heightMap, "channel", 0, noOfChannels - 1);
            addField("Repeat:", heightMap, "repeat");
            addField("Smooth scaling:", heightMap, "smoothScaling");
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weighty = 1.0;
        add(Box.createGlue(), constraints);
        validate();
        repaint();
    }

    public HeightMapListener getListener() {
        return listener;
    }

    public void setListener(HeightMapListener listener) {
        this.listener = listener;
    }

    private void addField(String name, Object bean, String propertyName) {
        addField(name, bean, propertyName, null, null);
    }

    private void addField(String name, Object bean, String propertyName, Number min, Number max) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        JLabel label = new JLabel(name);
        add(label, constraints);
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            for (PropertyDescriptor propertyDescriptor: beanInfo.getPropertyDescriptors()) {
                if (propertyDescriptor.getName().equalsIgnoreCase(propertyName)) {
                    constraints.gridwidth = GridBagConstraints.REMAINDER;
                    constraints.weightx = 1.0;
                    if (propertyDescriptor.getWriteMethod() == null) {
                        JLabel valueLabel = new JLabel();
                        Object value = propertyDescriptor.getReadMethod().invoke(bean);
                        if (value != null) {
                            valueLabel.setText(value.toString());
                        }
                        add(valueLabel, constraints);
                        return;
                    }
                    Class<?> propertyType = propertyDescriptor.getPropertyType();
                    if (propertyType == String.class) {
                        JTextField field = new JTextField();
                        field.setText((String) propertyDescriptor.getReadMethod().invoke(bean));
                        field.getDocument().addDocumentListener(new DocumentListener() {
                            @Override
                            public void insertUpdate(DocumentEvent event) {
                                updateProperty();
                            }

                            @Override
                            public void removeUpdate(DocumentEvent event) {
                                updateProperty();
                            }

                            @Override
                            public void changedUpdate(DocumentEvent event) {
                                updateProperty();
                            }

                            private void updateProperty() {
                                try {
                                    propertyDescriptor.getWriteMethod().invoke(bean, field.getText());
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                                updateListener(propertyName);
                            }
                        });
                        add(field, constraints);
                    } else if ((propertyType == boolean.class) || (propertyType == Boolean.class)) {
                        JCheckBox checkBox = new JCheckBox(" ");
                        checkBox.setSelected(Boolean.TRUE.equals(propertyDescriptor.getReadMethod().invoke(bean)));
                        checkBox.addActionListener(event -> {
                            try {
                                propertyDescriptor.getWriteMethod().invoke(bean, checkBox.isSelected());
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                            updateListener(propertyName);
                        });
                        add(checkBox, constraints);
                    } else if ((Number.class.isAssignableFrom(propertyType)) || (propertyType.isPrimitive() && (propertyType != boolean.class) && (propertyType != char.class))) {
                        JSpinner spinner = new JSpinner(new SpinnerNumberModel((Number) propertyDescriptor.getReadMethod().invoke(bean), (Comparable) min, (Comparable) max, 1));
                        spinner.addChangeListener(event -> {
                            try {
                                propertyDescriptor.getWriteMethod().invoke(bean, spinner.getValue());
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                            updateListener(propertyName);
                        });
                        add(spinner, constraints);
                    } else {
                        throw new IllegalArgumentException("Property " + propertyName + " of type " + propertyType.getSimpleName() + " not supported");
                    }
                    return;
                }
            }
            throw new IllegalArgumentException("Bean of type " + bean.getClass().getSimpleName() + " has no property named " + propertyName);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateListener(String propertyName) {
        if (listener != null) {
            listener.heightMapChanged(heightMap, propertyName);
        }
    }

    private HeightMap heightMap;
    private HeightMapListener listener;

    public interface HeightMapListener {
        void heightMapChanged(HeightMap heightMap, String propertyName);
    }
}