package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.vo.UsageVO;

/**
 * Created by pepijn on 8-2-2015.
 */
public interface PrivateContext {
    void checkForUpdates();
    void submitUsageData(UsageVO usageData);
}