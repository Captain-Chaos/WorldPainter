/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ExportWorldDialog.java
 *
 * Created on Mar 29, 2011, 5:09:50 PM
 */

package org.pepsoft.worldpainter;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.*;

import org.pepsoft.util.swing.ProgressComponent.Listener;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pepijn
 */
public abstract class MultiProgressDialog<T> extends javax.swing.JDialog implements Listener<T>, ComponentListener {
    /** Creates new form ExportWorldDialog */
    public MultiProgressDialog(Window parent, String title) {
        super(parent, ModalityType.APPLICATION_MODAL);
        initComponents();
        setTitle(title);

        setLocationRelativeTo(parent);
        
        addComponentListener(this);
    }

    /**
     * Get the unconjugated verb describing the operation, starting with a
     * capital letter.
     * 
     * @return The unconjugated verb describing the operation.
     */
    protected abstract String getVerb();
    
    /**
     * Transform the results object into a text describing the results, suitable
     * for inclusion in a {@link JOptionPane}. HTML is allowed, and must be
     * enclosed in &lt;html&gt;&lt;/html&gt; tags
     * 
     * @param results The result returned by the task.
     * @param duration The duration in ms.
     * @return A text containing a report of the results.
     */
    protected abstract String getResultsReport(T results, long duration);
    
    /**
     * Get the message to show to the user in a {@link JOptionPane} after they
     * cancel the operation.
     * 
     * @return The message to show to the user in a {@link JOptionPane} after
     * they cancel the operation.
     */
    protected abstract String getCancellationMessage();
    
    /**
     * Get the task to perform. The task may use nested
     * {@link SubProgressReceiver}s to report progress, which will be reported
     * separately on the screen.
     * 
     * @return The task to perform.
     */
    protected abstract ProgressTask<T> getTask();

    /**
     * Add a {@link JButton} to the panel, to the left of the Cancel button.
     *
     * @param button The button to add.
     */
    protected void addButton(JButton button) {
        multiProgressComponent1.addButton(button);
    }

    // ProgressComponent.Listener
    
    @Override
    public void exceptionThrown(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof FileInUseException) {
            JOptionPane.showMessageDialog(MultiProgressDialog.this, "Could not " + getVerb().toLowerCase() + " the world because the existing map directory is in use.\nPlease close Minecraft and all other windows and try again.", "Map In Use", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof MissingCustomTerrainException) {
            JOptionPane.showMessageDialog(MultiProgressDialog.this,
                "Custom Terrain " + ((MissingCustomTerrainException) exception).getIndex() + " not configured!\n" +
                "Please configure it on the Custom Terrain panel.\n" +
                "\n" +
                "The partially processed map is now probably corrupted.\n" +
                "You should delete it, or export the map again.", "Unconfigured Custom Terrain", JOptionPane.ERROR_MESSAGE);
        } else {
            ErrorDialog dialog = new ErrorDialog(MultiProgressDialog.this);
            dialog.setException(exception);
            dialog.setVisible(true);
        }
        close();
    }

    @Override
    public void done(T result) {
        long end = System.currentTimeMillis();
        long duration = (end - start) / 1000;
        String resultsReport = getResultsReport(result, duration);
        JOptionPane.showMessageDialog(this, resultsReport, "Success", JOptionPane.INFORMATION_MESSAGE);
        close();
    }

    @Override
    public void cancelled() {
        logger.info(getVerb() + " cancelled by user");
        JOptionPane.showMessageDialog(this, getCancellationMessage(), getVerb() + " Cancelled", JOptionPane.WARNING_MESSAGE);
        close();
    }

    // ComponentListener
    
    @Override
    public void componentShown(ComponentEvent e) {
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        multiProgressComponent1.setTask(getTask());
        multiProgressComponent1.setListener(this);
        start = System.currentTimeMillis();
        multiProgressComponent1.start();
    }

    @Override public void componentResized(ComponentEvent e) {}
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}

    // Implementation details
    
    private void close() {
        dispose();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        multiProgressComponent1 = new org.pepsoft.util.swing.MultiProgressComponent();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(multiProgressComponent1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(multiProgressComponent1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.pepsoft.util.swing.MultiProgressComponent<T> multiProgressComponent1;
    // End of variables declaration//GEN-END:variables

    private long start;

    private static final Logger logger = LoggerFactory.getLogger(MultiProgressDialog.class);
    private static final long serialVersionUID = 1L;
}