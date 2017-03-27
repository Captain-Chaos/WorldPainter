/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pepijn Schmitz
 * @param <L> The type of layer which the dialog should edit.
 */
public class EditLayerDialog<L extends Layer> extends WorldPainterDialog implements LayerEditor.LayerEditorContext, ActionListener {
    /**
     * Creates new form EditLayerDialog for creating a new instance of a
     * specific layer type.
     * 
     * @param parent The window relative to which to display the dialog.
     * @param layerType The type of layer of which to create a new instance.
     */
    public EditLayerDialog(Window parent, Class<L> layerType) {
        this(parent, null, LayerEditorManager.getInstance().createEditor(layerType));
    }
    
    /**
     * Creates new form EditLayerDialog for editing an existing layer.
     * 
     * @param parent The window relative to which to display the dialog.
     * @param layer The layer to edit..
     */
    public EditLayerDialog(Window parent, L layer) {
        this(parent, layer, LayerEditorManager.getInstance().createEditor((Class<L>) layer.getClass()));
    }

    private EditLayerDialog(Window parent, L layer, LayerEditor<L> editor) {
        super(parent);
        if (editor == null) {
            throw new IllegalArgumentException("No editor available for layer type" + ((layer != null) ? layer.getClass() : ""));
        }
        this.editor = editor;
        if (layer == null) {
            layer = editor.createLayer();
        }
        this.app = App.getInstance();
        previewCreator = LayerPreviewCreator.createPreviewerForLayer(layer, app.getDimension());

        initComponents();
        if (! (layer instanceof CustomLayer)) {
            setIconImage(layer.getIcon());
        }
        
        previewTimer.setRepeats(false);
        
        editor.setContext(this);
        editor.setLayer(layer);
        JComponent editorComponent = editor.getComponent();
        editorPanel.add(editorComponent, BorderLayout.CENTER);
        // For some strange reason the look&feel isn't applied by Swing
        SwingUtilities.updateComponentTreeUI(editorComponent);

        buttonOK.setEnabled(editor.isCommitAvailable());
        
        jComboBox1.setModel(new DefaultComboBoxModel(LayerPreviewCreator.PATTERNS));
        jComboBox1.setRenderer(new PatternListCellRenderer());
        jComboBox1.setSelectedItem(previewCreator.getPattern());

        dynMapPreviewer1.setZoom(-2);
        dynMapPreviewer1.setInclination(30.0);
        dynMapPreviewer1.setAzimuth(60.0);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cancelPreviewUpdate();
            }
        });

        pack();
        setLocationRelativeTo(parent);
    }

    public L getLayer() {
        return editor.getLayer();
    }
    
    // LayerEditorContext
    
    @Override
    public void settingsChanged() {
        boolean commitAvailable = editor.isCommitAvailable();
        buttonOK.setEnabled(commitAvailable);
        if (commitAvailable) {
            schedulePreviewUpdate();
        } else {
            cancelPreviewUpdate();
        }
    }

    @Override
    public Dimension getDimension() {
        return app.getDimension();
    }

    @Override
    public ColourScheme getColourScheme() {
        return app.getColourScheme();
    }

    @Override
    public boolean isExtendedBlockIds() {
        return app.getWorld().isExtendedBlockIds();
    }

    @Override
    public CustomBiomeManager getCustomBiomeManager() {
        return app.getCustomBiomeManager();
    }

    @Override
    public List<Layer> getAllLayers() {
        return new ArrayList<>(app.getAllLayers());
    }

    // ActionListener
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == previewTimer) {
            updatePreview();
        }
    }
    
    private void schedulePreviewUpdate() {
        previewTimer.restart();
    }
    
    private void cancelPreviewUpdate() {
        previewTimer.stop();
        synchronized (PREVIEW_STATE_LOCK) {
            // We can't interrupt an existing render, but we can make sure
            // another one isn't started immediately after
            if (previewRenderState == PreviewRenderState.SECOND_RENDER_SCHEDULED) {
                previewRenderState = PreviewRenderState.RENDERING;
            }
        }
    }
    
    private void updatePreview() {
        // Check again whether the current settings are valid, although the
        // chance is remote
        if (! editor.isCommitAvailable()) {
            return;
        }
        final ExporterSettings settings = editor.getSettings();
        synchronized (PREVIEW_STATE_LOCK) {
            switch (previewRenderState) {
                case IDLE:
                    // Not rendering anything yet
                    new Thread("Preview Creator for " + editor.getLayer().getName()) {
                        @Override
                        public void run() {
renderLoop:                 do {
                                synchronized (PREVIEW_RENDERER_LOCK) {
                                    previewCreator.setLayer(settings.getLayer());
                                    previewCreator.setSettings(settings);
                                    final MinecraftWorldObject preview = previewCreator.renderPreview();
                                    SwingUtilities.invokeLater(() -> dynMapPreviewer1.setObject(preview, app.getDimension()));
                                }
                                synchronized (PREVIEW_STATE_LOCK) {
                                    switch (previewRenderState) {
                                        case SECOND_RENDER_SCHEDULED:
                                            previewRenderState = PreviewRenderState.RENDERING;
                                            break; // Loop again
                                        case RENDERING:
                                            previewRenderState = PreviewRenderState.IDLE;
                                            // Fall through to break loop
                                        default:
                                            // Break loop
                                            break renderLoop;
                                    }
                                }
                            } while(true);
                        }
                    }.start();
                    previewRenderState = PreviewRenderState.RENDERING;
                    break;
                case RENDERING:
                    // A preview is already being rendered; set the state such
                    // that when it is finished another render will immediately
                    // be started
                    previewRenderState = PreviewRenderState.SECOND_RENDER_SCHEDULED;
                    break;
                case SECOND_RENDER_SCHEDULED:
                    // A second render has already been scheduled for when the
                    // current render is finished. There is nothing more we can
                    // do
                    break;
            }
        }
    }

    private void updatePattern() {
        previewCreator.setPattern((LayerPreviewCreator.Pattern) jComboBox1.getSelectedItem());
        updatePreview();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editorPanel = new javax.swing.JPanel();
        dynMapPreviewer1 = new org.pepsoft.worldpainter.dynmap.DynMapPreviewer();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Layer Settings");

        editorPanel.setLayout(new java.awt.BorderLayout());

        dynMapPreviewer1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel1.setText("Preview pattern:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(editorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dynMapPreviewer1, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(buttonOK)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCancel))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(dynMapPreviewer1, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonCancel)
                            .addComponent(buttonOK)))
                    .addComponent(editorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        editor.commit();
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        updatePattern();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private org.pepsoft.worldpainter.dynmap.DynMapPreviewer dynMapPreviewer1;
    private javax.swing.JPanel editorPanel;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

    private final LayerEditor<L> editor;
    private final Timer previewTimer = new Timer(1000, this);
    private final LayerPreviewCreator previewCreator;
    private final App app;
    private PreviewRenderState previewRenderState = PreviewRenderState.IDLE;
    
    /**
     * This lock guards the preview rendering state.
     */
    private static final Object PREVIEW_STATE_LOCK = new Object();
    /**
     * The preview renderer is stateful; this lock prevents more than one thread
     * from using it at once.
     */
    private static final Object PREVIEW_RENDERER_LOCK = new Object();
    
    enum PreviewRenderState {IDLE, RENDERING, SECOND_RENDER_SCHEDULED}
}