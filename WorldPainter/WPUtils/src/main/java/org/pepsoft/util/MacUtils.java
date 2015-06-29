/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.util;

import com.apple.eawt.AppEvent;
import com.apple.eawt.Application;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitResponse;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Utility methods for integrating into the Mac OS X desktop environment.
 *
 * Created by pepijn on 16-04-15.
 */
public final class MacUtils {
    private MacUtils() {
        // Prevent instantiation
    }

    /**
     * When the user requests to quit the application, invokes the specified
     * handler on the event dispatch thread and allows the quit to proceed if it
     * returns <code>true</code> or cancels it otherwise.
     *
     * @param quitHandler The handler to invoke.
     * @return <code>true</code> if the handler was successfully installed;
     *     <code>false</code> if the Apple Java extensions cannot be found or
     *     are too old.
     */
    public static boolean installQuitHandler(final QuitHandler quitHandler) {
        Application application = Application.getApplication();
        application.setQuitHandler((quitEvent, quitResponse) -> {
            boolean shouldQuit = AwtUtils.resultOfOnEventThread(quitHandler::quitRequested);
            if (shouldQuit) {
                quitResponse.performQuit();
            } else {
                quitResponse.cancelQuit();
            }
        });
        return true;
    }

    /**
     * When the user requests to view the About screen, invokes the specified
     * handler on the event dispatch thread.
     *
     * @param aboutHandler The handler to invoke.
     * @return <code>true</code> if the handler was successfully installed;
     *     <code>false</code> if the Apple Java extensions cannot be found or
     *     are too old.
     */
    public static boolean installAboutHandler(final AboutHandler aboutHandler) {
        Application application = Application.getApplication();
        application.setAboutHandler(aboutEvent -> AwtUtils.doLaterOnEventThread(aboutHandler::aboutRequested));
        return true;
    }

    /**
     * When the user requests to open (a) file(s) associated with the
     * application, invokes the specified handler on the event dispatch thread.
     *
     * @param openFilesHandler The handler to invoke.
     * @return <code>true</code> if the handler was successfully installed;
     *     <code>false</code> if the Apple Java extensions cannot be found or
     *     are too old.
     */
    public static boolean installOpenFilesHandler(final OpenFilesHandler openFilesHandler) {
        Application application = Application.getApplication();
        application.setOpenFileHandler(openFilesEvent -> {
            final List<File> files = openFilesEvent.getFiles();
            AwtUtils.doLaterOnEventThread(() -> openFilesHandler.filesOpened(files));
        });
        return true;
    }

    public static boolean installPreferencesHandler(final PreferencesHandler preferencesHandler) {
        Application application = Application.getApplication();
        application.setPreferencesHandler(preferencesEvent -> AwtUtils.doLaterOnEventThread(preferencesHandler::preferencesRequested));
        return true;
    }

    public interface QuitHandler {
        /**
         * Invoked when the user has requested to quit the application.
         *
         * @return <code>true</code> if the quit should proceed.
         */
        boolean quitRequested();
    }

    public interface AboutHandler {
        /**
         * Invoked when the user has requested to view the About screen.
         */
        void aboutRequested();
    }

    public interface OpenFilesHandler {
        /**
         * Invoked when the user has requested to open (a) file(s) associated
         * with this application.
         *
         * @param files The list of files requested to be opened.
         */
        void filesOpened(List<File> files);
    }

    public interface PreferencesHandler {
        void preferencesRequested();
    }
}