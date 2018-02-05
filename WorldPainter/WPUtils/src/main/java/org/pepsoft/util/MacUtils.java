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

import java.io.File;
import java.util.List;

import static org.pepsoft.util.SystemUtils.*;

/**
 * Utility methods for integrating into the Mac OS X desktop environment.
 *
 * Created by pepijn on 16-04-15.
 */
public abstract class MacUtils {
    protected MacUtils() {
        // Prevent illegal instantiation
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
        return IMPL.doInstallQuitHandler(quitHandler);
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
        return IMPL.doInstallAboutHandler(aboutHandler);
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
        return IMPL.doInstallOpenFilesHandler(openFilesHandler);
    }

    public static boolean installPreferencesHandler(final PreferencesHandler preferencesHandler) {
        return IMPL.doInstallPreferencesHandler(preferencesHandler);
    }

    protected abstract boolean doInstallQuitHandler(QuitHandler quitHandler);
    protected abstract boolean doInstallAboutHandler(AboutHandler aboutHandler);
    protected abstract boolean doInstallOpenFilesHandler(OpenFilesHandler openFilesHandler);
    protected abstract boolean doInstallPreferencesHandler(PreferencesHandler preferencesHandler);

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

    private static final MacUtils IMPL;

    static {
        if (JAVA_VERSION.isAtLeast(JAVA_9)) {
            try {
                IMPL = (MacUtils) Class.forName("org.pepsoft.util.MacUtilsJava9").newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e.getClass().getSimpleName() + " while loading Mac OS X support for Java 9", e);
            }
        } else {
            IMPL = new MacUtilsJava8();
        }
    }
}