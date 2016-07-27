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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.util.swing.ProgressComponent.Listener;

/**
 * A component which can execute a task in the background, reporting its
 * progress on a progress bar, displaying status messages from the task, and
 * optionally allowing the user to cancel the task.
 *
 * @author pepijn
 */
public class MultiProgressComponent<T> extends javax.swing.JPanel implements ProgressReceiver, ActionListener {
    /**
     * Creates a new ProgressComponent
     */
    public MultiProgressComponent() {
        initComponents();

        // Size the scroll pane so that it can show eight progress viewers
        Dimension panelPrefdSize = scrollablePanel1.getPreferredSize();
        ProgressViewer testProgressViewer = new ProgressViewer();
        scrollablePanel1.add(testProgressViewer);
        panelPrefdSize.setSize(panelPrefdSize.getWidth(), testProgressViewer.getPreferredSize().getHeight() * 8);
        scrollablePanel1.remove(testProgressViewer);
        jScrollPane1.setMinimumSize(panelPrefdSize);

        scrollablePanel1.setTrackViewportWidth(true);
        scrollablePanel1.setTrackViewportHeight(false);
    }

    public void setListener(Listener<T> listener) {
        this.listener = listener;
    }

    public Listener<T> getListener() {
        return listener;
    }

    public void setTask(ProgressTask<T> task) {
        this.task = task;
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
        thread = new Thread(task.getName()) {
            @Override
            public void run() {
                try {
                    result = task.execute(MultiProgressComponent.this);
                    done();
                } catch (Throwable t) {
                    exceptionThrown(t);
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
    public synchronized void setProgress(final float progress) throws OperationCancelled {
        checkForCancellation();
        doOnEventThread(() -> {
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
            jProgressBar1.setValue((int) (progress * 100f + 0.5f));
        });
    }

    @Override
    public synchronized void exceptionThrown(final Throwable exception) {
        if (! exceptionReported) {
            doOnEventThread(() -> {
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
    public synchronized void done() {
        doOnEventThread(() -> {
            timer.stop();
            if (jProgressBar1.isIndeterminate()) {
                jProgressBar1.setIndeterminate(false);
            }
            jProgressBar1.setValue(100);
            jButton1.setEnabled(false);
            jLabel2.setText("Done");
            scrollablePanel1.removeAll();
            if (stats != null) {
                try (PrintWriter out = new PrintWriter(new File("logs/" + FileUtils.sanitiseName(task.getName() + "-" + new Date() + ".csv")))) {
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
    public synchronized void setMessage(final String message) throws OperationCancelled {
        checkForCancellation();
    }

    @Override
    public synchronized void checkForCancellation() throws OperationCancelled {
        if (cancelRequested) {
            throw new OperationCancelledByUser();
        }
    }

    @Override
    public synchronized void reset() throws OperationCancelled {
        checkForCancellation();
        doOnEventThread(() -> {
            if (stats != null) {
                try {
                    try (PrintWriter out = new PrintWriter(new File("logs/" + FileUtils.sanitiseName(task.getName() + "-" + new Date() + ".csv")))) {
                        int second = 1;
                        out.println("second,calculated,displayed");
                        for (int[] statsRow: stats) {
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
    public synchronized void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {
        checkForCancellation();
        doOnEventThread(() -> {
            synchronized (MultiProgressComponent.this) {
                ProgressViewer progressViewer = new ProgressViewer(subProgressReceiver);
                JPanel progressPanel = null;
                ProgressReceiver parent = subProgressReceiver.getParent();
                if (parent == null) {
                    // No parent; insert at start
                    scrollablePanel1.add(progressViewer, 0);
                    progressPanel = progressViewer;
                } else {
                    boolean parentFound = false;
                    do {
                        for (int i = 0; i < scrollablePanel1.getComponentCount(); i++) {
                            Component component = scrollablePanel1.getComponent(i);
                            ProgressViewer parentViewer;
                            try {
                                parentViewer = (ProgressViewer) ((component instanceof ProgressViewer) ? component : ((JPanel) component).getComponent(1));
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.println("Component " + i + " is a " + component.getClass());
                                throw e;
                            }
                            if (parentViewer.getSubProgressReceiver() == parent) {
                                // Progress viewer for parent found; insert below
                                Integer parentIndentation = (Integer) parentViewer.getClientProperty(CLIENT_PROPERTY_INDENTATION);
                                int indentation = (parentIndentation != null) ? parentIndentation + 1 : 1;
                                progressPanel = new JPanel();
                                progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.LINE_AXIS));
                                progressPanel.add(Box.createHorizontalStrut(indentation * INDENTATION_SIZE));
                                progressPanel.add(progressViewer);
                                scrollablePanel1.add(progressPanel, i + 1);
                                parentFound = true;
                                break;
                            }
                        }
                        if (parent instanceof SubProgressReceiver) {
                            parent = ((SubProgressReceiver) parent).getParent();
                        } else {
                            parent = null;
                        }
                    } while ((! parentFound) && (parent != null));
                    if (! parentFound) {
                        // Progress viewer not found for any ancestor; append to end
                        scrollablePanel1.add(progressViewer);
                        progressPanel = progressViewer;
                    }
                }

                final Component componentToRemove = progressPanel;
                subProgressReceiver.addListener(new ProgressReceiver() {
                    @Override
                    public void setProgress(float progress) throws OperationCancelled {
                        if (progress >= 1.0) {
                            doOnEventThread(() -> scrollablePanel1.remove(componentToRemove));
                        }
                    }

                    @Override
                    public void exceptionThrown(Throwable exception) {
                        doOnEventThread(() -> scrollablePanel1.remove(componentToRemove));
                    }

                    @Override
                    public void done() {
                        doOnEventThread(() -> scrollablePanel1.remove(componentToRemove));
                    }

                    @Override public void setMessage(String message) throws OperationCancelled {}
                    @Override public void checkForCancellation() throws OperationCancelled {}
                    @Override public void reset() throws OperationCancelled {}
                    @Override public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {}
                });
            }
            jScrollPane1.validate();
        });
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
    
    private void doOnEventThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
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

        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        scrollablePanel1 = new org.pepsoft.util.swing.ScrollablePanel();

        jLabel2.setText(" ");

        jButton1.setText("Cancel");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollablePanel1.setLayout(new java.awt.GridLayout(0, 1));
        jScrollPane1.setViewportView(scrollablePanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 313, Short.MAX_VALUE)
                .addComponent(jButton1))
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
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
    private javax.swing.JLabel jLabel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private org.pepsoft.util.swing.ScrollablePanel scrollablePanel1;
    // End of variables declaration//GEN-END:variables

    private ProgressTask<T> task;
    private volatile boolean cancelRequested;
    private volatile T result;
    private Thread thread;
    private long start, remaining, lastUpdate;
    private int progressReports, lastReportedMinutes = Integer.MAX_VALUE;
    private Timer timer;
    private Listener<T> listener;
    private boolean timeEstimatesActivated, inhibitDone, cancelable = true, exceptionReported;
    private List<int[]> stats;

    private static final String CLIENT_PROPERTY_INDENTATION = MultiProgressComponent.class.getName() + ".indentation";
    private static final int INDENTATION_SIZE = 32;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MultiProgressComponent.class);
    private static final long serialVersionUID = 1L;
}