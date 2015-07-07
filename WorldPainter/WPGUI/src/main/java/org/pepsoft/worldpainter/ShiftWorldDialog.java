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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.history.HistoryEntry;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class ShiftWorldDialog extends javax.swing.JDialog implements ProgressReceiver {
    /** Creates new form RotateWorldDialog */
    public ShiftWorldDialog(java.awt.Frame parent, World2 world, int dim) {
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
            oppositeDim = Integer.MIN_VALUE;
        }

        initComponents();
        jCheckBox1.setEnabled(oppositeDim != Integer.MIN_VALUE);

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
        
        getRootPane().setDefaultButton(buttonShift);
        
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
            ErrorDialog errorDialog = new ErrorDialog(ShiftWorldDialog.this);
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

    private void shift() {
        buttonShift.setEnabled(false);
        buttonCancel.setEnabled(false);
        final int east = (Integer) jSpinner1.getValue(), south = (Integer) jSpinner2.getValue();
        final CoordinateTransform transform = new Translation(east, south);
        new Thread("World Shifter") {
            @Override
            public void run() {
                try {
                    if ((oppositeDim == Integer.MIN_VALUE) || (! jCheckBox1.isSelected())) {
                        world.transform(dim, transform, ShiftWorldDialog.this);
                        world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_SHIFTED_HORIZONTALLY, world.getDimension(dim).getName(), east, south);
                    } else {
                        world.transform(dim, transform, new SubProgressReceiver(ShiftWorldDialog.this, 0.0f, 0.5f));
                        world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_SHIFTED_HORIZONTALLY, world.getDimension(dim).getName(), east, south);
                        world.transform(oppositeDim, transform, new SubProgressReceiver(ShiftWorldDialog.this, 0.5f, 0.5f));
                        world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_SHIFTED_HORIZONTALLY, world.getDimension(oppositeDim).getName(), east, south);
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
    
    private void setControlStates() {
        buttonShift.setEnabled((((Integer) jSpinner1.getValue()) != 0) || (((Integer) jSpinner2.getValue()) != 0));
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
        buttonShift = new javax.swing.JButton();
        labelProgressMessage = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSpinner1 = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jSpinner2 = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Shift World");
        setResizable(false);

        jLabel1.setText("Choose a shift amount and press the Shift button to shift the world horizontally (by whole tiles):");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonShift.setText("Shift");
        buttonShift.setEnabled(false);
        buttonShift.addActionListener(this::buttonShiftActionPerformed);

        labelProgressMessage.setText(" ");

        jLabel2.setText("X axis:");

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(0, -2147483648, 2147483647, 128));
        jSpinner1.addChangeListener(this::jSpinner1StateChanged);

        jLabel3.setText("Z axis:");

        jSpinner2.setModel(new javax.swing.SpinnerNumberModel(0, -2147483648, 2147483647, 128));
        jSpinner2.addChangeListener(this::jSpinner2StateChanged);

        jLabel4.setText("(negative values shift west; positive values shift east)");

        jLabel5.setText("(negative values shift north; positive values shift south)");

        jLabel6.setText("<html><em>This operation cannot be undone!</em>   </html>");

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("also shift corresponding ceiling or surface");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonShift)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(labelProgressMessage)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel5))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel4))))
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBox1))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox1)
                .addGap(18, 18, 18)
                .addComponent(labelProgressMessage)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonShift))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonShiftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShiftActionPerformed
        shift();
    }//GEN-LAST:event_buttonShiftActionPerformed

    private void jSpinner1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinner1StateChanged
        int value = (Integer) jSpinner1.getValue();
        if ((value % 128 ) != 0) {
            jSpinner1.setValue(Math.round(value / 128f) * 128);
        }
        setControlStates();
    }//GEN-LAST:event_jSpinner1StateChanged

    private void jSpinner2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinner2StateChanged
        int value = (Integer) jSpinner2.getValue();
        if ((value % 128 ) != 0) {
            jSpinner2.setValue(Math.round(value / 128f) * 128);
        }
        setControlStates();
    }//GEN-LAST:event_jSpinner2StateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonShift;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JLabel labelProgressMessage;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final int dim, oppositeDim;
    private boolean cancelled = true;
    
    private static final long serialVersionUID = 1L;
}