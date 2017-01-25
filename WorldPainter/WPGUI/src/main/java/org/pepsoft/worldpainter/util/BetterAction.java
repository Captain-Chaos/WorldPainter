/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.EventLogger;
import org.pepsoft.worldpainter.vo.EventVO;

/**
 *
 * @author pepijn
 */
public abstract class BetterAction extends org.pepsoft.util.swing.BetterAction {
    public BetterAction(@NonNls String id, String name, Icon icon) {
        this(id, name, icon, true);
    }

    public BetterAction(@NonNls String id, String name) {
        this(id, name, true);
    }

    public BetterAction(@NonNls String id, String name, Icon icon, boolean logEvent) {
        super(name, icon);
        statisticsKey = createStatisticsKey(id);
        this.logEvent = logEvent;
        putValue(App.HELP_KEY_KEY, "Action/" + id);
    }

    public BetterAction(String id, String name, boolean logEvent) {
        super(name);
        statisticsKey = createStatisticsKey(id);
        this.logEvent = logEvent;
        putValue(App.HELP_KEY_KEY, "Action/" + id);
    }

    public final String getStatisticsKey() {
        return statisticsKey;
    }

    public final boolean isLogEvent() {
        return logEvent;
    }
    
    @Override
    public final void actionPerformed(ActionEvent e) {
        performAction(e);
        if (logEvent) {
            synchronized (actionCounts) {
                if (actionCounts.containsKey(statisticsKey)) {
                    actionCounts.put(statisticsKey, actionCounts.get(statisticsKey) + 1);
                } else {
                    actionCounts.put(statisticsKey, 1L);
                }
            }
        }
    }
    
    protected abstract void performAction(ActionEvent e);
    
    public static void flushEvents(EventLogger eventLogger) {
        synchronized (actionCounts) {
            for (Map.Entry<String, Long> entry: actionCounts.entrySet()) {
                eventLogger.logEvent(new EventVO(entry.getKey()).count(entry.getValue()));
            }
            actionCounts.clear();
        }
    }
    
    private static String createStatisticsKey(String id) {
        return "action." + id.replaceAll("[ \\t\\n\\x0B\\f\\r.]", "");
    }
    
    private final String statisticsKey;
    private final boolean logEvent;

    private static final Map<String, Long> actionCounts = new HashMap<>();
}