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
class ProgressHelper {
    @SuppressWarnings("unchecked") // Guaranteed by BridJ
    private ProgressHelper(Window window) {
        try {
            taskbarList = COMRuntime.newInstance(ITaskbarList3.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        long hwndVal = JAWTUtils.getNativePeerHandle(window);
        hwnd = (Pointer<Integer>) Pointer.pointerToAddress(hwndVal);
    }

    static void setProgress(Window window, int percentage) {
        ProgressHelper progressHelper = progressHelpers.get(window);
        if (progressHelper == null) {
            progressHelper = new ProgressHelper(window);
            progressHelpers.put(window, progressHelper);
            progressHelper.taskbarList.SetProgressState(progressHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_NORMAL);
        }
        if (! progressHelper.errorReported) {
            progressHelper.taskbarList.SetProgressValue(progressHelper.hwnd, percentage, 100);
        }
    }

    static void setProgressDone(Window window) {
        ProgressHelper progressHelper = progressHelpers.get(window);
        if (progressHelper != null) {
            progressHelper.taskbarList.SetProgressState(progressHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS);
            progressHelpers.remove(window);
        }
    }

    static void setProgressError(Window window) {
        ProgressHelper progressHelper = progressHelpers.get(window);
        if (progressHelper != null) {
            progressHelper.taskbarList.SetProgressState(progressHelper.hwnd, ITaskbarList3.TbpFlag.TBPF_ERROR);
            progressHelper.errorReported = true;
        }
    }

    private final ITaskbarList3 taskbarList;
    private final Pointer<Integer> hwnd;
    private boolean errorReported;

    private static final Map<Window, ProgressHelper> progressHelpers = new WeakHashMap<>();
}