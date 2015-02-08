/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author pepijn
 */
public class DamagedWorldFinder {
    public static void main(String[] args) {
        File dir = new File(args[0]);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".world");
            }
        });
        for (File file: files) {
            try {
                ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
                try {
                    Object world = in.readObject();
                } finally {
                    in.close();
                }
                System.out.println(file.getName() + " loaded successfully");
            } catch (Throwable t) {
                System.out.println("Could not load " + file.getName() + " due to " + t.getClass().getSimpleName());
            }
        }
    }
}