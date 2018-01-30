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

import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

// TODO: directly invoke Java 9 API, once bug IDEA-185700 is fixed in IntelliJ

/**
 * Utility methods for integrating into the Mac OS X desktop environment on
 * Java 9 or later.
 *
 * Created by pepijn on 16-04-15.
 */
@SuppressWarnings({"JavaReflectionInvocation", "JavaReflectionMemberAccess"}) // TODO: remove once the use of reflection is no longer necessary
final class MacUtilsJava9 extends MacUtils {
    MacUtilsJava9() {
        // Prevent instantiation outside package
    }

    /**
     * When the user requests to quit the application, invokes the specified
     * handler on the event dispatch thread and allows the quit to proceed if it
     * returns <code>true</code> or cancels it otherwise.
     *
     * @param quitHandler The handler to invoke.
     * @return <code>true</code> if the handler was successfully installed;
     *     <code>false</code> if not.
     */
    protected boolean doInstallQuitHandler(final MacUtils.QuitHandler quitHandler) {
        try {
            Class<?> quitHandlerClass = Class.forName("java.awt.desktop.QuitHandler");
            Class<?> quitResponseClass = Class.forName("java.awt.desktop.QuitResponse");
            Method setQuitHandlerMethod = Desktop.class.getMethod("setQuitHandler", quitHandlerClass);
            Method performQuitMethod = quitResponseClass.getMethod("performQuit");
            Method cancelQuitMethod = quitResponseClass.getMethod("cancelQuit");
            Object quitHandlerProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] {quitHandlerClass}, (proxy, method, args) -> {
                boolean shouldQuit = AwtUtils.resultOfOnEventThread(quitHandler::quitRequested);
                if (shouldQuit) {
                    performQuitMethod.invoke(args[1]);
                } else {
                    cancelQuitMethod.invoke(args[1]);
                }
                return null;
            });
            Desktop desktop = Desktop.getDesktop();
            setQuitHandlerMethod.invoke(desktop, quitHandlerProxy);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When the user requests to view the About screen, invokes the specified
     * handler on the event dispatch thread.
     *
     * @param aboutHandler The handler to invoke.
     * @return <code>true</code> if the handler was successfully installed;
     *     <code>false</code> if not.
     */
    protected boolean doInstallAboutHandler(final MacUtils.AboutHandler aboutHandler) {
        try {
            Class<?> aboutHandlerClass = Class.forName("java.awt.desktop.AboutHandler");
            Method setAboutHandlerMethod = Desktop.class.getMethod("setAboutHandler", aboutHandlerClass);
            Object aboutHandlerProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] {aboutHandlerClass}, (proxy, method, args) -> {
                AwtUtils.doLaterOnEventThread(aboutHandler::aboutRequested);
                return null;
            });
            Desktop desktop = Desktop.getDesktop();
            setAboutHandlerMethod.invoke(desktop, aboutHandlerProxy);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When the user requests to open (a) file(s) associated with the
     * application, invokes the specified handler on the event dispatch thread.
     *
     * @param openFilesHandler The handler to invoke.
     * @return <code>true</code> if the handler was successfully installed;
     *     <code>false</code> if not.
     */
    protected boolean doInstallOpenFilesHandler(final MacUtils.OpenFilesHandler openFilesHandler) {
        try {
            Class<?> openFilesHandlerClass = Class.forName("java.awt.desktop.OpenFilesHandler");
            Class<?> openFilesEventClass = Class.forName("java.awt.desktop.OpenFilesEvent");
            Method setOpenFileHandlerMethod = Desktop.class.getMethod("setOpenFileHandler", openFilesHandlerClass);
            Method getFilesMethod = openFilesEventClass.getMethod("getFiles");
            Object openFilesHandlerProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] {openFilesHandlerClass}, (proxy, method, args) -> {
                @SuppressWarnings("unchecked") // Guaranteed by Java reflection
                final List<File> files = (List<File>) getFilesMethod.invoke(args[0]);
                AwtUtils.doLaterOnEventThread(() -> openFilesHandler.filesOpened(files));
                return null;
            });
            Desktop desktop = Desktop.getDesktop();
            setOpenFileHandlerMethod.invoke(desktop, openFilesHandlerProxy);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean doInstallPreferencesHandler(final MacUtils.PreferencesHandler preferencesHandler) {
        try {
            Class<?> preferencesHandlerClass = Class.forName("java.awt.desktop.PreferencesHandler");
            Method setPreferencesHandlerMethod = Desktop.class.getMethod("setPreferencesHandler", preferencesHandlerClass);
            Object preferencesHandlerProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] {preferencesHandlerClass}, (proxy, method, args) -> {
                AwtUtils.doLaterOnEventThread(preferencesHandler::preferencesRequested);
                return null;
            });
            Desktop desktop = Desktop.getDesktop();
            setPreferencesHandlerMethod.invoke(desktop, preferencesHandlerProxy);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}