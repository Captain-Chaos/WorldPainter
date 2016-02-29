/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import org.pepsoft.util.ProgressReceiver;

/**
 * A named task which may report its progress and may return a value.
 *
 * @param <T> The return type of the task. Use {@link Void} if it does not
 *            return a value.
 * @author pepijn
 */
public interface ProgressTask<T> {
    /**
     * Get the short descriptive name of the task.
     *
     * @return The name of the task.
     */
    String getName();

    /**
     * Perform the task, optionally reporting progress to a progress receiver
     * and optionally returning a value.
     *
     * @param progressReceiver The progress receiver to which to report
     *                         progress. May be <code>null</code>.
     * @return The result of the task, or <code>null</code> if it does not
     *     return a result.
     * @throws ProgressReceiver.OperationCancelled If the user cancelled the
     *     operation, as indicated by the progress receiver throwing an
     *     <code>OperationCancelled</code> exception.
     */
    T execute(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled;
}