/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ProgressComponent.java
 *
 * Created on Apr 19, 2012, 7:02:06 PM
 */
package org.pepsoft.util.swing;

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.slf4j.MDC;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.pepsoft.util.AwtUtils.doOnEventThreadAndWait;

/**
 * A component which can execute a task in the background, reporting its
 * progress on a progress bar, displaying status messages from the task, and
 * optionally allowing the user to cancel the task.
 *
 * @author pepijn
 */
public class ProgressComponent<T> extends javax.swing.JPanel implements ProgressReceiver, ActionListener {
    /**
     * Creates a new ProgressComponent
     */
    public ProgressComponent() {
        initComponents();
    }

    public void setListener(Listener<T> listener) {
        this.listener = listener;
    }

    public Listener<T> getListener() {
        return listener;
    }

    public void setTask(ProgressTask<T> task) {
        this.task = task;
        jLabel1.setText(task.getName());
    }

    public ProgressTask<?> getTask() {
        return task;
    }
    
    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }
    
    public boolean getCancelable() {
        return cancelable;
    }

    public void start() {
        if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.devMode"))) {
            stats = new ArrayList<>();
        }
        jButton1.setEnabled(cancelable);
        jProgressBar1.setIndeterminate(true);
        final Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
        final Thread thread = new Thread(task.getName()) {
            @Override
            public void run() {
                if (mdcContextMap != null) {
                    MDC.setContextMap(mdcContextMap);
                } else {
                    MDC.clear();
                }
                try {
                    try {
                        result = task.execute(ProgressComponent.this);
                    } catch (RuntimeException | Error e) {
                        throw new MDCCapturingRuntimeException(e);
                    }
                    done();
                } catch (Throwable t) {
                    exceptionThrown(t);
                } finally {
                    MDC.clear();
                }
            }
        };
        thread.start();
        start = System.currentTimeMillis();
        timer = new Timer(1000, this);
        timer.start();
    }

    // ProgressReceiver
    
    @Override
    public void setProgress(final float progress) throws OperationCancelled {
        checkForCancellation();
        doOnEventThreadAndWait(() -> {
            progressReports++;
            long now = System.currentTimeMillis();
            long elapsed = now - start;
            float speed = elapsed / progress;
            remaining = (long) ((1.0f - progress) * speed);
            lastUpdate = now;
            if ((! timeEstimatesActivated) && (elapsed >= 60000) && (progressReports >= 10)) {
                timeEstimatesActivated = true;
            }
            if (jProgressBar1.isIndeterminate()) {
                jProgressBar1.setIndeterminate(false);
            }
            jProgressBar1.setValue(Math.round(progress * 100f));
        });
    }

    @Override
    public void exceptionThrown(final Throwable exception) {
        if (! exceptionReported) {
            doOnEventThreadAndWait(() -> {
                timer.stop();
                if (jProgressBar1.isIndeterminate()) {
                    jProgressBar1.setIndeterminate(false);
                }
                jButton1.setEnabled(false);
                inhibitDone = true;
                if (exception instanceof OperationCancelled) {
                    jLabel2.setText("Cancelled");
                    if (listener != null) {
                        listener.cancelled();
                    }
                } else {
                    jLabel2.setText("Error");
                    if (listener != null) {
                        listener.exceptionThrown(exception);
                    }
                }
            });
            exceptionReported = true;
        }
    }

    @Override
    public void done() {
        doOnEventThreadAndWait(() -> {
            timer.stop();
            if (jProgressBar1.isIndeterminate()) {
                jProgressBar1.setIndeterminate(false);
            }
            jProgressBar1.setValue(100);
            jButton1.setEnabled(false);
            jLabel2.setText("Done");
            if (stats != null) {
                try (PrintWriter out = new PrintWriter("logs/" + FileUtils.sanitiseName(task.getName() + "-" + new Date() + ".csv"))) {
                    int second = 1;
                    out.println("second,calculated,displayed");
                    for (int[] statsRow : stats) {
                        out.println(second++ + "," + statsRow[0] + "," + statsRow[1]);
                    }
                } catch (IOException e) {
                    logger.error("I/O error while dumping statistics", e);
                }
            }
            if ((listener != null) && (! inhibitDone)) {
                listener.done(result);
            }
        });
    }
    
    @Override
    public void setMessage(final String message) throws OperationCancelled {
        checkForCancellation();
        doOnEventThreadAndWait(() -> jLabel1.setText(task.getName() + ((message != null) ? (", " + message) : "")));
    }

    @Override
    public void checkForCancellation() throws OperationCancelled {
        if (cancelRequested) {
            throw new OperationCancelledByUser();
        }
    }

    @Override
    public void reset() throws OperationCancelled {
        checkForCancellation();
        doOnEventThreadAndWait(() -> {
            if (stats != null) {
                try {
                    try (PrintWriter out = new PrintWriter("logs/" + FileUtils.sanitiseName(task.getName() + "-" + new Date() + ".csv"))) {
                        int second = 1;
                        out.println("second,calculated,displayed");
                        for (int[] statsRow : stats) {
                            out.println(second++ + "," + statsRow[0] + "," + statsRow[1]);
                        }
                    }
                } catch (IOException e) {
                    logger.error("I/O error while dumping statistics", e);
                }
                stats = new ArrayList<>();
            }
            jProgressBar1.setIndeterminate(true);
            start = System.currentTimeMillis();
            progressReports = 0;
            lastReportedMinutes = Integer.MAX_VALUE;
            timeEstimatesActivated = false;
            jLabel2.setText(" ");
        });
    }

    @Override
    public void subProgressStarted(SubProgressReceiver subProgressReceiver) {
        // Do nothing
    }

    // ActionListener
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (timeEstimatesActivated) {
            long now = System.currentTimeMillis();
            remaining -= (now - lastUpdate);
            lastUpdate = now;
            int minutes = (int) (remaining / 60000);
            if ((minutes < lastReportedMinutes) || (minutes > (lastReportedMinutes + 1))) {
                lastReportedMinutes = minutes;
                if (minutes < 1) {
                    jLabel2.setText("Less than a minute remaining");
                } else if (minutes < 90) {
                    jLabel2.setText("About " + (minutes + 1) + " minutes remaining");
                } else {
                    int hours = (minutes + 30) / 60;
                    jLabel2.setText("About " + hours + " hours remaining");
                }
            }
            if (stats != null) {
                stats.add(new int[] {minutes, lastReportedMinutes + 1});
            }
        } else if (stats != null) {
            stats.add(new int[] {-1, -1});
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        jLabel1.setText(" ");

        jLabel2.setText(" ");

        jButton1.setText("Cancel");
        jButton1.setEnabled(false);
        jButton1.addActionListener(this::jButton1ActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
            .addComponent(jLabel1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 313, Short.MAX_VALUE)
                .addComponent(jButton1))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jLabel2)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        cancelRequested = true;
        jButton1.setEnabled(false);
    }//GEN-LAST:event_jButton1ActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables

    private ProgressTask<T> task;
    private volatile boolean cancelRequested;
    private volatile T result;
    private long start, remaining, lastUpdate;
    private int progressReports, lastReportedMinutes = Integer.MAX_VALUE;
    private Timer timer;
    private Listener<T> listener;
    private boolean timeEstimatesActivated, inhibitDone, cancelable = true, exceptionReported;
    private List<int[]> stats;
 
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProgressComponent.class);
    private static final long serialVersionUID = 1L;
    
    public interface Listener<T> {
        void exceptionThrown(Throwable exception);
        
        void done(T result);
        
        void cancelled();
    }
}