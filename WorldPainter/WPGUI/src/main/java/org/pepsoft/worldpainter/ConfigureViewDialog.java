/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ConfigureViewDialog.java
 *
 * Created on 3-dec-2011, 23:13:02
 */
package org.pepsoft.worldpainter;

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author pepijn
 */
public class ConfigureViewDialog extends javax.swing.JDialog implements DocumentListener, WindowListener {
    /** Creates new form ConfigureViewDialog */
    public ConfigureViewDialog(Frame parent, Dimension dimension, WorldPainter view) {
        this(parent, dimension, view, false);
    }
    
    /** Creates new form ConfigureViewDialog */
    public ConfigureViewDialog(Frame parent, Dimension dimension, WorldPainter view, boolean enableOverlay) {
        super(parent, true);
        this.dimension = dimension;
        this.view = view;
        this.enableOverlay = enableOverlay;
        initComponents();
        checkBoxGrid.setSelected(view.isDrawGrid());
        spinnerGridSize.setValue(view.getGridSize());
        checkBoxImageOverlay.setSelected(view.isDrawOverlay());
        if (dimension.getOverlay() != null) {
            fieldImage.setText(dimension.getOverlay().getAbsolutePath());
        }
        spinnerScale.setValue((int) (dimension.getOverlayScale() * 100));
        spinnerTransparency.setValue((int) (view.getOverlayTransparency() * 100));
        spinnerXOffset.setValue(view.getOverlayOffsetX());
        spinnerYOffset.setValue(view.getOverlayOffsetY());
        checkBoxContours.setSelected(view.isDrawContours());
        spinnerContourSeparation.setValue(view.getContourSeparation());
        fieldImage.getDocument().addDocumentListener(this);
        setControlStates();
        setLocationRelativeTo(parent);
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }

            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        
        if (enableOverlay) {
            addWindowListener(this);
        }
    }

    // DocumentListener
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        updateImageFile();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateImageFile();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateImageFile();
    }
    
    // WindowListener

    @Override
    public void windowOpened(WindowEvent e) {
        if (enableOverlay) {
            enableOverlay();
        }
    }

    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}

    private void setControlStates() {
        spinnerGridSize.setEnabled(checkBoxGrid.isSelected());
        boolean imageOverlayEnabled = checkBoxImageOverlay.isSelected();
        fieldImage.setEnabled(imageOverlayEnabled);
        buttonSelectImage.setEnabled(imageOverlayEnabled);
        spinnerScale.setEnabled(imageOverlayEnabled);
        spinnerTransparency.setEnabled(imageOverlayEnabled);
        spinnerXOffset.setEnabled(imageOverlayEnabled);
        spinnerYOffset.setEnabled(imageOverlayEnabled);
        spinnerContourSeparation.setEnabled(checkBoxContours.isSelected());
    }
    
    private void updateImageFile() {
        File file = new File(fieldImage.getText());
        if (file.isFile() && file.canRead()) {
            logger.info("Loading image");
            BufferedImage image;
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "I/O error while loading image " + file ,e);
                JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nIt may not be a valid or supported image file, or the file may be corrupted.", "Error Loading Image", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, e.getClass().getSimpleName() + " while loading image " + file ,e);
                JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error Loading Image", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Error e) {
                logger.log(Level.SEVERE, e.getClass().getSimpleName() + " while loading image " + file ,e);
                JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error Loading Image", JOptionPane.ERROR_MESSAGE);
                return;
            }
            image = scaleImage(image, getGraphicsConfiguration(), (Integer) spinnerScale.getValue());
            if (image != null) {
                // The scaling succeeded
                dimension.setOverlay(file);
                view.setOverlay(image);
            }
        }
    }
    
    static BufferedImage scaleImage(BufferedImage image, GraphicsConfiguration graphicsConfiguration, int scale) {
        try {
            boolean alpha = image.getColorModel().hasAlpha();
            if (scale == 100) {
                logger.info("Optimising image");
                BufferedImage optimumImage = graphicsConfiguration.createCompatibleImage(image.getWidth(), image.getHeight(), alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
                Graphics2D g2 = optimumImage.createGraphics();
                try {
                    g2.drawImage(image, 0, 0, null);
                } finally {
                    g2.dispose();
                }
                return optimumImage;
            } else {
                logger.info("Scaling image");
                int width = image.getWidth() * scale / 100;
                int height = image.getHeight() * scale / 100;
                BufferedImage optimumImage = graphicsConfiguration.createCompatibleImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
                Graphics2D g2 = optimumImage.createGraphics();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(image, 0, 0, width, height, null);
                } finally {
                    g2.dispose();
                }
                return optimumImage;
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, e.getClass().getSimpleName() + " while scaling image of size " + image.getWidth() + "x" + image.getHeight() + " and type " + image.getType() + " to " + scale + "%", e);
            JOptionPane.showMessageDialog(null, "An error occurred while scaling the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error Scaling Image", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (Error e) {
            logger.log(Level.SEVERE, e.getClass().getSimpleName() + " while scaling image of size " + image.getWidth() + "x" + image.getHeight() + " and type " + image.getType() + " to " + scale + "%", e);
            JOptionPane.showMessageDialog(null, "An error occurred while scaling the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error Scaling Image", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        final Set<String> extensions = new HashSet<String>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
        StringBuilder sb = new StringBuilder("Supported image formats (");
        boolean first = true;
        for (String extension: extensions) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("*.");
            sb.append(extension);
        }
        sb.append(')');
        final String description = sb.toString();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String filename = f.getName();
                int p = filename.lastIndexOf('.');
                if (p != -1) {
                    String extension = filename.substring(p + 1).toLowerCase();
                    return extensions.contains(extension);
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File selectedFile = new File(fieldImage.getText());
        if (selectedFile.isFile()) {
            fileChooser.setSelectedFile(selectedFile);
        }
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fieldImage.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void enableOverlay() {
        if (! checkBoxImageOverlay.isSelected()) {
            checkBoxImageOverlay.setSelected(true);
            view.setDrawOverlay(true);
            dimension.setOverlayEnabled(true);
            setControlStates();
        }
        selectImage();
        if (dimension.getOverlay() == null) {
            checkBoxImageOverlay.setSelected(false);
            view.setDrawOverlay(false);
            dimension.setOverlayEnabled(false);
            setControlStates();
        }
    }

    private void scheduleImageUpdate() {
        if (imageUpdateTimer == null) {
            imageUpdateTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateImageFile();
                    imageUpdateTimer = null;
                }
            });
            imageUpdateTimer.setRepeats(false);
            imageUpdateTimer.start();
        } else {
            imageUpdateTimer.restart();
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        checkBoxGrid = new javax.swing.JCheckBox();
        spinnerGridSize = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        checkBoxImageOverlay = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        fieldImage = new javax.swing.JTextField();
        buttonSelectImage = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        spinnerTransparency = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        spinnerXOffset = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        spinnerYOffset = new javax.swing.JSpinner();
        buttonClose = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        checkBoxContours = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        spinnerContourSeparation = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Configure View");

        checkBoxGrid.setText("Grid");
        checkBoxGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxGridActionPerformed(evt);
            }
        });

        spinnerGridSize.setModel(new javax.swing.SpinnerNumberModel(128, 2, 9999, 1));
        spinnerGridSize.setEnabled(false);
        spinnerGridSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerGridSizeStateChanged(evt);
            }
        });

        jLabel1.setText("Grid size:");

        checkBoxImageOverlay.setText("Image overlay");
        checkBoxImageOverlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxImageOverlayActionPerformed(evt);
            }
        });

        jLabel2.setText("Image:");

        fieldImage.setEnabled(false);

        buttonSelectImage.setText("...");
        buttonSelectImage.setEnabled(false);
        buttonSelectImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectImageActionPerformed(evt);
            }
        });

        jLabel3.setText("Scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 9999, 1));
        spinnerScale.setEnabled(false);
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

        jLabel4.setText("%");

        jLabel5.setText("Transparency:");

        spinnerTransparency.setModel(new javax.swing.SpinnerNumberModel(50, 0, 99, 1));
        spinnerTransparency.setEnabled(false);
        spinnerTransparency.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTransparencyStateChanged(evt);
            }
        });

        jLabel6.setText("%");

        jLabel7.setText("X offset:");

        spinnerXOffset.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerXOffset.setEnabled(false);
        spinnerXOffset.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerXOffsetStateChanged(evt);
            }
        });

        jLabel8.setText(", Y offset:");

        spinnerYOffset.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerYOffset.setEnabled(false);
        spinnerYOffset.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerYOffsetStateChanged(evt);
            }
        });

        buttonClose.setText("Close");
        buttonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCloseActionPerformed(evt);
            }
        });

        jLabel9.setText("blocks");

        checkBoxContours.setSelected(true);
        checkBoxContours.setText("Contour lines");
        checkBoxContours.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxContoursActionPerformed(evt);
            }
        });

        jLabel10.setText("Separation:");

        spinnerContourSeparation.setModel(new javax.swing.SpinnerNumberModel(10, 2, 999, 1));
        spinnerContourSeparation.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerContourSeparationStateChanged(evt);
            }
        });

        jLabel11.setText("blocks");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxGrid)
                    .addComponent(checkBoxImageOverlay)
                    .addComponent(buttonClose, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(checkBoxContours)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerContourSeparation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel11))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerGridSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel9))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(jLabel4))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldImage)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonSelectImage))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerTransparency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(jLabel6))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerXOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerYOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBoxGrid)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spinnerGridSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addComponent(checkBoxContours)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(spinnerContourSeparation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addGap(18, 18, 18)
                .addComponent(checkBoxImageOverlay)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(fieldImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSelectImage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(spinnerTransparency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerXOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(spinnerYOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonClose)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void checkBoxGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxGridActionPerformed
        setControlStates();
        boolean gridEnabled = checkBoxGrid.isSelected();
        view.setDrawGrid(gridEnabled);
        dimension.setGridEnabled(gridEnabled);
    }//GEN-LAST:event_checkBoxGridActionPerformed

    private void buttonSelectImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectImageActionPerformed
        selectImage();
    }//GEN-LAST:event_buttonSelectImageActionPerformed

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        float scale = ((Number) spinnerScale.getValue()).intValue() / 100.0f;
        dimension.setOverlayScale(scale);
        scheduleImageUpdate();
    }//GEN-LAST:event_spinnerScaleStateChanged

    private void spinnerTransparencyStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTransparencyStateChanged
        float transparency = ((Number) spinnerTransparency.getValue()).intValue() / 100.0f;
        view.setOverlayTransparency(transparency);
        dimension.setOverlayTransparency(transparency);
    }//GEN-LAST:event_spinnerTransparencyStateChanged

    private void spinnerXOffsetStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerXOffsetStateChanged
        int xOffset = ((Number) spinnerXOffset.getValue()).intValue();
        view.setOverlayOffsetX(xOffset);
        dimension.setOverlayOffsetX(xOffset);
    }//GEN-LAST:event_spinnerXOffsetStateChanged

    private void spinnerYOffsetStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerYOffsetStateChanged
        int yOffset = ((Number) spinnerYOffset.getValue()).intValue();
        view.setOverlayOffsetY(yOffset);
        dimension.setOverlayOffsetY(yOffset);
    }//GEN-LAST:event_spinnerYOffsetStateChanged

    private void checkBoxImageOverlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxImageOverlayActionPerformed
        setControlStates();
        boolean overlayEnabled = checkBoxImageOverlay.isSelected();
        view.setDrawOverlay(overlayEnabled);
        overlayEnabled = view.isDrawOverlay(); // Enabling the overlay may have failed
        dimension.setOverlayEnabled(overlayEnabled);
        if (overlayEnabled && (dimension.getOverlay() == null)) {
            selectImage();
        }
    }//GEN-LAST:event_checkBoxImageOverlayActionPerformed

    private void spinnerGridSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerGridSizeStateChanged
        int gridSize = ((Number) spinnerGridSize.getValue()).intValue();
        view.setGridSize(gridSize);
        dimension.setGridSize(gridSize);
    }//GEN-LAST:event_spinnerGridSizeStateChanged

    private void buttonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseActionPerformed
        if (imageUpdateTimer != null) {
            imageUpdateTimer.stop();
            imageUpdateTimer = null;
            updateImageFile();
        }
        dispose();
    }//GEN-LAST:event_buttonCloseActionPerformed

    private void checkBoxContoursActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxContoursActionPerformed
        setControlStates();
        boolean contoursEnabled = checkBoxContours.isSelected();
        view.setDrawContours(contoursEnabled);
        dimension.setContoursEnabled(contoursEnabled);
    }//GEN-LAST:event_checkBoxContoursActionPerformed

    private void spinnerContourSeparationStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerContourSeparationStateChanged
        int contourSeparation = ((Number) spinnerContourSeparation.getValue()).intValue();
        view.setContourSeparation(contourSeparation);
        dimension.setContourSeparation(contourSeparation);
    }//GEN-LAST:event_spinnerContourSeparationStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClose;
    private javax.swing.JButton buttonSelectImage;
    private javax.swing.JCheckBox checkBoxContours;
    private javax.swing.JCheckBox checkBoxGrid;
    private javax.swing.JCheckBox checkBoxImageOverlay;
    private javax.swing.JTextField fieldImage;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSpinner spinnerContourSeparation;
    private javax.swing.JSpinner spinnerGridSize;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JSpinner spinnerTransparency;
    private javax.swing.JSpinner spinnerXOffset;
    private javax.swing.JSpinner spinnerYOffset;
    // End of variables declaration//GEN-END:variables

    private final Dimension dimension;
    private final WorldPainter view;
    private final boolean enableOverlay;
    private Timer imageUpdateTimer;

    private static final Logger logger = Logger.getLogger(ConfigureViewDialog.class.getName());
    private static final long serialVersionUID = 1L;
}