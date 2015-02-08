/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.vo.EventVO;

/**
 *
 * @author pepijn
 */
public interface EventLogger {
    void logEvent(EventVO event);
}