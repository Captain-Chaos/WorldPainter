/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

/**
 *
 * @author pepijn
 */
public class SubProgressReceiver implements ProgressReceiver {
    public SubProgressReceiver(ProgressReceiver progressReceiver, float offset, float extent) {
        if ((offset < 0.0f) || (offset > 1.0f) || (extent <= 0.0f)) {
            throw new IllegalArgumentException();
        }
        this.progressReceiver = progressReceiver;
        this.offset = offset;
        this.extent = extent;
    }

    @Override
    public void setProgress(float progress) throws OperationCancelled {
        progress = offset + progress * extent;
        if (progress < 0.0f) {
            progressReceiver.setProgress(0.0f);
        } else if (progress > 1.0f) {
            progressReceiver.setProgress(1.0f);
        } else {
            progressReceiver.setProgress(progress);
        }
    }

    @Override
    public void exceptionThrown(Throwable exception) {
        progressReceiver.exceptionThrown(exception);
    }

    @Override
    public void done() {
        progressReceiver.done();
    }

    @Override
    public void setMessage(String message) throws OperationCancelled {
        progressReceiver.setMessage(message);
    }

    @Override
    public void checkForCancellation() throws OperationCancelled {
        progressReceiver.checkForCancellation();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported");
    }

    private final ProgressReceiver progressReceiver;
    private final float offset, extent;
}