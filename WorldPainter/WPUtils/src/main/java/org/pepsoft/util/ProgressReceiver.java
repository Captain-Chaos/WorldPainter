package org.pepsoft.util;

public interface ProgressReceiver {
    /**
     * Set the progress as a value from 0.0f to 1.0f (inclusive).
     * 
     * @param progress The progress to report.
     * @throws org.pepsoft.util.ProgressReceiver.OperationCancelled If the
     *     operation has been canceled, for instance by the user, or because
     *     of an exception on another thread.
     */
    void setProgress(float progress) throws OperationCancelled;
    
    /**
     * Report that the operation has been aborted due to an exception having
     * been thrown. Any other threads working on the same operation will be
     * canceled.
     * 
     * @param exception The exception which has been thrown. 
     */
    void exceptionThrown(Throwable exception);
    
    /**
     * Report that the operation has succeeded successfully.
     */
    void done();
    
    /**
     * Change the message describing the current operation.
     * 
     * @param message A message describing the current operation.
     * @throws org.pepsoft.util.ProgressReceiver.OperationCancelled If the
     *     operation has been canceled, for instance by the user, or because
     *     of an exception on another thread.
     */
    void setMessage(String message) throws OperationCancelled;
    
    /**
     * Check whether the operation has been canceled. An
     * <code>OperationCancelled</code> exception will be thrown if it has.
     * 
     * @throws org.pepsoft.util.ProgressReceiver.OperationCancelled If the
     *     operation has been canceled, for instance by the user, or because
     *     of an exception on another thread.
     */
    void checkForCancellation() throws OperationCancelled;
    
    /**
     * Restart the progress bar. All progress will be wiped out and the next
     * expected progress report will be expected to be as if progress started
     * from 0.0f again.
     * 
     * @throws org.pepsoft.util.ProgressReceiver.OperationCancelled If the
     *     operation has been canceled, for instance by the user, or because
     *     of an exception on another thread.
     */
    void reset() throws OperationCancelled;

    /**
     * Invoked when a subordinate {@link SubProgressReceiver} is started,
     * reporting to this progress receiver.
     *
     * @param subProgressReceiver The sub progress receiver which has been
     *                            started.
     * @throws org.pepsoft.util.ProgressReceiver.OperationCancelled If the
     *     operation has been canceled, for instance by the user, or because
     *     of an exception on another thread.
     */
    void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled;

    /**
     * Indicates that the operation should be aborted, for instance because the
     * user has requested that it be cancelled, or because an error has
     * occurred. The message may indicate the reason of the cancellation.
     */
    class OperationCancelled extends Exception {
        public OperationCancelled(String message) {
            super(message);
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Indicates that the operation should be aborted because the user has
     * requested that it be cancelled. The message is "Cancelled by user".
     */
    class OperationCancelledByUser extends OperationCancelled {
        public OperationCancelledByUser() {
            super("Cancelled by user");
        }

        private static final long serialVersionUID = 1L;
    }
}