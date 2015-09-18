/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import javax.swing.SwingUtilities;

/**
 *
 * @author pepijn
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    public void handle(Throwable t) {
//        t.printStackTrace();
        if (shouldIgnore(t)) {
            t.printStackTrace();
        } else {
            ErrorDialog dialog = new ErrorDialog(App.getInstanceIfExists());
            dialog.setException(t);
            dialog.setVisible(true);
        }
    }

    @Override
    public void uncaughtException(Thread t, final Throwable e) {
//        e.printStackTrace();
        if (shouldIgnore(e)) {
            e.printStackTrace();
        } else  if (SwingUtilities.isEventDispatchThread()) {
            handle(e);
        } else {
            SwingUtilities.invokeLater(() -> handle(e));
        }
    }
    
    private boolean shouldIgnore(Throwable rootCause) {
        if ((rootCause instanceof NullPointerException)
                && (rootCause.getStackTrace() != null)
                && (rootCause.getStackTrace().length > 0)) {
            if ((rootCause.getStackTrace()[0].getClassName().equals("javax.swing.SwingUtilities")
                        && rootCause.getStackTrace()[0].getMethodName().equals("getWindowAncestor"))
                    || (rootCause.getStackTrace()[0].getClassName().equals("javax.swing.plaf.basic.BasicProgressBarUI")
                        && rootCause.getStackTrace()[0].getMethodName().equals("sizeChanged"))) {
                // This happens now and again with no WP code on the stack.
                // Probably a bug in Java and most likely not something we can
                // do anything about, so ignore it
                return true;
            }
        }
        return false;
    }
}