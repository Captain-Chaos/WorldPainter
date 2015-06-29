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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for accessing the freedesktop.org "XDG Base Directory
 * Specification" directories as well as the "well known" user directories as
 * managed by the xdg-user-dirs tool.
 *
 * <p>The string-typed constants are set whether or not the directory exists;
 * either to the configured value or a default, if applicable. The file-typed
 * constants are only set if the directory actually exists.
 *
 * <p>See
 * <a href="http://standards.freedesktop.org/basedir-spec/latest/">http://standards.freedesktop.org/basedir-spec/latest/</a>
 * and
 * <a href="http://www.freedesktop.org/wiki/Software/xdg-user-dirs/">http://www.freedesktop.org/wiki/Software/xdg-user-dirs/</a>
 *
 * <p>Created by Pepijn Schmitz on 19-03-15.
 */
public final class XDG {
    private XDG() {
        // Prevent instantiation
    }

    // Base dirs
    /**
     * The current user's home directory. Set to the value of the
     * <code>user.home</code> system property.
     */
    public static final String HOME = System.getProperty("user.home");

    /**
     * Defines the base directory relative to which user specific data files
     * should be stored. Set to the contents of the XDG_DATA_HOME environment
     * variable, or "{@link #HOME}/.local/share" if that variable is not present.
     */
    public static final String XDG_DATA_HOME = System.getenv("XDG_DATA_HOME") != null ? System.getenv("XDG_DATA_HOME") : HOME + File.separatorChar + ".local" + File.separatorChar + "share";

    /**
     * Defines the base directory relative to which user specific configuration
     * files should be stored. Set to the contents of the XDG_CONFIG_HOME
     * environment variable, or "{@link #HOME}/.config" if that variable is not present.
     */
    public static final String XDG_CONFIG_HOME = System.getenv("XDG_CONFIG_HOME") != null ? System.getenv("XDG_CONFIG_HOME") : HOME + File.separatorChar + ".config";

    /**
     * Defines the preference-ordered set of base directories to search for data
     * files in addition to the {@link #XDG_DATA_HOME} base directory. Set to
     * the contents of the XDG_DATA_DIRS environment variable, or
     * "/usr/local/share:/usr/share" if that variable is not present.
     */
    public static final String XDG_DATA_DIRS = System.getenv("XDG_DATA_DIRS") != null ? System.getenv("XDG_DATA_DIRS") : File.separatorChar + "usr" + File.separatorChar + "local" + File.separatorChar + "share" + File.pathSeparatorChar + File.separatorChar + "usr" + File.separatorChar + "share";

    /**
     * Defines the preference-ordered set of base directories to search for
     * configuration files in addition to the {@link #XDG_CONFIG_HOME} base
     * directory. Set to the contents of the XDG_CONFIG_DIRS environment
     * variable, or "/etc/xdg" if that variable is not present.
     */
    public static final String XDG_CONFIG_DIRS = System.getenv("XDG_CONFIG_DIRS") != null ? System.getenv("XDG_CONFIG_DIRS") : File.separatorChar + "etc" + File.separatorChar + "xdg";

    /**
     * Defines the base directory relative to which user specific non-essential
     * data files should be stored. Set to the contents of the XDG_CACHE_HOME
     * environment variable, or "{@link #HOME}/.cache" if that variable is not present.
     */
    public static final String XDG_CACHE_HOME = System.getenv("XDG_CACHE_HOME") != null ? System.getenv("XDG_CACHE_HOME") : HOME + File.separatorChar + ".cache";

    /**
     * Defines the base directory relative to which user-specific non-essential
     * runtime files and other file objects (such as sockets, named pipes, ...)
     * should be stored. Set to the contents of the XDG_RUNTIME_DIR environment
     * variable, or <code>null</code> if that variable is not present.
     */
    public static final String XDG_RUNTIME_DIR = System.getenv("XDG_RUNTIME_DIR");

    /**
     * {@link #XDG_DATA_HOME} as a {@link File} object, but only if that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_DATA_HOME_FILE = new File(XDG_DATA_HOME).isDirectory() ? new File(XDG_DATA_HOME) : null;

    /**
     * {@link #XDG_CONFIG_HOME} as a {@link File} object, but only if that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_CONFIG_HOME_FILE = new File(XDG_CONFIG_HOME).isDirectory() ? new File(XDG_CONFIG_HOME) : null;

    /**
     * {@link #XDG_CACHE_HOME} as a {@link File} object, but only if that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_CACHE_HOME_FILE = new File(XDG_CACHE_HOME).isDirectory() ? new File(XDG_CACHE_HOME) : null;

    /**
     * {@link #XDG_RUNTIME_DIR} as a {@link File} object, but only if
     * <code>XDG_RUNTIME_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_RUNTIME_DIR_FILE = XDG_RUNTIME_DIR != null ? (new File(XDG_RUNTIME_DIR).isDirectory() ? new File(XDG_RUNTIME_DIR) : null) : null;

    // User dirs
    /**
     * The location of the "desktop" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_DESKTOP_DIR;

    /**
     * The location of the "documents" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_DOCUMENTS_DIR;

    /**
     * The location of the "download" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_DOWNLOAD_DIR;

    /**
     * The location of the "music" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_MUSIC_DIR;

    /**
     * The location of the "pictures" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_PICTURES_DIR;

    /**
     * The location of the "public share" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_PUBLICSHARE_DIR;

    /**
     * The location of the "templates" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_TEMPLATES_DIR;

    /**
     * The location of the "videos" well known user directory, as specified
     * in the appropriate system or user config file, or <code>null</code> if
     * it is not configured.
     */
    public static final String XDG_VIDEOS_DIR;

    static {
        Properties userDirs = new Properties();
        String[] configDirs = XDG_CONFIG_DIRS.split(File.pathSeparator);
        for (String configDir: configDirs) {
            File userDirFile = new File(configDir, "user-dirs.default");
            if (userDirFile.isFile()) {
                readShellVarScript(userDirFile, userDirs);
            }
        }
        File userConfigFile = new File(XDG_CONFIG_HOME, "user-dirs.dirs");
        if (userConfigFile.isFile()) {
            readShellVarScript(userConfigFile, userDirs);
        }
        XDG_DESKTOP_DIR = userDirs.getProperty("XDG_DESKTOP_DIR");
        XDG_DOCUMENTS_DIR = userDirs.getProperty("XDG_DOCUMENTS_DIR");
        XDG_DOWNLOAD_DIR = userDirs.getProperty("XDG_DOWNLOAD_DIR");
        XDG_MUSIC_DIR = userDirs.getProperty("XDG_MUSIC_DIR");
        XDG_PICTURES_DIR = userDirs.getProperty("XDG_PICTURES_DIR");
        XDG_PUBLICSHARE_DIR = userDirs.getProperty("XDG_PUBLICSHARE_DIR");
        XDG_TEMPLATES_DIR = userDirs.getProperty("XDG_TEMPLATES_DIR");
        XDG_VIDEOS_DIR = userDirs.getProperty("XDG_VIDEOS_DIR");
    }

    /**
     * {@link #XDG_DESKTOP_DIR} as a {@link File} object, but only if
     * <code>XDG_DESKTOP_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_DESKTOP_DIR_FILE = ((XDG_DESKTOP_DIR != null) && new File(XDG_DESKTOP_DIR).isDirectory()) ? new File(XDG_DESKTOP_DIR) : null;

    /**
     * {@link #XDG_DOCUMENTS_DIR} as a {@link File} object, but only if
     * <code>XDG_DOCUMENTS_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_DOCUMENTS_DIR_FILE = ((XDG_DOCUMENTS_DIR != null) && new File(XDG_DOCUMENTS_DIR).isDirectory()) ? new File(XDG_DOCUMENTS_DIR) : null;

    /**
     * {@link #XDG_DOWNLOAD_DIR} as a {@link File} object, but only if
     * <code>XDG_DOWNLOAD_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_DOWNLOAD_DIR_FILE = ((XDG_DOWNLOAD_DIR != null) && new File(XDG_DOWNLOAD_DIR).isDirectory()) ? new File(XDG_DOWNLOAD_DIR) : null;

    /**
     * {@link #XDG_MUSIC_DIR} as a {@link File} object, but only if
     * <code>XDG_MUSIC_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_MUSIC_DIR_FILE = ((XDG_MUSIC_DIR != null) && new File(XDG_MUSIC_DIR).isDirectory()) ? new File(XDG_MUSIC_DIR) : null;

    /**
     * {@link #XDG_PICTURES_DIR} as a {@link File} object, but only if
     * <code>XDG_PICTURES_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_PICTURES_DIR_FILE = ((XDG_PICTURES_DIR != null) && new File(XDG_PICTURES_DIR).isDirectory()) ? new File(XDG_PICTURES_DIR) : null;

    /**
     * {@link #XDG_PUBLICSHARE_DIR} as a {@link File} object, but only if
     * <code>XDG_PUBLICSHARE_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_PUBLICSHARE_DIR_FILE = ((XDG_PUBLICSHARE_DIR != null) && new File(XDG_PUBLICSHARE_DIR).isDirectory()) ? new File(XDG_PUBLICSHARE_DIR) : null;

    /**
     * {@link #XDG_TEMPLATES_DIR} as a {@link File} object, but only if
     * <code>XDG_TEMPLATES_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_TEMPLATES_DIR_FILE = ((XDG_TEMPLATES_DIR != null) && new File(XDG_TEMPLATES_DIR).isDirectory()) ? new File(XDG_TEMPLATES_DIR) : null;

    /**
     * {@link #XDG_VIDEOS_DIR} as a {@link File} object, but only if
     * <code>XDG_VIDEOS_DIR</code> is not <code>null</code> <em>and</em> that
     * directory actually exists. <code>null</code> otherwise.
     */
    public static final File XDG_VIDEOS_DIR_FILE = ((XDG_VIDEOS_DIR != null) && new File(XDG_VIDEOS_DIR).isDirectory()) ? new File(XDG_VIDEOS_DIR) : null;

    public static void main(String[] args) {
        System.out.println("HOME: " + HOME);
        System.out.println("XDG_DATA_HOME: " + XDG_DATA_HOME);
        System.out.println("XDG_CONFIG_HOME: " + XDG_CONFIG_HOME);
        System.out.println("XDG_DATA_DIRS: " + XDG_DATA_DIRS);
        System.out.println("XDG_CONFIG_DIRS: " + XDG_CONFIG_DIRS);
        System.out.println("XDG_CACHE_HOME: " + XDG_CACHE_HOME);
        System.out.println("XDG_RUNTIME_DIR: " + XDG_RUNTIME_DIR);
        System.out.println("XDG_DATA_HOME_FILE: " + XDG_DATA_HOME_FILE);
        System.out.println("XDG_CONFIG_HOME_FILE: " + XDG_CONFIG_HOME_FILE);
        System.out.println("XDG_CACHE_HOME_FILE: " + XDG_CACHE_HOME_FILE);
        System.out.println("XDG_RUNTIME_DIR_FILE: " + XDG_RUNTIME_DIR_FILE);
        System.out.println("XDG_DESKTOP_DIR: " + XDG_DESKTOP_DIR);
        System.out.println("XDG_DOCUMENTS_DIR: " + XDG_DOCUMENTS_DIR);
        System.out.println("XDG_DOWNLOAD_DIR: " + XDG_DOWNLOAD_DIR);
        System.out.println("XDG_MUSIC_DIR: " + XDG_MUSIC_DIR);
        System.out.println("XDG_PICTURES_DIR: " + XDG_PICTURES_DIR);
        System.out.println("XDG_PUBLICSHARE_DIR: " + XDG_PUBLICSHARE_DIR);
        System.out.println("XDG_TEMPLATES_DIR: " + XDG_TEMPLATES_DIR);
        System.out.println("XDG_VIDEOS_DIR: " + XDG_VIDEOS_DIR);
        System.out.println("XDG_DESKTOP_DIR_FILE: " + XDG_DESKTOP_DIR_FILE);
        System.out.println("XDG_DOCUMENTS_DIR_FILE: " + XDG_DOCUMENTS_DIR_FILE);
        System.out.println("XDG_DOWNLOAD_DIR_FILE: " + XDG_DOWNLOAD_DIR_FILE);
        System.out.println("XDG_MUSIC_DIR_FILE: " + XDG_MUSIC_DIR_FILE);
        System.out.println("XDG_PICTURES_DIR_FILE: " + XDG_PICTURES_DIR_FILE);
        System.out.println("XDG_PUBLICSHARE_DIR_FILE: " + XDG_PUBLICSHARE_DIR_FILE);
        System.out.println("XDG_TEMPLATES_DIR_FILE: " + XDG_TEMPLATES_DIR_FILE);
        System.out.println("XDG_VIDEOS_DIR_FILE: " + XDG_VIDEOS_DIR_FILE);
    }

    private static void readShellVarScript(File script, Properties props) {
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(script))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();

                    // Separate the line into key and value
                    int p = line.indexOf('=');
                    if (p < 0) {
                        continue;
                    }
                    String key = line.substring(0, p);
                    String value = line.substring(p + 1);

                    // If the value is surrounded by double quotes, strip them
                    if ((value.length() >= 2) && value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Expand any variables recursively
                    String previousValue;
                    do {
                        previousValue = value;
                        value = expandVariables(value);
                    } while (!value.equals(previousValue));

                    // Store the variables as properties
                    props.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading " + script);
        }
    }

    private static String expandVariables(String value) {
        StringBuilder result = new StringBuilder(value.length());
        StringBuilder varName = new StringBuilder(100);
        final int COPYING = 1;
        final int DOLLAR_ENCOUNTERED = 2;
        final int READING_VAR_NAME = 3;
        int state = COPYING;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (state) {
                case COPYING:
                    if (c == '$') {
                        state = DOLLAR_ENCOUNTERED;
                    } else {
                        result.append(c);
                    }
                    break;
                case DOLLAR_ENCOUNTERED:
                    if (Character.isLetter(c) || (c == '_')) {
                        varName.setLength(0);
                        varName.append(c);
                        state = READING_VAR_NAME;
                    } else {
                        result.append('$');
                        result.append(c);
                        state = COPYING;
                    }
                    break;
                case READING_VAR_NAME:
                    if (Character.isLetter(c) || Character.isDigit(c) || (c == '_')) {
                        varName.append(c);
                    } else {
                        String varNameStr = varName.toString();
                        if (varNameStr.equals("HOME")) {
                            result.append(System.getProperty("user.home"));
                        } else if (System.getenv(varNameStr) != null) {
                            result.append(varNameStr);
                        }
                        result.append(c);
                        state = COPYING;
                    }
                    break;
            }
        }
        if (state == DOLLAR_ENCOUNTERED) {
            result.append('$');
        } else if (state == READING_VAR_NAME) {
            String varNameStr = varName.toString();
            if (varNameStr.equals("HOME")) {
                result.append(System.getProperty("user.home"));
            } else if (System.getenv(varNameStr) != null) {
                result.append(varNameStr);
            }
        }
        return result.toString();
    }
}