/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author pepijn
 */
public class SubProgressReceiver implements ProgressReceiver {
    public SubProgressReceiver(ProgressReceiver progressReceiver, float offset, float extent) throws OperationCancelled {
        if ((offset < 0.0f) || (offset > 1.0f) || (extent <= 0.0f)) {
            throw new IllegalArgumentException();
        }
        this.progressReceiver = progressReceiver;
        this.offset = offset;
        this.extent = extent;
        creationTrace = new Throwable("Creation");
        lastMessage = creationTrace.getStackTrace()[1].getClassName() + '.' + creationTrace.getStackTrace()[1].getMethodName() + '#' + creationTrace.getStackTrace()[1].getLineNumber();
    }
    
    /**
     * Adds an additional progress receiver, to which the
     * {@link #setProgress(float)}, {@link #setMessage(java.lang.String)},
     * {@link #done()} and {@link #exceptionThrown(java.lang.Throwable)} methods
     * will be forwarded (without remapping the progress). Note that recursive
     * invocations of <code>setMessage()</code>, </code><code>exceptionThrown()</code>
     * and <code>done()</code> are not reported.
     * 
     * @param listener 
     */
    public synchronized void addListener(ProgressReceiver listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }
    
    public synchronized void removeListener(ProgressReceiver listener) {
        listeners.remove(listener);
    }

    public ProgressReceiver getParent() {
        return progressReceiver;
    }

    public synchronized String getLastMessage() {
        return lastMessage;
    }

    public Throwable getCreationTrace() {
        return creationTrace;
    }

    // ProgressReceiver

    @Override
    public void setProgress(float progress) throws OperationCancelled {
        if (! reportedToParent) {
            progressReceiver.subProgressStarted(this);
            reportedToParent = true;
        }
        float parentProgress = offset + progress * extent;
        if (parentProgress < 0.0f) {
            progressReceiver.setProgress(0.0f);
        } else if (parentProgress > 1.0f) {
            progressReceiver.setProgress(1.0f);
        } else {
            progressReceiver.setProgress(parentProgress);
        }
        synchronized (this) {
            if (listeners != null) {
                for (ProgressReceiver listener: listeners) {
                    listener.setProgress(progress);
                }
            }
        }
    }

    @Override
    public void exceptionThrown(Throwable exception) {
        if (! recursiveCall.get().get()) {
            recursiveCall.get().set(true);
            try {
                progressReceiver.exceptionThrown(exception);
                synchronized (this) {
                    if (listeners != null) {
                        for (ProgressReceiver listener: listeners) {
                            listener.exceptionThrown(exception);
                        }
                    }
                }
            } finally {
                recursiveCall.get().set(false);
            }
        } else {
            progressReceiver.exceptionThrown(exception);
        }
    }

    @Override
    public synchronized void done() {
        if (listeners != null) {
            for (ProgressReceiver listener: listeners) {
                listener.done();
            }
        }
    }

    @Override
    public void setMessage(String message) throws OperationCancelled {
        if (! recursiveCall.get().get()) {
            recursiveCall.get().set(true);
            try {
                if (! reportedToParent) {
                    progressReceiver.subProgressStarted(this);
                    reportedToParent = true;
                }
                progressReceiver.setMessage(message);
                synchronized (this) {
                    if (listeners != null) {
                        for (ProgressReceiver listener: listeners) {
                            listener.setMessage(message);
                        }
                    }
                }
                synchronized (this) {
                    lastMessage = message;
                }
            } finally {
                recursiveCall.get().set(false);
            }
        } else {
            progressReceiver.setMessage(message);
        }
    }

    @Override
    public void checkForCancellation() throws OperationCancelled {
        progressReceiver.checkForCancellation();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void subProgressStarted(SubProgressReceiver subProgressReceiver) throws OperationCancelled {
        progressReceiver.subProgressStarted(subProgressReceiver);
    }

    private final ProgressReceiver progressReceiver;
    private final float offset, extent;
    private final Throwable creationTrace;
    private List<ProgressReceiver> listeners;
    private String lastMessage;
    private boolean reportedToParent;

    private static final ThreadLocal<AtomicBoolean> recursiveCall = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return new AtomicBoolean(false);
        }
    };
}