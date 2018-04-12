package org.pepsoft.util;

import java.awt.*;
import java.util.Map;
import java.util.WeakHashMap;

import static java.awt.Taskbar.Feature.PROGRESS_STATE_WINDOW;
import static java.awt.Taskbar.Feature.PROGRESS_VALUE_WINDOW;

class ProgressHelperJava9 extends ProgressHelper {
    ProgressHelperJava9() {
        enabled = Taskbar.isTaskbarSupported()
                && Taskbar.getTaskbar().isSupported(PROGRESS_VALUE_WINDOW)
                && Taskbar.getTaskbar().isSupported(PROGRESS_STATE_WINDOW);
    }

    @Override
    void setProgress(Window window, int percentage) {
        if (! enabled) {
            return;
        }
        if ((errorStates.get(window) != null) && errorStates.get(window)) {
            return;
        }
        Taskbar.getTaskbar().setWindowProgressValue(window, percentage);
    }

    @Override
    void setProgressDone(Window window) {
        if (! enabled) {
            return;
        }
        Taskbar.getTaskbar().setWindowProgressState(window, Taskbar.State.OFF);
        errorStates.remove(window);
    }

    @Override
    void setProgressError(Window window) {
        if (! enabled) {
            return;
        }
        if ((errorStates.get(window) != null) && errorStates.get(window)) {
            return;
        } else {
            errorStates.put(window, true);
        }
        Taskbar.getTaskbar().setWindowProgressState(window, Taskbar.State.ERROR);
    }

    private final boolean enabled;
    private final Map<Window, Boolean> errorStates = new WeakHashMap<>();
}