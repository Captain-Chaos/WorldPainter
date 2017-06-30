/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.util;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Utility methods for working with the AWT.
 *
 * Created by pepijn on 16-04-15.
 */
public class AwtUtils {
    /**
     * Execute a task on the event dispatch thread and return the result. The
     * task <em>may</em> be executed on a different thread, so it must be
     * thread-safe. Since it will block the event thread it should be short and
     * to the point.
     *
     * @param task The task to execute.
     * @param <T> The type of the result to return.
     * @return The result of the task.
     */
    @SuppressWarnings("unchecked") // Responsibility of caller
    public static <T> T resultOfOnEventThread(final Callable<T> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e.getClass().getSimpleName() + " thrown by task", e);
                }
            }
        } else {
            final Object[] result = new Object[1];
            final Exception[] exception = new Exception[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        synchronized (result) {
                            result[0] = task.call();
                        }
                    } catch (Exception e) {
                        synchronized (exception) {
                            exception[0] = e;
                        }
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted while waiting for task to execute on event dispatch thread", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                throw new RuntimeException(cause.getClass().getSimpleName() + " thrown by task on event dispatch thread", cause);
            }
            synchronized (exception) {
                if (exception[0] != null) {
                    throw new RuntimeException(exception[0].getClass().getSimpleName() + " thrown by task on event dispatch thread", exception[0]);
                }
            }
            synchronized (result) {
                return (T) result[0];
            }
        }
    }

    /**
     * Execute a task on the even dispatch thread. The task <em>may</em> be
     * executed on a different thread, so it must be thread-safe. Since it will
     * block the event thread it should be short and to the point. If the
     * current thread is not the event dispatch thread this method does
     * <em>not</em> wait for the task to finish.
     *
     * @param task The task to execute.
     */
    public static void doOnEventThread(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /**
     * Execute a task on the even dispatch thread and wait for it to finish. The
     * task <em>may</em> be executed on a different thread, so it must be
     * thread-safe. Since it will block the event thread it should be short and
     * to the point.
     *
     * @param task The task to execute.
     */
    public static void doOnEventThreadAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted while waiting for task to execute on event dispatch thread", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException();
                throw new RuntimeException(cause.getClass().getSimpleName() + " thrown by task on event dispatch thread", cause);
            }
        }
    }

    /**
     * Schedule a task for later execution on the even dispatch thread and
     * return immediately. The task <em>may</em> be executed on a different
     * thread, so it must be thread-safe. Since it will block the event thread
     * it should be short and to the point.
     *
     * @param task The task to execute.
     */
    public static void doLaterOnEventThread(Runnable task) {
        SwingUtilities.invokeLater(task);
    }
}