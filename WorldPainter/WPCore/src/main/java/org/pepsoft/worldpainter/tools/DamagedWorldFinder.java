/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author pepijn
 */
public class DamagedWorldFinder {
    public static void main(String[] args) {
        File dir = new File(args[0]);
        File[] files = dir.listFiles((dir1, name) -> {
            return name.toLowerCase().endsWith(".world");
        });
        for (File file: files) {
            try {
                try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                    in.readObject();
                }
                System.out.println(file.getName() + " loaded successfully");
            } catch (Throwable t) {
                System.out.println("Could not load " + file.getName() + " due to " + t.getClass().getSimpleName());
            }
        }
    }
}