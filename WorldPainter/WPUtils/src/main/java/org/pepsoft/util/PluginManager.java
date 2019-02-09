/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import com.google.common.collect.ImmutableList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
     * @param filename The resource path of the file containing the plugin
     *                 descriptor.
     */
    public static void loadPlugins(File pluginDir, PublicKey publicKey, String filename) {
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
                    checkForUpdates(jarFile, publicKey, filename);
                    ClassLoader pluginClassLoader = new URLClassLoader(new URL[] {pluginFile.toURI().toURL()});
                    jarClassLoaders.put(jarFile, pluginClassLoader);
                } catch (IOException e) {
                    logger.error("{} while loading plugin from file {} (message: {}); not loading it", e.getClass().getSimpleName(), pluginFile.getName(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Obtain a list of instances of all plugins available through a particular
     * classloader, or from plugin jars discovered by a previous invocation of
     * {@link #loadPlugins(File, PublicKey, String)}, which implement a
     * particular type.
     *
     * @param type The type of plugin to return.
     * @param filename The resource path of the file containing the plugin
     *                 descriptor.
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
                try {
                    findPlugins(type, filename, pluginJar, plugins);
                } catch (ParseException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    logger.error("{} while instantiating plugin {} (message: {}); skipping plugin", e.getClass().getSimpleName(), pluginJar.getName(), e.getMessage(), e);
                }
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
    
    @SuppressWarnings({"StatementWithEmptyBody", "BooleanMethodIsAlwaysInverted"})
    private static boolean isSigned(JarFile jarFile, PublicKey publicKey) throws IOException {
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
            // Iterator over all the entries in the jar except directories and
            // signature files
            JarEntry jarEntry = e.nextElement();
            String entryName = jarEntry.getName().toUpperCase();
            if (jarEntry.isDirectory() || entryName.endsWith(".SF") || entryName.endsWith(".DSA") || entryName.endsWith(".EC") || entryName.endsWith(".RSA")) {
                continue;
            }

            // Read the entry fully, otherwise the certificates won't be available
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream in = jarFile.getInputStream(jarEntry)) {
                while (in.read(buffer) != -1) ;
            }

            // Get the signing certificate chain and check if one of them is the
            // WorldPainter plugin signing certificate
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
    private static <T> void findPlugins(Class<T> type, String filename, JarFile jarFile, List<T> plugins) throws IOException, ParseException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Descriptor descriptor = loadDescriptor(jarFile, filename);
        for (String class_: descriptor.classes) {
            Class<?> pluginType = classLoader.loadClass(class_);
            if (type.isAssignableFrom(pluginType)) {
                plugins.add(((Class<T>) pluginType).newInstance());
            }
        }
    }

    @SuppressWarnings("unchecked") // Guaranteed by isAssignableFrom
    private static <T> void findPlugins(Class<T> type, String filename, ClassLoader classLoader, List<T> plugins) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(filename);
        while (resources.hasMoreElements()) {
            try (InputStream in = resources.nextElement().openStream()) {
                Descriptor descriptor = loadDescriptor(in);
                for (String class_: descriptor.classes) {
                    Class<?> pluginType = classLoader.loadClass(class_);
                    if (type.isAssignableFrom(pluginType)) {
                        plugins.add(((Class<T>) pluginType).newInstance());
                    }
                }
            } catch (IOException | ClassNotFoundException | IllegalAccessException | InstantiationException | ParseException e) {
                logger.error("{} while instantiating plugin (message: {}); skipping plugin", e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Checks whether the specified plugin jar specifies an update URL, and if
     * so checks whether there is a newer version and downloads it if so,
     * replacing the original file.
     *
     * @param jarFile The plugin jar for which to check for updates.
     * @param publicKey The public key corresponding to the private key with
     *                  which the update must have been signed.
     * @param filename The resource path of the file containing the plugin
     *                 descriptor.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored") // Best effort
    private static void checkForUpdates(JarFile jarFile, PublicKey publicKey, String filename) {
        File originalFile = new File(jarFile.getName());

        // Find, read and parse the descriptor
        try {
            Descriptor descriptor = loadDescriptor(jarFile, filename);

            // If there is an update URL, connect to it, specifying the
            // timestamp of the plugin file as the If-Modified-Since header
            if (descriptor.updateUrl != null) {
                URL updateURL = new URL(descriptor.updateUrl);
                HttpURLConnection connection = (HttpURLConnection) updateURL.openConnection();
                connection.setAllowUserInteraction(false);
                connection.setUseCaches(false);
                connection.setIfModifiedSince(originalFile.lastModified());
                connection.setConnectTimeout(UPDATE_TIMEOUT);
                connection.setReadTimeout(UPDATE_TIMEOUT);
                connection.connect();

                // If the server does not indicate that the file was not
                // modified, download it to a temporary file
                if (connection.getResponseCode() != HTTP_NOT_MODIFIED) {
                    logger.info("Update found for plugin {}; downloading update", originalFile);
                    File tempFile = File.createTempFile("wpdownloadedplugin", null);
                    try {
                        try (InputStream in2 = connection.getInputStream(); FileOutputStream out = new FileOutputStream(tempFile)) {
                            StreamUtils.copy(in2, out, MAXIMUM_PLUGIN_UPDATE_SIZE);
                        }

                        // Check that the update is signed, and if so copy it
                        // over the original plugin file (creating a backup of
                        // the plugin file just in case)
                        if (!isSigned(new JarFile(tempFile), publicKey)) {
                            logger.error("Update for {} downloaded, but is not official or has been tampered with; not updating plugin", originalFile);
                            return;
                        }
                        Files.move(originalFile.toPath(),
                                new File(originalFile.getParentFile(), originalFile.getName() + ".bak").toPath(),
                                REPLACE_EXISTING);
                        Files.move(tempFile.toPath(), originalFile.toPath());
                    } finally {
                        // Make sure the temporary file is deleted in all
                        // circumstances
                        tempFile.delete();
                    }
                }
            }
        } catch (IOException | ParseException | ClassCastException e) {
            logger.error("{} while checking for updates for plugin {} (message: {})", e.getClass().getSimpleName(), originalFile, e.getMessage(), e);
        }
    }

    /**
     * Load the plugin descriptor from a plugin jar. Both the old format and the
     * new JSON-based format are supported.
     *
     * @param jarFile The plugin jar from which to load the descriptor.
     * @param filename The resource path of the file containing the plugin
     *                 descriptor.
     * @return The plugin descriptor read from the plugin jar.
     */
    private static Descriptor loadDescriptor(JarFile jarFile, String filename) throws IOException, ParseException {
        try (InputStream in = jarFile.getInputStream(new ZipEntry(filename))) {
            return loadDescriptor(in);
        }
    }

    /**
     * Load the plugin descriptor from an input stream. Both the old format and the
     * new JSON-based format are supported.
     *
     * @param in The input stream form which to read the descriptor.
     * @return The plugin descriptor read from the stream.
     */
    @SuppressWarnings("unchecked") // Responsibility of plugin author
    private static Descriptor loadDescriptor(InputStream in) throws IOException, ParseException {
        try (BufferedInputStream in2 = new BufferedInputStream(in)) {
            // If the first character is not a { we just silently assume this is
            // an old style non-JSON descriptor
            in2.mark(1);
            boolean json = in2.read() == '{';
            in2.reset();
            if ((! json) && logger.isDebugEnabled()) {
                logger.debug("Plugin descriptor does not start with {; assuming it is not JSON");
            }

            if (json) {
                JSONParser parser = new JSONParser();
                JSONObject descriptor = (JSONObject) parser.parse(new InputStreamReader(in2, UTF_8));
                JSONArray classes = (JSONArray) descriptor.get("classes");
                String updateUrl = (String) descriptor.get("updateUrl");
                return new Descriptor(classes, updateUrl);
            } else {
                List<String> classes = new ArrayList<>();
                try (BufferedReader lineReader = new BufferedReader(new InputStreamReader(in2, UTF_8))) {
                    String line;
                    while ((line = lineReader.readLine()) != null) {
                        classes.add(line.trim());
                    }
                }
                return new Descriptor(classes, null);
            }
        }
    }

    private static final Map<JarFile, ClassLoader> jarClassLoaders = new HashMap<>();
    private static final int BUFFER_SIZE = 32768;
    private static final int UPDATE_TIMEOUT = 200; // milliseconds
    private static final int MAXIMUM_PLUGIN_UPDATE_SIZE = 2 * 1024 * 1024; // 2 MB TODO: this is too large; find out how to have the shader minimize the jar, preferably from the command line
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

    public static class Descriptor {
        public Descriptor(List<String> classes, String updateUrl) {
            this.classes = ImmutableList.copyOf(classes);
            this.updateUrl = updateUrl;
        }

        public final List<String> classes;
        public final String updateUrl;
    }
}