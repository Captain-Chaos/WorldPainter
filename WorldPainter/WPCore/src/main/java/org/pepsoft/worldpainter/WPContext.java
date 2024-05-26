/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.util.MinecraftJarProvider;

/**
 * WorldPainter application context;
 *
 * @author pepijn
 */
public interface WPContext {
    /**
     * Get the event logger, if any. May be {@code null}.
     */
    EventLogger getStatisticsRecorder();

    /**
     * Get the Minecraft jar provider, if any. May be {@code null}.
     */
    MinecraftJarProvider getMinecraftJarProvider();

    /**
     * Get the {@link WorldPainterView WorldPainter view}, if any. May be {@code null}. It will not exist in headless
     * mode, for example.
     */
    WorldPainterView getView();
}