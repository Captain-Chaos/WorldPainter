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
    
    public static void loadPlugins(File pluginDir, PublicKey publicKey) {
        loadPlugins(pluginDir, publicKey, "");
    }
    
    public static void loadPlugins(File pluginDir, PublicKey publicKey, String logPrefix) {
        if (logger.isDebugEnabled()) {
            logger.debug(logPrefix + "Loading plugins");
        }
        File[] pluginFiles = pluginDir.listFiles((dir, name) -> {
            return name.toLowerCase().endsWith(".jar");
        });
        if (pluginFiles != null) {
            for (File pluginFile: pluginFiles) {
                try {
                    JarFile jarFile = new JarFile(pluginFile);
                    if (! isSigned(jarFile, publicKey)) {
                        logger.error(logPrefix + jarFile.getName() + " is not official or has been tampered with; not loading it");
                        continue;
                    }
                    ClassLoader pluginClassLoader = new URLClassLoader(new URL[] {pluginFile.toURI().toURL()});
                    pluginJars.put(jarFile, pluginClassLoader);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Malformed URL exception while trying to load plugins", e);
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while trying to load plugins", e);
                }
            }
        }
    }
    
    public static <T> List<T> findPlugins(Class<T> type, String filename, ClassLoader classLoader) {
        try {
            List<T> plugins = new ArrayList<>();
            findPlugins(filename, classLoader, plugins);
            for (JarFile pluginJar: pluginJars.keySet()) {
                findPlugins(filename, pluginJar, plugins);
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
    
    @SuppressWarnings("empty-statement")
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
    
    private static <T> void findPlugins(String filename, JarFile jarFile, List<T> plugins) throws IOException {
        JarEntry jarEntry = jarFile.getJarEntry(filename);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry), Charset.forName("UTF-8")))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<T> pluginType = (Class<T>) pluginJars.get(jarFile).loadClass(line);
                    plugins.add(pluginType.newInstance());
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
    
    private static <T> void findPlugins(String filename, ClassLoader classLoader, List<T> plugins) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(filename);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")))) {
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<T> pluginType = (Class<T>) classLoader.loadClass(line);
                        plugins.add(pluginType.newInstance());
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
    
    private static final Map<JarFile, ClassLoader> pluginJars = new HashMap<>();
    private static final int BUFFER_SIZE = 32768;
    private static final ClassLoader classLoader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            for (Map.Entry<JarFile, ClassLoader> entry: pluginJars.entrySet()) {
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