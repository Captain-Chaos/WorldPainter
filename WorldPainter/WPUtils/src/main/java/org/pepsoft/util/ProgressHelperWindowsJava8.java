package org.pepsoft.util;

import org.bridj.Pointer;
import org.bridj.cpp.com.COMRuntime;
import org.bridj.cpp.com.shell.ITaskbarList3;
import org.bridj.jawt.JAWTUtils;

import java.awt.*;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by Pepijn on 27-11-2016.
 */
class ProgressHelperWindowsJava8 extends ProgressHelper {
    ProgressHelperWindowsJava8() {
        enabled = SystemUtils.isWindows();
    }

    @Override
    void setProgress(Window window, int percentage) {
        if (! enabled) {
            return;
        }
        WindowProgress windowHelper = windowHelpers.get(window);
        if (windowHelper == null) {
            windowHelper = new WindowProgress(window);
            windowHelpers.put(window, windowHelper);
            if (windowHelper.taskbarList != null) {
                windowHelper.taskbarList.SetProgressState(windowHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_NORMAL);
            }
        }
        if ((windowHelper.taskbarList != null) && (! windowHelper.errorReported)) {
            windowHelper.taskbarList.SetProgressValue(windowHelper.hwnd, percentage, 100);
        }
    }

    @Override
    void setProgressDone(Window window) {
        if (! enabled) {
            return;
        }
        WindowProgress windowHelper = windowHelpers.get(window);
        if (windowHelper != null) {
            if (windowHelper.taskbarList != null) {
                windowHelper.taskbarList.SetProgressState(windowHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS);
            }
            windowHelpers.remove(window);
        }
    }

    @Override
    void setProgressError(Window window) {
        if (! enabled) {
            return;
        }
        WindowProgress windowHelper = windowHelpers.get(window);
        if ((windowHelper != null) && (windowHelper.taskbarList != null)) {
            windowHelper.taskbarList.SetProgressState(windowHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_ERROR);
            windowHelper.errorReported = true;
        }
    }

    static class WindowProgress {
        WindowProgress(Window window) {
            ITaskbarList3 taskbarList;
            Pointer<Integer> hwnd;
            try {
                taskbarList = COMRuntime.newInstance(ITaskbarList3.class);
                long hwndVal = JAWTUtils.getNativePeerHandle(window);
                hwnd = (Pointer<Integer>) Pointer.pointerToAddress(hwndVal);
            } catch (ClassNotFoundException | RuntimeException e) {
                // Probably too old a Windows version
                taskbarList = null;
                hwnd = null;
            }
            this.taskbarList = taskbarList;
            this.hwnd = hwnd;
        }

        final ITaskbarList3 taskbarList;
        final Pointer<Integer> hwnd;
        boolean errorReported;
    }

    private final boolean enabled;

    private static final Map<Window, WindowProgress> windowHelpers = new WeakHashMap<>();
}