/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.util.MinecraftJarProvider;

/**
 *
 * @author pepijn
 */
public interface WPContext {
    EventLogger getStatisticsRecorder();
    MinecraftJarProvider getMinecraftJarProvider();
}