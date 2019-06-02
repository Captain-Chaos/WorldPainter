package org.pepsoft.util;

/**
 * A simple text-based progress received which prints a bar of fifty dots.
 */
public class TextProgressReceiver implements ProgressReceiver {
    @Override
    public void setProgress(float progressFraction) {
        int progress = (int) (progressFraction * 50);
        while (progress > previousProgress) {
            System.out.print('.');
            previousProgress++;
        }
    }

    @Override
    public void exceptionThrown(Throwable exception) {
        exception.printStackTrace();
        System.exit(1);
    }

    @Override public void reset() {
        System.out.println();
        previousProgress = -1;
    }

    @Override public void done() {}

    @Override public void setMessage(String message) {}

    @Override public void checkForCancellation() {}

    @Override public void subProgressStarted(SubProgressReceiver subProgressReceiver) {}

    private int previousProgress = -1;
}
