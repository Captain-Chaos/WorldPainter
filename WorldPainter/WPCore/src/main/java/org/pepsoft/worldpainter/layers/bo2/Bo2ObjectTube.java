/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.io.File;
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
import static org.pepsoft.worldpainter.objects.WPObject.*;

/**
 *
 * @author pepijn
 */
public class Bo2ObjectTube implements Bo2ObjectProvider {
    public Bo2ObjectTube(String name, List<WPObject> objects) {
        this.name = name;
        this.objects = objects;
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
        if (weightedObjects == null) {
            weightedObjects = new TreeMap<>();
            totalObjectWeight = 0;
            for (WPObject object: objects) {
                int frequency = object.getAttribute(ATTRIBUTE_FREQUENCY, 100);
                totalObjectWeight += frequency;
                weightedObjects.put(totalObjectWeight, object);
            }
            
        }
        return weightedObjects.tailMap(random.nextInt(totalObjectWeight)).values().iterator().next();
    }

    @Override
    public List<WPObject> getAllObjects() {
        return Collections.unmodifiableList(objects);
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        random = new Random();
    }
    
    public static Bo2ObjectTube load(File dir) throws IOException {
        return load(dir.getName(), dir);
    }
    
    public static Bo2ObjectTube load(String name, File dir) throws IOException {
        File[] files = dir.listFiles((dir1, name1) -> name1.toLowerCase().endsWith(".bo2") || name1.toLowerCase().endsWith(".schematic"));
        return load(name, Arrays.asList(files));
    }
    
    public static Bo2ObjectTube load(String name, Collection<File> files) throws IOException {
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Cannot create an object tube with no objects");
        }
        List<WPObject> objects = new ArrayList<>(files.size());
        for (File file: files) {
            String filename = file.getName().toLowerCase();
            if (filename.endsWith(".bo2")) {
                objects.add(Bo2Object.load(file));
            } else if (filename.endsWith(".schematic")) {
                objects.add(Schematic.load(file));
            } else {
                throw new IllegalArgumentException("Unsupported file encountered: " + file);
            }
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