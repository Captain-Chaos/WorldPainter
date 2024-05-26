/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.vo.EventVO;

/**
 * A remote event logger. Collects evens for later transmission to a backend server.
 *
 * @author pepijn
 */
public interface EventLogger {
    /**
     * Store an event for later remote transmission.
     */
    void logEvent(EventVO event);
}