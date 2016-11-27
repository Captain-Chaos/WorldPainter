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
        DesktopUtils.setProgress(window, (int) (progress * 100 + 0.5f));
        nestedReceiver.setProgress(progress);
    }

    @Override
    public void exceptionThrown(Throwable exception) {
        DesktopUtils.setProgressError(window);
        nestedReceiver.exceptionThrown(exception);
    }

    @Override
    public void done() {
        DesktopUtils.setProgress(window, 100);
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
        DesktopUtils.setProgress(window, 0);
        nestedReceiver.reset();
    }

    @Override
    public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {
        nestedReceiver.subProgressStarted(subProgressReceiver);
    }

    private final Window window;
    private final ProgressReceiver nestedReceiver;
}