/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.Checksum;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.GUIUtils;
import org.pepsoft.util.Version;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class BiomeSchemeManager {
    public static BiomeScheme getNewBiomeScheme(final int biomeAlgorithm) {
        return getBiomeScheme(biomeAlgorithm, false);
    }

    public static BiomeScheme getSharedBiomeScheme(final int biomeAlgorithm) {
        return getBiomeScheme(biomeAlgorithm, true);
    }

    public static BiomeScheme getBiomeScheme(final int biomeAlgorithm, final boolean shared) {
        if (logger.isTraceEnabled()) {
            logger.trace("Thread {} requesting biome scheme {}", Thread.currentThread().getName(), biomeAlgorithm, new Throwable("Invoked from"));
        }

        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        if (shared && BIOME_SCHEMES.containsKey(biomeAlgorithm)) {
            // We already previously found and initialised a biome scheme for
            // this algorithm, so reuse it
            return BIOME_SCHEMES.get(biomeAlgorithm);
        } else {
            final String version;
            switch (biomeAlgorithm) {
                case BIOME_ALGORITHM_1_1:
                    version = "1.1";
                    break;
                case BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT:
                    version = "1.6.4 or 1.2.3 - 1.6.2";
                    break;
                case BIOME_ALGORITHM_1_3_LARGE:
                    version = "1.6.4 or 1.3.1 - 1.6.2";
                    break;
                case BIOME_ALGORITHM_1_7_DEFAULT:
                    version = "1.12.2 or 1.7.2 - 1.10.2";
                    break;
                case BIOME_ALGORITHM_1_7_LARGE:
                    version = "1.12.2 or 1.7.2 - 1.10.2";
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            SortedMap<Version, BiomeJar> biomeJars = BIOME_JARS.get(biomeAlgorithm);
            if ((biomeJars != null) && (! biomeJars.isEmpty())) {
                // We have a jar for this biome scheme
                if (Configuration.getInstance().isSafeMode()) {
                    logger.info("[SAFE MODE] Not creating biome scheme");
                    return null;
                } else {
                    final BiomeJar biomeJar = biomeJars.get(biomeJars.lastKey());
                    logger.info("Creating biome scheme " + version + " from " + biomeJar.file.getAbsolutePath());
                    try {
                        BiomeScheme biomeScheme = biomeJar.descriptor.instantiate(biomeJar.file, minecraftDir, biomeJar.checksum);
                        if (shared) {
                            BIOME_SCHEMES.put(biomeAlgorithm, biomeScheme);
                        }
                        return biomeScheme;
                    } catch (RuntimeException | Error e) {
                        logger.error("{} while instantiating biome scheme of type {} from {}", e.getClass().getSimpleName(), biomeJar.descriptor._class.getName(), biomeJar.file.getAbsolutePath(), e);
                        return null;
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not find compatible jar for biome scheme " + version);
                }
                return null;
            }
        }
    }

    /**
     * Get all found Minecraft jars.
     *
     * @return All found Minecraft jars.
     */
    public static SortedMap<Version, File> getAllMinecraftJars() {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        return Collections.unmodifiableSortedMap(ALL_JARS);
    }

    /**
     * Get the most recent version of Minecraft this class has been able to find.
     *
     * @return The most recent version of Minecraft this class has been able to find, or {@code null} if no Minecraft
     * jar is available.
     */
    public static Version getLatestMinecraftVersion() {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        return ALL_JARS.isEmpty() ? null : ALL_JARS.lastKey();
    }

    /**
     * Get a Minecraft jar of a specific version.
     *
     * @param version The Minecraft version for which to return a jar.
     * @return The Minecraft jar of the specified version, or {@code null} if no Minecraft jar is available for the
     * specified version.
     */
    public static File getMinecraftJar(Version version) {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        return ALL_JARS.get(version);
    }

    /**
     * Get the highest version Minecraft jar available.
     *
     * @return The highest version Minecraft jar available, or {@code null} if
     * no Minecraft jar is available.
     */
    public static File getLatestMinecraftJar() {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        return ALL_JARS.isEmpty() ? null : ALL_JARS.get(ALL_JARS.lastKey());
    }

    /**
     * Get the highest version Minecraft jar available of the specified version
     * or lower.
     *
     * @param version The highest version to return.
     * @return The highest version Minecraft jar found that is not higher than
     * the specified version, or {@code null} if no such Minecraft jar is
     * available.
     */
    public static File getMinecraftJarNoNewerThan(Version version) {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        return ALL_JARS.entrySet().stream()
                .filter(e -> e.getKey().compareTo(version) <= 0)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).orElse(null);
    }

    /**
     * Get the highest version Minecraft jar available of the specified version
     * or higher.
     *
     * @param version The lowest version to return.
     * @return The highest version Minecraft jar found that is not lower than
     * the specified version, or {@code null} if no such Minecraft jar is
     * available.
     */
    public static File getMinecraftJarNoOlderThan(Version version) {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        return ALL_JARS.entrySet().stream()
                .filter(e -> e.getKey().compareTo(version) >= 0)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).orElse(null);
    }

    public static BufferedImage createImage(BiomeScheme biomeScheme, int biome, ColourScheme colourScheme) {
        int backgroundColour = biomeScheme.getColour(biome, colourScheme);
        boolean[][] pattern = biomeScheme.getPattern(biome);
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if ((pattern != null) && pattern[x][y]) {
                    image.setRGB(x, y, 0);
                } else {
                    image.setRGB(x, y, backgroundColour);
                }
            }
        }
        return GUIUtils.scaleToUI(image, true);
    }

    public static List<Integer> getAvailableBiomeAlgorithms() {
        synchronized (initialisationLock) {
            if (! initialised) {
                initialise();
            }
        }

        List<Integer> availableBiomeAlgorithms = new ArrayList<>(BIOME_JARS.keySet());
        availableBiomeAlgorithms.sort(Comparator.reverseOrder());
        return availableBiomeAlgorithms;
    }

    /**
     * Starts background initialisation of the biome scheme manager, so that
     * subsequent invocations of the {@code getSharedBiomeScheme} methods won't
     * have to wait for initialisation.
     */
    public static void initialiseInBackground() {
        synchronized (initialisationLock) {
            if (initialised || initialising) {
                return;
            }
            initialising = true;
            new Thread(() -> {
                try {
                    doInitialisation();
                } catch (Throwable t) {
                    logger.error(t.getClass().getSimpleName() + " while scanning for Minecraft jars", t);
                }
            }, "Biome Scheme Manager Initialiser").start();
        }
    }

    private static void scanDir(File dir) {
        final File[] files = dir.listFiles(file -> {
            final String name = file.getName().toLowerCase();
            return (file.isDirectory() && VERSION_NUMBERS_ONLY.matcher(name).matches())
                    || (name.endsWith(".jar") && VERSION_NUMBERS_ONLY.matcher(name.substring(0, name.length() - 4)).matches());
        });
        if (files != null) {
            for (File file: files) {
                try  {
                    if (file.isDirectory()) {
                        scanDir(file);
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Scanning file {}", file);
                        }
                        final Checksum hash = FileUtils.getMD5(file);
                        if (DESCRIPTORS.containsKey(hash)) {
                            for (BiomeSchemeDescriptor descriptor: DESCRIPTORS.get(hash)) {
                                BIOME_JARS.computeIfAbsent(descriptor.biomeScheme, k -> new TreeMap<>())
                                        .put(descriptor.minecraftVersion, new BiomeJar(file, hash, descriptor));
                                // Also store it as a resources jar
                                ALL_JARS.put(descriptor.minecraftVersion, file);
                            }
                        } else {
                            // It's not a supported jar, but at least use for loading resources
                            Version version = Version.parse(file.getName().substring(0, file.getName().length() - 4));
                            ALL_JARS.put(version, file);
                        }
                    }
                } catch (IOException e) {
                    logger.error("I/O error while scanning potential Minecraft jar or directory " + file.getAbsolutePath() + "; skipping file", e);
                }
            }
        }
    }

    private static void initialise() {
        synchronized (initialisationLock) {
            if (initialised) {
                return;
            } else if (initialising) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Thread {} waiting for another thread to finish initialisation", Thread.currentThread().getName());
                }
                // Another thread is initialising us; wait for it to finish
                while (initialising) {
                    try {
                        initialisationLock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Thread interrupted while waiting for biome scheme manager initialisation", e);
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Thread {} continuing", Thread.currentThread().getName());
                }
                return;
            } else {
                doInitialisation();
            }
        }
    }

    /**
     * Must be invoked while holding {@link #initialisationLock}.
     */
    private static void doInitialisation() {
        synchronized (initialisationLock) {
            if (logger.isDebugEnabled()) {
                logger.debug("Performing initialisation on thread {}", Thread.currentThread().getName());
            }
            try {
                // Scan the Minecraft directory for supported jars
                minecraftDir = MinecraftUtil.findMinecraftDir();
                if (minecraftDir != null) {
                    scanDir(new File(minecraftDir, "versions"));
                }

                // Collect the names of the files we already looked at so we can skip them below
                final Set<File> processedFiles = new HashSet<>();
                for (Map.Entry<Integer, SortedMap<Version, BiomeJar>> entry: BIOME_JARS.entrySet()) {
                    processedFiles.addAll(entry.getValue().values().stream().map(biomeJar -> biomeJar.file).collect(Collectors.toList()));
                }

                // Check the jars stored in the configuration (if we haven't encountered them above)
                final Configuration config = Configuration.getInstance();
                final Map<Integer, File> minecraftJars = config.getMinecraftJars();
                for (Iterator<Map.Entry<Integer, File>> i = minecraftJars.entrySet().iterator(); i.hasNext(); ) {
                    final Map.Entry<Integer, File> entry = i.next();
                    final File file = entry.getValue();
                    final String name = file.getName();
                    if ((name.length() <= 4) || (! VERSION_NUMBERS_ONLY.matcher(name.substring(0, name.length() - 4)).matches())) {
                        // We are only interested in vanilla jars
                        i.remove();
                        continue;
                    } else if (processedFiles.contains(file)) {
                        continue;
                    } else if ((! file.isFile()) || (! file.canRead())) {
                        // The file is no longer there, or it's not accessible; remove it from the configuration
                        i.remove();
                        continue;
                    }
                    try {
                        Checksum checksum = FileUtils.getMD5(file);
                        if (DESCRIPTORS.containsKey(checksum)) {
                            for (BiomeSchemeDescriptor descriptor: DESCRIPTORS.get(checksum)) {
                                SortedMap<Version, BiomeJar> jars = BIOME_JARS.computeIfAbsent(descriptor.biomeScheme, key -> new TreeMap<>());
                                jars.put(descriptor.minecraftVersion, new BiomeJar(file, checksum, descriptor));
                                // Also store it as a resources jar
                                ALL_JARS.put(descriptor.minecraftVersion, file);
                            }
                        } else {
                            // It's not a supported jar, but at least use it for loading resources
                            Version version = Version.parse(name.substring(0, name.length() - 4));
                            ALL_JARS.put(version, file);
                        }
                    } catch (IOException e) {
                        logger.error("I/O error while scanning Minecraft jar " + file.getAbsolutePath() + "; skipping file", e);
                    }
                }
            } finally {
                // Done
                initialised = true;
                initialising = false;
                initialisationLock.notifyAll();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Thread {} finished initialisation", Thread.currentThread().getName());
            }
        }
    }

    private static final Map<Checksum, Set<BiomeSchemeDescriptor>> DESCRIPTORS = new HashMap<>();
    private static final Map<Integer, BiomeScheme> BIOME_SCHEMES = new HashMap<>();
    private static final Map<Integer, SortedMap<Version, BiomeJar>> BIOME_JARS = new HashMap<>();
    private static final SortedMap<Version, File> ALL_JARS = new TreeMap<>();
    private static final Object initialisationLock = new Object();
    private static File minecraftDir;
    private static boolean initialised, initialising;

    static {
        addDescriptor(new Checksum(new byte[] {(byte) -23, (byte) 35, (byte) 2, (byte) -46, (byte) -84, (byte) -37, (byte) -89, (byte) -55, (byte) 126, (byte) 13, (byte) -115, (byte) -15, (byte) -31, (byte) 13, (byte) 32, (byte) 6}), new BiomeSchemeDescriptor(new Version(1, 1), BIOME_ALGORITHM_1_1, Minecraft1_1BiomeScheme.class, false));

        addDescriptor(new Checksum(new byte[] {(byte) 18, (byte) -10, (byte) -60, (byte) -79, (byte) -67, (byte) -52, (byte) 99, (byte) -16, (byte) 41, (byte) -29, (byte) -64, (byte) -120, (byte) -93, (byte) 100, (byte) -72, (byte) -28}),  new BiomeSchemeDescriptor(new Version(1, 2, 3), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false)); // guess
        addDescriptor(new Checksum(new byte[] {(byte) 37, (byte) 66, (byte) 62, (byte) -85, (byte) 109, (byte) -121, (byte) 7, (byte) -7, (byte) 108, (byte) -58, (byte) -83, (byte) -118, (byte) 33, (byte) -89, (byte) 37, (byte) 10}),       new BiomeSchemeDescriptor(new Version(1, 2, 4), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false)); // guess
        addDescriptor(new Checksum(new byte[] {(byte) -114, (byte) -121, (byte) 120, (byte) 7, (byte) -118, (byte) 23, (byte) 90, (byte) 51, (byte) 96, (byte) 58, (byte) 88, (byte) 82, (byte) 87, (byte) -14, (byte) -123, (byte) 99}),       new BiomeSchemeDescriptor(new Version(1, 2, 5), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false)); // guess
        addDescriptor(new Checksum(new byte[] {(byte) 38, (byte) 108, (byte) -53, (byte) -55, (byte) 121, (byte) -118, (byte) -3, (byte) 46, (byte) -83, (byte) -13, (byte) -42, (byte) -64, (byte) 27, (byte) 76, (byte) 86, (byte) 42}),      new BiomeSchemeDescriptor(new Version(1, 3, 1), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false)); // guess
        addDescriptor(new Checksum(new byte[] {(byte) -106, (byte) -106, (byte) -103, (byte) -15, (byte) 62, (byte) 91, (byte) -66, (byte) 127, (byte) 18, (byte) -28, (byte) 10, (byte) -60, (byte) -13, (byte) 43, (byte) 125, (byte) -102}), new BiomeSchemeDescriptor(new Version(1, 3, 2), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) 119, (byte) 17, (byte) 117, (byte) -64, (byte) 23, (byte) 120, (byte) -22, (byte) 103, (byte) 57, (byte) 91, (byte) -58, (byte) -111, (byte) -102, (byte) 90, (byte) -99, (byte) -59}),   new BiomeSchemeDescriptor(new Version(1, 4, 2), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) -114, (byte) -128, (byte) -5, (byte) 1, (byte) -77, (byte) 33, (byte) -58, (byte) -77, (byte) -57, (byte) -17, (byte) -54, (byte) 57, (byte) 122, (byte) 62, (byte) -22, (byte) 53}),     new BiomeSchemeDescriptor(new Version(1, 4, 7), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) 92, (byte) 18, (byte) 25, (byte) -40, (byte) 105, (byte) -72, (byte) 125, (byte) 35, (byte) 61, (byte) -29, (byte) 3, (byte) 54, (byte) -120, (byte) -20, (byte) 117, (byte) 103}),       new BiomeSchemeDescriptor(new Version(1, 5, 1), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) 104, (byte) -105, (byte) -61, (byte) 40, (byte) 127, (byte) -71, (byte) 113, (byte) -55, (byte) -13, (byte) 98, (byte) -21, (byte) 58, (byte) -78, (byte) 15, (byte) 93, (byte) -35}),    new BiomeSchemeDescriptor(new Version(1, 5, 2), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_2BiomeScheme.class, false));

        addDescriptor(new Checksum(new byte[] {(byte) -106, (byte) -106, (byte) -103, (byte) -15, (byte) 62, (byte) 91, (byte) -66, (byte) 127, (byte) 18, (byte) -28, (byte) 10, (byte) -60, (byte) -13, (byte) 43, (byte) 125, (byte) -102}), new BiomeSchemeDescriptor(new Version(1, 3, 2), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_3LargeBiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) 119, (byte) 17, (byte) 117, (byte) -64, (byte) 23, (byte) 120, (byte) -22, (byte) 103, (byte) 57, (byte) 91, (byte) -58, (byte) -111, (byte) -102, (byte) 90, (byte) -99, (byte) -59}),   new BiomeSchemeDescriptor(new Version(1, 4, 2), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_3LargeBiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) -114, (byte) -128, (byte) -5, (byte) 1, (byte) -77, (byte) 33, (byte) -58, (byte) -77, (byte) -57, (byte) -17, (byte) -54, (byte) 57, (byte) 122, (byte) 62, (byte) -22, (byte) 53}),     new BiomeSchemeDescriptor(new Version(1, 4, 7), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_3LargeBiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) 92, (byte) 18, (byte) 25, (byte) -40, (byte) 105, (byte) -72, (byte) 125, (byte) 35, (byte) 61, (byte) -29, (byte) 3, (byte) 54, (byte) -120, (byte) -20, (byte) 117, (byte) 103}),       new BiomeSchemeDescriptor(new Version(1, 5, 1), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_3LargeBiomeScheme.class, false));
        addDescriptor(new Checksum(new byte[] {(byte) 104, (byte) -105, (byte) -61, (byte) 40, (byte) 127, (byte) -71, (byte) 113, (byte) -55, (byte) -13, (byte) 98, (byte) -21, (byte) 58, (byte) -78, (byte) 15, (byte) 93, (byte) -35}),    new BiomeSchemeDescriptor(new Version(1, 5, 2), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_3LargeBiomeScheme.class, false));

        addDescriptor(new Checksum(new byte[] {(byte) 29, (byte) 67, (byte) -51, (byte) -70, (byte) -117, (byte) -105, (byte) 82, (byte) -41, (byte) -11, (byte) 87, (byte) -85, (byte) 125, (byte) 62, (byte) 54, (byte) 89, (byte) 100}),     new BiomeSchemeDescriptor(new Version(1, 6, 2), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_6BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 46, (byte) 80, (byte) 68, (byte) -11, (byte) 53, (byte) -98, (byte) -126, (byte) 36, (byte) 85, (byte) 81, (byte) 22, (byte) 122, (byte) 35, (byte) 127, (byte) 49, (byte) 103}),         new BiomeSchemeDescriptor(new Version(1, 6, 4), BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, Minecraft1_6BiomeScheme.class, true));

        addDescriptor(new Checksum(new byte[] {(byte) 29, (byte) 67, (byte) -51, (byte) -70, (byte) -117, (byte) -105, (byte) 82, (byte) -41, (byte) -11, (byte) 87, (byte) -85, (byte) 125, (byte) 62, (byte) 54, (byte) 89, (byte) 100}),     new BiomeSchemeDescriptor(new Version(1, 6, 2), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_6LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 46, (byte) 80, (byte) 68, (byte) -11, (byte) 53, (byte) -98, (byte) -126, (byte) 36, (byte) 85, (byte) 81, (byte) 22, (byte) 122, (byte) 35, (byte) 127, (byte) 49, (byte) 103}),         new BiomeSchemeDescriptor(new Version(1, 6, 4), BIOME_ALGORITHM_1_3_LARGE, Minecraft1_6LargeBiomeScheme.class, true));

        addDescriptor(new Checksum(new byte[] {(byte) 122, (byte) 48, (byte) 69, (byte) 84, (byte) -3, (byte) -22, (byte) -121, (byte) -102, (byte) 121, (byte) -98, (byte) -2, (byte) 110, (byte) -82, (byte) -35, (byte) -116, (byte) -107}), new BiomeSchemeDescriptor(new Version(1,  7, 2), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_7BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 95, (byte) 124, (byte) -57, (byte) -21, (byte) 1, (byte) -53, (byte) -39, (byte) 53, (byte) -87, (byte) -105, (byte) 60, (byte) -74, (byte) -23, (byte) -60, (byte) -63, (byte) 14}),     new BiomeSchemeDescriptor(new Version(1,  7, 9), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_7BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -122, (byte) 99, (byte) -95, (byte) 12, (byte) -20, (byte) -63, (byte) 14, (byte) -86, (byte) 104, (byte) 58, (byte) -110, (byte) 126, (byte) -11, (byte) 55, (byte) 24, (byte) 82}),     new BiomeSchemeDescriptor(new Version(1,  8),    BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_8BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 92, (byte) -102, (byte) -81, (byte) 49, (byte) 25, (byte) -97, (byte) 118, (byte) 62, (byte) -7, (byte) 8, (byte) 92, (byte) -55, (byte) -74, (byte) -112, (byte) 43, (byte) 29}),        new BiomeSchemeDescriptor(new Version(1,  8, 1), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_8BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -108, (byte) 55, (byte) -76, (byte) -114, (byte) 5, (byte) 27, (byte) 12, (byte) -24, (byte) -127, (byte) 124, (byte) -104, (byte) -98, (byte) 89, (byte) -25, (byte) -103, (byte) 79}),  new BiomeSchemeDescriptor(new Version(1,  8, 3), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_8BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 101, (byte) -123, (byte) -119, (byte) 23, (byte) -111, (byte) -32, (byte) -41, (byte) 93, (byte) -59, (byte) 76, (byte) -5, (byte) 84, (byte) 122, (byte) -122, (byte) 109, (byte) -80}), new BiomeSchemeDescriptor(new Version(1,  8, 8), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_8BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 57, (byte) 96, (byte) -103, (byte) -30, (byte) 62, (byte) 88, (byte) 48, (byte) -99, (byte) 33, (byte) 113, (byte) 58, (byte) -75, (byte) -41, (byte) -63, (byte) -47, (byte) -88}),      new BiomeSchemeDescriptor(new Version(1,  8, 9), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_8BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -66, (byte) 112, (byte) 66, (byte) 2, (byte) 51, (byte) 112, (byte) -101, (byte) 61, (byte) 119, (byte) -113, (byte) 118, (byte) 39, (byte) -35, (byte) 80, (byte) -34, (byte) 98}),      new BiomeSchemeDescriptor(new Version(1,  9),    BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_8BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 55, (byte) -8, (byte) 13, (byte) 38, (byte) 104, (byte) 114, (byte) -20, (byte) 17, (byte) 86, (byte) 10, (byte) -80, (byte) -119, (byte) 95, (byte) 124, (byte) -10, (byte) 59}),        new BiomeSchemeDescriptor(new Version(1, 10, 2), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_10BiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -116, (byte) 4, (byte) 67, (byte) -122, (byte) -117, (byte) -98, (byte) 70, (byte) -57, (byte) 125, (byte) 57, (byte) -37, (byte) 97, (byte) -57, (byte) 85, (byte) 103, (byte) -99}),    new BiomeSchemeDescriptor(new Version(1, 12, 2), BIOME_ALGORITHM_1_7_DEFAULT, Minecraft1_12BiomeScheme.class, true));

        addDescriptor(new Checksum(new byte[] {(byte) 122, (byte) 48, (byte) 69, (byte) 84, (byte) -3, (byte) -22, (byte) -121, (byte) -102, (byte) 121, (byte) -98, (byte) -2, (byte) 110, (byte) -82, (byte) -35, (byte) -116, (byte) -107}), new BiomeSchemeDescriptor(new Version(1,  7, 2), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_7LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 95, (byte) 124, (byte) -57, (byte) -21, (byte) 1, (byte) -53, (byte) -39, (byte) 53, (byte) -87, (byte) -105, (byte) 60, (byte) -74, (byte) -23, (byte) -60, (byte) -63, (byte) 14}),     new BiomeSchemeDescriptor(new Version(1,  7, 9), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_7LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -122, (byte) 99, (byte) -95, (byte) 12, (byte) -20, (byte) -63, (byte) 14, (byte) -86, (byte) 104, (byte) 58, (byte) -110, (byte) 126, (byte) -11, (byte) 55, (byte) 24, (byte) 82}),     new BiomeSchemeDescriptor(new Version(1,  8),    BIOME_ALGORITHM_1_7_LARGE, Minecraft1_8LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 92, (byte) -102, (byte) -81, (byte) 49, (byte) 25, (byte) -97, (byte) 118, (byte) 62, (byte) -7, (byte) 8, (byte) 92, (byte) -55, (byte) -74, (byte) -112, (byte) 43, (byte) 29}),        new BiomeSchemeDescriptor(new Version(1,  8, 1), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_8LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -108, (byte) 55, (byte) -76, (byte) -114, (byte) 5, (byte) 27, (byte) 12, (byte) -24, (byte) -127, (byte) 124, (byte) -104, (byte) -98, (byte) 89, (byte) -25, (byte) -103, (byte) 79}),  new BiomeSchemeDescriptor(new Version(1,  8, 3), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_8LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 101, (byte) -123, (byte) -119, (byte) 23, (byte) -111, (byte) -32, (byte) -41, (byte) 93, (byte) -59, (byte) 76, (byte) -5, (byte) 84, (byte) 122, (byte) -122, (byte) 109, (byte) -80}), new BiomeSchemeDescriptor(new Version(1,  8, 8), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_8LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 57, (byte) 96, (byte) -103, (byte) -30, (byte) 62, (byte) 88, (byte) 48, (byte) -99, (byte) 33, (byte) 113, (byte) 58, (byte) -75, (byte) -41, (byte) -63, (byte) -47, (byte) -88}),      new BiomeSchemeDescriptor(new Version(1,  8, 9), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_8LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -66, (byte) 112, (byte) 66, (byte) 2, (byte) 51, (byte) 112, (byte) -101, (byte) 61, (byte) 119, (byte) -113, (byte) 118, (byte) 39, (byte) -35, (byte) 80, (byte) -34, (byte) 98}),      new BiomeSchemeDescriptor(new Version(1,  9),    BIOME_ALGORITHM_1_7_LARGE, Minecraft1_8LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) 55, (byte) -8, (byte) 13, (byte) 38, (byte) 104, (byte) 114, (byte) -20, (byte) 17, (byte) 86, (byte) 10, (byte) -80, (byte) -119, (byte) 95, (byte) 124, (byte) -10, (byte) 59}),        new BiomeSchemeDescriptor(new Version(1, 10, 2), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_10LargeBiomeScheme.class, true));
        addDescriptor(new Checksum(new byte[] {(byte) -116, (byte) 4, (byte) 67, (byte) -122, (byte) -117, (byte) -98, (byte) 70, (byte) -57, (byte) 125, (byte) 57, (byte) -37, (byte) 97, (byte) -57, (byte) 85, (byte) 103, (byte) -99}),    new BiomeSchemeDescriptor(new Version(1, 12, 2), BIOME_ALGORITHM_1_7_LARGE, Minecraft1_12LargeBiomeScheme.class, true));
    }

    private static void addDescriptor(Checksum checksum, BiomeSchemeDescriptor descriptor) {
        Set<BiomeSchemeDescriptor> descriptors = DESCRIPTORS.computeIfAbsent(checksum, key -> new HashSet<>());
        descriptors.add(descriptor);
    }

    private static final Pattern VERSION_NUMBERS_ONLY = Pattern.compile("([0-9]+\\.)*[0-9]+");
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BiomeSchemeManager.class);

    static class BiomeSchemeDescriptor {
        BiomeSchemeDescriptor(Version minecraftVersion, int biomeScheme, Class<? extends BiomeScheme> _class, boolean addLibraries) {
            this.minecraftVersion = minecraftVersion;
            this.biomeScheme = biomeScheme;
            this._class = _class;
            this.addLibraries = addLibraries;
            try {
                constructor = addLibraries
                        ? _class.getConstructor(File.class, File.class, Checksum.class)
                        : _class.getConstructor(File.class, Checksum.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public BiomeScheme instantiate(File jarFile, File minecraftDir, Checksum checksum) {
            try {
                if (addLibraries) {
                    File libDir = new File(minecraftDir, "libraries");
                    return constructor.newInstance(jarFile, libDir, checksum);
                } else {
                    return constructor.newInstance(jarFile, checksum);
                }
            } catch (InstantiationException e) {
                throw new RuntimeException("Could not instantiate biome scheme", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied while instantiating biome scheme", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Exception thrown while instantiating biome scheme", e);
            }
        }

        final Version minecraftVersion;
        final int biomeScheme;
        final Class<? extends BiomeScheme> _class;
        final Constructor<? extends BiomeScheme> constructor;
        private final boolean addLibraries;
    }

    static class BiomeJar {
        BiomeJar(File file, Checksum checksum, BiomeSchemeDescriptor descriptor) {
            this.file = file;
            this.checksum = checksum;
            this.descriptor = descriptor;
        }

        final File file;
        final Checksum checksum;
        final BiomeSchemeDescriptor descriptor;
    }
}