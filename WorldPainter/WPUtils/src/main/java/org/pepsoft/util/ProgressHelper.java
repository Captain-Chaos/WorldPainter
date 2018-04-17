package org.pepsoft.util;

import java.awt.*;

import static org.pepsoft.util.SystemUtils.JAVA_9;
import static org.pepsoft.util.SystemUtils.JAVA_VERSION;

/**
 * Created by Pepijn on 27-11-2016.
 */
abstract class ProgressHelper {
    static ProgressHelper getInstance() {
        return IMPL;
    }

    abstract void setProgress(Window window, int percentage);

    abstract void setProgressDone(Window window);

    abstract void setProgressError(Window window);

    private static final ProgressHelper IMPL;

    static {
        if (JAVA_VERSION.isAtLeast(JAVA_9)) {
            try {
                IMPL = (ProgressHelper) Class.forName("org.pepsoft.util.ProgressHelperJava9").newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e.getClass().getSimpleName() + " while loading progress reporting support for Java 9", e);
            }
        } else if (SystemUtils.isWindows()) {
            IMPL = new ProgressHelperWindowsJava8();
        } else {
            IMPL = new ProgressHelper() {
                @Override
                void setProgress(Window window, int percentage) {
                    // Do nothing
                }

                @Override
                void setProgressDone(Window window) {
                    // Do nothing
                }

                @Override
                void setProgressError(Window window) {
                    // Do nothing
                }
            };
        }
    }
}