/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ProgressDialog.java
 *
 * Created on 17-okt-2011, 14:52:21
 */
package org.pepsoft.util.swing;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.Callable;

/**
 *
 * @author pepijn
 */
public class ProgressDialog<T> extends javax.swing.JDialog implements ComponentListener, ProgressComponent.Listener<T> {
    /**
     * Creates new form ProgressDialog.
     *
     * @param parent The parent window for the modal dialog.
     * @param task The task to execute.
     * @param options Optional modifiers to change the behaviour. See
     * {@link #NOT_CANCELABLE} and {@link #NO_FOCUS_STEALING}.
     */
    public ProgressDialog(Window parent, ProgressTask<T> task, Option... options) {
        super(parent, ModalityType.APPLICATION_MODAL);
        initComponents();
        setTitle(task.getName());
        progressComponent1.setListener(this);
        progressComponent1.setTask(task);
        if (options != null) {
            for (Option option: options) {
                option.apply(this);
            }
        }
        setLocationRelativeTo(parent);
        addComponentListener(this);
    }

    /**
     * When invoked with {@code true}, displays the dialog and starts the
     * configured {@link ProgressTask} in a background thread, then blocks until
     * the task has completed and the dialog is disposed of. Events are
     * dispatched while this method is blocked.
     *
     * @param b {@code true} to show the dialog and start the task in a
     *          background thread.
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
    }

    /**
     * Execute a task in the background with progress reporting via a modal
     * dialog with a progress bar. The task is executed on a separate thread.
     * This method blocks until the task has completed, but events are
     * dispatched while the method is blocked. If the task throws an exception,
     * that exception will be rethrown by this method.
     *
     * <p>By default the Cancel button is enabled and the popup will steal the
     * keyboard focus. Use one or more of the {@link #NOT_CANCELABLE} and
     * {@link #NO_FOCUS_STEALING} options to modify this.
     *
     * @param parent The parent window for the modal dialog.
     * @param task The task to execute.
     * @param <T> The return type of the task. Use {@link Void} for tasks which
     *     don't return a value.
     * @param options Optional modifiers to change the behaviour. See
     * {@link #NOT_CANCELABLE} and {@link #NO_FOCUS_STEALING}.
     * @return The result of the task, or {@code null} if the task does not
     *     return a result or if it was cancelled.
     * @throws Error If the task threw an {@link Error}.
     * @throws RuntimeException If the task threw a {@link RuntimeException}.
     */
    public static <T> T executeTask(Window parent, ProgressTask<T> task, Option... options) {
        ProgressDialog<T> dialog = new ProgressDialog<>(parent, task, options);
        dialog.setVisible(true);
        if (dialog.cancelled) {
            return null;
        } else if (dialog.exception != null) {
            if (dialog.exception instanceof Error) {
                throw (Error) dialog.exception;
            } else if (dialog.exception instanceof RuntimeException) {
                throw (RuntimeException) dialog.exception;
            } else {
                throw new MDCCapturingRuntimeException("Checked exception thrown by task", dialog.exception);
            }
        } else {
            return dialog.result;
        }
    }

    /**
     * Execute a task in the background with progress reporting via a modal
     * dialog with a progress bar. The task is executed on a separate thread.
     * This method blocks until the task has completed, but events are
     * dispatched while the method is blocked. If the task throws an exception,
     * that exception will be rethrown by this method.
     *
     * <p>By default the Cancel button is enabled and the popup will steal the
     * keyboard focus. Use one or more of the {@link #NOT_CANCELABLE} and
     * {@link #NO_FOCUS_STEALING} options to modify this.
     *
     * @param parent The parent window for the modal dialog.
     * @param name The name of the task to execute. Will be displayed to the
     *             user.
     * @param task The task to execute.
     * @param <T> The return type of the task. Use {@link Void} for tasks which
     *     don't return a value.
     * @param options Optional modifiers to change the behaviour. See
     * {@link #NOT_CANCELABLE} and {@link #NO_FOCUS_STEALING}.
     * @return The result of the task, or {@code null} if the task does not
     *     return a result or if it was cancelled.
     * @throws Error If the task threw an {@link Error}.
     * @throws RuntimeException If the task threw a {@link RuntimeException}.
     */
    public static <T> T executeTask(Window parent, String name, Callable<T> task, Option... options) {
        return executeTask(parent, new ProgressTask<T>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public T execute(ProgressReceiver progressReceiver) {
                try {
                    return task.call();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Exception e) {
                    throw new MDCCapturingRuntimeException(e.getClass().getSimpleName() + " while performing task \"" + name + "\" (message: " + e.getMessage() + ")", e);
                }
            }
        }, options);
    }

    // ComponentListener
    
    @Override
    public synchronized void componentShown(ComponentEvent e) {
        progressComponent1.start();
    }

    @Override public void componentResized(ComponentEvent e) {}
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}

    // ProgressComponent.Listener

    @Override
    public void exceptionThrown(Throwable exception) {
        this.exception = exception;
        dispose();
    }

    @Override
    public void done(T result) {
        this.result = result;
        dispose();
    }

    @Override
    public void cancelled() {
        cancelled = true;
        dispose();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progressComponent1 = new org.pepsoft.util.swing.ProgressComponent<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressComponent1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressComponent1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.pepsoft.util.swing.ProgressComponent<T> progressComponent1;
    // End of variables declaration//GEN-END:variables

    private boolean cancelled;
    private Throwable exception;
    private T result;
    
    private static final long serialVersionUID = 2011101701L;

    public static abstract class Option {
        abstract void apply(ProgressDialog<?> dialog);
    }

    /**
     * Option to pass to make the Cancel button inactive, forcing the user to
     * wait until the task is completed.
     */
    public static final Option NOT_CANCELABLE = new Option() {
        @Override
        void apply(ProgressDialog<?> dialog) {
            dialog.progressComponent1.setCancelable(false);
        }
    };

    /**
     * Option to pass to prevent the popup that opens while the task is running
     * to display its progress, from stealing the keyboard focus.
     */
    public static final Option NO_FOCUS_STEALING = new Option() {
        @Override
        void apply(ProgressDialog<?> dialog) {
            dialog.setAutoRequestFocus(false);
        }
    };
}