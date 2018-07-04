/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.CustomObjectManager;

import static org.pepsoft.worldpainter.objects.WPObject.*;

/**
 * A custom object provider which maintains a collection of objects which it
 * returns randomly (weighted according to their {@link
 * WPObject#ATTRIBUTE_FREQUENCY} attributes).
 *
 * @author pepijn
 */
public final class Bo2ObjectTube implements Bo2ObjectProvider {
    public Bo2ObjectTube(String name, List<WPObject> objects) {
        this.name = name;
        this.objects = Collections.unmodifiableList(objects);
        initWeightedObjects();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
    
    @Override
    public WPObject getObject() {
        return weightedObjects.tailMap(random.nextInt(totalObjectWeight)).values().iterator().next();
    }

    @Override
    public List<WPObject> getAllObjects() {
        return objects;
    }

    // Cloneable

    @Override
    public Bo2ObjectTube clone() {
        List<WPObject> clonedObjects = new ArrayList<>(objects.size());
        clonedObjects.forEach(object -> clonedObjects.add(object.clone()));
        return new Bo2ObjectTube(name, clonedObjects);
    }

    private void initWeightedObjects() {
        weightedObjects = new TreeMap<>();
        totalObjectWeight = 0;
        for (WPObject object: objects) {
            int frequency = object.getAttribute(ATTRIBUTE_FREQUENCY);
            totalObjectWeight += frequency;
            weightedObjects.put(totalObjectWeight, object);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        random = new Random();
        initWeightedObjects();
    }

    /**
     * Create a new <code>Bo2ObjectTube</code> containing all supported custom
     * objects from a specific directory. The name of the
     * <code>Bo2ObjectTube</code> will be set to the name of the directory.
     *
     * @param dir The directory containing the objects to load.
     * @return A new <code>Bo2ObjectTube</code> containing the supported objects
     *     from the specified directory.
     * @throws IOException If there was an I/O error reading one of the files.
     */
    public static Bo2ObjectTube load(File dir) throws IOException {
        return load(dir.getName(), dir);
    }
    
    /**
     * Create a new <code>Bo2ObjectTube</code> with a specific name, containing
     * all supported custom objects from a specific directory.
     *
     * @param name The name of the new <code>Bo2ObjectTube</code>.
     * @param dir The directory containing the objects to load.
     * @return A new <code>Bo2ObjectTube</code> containing the supported objects
     *     from the specified directory.
     * @throws IOException If there was an I/O error reading one of the files.
     */
    public static Bo2ObjectTube load(String name, File dir) throws IOException {
        File[] files = dir.listFiles((FilenameFilter) CustomObjectManager.getInstance().getFileFilter());
        //noinspection ConstantConditions // Responsibility of caller to provide extant directory
        return load(name, Arrays.asList(files));
    }
    
    /**
     * Create a new <code>Bo2ObjectTube</code> with a specific name from a list
     * of specific custom object files.
     *
     * @param name The name of the new <code>Bo2ObjectTube</code>.
     * @param files The list of files containing the objects to load.
     * @return A new <code>Bo2ObjectTube</code> containing the custom objects
     *     from the specified file(s).
     * @throws IOException If there was an I/O error reading one of the files.
     */
    public static Bo2ObjectTube load(String name, Collection<File> files) throws IOException {
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Cannot create an object tube with no objects");
        }
        List<WPObject> objects = new ArrayList<>(files.size());
        CustomObjectManager customObjectManager = CustomObjectManager.getInstance();
        for (File file: files) {
            objects.add(customObjectManager.loadObject(file));
        }
        return new Bo2ObjectTube(name, objects);
    }
    
    private final String name;
    private final List<WPObject> objects;
    private transient Random random = new Random();
    private transient SortedMap<Integer, WPObject> weightedObjects;
    private transient int totalObjectWeight;

    private static final long serialVersionUID = 1L;
}