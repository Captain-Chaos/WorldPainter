package org.pepsoft.worldpainter.biomeschemes;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

class MinecraftRuntimeUtils {
    /**
     * Create a class loader for the specified Minecraft jar, including the jars
     * specified in its associated json descriptor file.
     *
     * @param minecraftJar The Minecraft jar for which to create a class loader.
     * @param libDir The directory from which to load the dependencies.
     * @return A class loader containing all (headless) dependencies for the
     *     specified Minecraft jar.
     */
    static ClassLoader getClassLoader(File minecraftJar, File libDir) {
        List<URL> classpath = new ArrayList<>(25);

        try {
            // Construct a classpath, starting with the Minecraft jar itself
            classpath.add(minecraftJar.toURI().toURL());

            // Find and parse the json descriptor, adding the libraries to the
            // claspath
            File minecraftJarDir = minecraftJar.getParentFile();
            String minecraftJarName = minecraftJar.getName();
            String jsonFileName = minecraftJarName.substring(0, minecraftJarName.length() - 4) + ".json";
            File jsonFile = new File(minecraftJarDir, jsonFileName);
            try (FileReader in = new FileReader(jsonFile)) {
                Map<?, ?> rootNode = (Map<?, ?>) new JSONParser().parse(in);
                List<Map<?, ?>> librariesNode = (List<Map<?, ?>>) rootNode.get("libraries");
                for (Map<?, ?> libraryNode : librariesNode) {
                    if (libraryNode.containsKey("rules")) {
                        // For now we just skip any library that has rules, on
                        // the assumption that it is a platform dependent
                        // library that's only needed by the actual game client
                        // and not by the Minecraft core
                        continue;
                    }
                    String libraryDescriptor = (String) libraryNode.get("name");
                    String[] parts = libraryDescriptor.split(":");
                    String libraryGroup = parts[0];
                    String libraryName = parts[1];
                    if (IGNORED_LIBRARY_NAMES.contains(libraryName)) {
                        continue;
                    }
                    String libraryVersion = parts[2];
                    File libraryDir = new File(libDir, libraryGroup.replace('.', '/') + '/' + libraryName + '/' + libraryVersion);
                    File libraryFile = new File(libraryDir, libraryName + '-' + libraryVersion + ".jar");
                    if (logger.isTraceEnabled()) {
                        logger.trace("Adding to biome scheme classpath: {}", libraryFile);
                    }
                    classpath.add(libraryFile.toURI().toURL());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to load Minecraft jar descriptor json file", e);
        } catch (ParseException e) {
            throw new RuntimeException("Parsing error while trying to load Minecraft jar descriptor json file", e);
        }

        // Create the class loader and return it
        return new URLClassLoader(classpath.toArray(new URL[classpath.size()]));
    }

    // We want all logging to be redirected to the slf4j API
    private static final Set<String> IGNORED_LIBRARY_NAMES = new HashSet<>(Arrays.asList("log4j-api", "log4j-core", "commons-logging"));
    private static final Logger logger = LoggerFactory.getLogger(AbstractMinecraft1_7BiomeScheme.class);
}