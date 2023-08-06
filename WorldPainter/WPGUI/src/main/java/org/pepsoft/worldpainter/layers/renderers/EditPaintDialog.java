/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.worldpainter.WorldPainterDialog;
import org.pepsoft.worldpainter.util.BufferedImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingInt;
import static org.pepsoft.util.IconUtils.createScaledColourIcon;
import static org.pepsoft.worldpainter.util.BufferedImageUtils.createColourSquare;

/**
 *
 * @author pepijn
 */
public class EditPaintDialog extends WorldPainterDialog {

    /**
     * Creates new form EditPaintDialog
     */
    public EditPaintDialog(Window parent, Object paint) {
        super(parent);
        if (paint instanceof Color) {
            this.colour = (Color) paint;
        } else if (paint instanceof BufferedImage) {
            this.pattern = BufferedImageUtils.clone((BufferedImage) paint);
        } else {
            throw new IllegalArgumentException("Paint type " + paint.getClass() + " not supported");
        }

        initComponents();

        getRootPane().setDefaultButton(buttonOk);
        if (colour != null) {
            iconEditor1.setIcon(createColourSquare(16, colour.getRGB()));
            eraseColour = colour.getRGB();
        } else {
            iconEditor1.setIcon(pattern);
            eraseColour = findBackgroundColour(pattern);
        }
        iconEditor1.setEraseColour(eraseColour);
        updatePreview();
        iconEditor1.addPropertyChangeListener("icon", evt -> {
            EditPaintDialog.this.colour = null;
            EditPaintDialog.this.pattern = (BufferedImage) evt.getNewValue();
            updatePreview();
        });

        scaleToUI();
        createColourButtons();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Returns the selected paint.
     *
     * <p><strong>Note:</strong> only valid after {@link #ok()} has been invoked!
     */
    public Object getPaint() {
        return (colour != null) ? colour : pattern;
    }

    @Override
    protected void ok() {
        pattern = iconEditor1.getIcon();
        int solidColour = pattern.getRGB(0, 0);
        for (int x = 0; x < pattern.getWidth(); x++) {
            for (int y = 0; y < pattern.getHeight(); y++) {
                if (pattern.getRGB(x, y) != solidColour) {
                    colour = null;
                    super.ok();
                    return;
                }
            }
        }
        pattern = null;
        colour = new Color(solidColour);
        super.ok();
    }

    private void createColourButtons() {
        for (int ega = 0; ega < 16; ega++) {
            final JToggleButton button = new JToggleButton(createScaledColourIcon(EGA_COLOURS[ega]));
            button.setToolTipText(EGA_NAMES[ega]);
            button.setMargin(new Insets(2, 2, 2, 2));
            final Color colour = new Color(EGA_COLOURS[ega]);
            button.addActionListener(e -> {
                paintColour = colour;
                iconEditor1.setPaintColour(colour.getRGB());
                if (! toggleButtonPencil.isSelected()) {
                    toggleButtonPencil.setSelected(true);
                }
            });
            if (ega == 0) {
                button.setSelected(true);
            }
            buttonGroupColours.add(button);
            panelColours.add(button);
        }
    }

    private void updatePreview() {
        if (colour != null) {
            rendererPreviewer1.setColour(colour);
        } else if (pattern != null) {
            rendererPreviewer1.setPattern(pattern);
        }
    }

    /**
     * Guesstimate the background colour by finding the most prevalent colour, where the outer rings of the image are
     * weighted heavier.
     */
    private int findBackgroundColour(BufferedImage image) {
        final Map<Integer, Integer> weightedCounts = new HashMap<>();
        final int w = image.getWidth(), h = image.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                final int rgb = image.getRGB(x, y);
                final int distanceFromEdge = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y));
                final int weight = Math.max(3 - distanceFromEdge, 1);
                int weightedCount = weightedCounts.getOrDefault(rgb, 0);
                weightedCounts.put(rgb, weightedCount + weight);
            }
        }
        final List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(weightedCounts.entrySet());
        entries.sort(comparingInt(Map.Entry::getValue));
        return entries.get(entries.size() - 1).getValue();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupTools = new javax.swing.ButtonGroup();
        buttonGroupColours = new javax.swing.ButtonGroup();
        iconEditor1 = new org.pepsoft.worldpainter.util.IconEditor();
        buttonSolidColour = new javax.swing.JButton();
        toggleButtonPencil = new javax.swing.JToggleButton();
        toggleButtonEraser = new javax.swing.JToggleButton();
        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        panelColours = new javax.swing.JPanel();
        rendererPreviewer1 = new org.pepsoft.worldpainter.layers.renderers.RendererPreviewer();
        buttonClear = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Paint");

        buttonSolidColour.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/fill.png"))); // NOI18N
        buttonSolidColour.setText("Make Solid Colour");
        buttonSolidColour.setToolTipText("<html>Select a colour and fill canvas completely<br>\nSelected colour will become background colour</html>");
        buttonSolidColour.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonSolidColour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSolidColourActionPerformed(evt);
            }
        });

        buttonGroupTools.add(toggleButtonPencil);
        toggleButtonPencil.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/pencil.png"))); // NOI18N
        toggleButtonPencil.setSelected(true);
        toggleButtonPencil.setText("Pencil");
        toggleButtonPencil.setToolTipText("<html>Left-click to paint with selected colour<br>\nRight-click to paint with background colour</html>");
        toggleButtonPencil.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        toggleButtonPencil.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleButtonPencilActionPerformed(evt);
            }
        });

        buttonGroupTools.add(toggleButtonEraser);
        toggleButtonEraser.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/sponge.png"))); // NOI18N
        toggleButtonEraser.setText("Eraser");
        toggleButtonEraser.setToolTipText("Click to erase to transparency");
        toggleButtonEraser.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        toggleButtonEraser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleButtonEraserActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOk.setText("OK");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        panelColours.setLayout(new java.awt.GridLayout(0, 4));

        rendererPreviewer1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        rendererPreviewer1.setToolTipText("Preview of pattern");

        buttonClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/clear_selection.png"))); // NOI18N
        buttonClear.setText("Clear");
        buttonClear.setToolTipText("<html>Clear entire canvas to transparency<br>\nSet background colour to transparency</html>");
        buttonClear.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonClearActionPerformed(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont().deriveFont((jLabel1.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel1.setText("Left-click to paint with selected colour; right-click for background colour");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(iconEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rendererPreviewer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(panelColours, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(buttonClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(toggleButtonEraser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(toggleButtonPencil, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(buttonSolidColour, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonSolidColour)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toggleButtonPencil)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toggleButtonEraser)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonClear)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelColours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rendererPreviewer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(iconEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOk)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonSolidColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSolidColourActionPerformed
        final Color selectedColour = JColorChooser.showDialog(this, "Choose A Colour", Color.ORANGE);
        if (selectedColour != null) {
            iconEditor1.fill(selectedColour.getRGB());
            colour = selectedColour;
            eraseColour = selectedColour.getRGB();
            iconEditor1.setEraseColour(eraseColour);
            pattern = null;
            updatePreview();
        }
    }//GEN-LAST:event_buttonSolidColourActionPerformed

    private void toggleButtonPencilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleButtonPencilActionPerformed
        iconEditor1.setPaintColour(paintColour.getRGB());
        jLabel1.setText("Left-click to paint with selected colour; right-click for background colour");
    }//GEN-LAST:event_toggleButtonPencilActionPerformed

    private void toggleButtonEraserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleButtonEraserActionPerformed
        iconEditor1.setPaintColour(0x00ffffff);
        jLabel1.setText("Click to erase to transparency");
    }//GEN-LAST:event_toggleButtonEraserActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        ok();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        iconEditor1.fill(0x00ffffff);
        colour = null;
        eraseColour = 0x00ffffff;
        iconEditor1.setEraseColour(eraseColour);
        pattern = iconEditor1.getIcon();
        updatePreview();
    }//GEN-LAST:event_buttonClearActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonClear;
    private javax.swing.ButtonGroup buttonGroupColours;
    private javax.swing.ButtonGroup buttonGroupTools;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonSolidColour;
    private org.pepsoft.worldpainter.util.IconEditor iconEditor1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel panelColours;
    private org.pepsoft.worldpainter.layers.renderers.RendererPreviewer rendererPreviewer1;
    private javax.swing.JToggleButton toggleButtonEraser;
    private javax.swing.JToggleButton toggleButtonPencil;
    // End of variables declaration//GEN-END:variables

    private Color colour, paintColour = Color.BLACK;
    private int eraseColour;
    private BufferedImage pattern;

    private static final int[] EGA_COLOURS = {
            0x000000,
            0x0000AA,
            0x00AA00,
            0x00AAAA,
            0xAA0000,
            0xAA00AA,
            0xAA5500,
            0xAAAAAA,
            0x555555,
            0x5555FF,
            0x55FF55,
            0x55FFFF,
            0xFF5555,
            0xFF55FF,
            0xFFFF55,
            0xFFFFFF
    };

    private static final String[] EGA_NAMES = {
            "Black",
            "Blue",
            "Green",
            "Cyan",
            "Red",
            "Magenta",
            "Brown",
            "Light Grey",
            "Dark Grey",
            "Bright Blue",
            "Bright Green",
            "Bright Cyan",
            "Bright Red",
            "Bright Magenta",
            "Bright Yellow",
            "Bright White"
    };
}