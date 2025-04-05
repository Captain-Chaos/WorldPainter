/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.util.MinecraftJarProvider;
import org.pepsoft.worldpainter.vo.EventVO;

import java.util.function.Consumer;

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
     * Register an event listener for a particular key, which will be notified whenever an event with that key is
     * published to the {@link EventLogger}.
     *
     * See the {@code EVENT_KEY_*} constants in the {@link Constants} class for the keys of the standard events
     * published by WorldPainter. Plugins may publish additional events.
     *
     * <strong>Note</strong> that the thread on which the listener is notified is
     * not defined! The listener must not make assumptions about the thread on which it will be executed, and must take
     * care to do proper synchronization and/or shift work to the Swing main event thread as appropriate.
     */
    void addEventListener(String eventKey, Consumer<EventVO> eventListener);

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