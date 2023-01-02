/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.util.mdc.MDCWrappingRuntimeException;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.InvalidPathException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.pepsoft.util.ExceptionUtils.getUltimateCause;

/**
 *
 * @author pepijn
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static void handleException(Throwable t) {
        handleException(t, App.getInstanceIfExists());
    }

    public static void handleException(Throwable t, Window parent) {
        if (reportingDisabled.get() > 0) {
            LoggerFactory.getLogger(ExceptionHandler.class)
                    .error("{} occurred while exception reporting was disabled (message: {})", t.getClass().getSimpleName(), t.getMessage(), t);
        } else if (shouldIgnore(t)) {
            t.printStackTrace();
        } else {
            final Runnable task = () -> {
                if (! handlingException) {
                    handlingException = true;
                    try {
                        ErrorDialog dialog = new ErrorDialog(parent);
                        dialog.setException(t);
                        dialog.setVisible(true);
                    } finally {
                        handlingException = false;
                    }
                } else {
                    LoggerFactory.getLogger(ExceptionHandler.class)
                            .error("{} occurred while exception dialog was open (message: {})", t.getClass().getSimpleName(), t.getMessage(), t);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        }
    }

    /**
     * Perform a task with exception reporting disabled. Exceptions will be logged, but will not result in an
     * {@link ErrorDialog} being shown or a report to be submitted to the backoffice.
     *
     * <p><strong>Note</strong> that this does not <em>catch</em> exceptions! Uncaught exceptions will be propagated as
     * normal.
     */
    public static void doWithoutExceptionReporting(Runnable task) {
        reportingDisabled.incrementAndGet();
        try {
            task.run();
        } finally {
            reportingDisabled.decrementAndGet();
        }
    }

    /**
     * Perform a task with exception reporting disabled. Exceptions will be logged, but will not result in an
     * {@link ErrorDialog} being shown or a report to be submitted to the backoffice.
     *
     * <p><strong>Note</strong> that this does not <em>catch</em> exceptions! Uncaught exceptions will be propagated as
     * normal. Checked exceptions will be wrapped in a {@link MDCWrappingRuntimeException}.
     */
    public static <T> T doWithoutExceptionReporting(Callable<T> task) {
        reportingDisabled.incrementAndGet();
        try {
            return task.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new MDCWrappingRuntimeException(e);
            }
        } finally {
            reportingDisabled.decrementAndGet();
        }
    }

    // UncaughtExceptionHandler

    @Override
    public void uncaughtException(Thread t, final Throwable e) {
        handleException(e);
    }
    
    private static boolean shouldIgnore(Throwable t) {
        final Throwable rootCause = getUltimateCause(t);
        if ((rootCause.getStackTrace() != null) && (rootCause.getStackTrace().length > 0)) {
            final StackTraceElement topOfStack = rootCause.getStackTrace()[0];
            if (rootCause instanceof NullPointerException) {
                // This happens now and again with no WP code on the stack. Probably a bug in Java and most likely not
                // something we can do anything about, so ignore it
                return (topOfStack.getClassName().equals("javax.swing.SwingUtilities")
                            && topOfStack.getMethodName().equals("getWindowAncestor"))
                        || (topOfStack.getClassName().equals("javax.swing.plaf.basic.BasicProgressBarUI")
                            && topOfStack.getMethodName().equals("sizeChanged"));
            } else if (rootCause instanceof InvalidPathException) {
                // This seems to be a bug in Java that occurs when the user enters a space as the filename. Not
                // something we can do anything about, so ignore it
                return topOfStack.getClassName().equals("sun.nio.fs.WindowsPathParser")
                        && topOfStack.getMethodName().equals("normalize");
            } else if (rootCause instanceof NoClassDefFoundError) {
                // This seems to be some kind of bug in Java2D in Java 8. Not something we can do anything about, so
                // don't bother the user with dozens of error dialogs
                return topOfStack.getClassName().equalsIgnoreCase("sun.dc.DuctusRenderingEngine")
                        && topOfStack.getMethodName().equalsIgnoreCase("getRasterizer");
            }
        }
        return false;
    }

    private static final AtomicInteger reportingDisabled = new AtomicInteger();
    private static boolean handlingException;
}