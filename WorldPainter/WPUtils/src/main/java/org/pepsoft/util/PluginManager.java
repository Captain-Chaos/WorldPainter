/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A general purpose plugin manager.
 * 
 * @author pepijn
 */
public final class PluginManager {
    private PluginManager() {
        // Prevent instantiation
    }

    /**
     * Load plugin jars from a directory, which are signed with a particular
     * private key.
     *
     * <p>This method should be invoked only once. Any discovered and properly
     * signed plugin jars will be available to be returned by later invocations
     * of the {@link #findPlugins(Class, String, ClassLoader)} method.
     *
     * @param pluginDir The directory from which to load the plugins.
     * @param publicKey The public key corresponding to the private key with
     *                  which the plugins must have been signed.
     */
    public static void loadPlugins(File pluginDir, PublicKey publicKey) {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading plugins");
        }
        File[] pluginFiles = pluginDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (pluginFiles != null) {
            for (File pluginFile: pluginFiles) {
                try {
                    JarFile jarFile = new JarFile(pluginFile);
                    if (! isSigned(jarFile, publicKey)) {
                        logger.error(jarFile.getName() + " is not official or has been tampered with; not loading it");
                        continue;
                    }
                    ClassLoader pluginClassLoader = new URLClassLoader(new URL[] {pluginFile.toURI().toURL()});
                    jarClassLoaders.put(jarFile, pluginClassLoader);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Malformed URL exception while trying to load plugins", e);
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while trying to load plugins", e);
                }
            }
        }
    }

    /**
     * Obtain a list of instances of all plugins available through a particular
     * classloader, or from plugin jars discovered by a previous invocation of
     * {@link #loadPlugins(File, PublicKey)}, which implement a particular type.
     *
     * @param type The type of plugin to return.
     * @param filename The resource path of the file containing the plugin
     *                 implementation classnames.
     * @param classLoader The classloader from which to discover plugins.
     * @param <T> The type of plugin to return.
     * @return A list of newly instantiated plugin objects of the specified
     * type available from the specified classloader and/or any earlier
     * discovered plugin jars.
     */
    public static <T> List<T> findPlugins(Class<T> type, String filename, ClassLoader classLoader) {
        try {
            List<T> plugins = new ArrayList<>();
            findPlugins(type, filename, classLoader, plugins);
            for (JarFile pluginJar: jarClassLoaders.keySet()) {
                findPlugins(type, filename, pluginJar, plugins);
            }
            return plugins;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while loading plugins", e);
        }
    }
    
    /**
     * Get a classloader which gives access to the classes of all the plugins.
     * 
     * @return A classloader which gives access to the classes of all the
     *     plugins.
     */
    public static ClassLoader getPluginClassLoader() {
        return classLoader;
    }
    
    @SuppressWarnings({"empty-statement", "StatementWithEmptyBody"})
    private static boolean isSigned(JarFile jarFile, PublicKey publicKey) throws IOException {
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
            JarEntry jarEntry = e.nextElement();
            if (jarEntry.isDirectory() || jarEntry.getName().startsWith("META-INF/")) {
                continue;
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream in = jarFile.getInputStream(jarEntry)) {
                while (in.read(buffer) != -1) ;
            }
            Certificate[] certificates = jarEntry.getCertificates();
            boolean signed = false;
            if (certificates != null) {
                for (Certificate certificate: certificates) {
                    if (certificate.getPublicKey().equals(publicKey)) {
                        signed = true;
                        break;
                    }
                }
            }
            if (! signed) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked") // Guaranteed by isAssignableFrom
    private static <T> void findPlugins(Class<T> type, String filename, JarFile jarFile, List<T> plugins) throws IOException {
        JarEntry jarEntry = jarFile.getJarEntry(filename);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry), Charset.forName("UTF-8")))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    Class<?> pluginType = jarClassLoaders.get(jarFile).loadClass(line);
                    if (type.isAssignableFrom(pluginType)) {
                        plugins.add(((Class<T>) pluginType).newInstance());
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class not found while instantiating plugin " + line, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Access denied while instantiating plugin " + line, e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Exception thrown while instantiating plugin " + line, e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked") // Guaranteed by isAssignableFrom
    private static <T> void findPlugins(Class<T> type, String filename, ClassLoader classLoader, List<T> plugins) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(filename);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")))) {
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        Class<?> pluginType = classLoader.loadClass(line);
                        if (type.isAssignableFrom(pluginType)) {
                            plugins.add(((Class<T>) pluginType).newInstance());
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Class not found while instantiating plugin " + line, e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Access denied while instantiating plugin " + line, e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException("Exception thrown while instantiating plugin " + line, e);
                    }
                }
            }
        }
    }
    
    private static final Map<JarFile, ClassLoader> jarClassLoaders = new HashMap<>();
    private static final int BUFFER_SIZE = 32768;
    private static final ClassLoader classLoader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            for (Map.Entry<JarFile, ClassLoader> entry: jarClassLoaders.entrySet()) {
                Class<?> _class;
                try {
                    _class = entry.getValue().loadClass(name);
                } catch (ClassNotFoundException e) {
                    continue;
                }
                logger.debug("Loading " + name + " from " + entry.getKey().getName());
                return _class;
            }
            throw new ClassNotFoundException("Class " + name + " not found in plugin class loaders");
        }
    };
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PluginManager.class);
}