/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.vo;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author pepijn
 */
public final class UsageVO implements Serializable {
    public String getWPVersion() {
        return wpVersion;
    }

    public void setWPVersion(String wpVersion) {
        this.wpVersion = wpVersion;
    }

    public UUID getInstall() {
        return install;
    }

    public void setInstall(UUID install) {
        this.install = install;
    }

    public List<EventVO> getEvents() {
        return events;
    }

    public void setEvents(List<EventVO> events) {
        this.events = events;
    }

    public int getLaunchCount() {
        return launchCount;
    }

    public void setLaunchCount(int launchCount) {
        this.launchCount = launchCount;
    }

    @Override
    public String toString() {
        return "UsageVO{" + "wpVersion=" + wpVersion + ", install=" + install + ", events=" + events + ", launchCount=" + launchCount + '}';
    }

    public String toSummary() {
        return "UsageVO{" + "wpVersion=" + wpVersion + ", install=" + install + ", " + ((events != null) ? events.size() : 0) + " events, launchCount=" + launchCount + '}';
    }

    private String wpVersion;
    private UUID install;
    private List<EventVO> events;
    private int launchCount;
    
    private static final long serialVersionUID = 1L;
}