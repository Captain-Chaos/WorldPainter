/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.InvalidPathException;

/**
 *
 * @author pepijn
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    public static void handleException(Throwable t) {
//        t.printStackTrace();
        if (shouldIgnore(t)) {
            t.printStackTrace();
        } else {
            final Runnable task = () -> {
                if (! handlingException) {
                    handlingException = true;
                    try {
                        ErrorDialog dialog = new ErrorDialog(App.getInstanceIfExists());
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

    @Override
    public void uncaughtException(Thread t, final Throwable e) {
        handleException(e);
    }
    
    private static boolean shouldIgnore(Throwable t) {
        Throwable rootCause = t;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
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
            }
        }
        return false;
    }

    private static boolean handlingException;
}