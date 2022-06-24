package org.pepsoft.util;

import java.awt.*;

/**
 * Created by Pepijn on 26-11-2016.
 */
public class TaskbarProgressReceiver implements ProgressReceiver {
    public TaskbarProgressReceiver(Window window, ProgressReceiver nestedReceiver) {
        this.window = window;
        this.nestedReceiver = nestedReceiver;
    }

    @Override
    public void setProgress(float progress) throws OperationCancelled {
        if (! done) {
            DesktopUtils.setProgress(window, Math.round(progress * 100));
            if (progress >= 1.0f) {
                done = true;
            }
        }
        nestedReceiver.setProgress(progress);
    }

    @Override
    public void exceptionThrown(Throwable exception) {
        if (! done) {
            DesktopUtils.setProgressError(window);
            done = true;
        }
        nestedReceiver.exceptionThrown(exception);
    }

    @Override
    public void done() {
        DesktopUtils.setProgressDone(window);
        done = true;
        nestedReceiver.done();
    }

    @Override
    public void setMessage(String message) throws OperationCancelled {
        nestedReceiver.setMessage(message);
    }

    @Override
    public void checkForCancellation() throws OperationCancelled {
        nestedReceiver.checkForCancellation();
    }

    @Override
    public void reset() throws OperationCancelled {
        done = false;
        DesktopUtils.setProgress(window, 0);
        nestedReceiver.reset();
    }

    @Override
    public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {
        nestedReceiver.subProgressStarted(subProgressReceiver);
    }

    private final Window window;
    private final ProgressReceiver nestedReceiver;
    private volatile boolean done;
}