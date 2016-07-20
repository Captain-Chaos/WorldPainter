/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * RotateWorldDialog.java
 *
 * Created on Apr 14, 2012, 3:57:24 PM
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.history.HistoryEntry;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class RotateWorldDialog extends javax.swing.JDialog implements ProgressReceiver {
    /** Creates new form RotateWorldDialog */
    public RotateWorldDialog(java.awt.Frame parent, World2 world, int dim) {
        super(parent, true);
        this.world = world;
        this.dim = dim;
        Dimension opposite = null;
        switch (dim) {
            case DIM_NORMAL:
                opposite = world.getDimension(DIM_NORMAL_CEILING);
                break;
            case DIM_NORMAL_CEILING:
                opposite = world.getDimension(DIM_NORMAL);
                break;
            case DIM_END:
                opposite = world.getDimension(DIM_END_CEILING);
                break;
            case DIM_END_CEILING:
                opposite = world.getDimension(DIM_END);
                break;
            case DIM_NETHER:
                opposite = world.getDimension(DIM_NETHER_CEILING);
                break;
            case DIM_NETHER_CEILING:
                opposite = world.getDimension(DIM_NETHER);
                break;
        }
        if (opposite != null) {
            oppositeDim = opposite.getDim();
        } else {
            oppositeDim = -1;
        }
        
        initComponents();
        jCheckBox1.setEnabled(oppositeDim != -1);

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
        
        getRootPane().setDefaultButton(buttonRotate);
        
        setLocationRelativeTo(parent);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    // ProgressReceiver
    
    @Override
    public synchronized void setProgress(final float progress) throws OperationCancelled {
        doOnEventThread(() -> jProgressBar1.setValue((int) (progress * 100)));
    }

    @Override
    public synchronized void exceptionThrown(final Throwable exception) {
        doOnEventThread(() -> {
            ErrorDialog errorDialog = new ErrorDialog(RotateWorldDialog.this);
            errorDialog.setException(exception);
            errorDialog.setVisible(true);
            dispose();
        });
    }

    @Override
    public synchronized void done() {
        doOnEventThread(() -> {
            cancelled = false;
            dispose();
        });
    }

    @Override
    public synchronized void setMessage(final String message) throws OperationCancelled {
        doOnEventThread(() -> labelProgressMessage.setText(message));
    }

    @Override
    public synchronized void checkForCancellation() throws OperationCancelled {
        // Do nothing
    }

    @Override
    public void reset() {
        doOnEventThread(() -> jProgressBar1.setValue(0));
    }

    @Override
    public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {
        // Do nothing
    }

    private void rotate() {
        buttonRotate.setEnabled(false);
        buttonCancel.setEnabled(false);
        final CoordinateTransform transform;
        final int degrees;
        if (jRadioButton1.isSelected()) {
            transform = CoordinateTransform.ROTATE_CLOCKWISE_90_DEGREES;
            degrees = 90;
        } else if (jRadioButton2.isSelected()) {
            transform = CoordinateTransform.ROTATE_180_DEGREES;
            degrees = 180;
        } else {
            transform = CoordinateTransform.ROTATE_CLOCKWISE_270_DEGREES;
            degrees = 270;
        }
        new Thread("World Rotator") {
            @Override
            public void run() {
                try {
                    if ((oppositeDim == -1) || (! jCheckBox1.isSelected())) {
                        world.transform(dim, transform, RotateWorldDialog.this);
                        world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_ROTATED, world.getDimension(dim).getName(), degrees);
                    } else {
                        world.transform(dim, transform, new SubProgressReceiver(RotateWorldDialog.this, 0.0f, 0.5f));
                        world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_ROTATED, world.getDimension(dim).getName(), degrees);
                        world.transform(oppositeDim, transform, new SubProgressReceiver(RotateWorldDialog.this, 0.5f, 0.5f));
                        world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_ROTATED, world.getDimension(oppositeDim).getName(), degrees);
                    }
                    done();
                } catch (Throwable t) {
                    exceptionThrown(t);
                }
            }
        }.start();
    }
    
    private void doOnEventThread(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        buttonCancel = new javax.swing.JButton();
        buttonRotate = new javax.swing.JButton();
        labelProgressMessage = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Rotate World");
        setResizable(false);

        jLabel1.setText("Choose a rotation angle and press the Rotate button to rotate the world:");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonRotate.setText("Rotate");
        buttonRotate.addActionListener(this::buttonRotateActionPerformed);

        labelProgressMessage.setText(" ");

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("90 degrees clockwise");

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("180 degrees");

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("90 degrees anticlockwise");

        jLabel2.setText("<html><em>This operation cannot be undone!</em>   </html>");

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("also rotate corresponding ceiling or surface");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonRotate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBox1)
                            .addComponent(jLabel1)
                            .addComponent(labelProgressMessage)
                            .addComponent(jRadioButton1)
                            .addComponent(jRadioButton2)
                            .addComponent(jRadioButton3)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jRadioButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRadioButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox1)
                .addGap(18, 18, 18)
                .addComponent(labelProgressMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonRotate))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonRotateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRotateActionPerformed
        rotate();
    }//GEN-LAST:event_buttonRotateActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonRotate;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JLabel labelProgressMessage;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final int dim, oppositeDim;
    private boolean cancelled = true;
    
    private static final long serialVersionUID = 1L;
}