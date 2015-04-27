/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.SecureRandom;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.WorldExporter;

/**
 *
 * @author pepijn
 */
public class Timings {
    public static void main(String[] args) throws IOException, ProgressReceiver.OperationCancelled, ClassNotFoundException {
        Random random = new SecureRandom();
//        final Configuration defaultConfig = new Configuration();
        final World2 world;
        ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(args[0])));
        try {
            world = (World2) in.readObject();
        } finally {
            in.close();
        }
        long totalDuration = 0;
        for (int i = 0; i < 5; i++) {
//            final World2 world = WorldFactory.createDefaultWorld(defaultConfig, random.nextLong());
            world.getDimension(0).getTileFactory().setSeed(random.nextLong());
            if (world.getVersion() == 0) {
                if (world.getMaxHeight() == Constants.DEFAULT_MAX_HEIGHT_2) {
                    world.setVersion(Constants.SUPPORTED_VERSION_2);
                } else {
                    world.setVersion(Constants.SUPPORTED_VERSION_1);
                }
            }
            final WorldExporter exporter = new WorldExporter(world);
            System.out.println("Starting export of world " + world.getName() + " " + i + " (seed: " + world.getDimension(0).getSeed() + ")");
            File baseDir = new File(System.getProperty("user.dir"));
            String name = world.getName() + ' ' + i;
            File worldDir = new File(baseDir, FileUtils.sanitiseName(name));
            if (worldDir.isDirectory()) {
                FileUtils.deleteDir(worldDir);
            }
            long start = System.currentTimeMillis();
            exporter.export(baseDir, name, null, null);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Exporting world took " + (duration / 1000f) + " s");
            totalDuration += duration;
        }
        System.out.println("Average duration: " + (totalDuration / 5000f) + " s");
    }
}