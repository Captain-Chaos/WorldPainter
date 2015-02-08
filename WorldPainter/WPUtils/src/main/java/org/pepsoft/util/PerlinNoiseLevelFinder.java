/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author pepijn
 */
public class PerlinNoiseLevelFinder {
    public static void main(String[] args) throws InterruptedException {
        new Thread("Thread 1") {
            @Override
            public void run() {
                findLevels(0, 1250);
            }
        }.start();
        new Thread("Thread 2") {
            @Override
            public void run() {
                findLevels(1250, 2500);
            }
        }.start();
        new Thread("Thread 3") {
            @Override
            public void run() {
                findLevels(2500, 3750);
            }
        }.start();
        new Thread("Thread 4") {
            @Override
            public void run() {
                findLevels(3750, 5000);
            }
        }.start();
        new Thread("Thread 5") {
            @Override
            public void run() {
                findLevels(5000, 6250);
            }
        }.start();
        new Thread("Thread 6") {
            @Override
            public void run() {
                findLevels(6250, 7500);
            }
        }.start();
        findLevels(7500, 10001, false);
        latch.await();
        synchronized (levels) {
            System.out.print('{');
            for (int i = 0; i <= 10000; i++) {
                System.out.print(levels[i]);
                System.out.print('f');
                if (i < 10000) {
                    System.out.print(", ");
                }
                if ((i + 1) % 10 == 0) {
                    System.out.println();
                }
            }
            System.out.print('}');
        }
    }
    
    private static void findLevels(int from , int to) {
        findLevels(from, to, true);
    }
    
    private static void findLevels(int from , int to, boolean decrementLatch) {
        PerlinNoise perlinNoise = new PerlinNoise(0);
        for (int i = from; i < to; i++) {
            float target = i / 10000f;
            float level = findLevelForPromillage(perlinNoise, target);
            synchronized (levels) {
                levels[i] = level;
            }
        }
        if (decrementLatch) {
            latch.countDown();
        }
    }

    private static float findLevelForPromillage(PerlinNoise perlinNoise, float target) {
        float level = 0f, step = 0.25f, previousLevel = 0f;
        float promillage = 0f;
        for (int i = 0; i < 100; i++) {
            int hits = numberOfHits(perlinNoise, level, 10000000);
            promillage = hits / 10000000.0f;
            if (promillage > target) {
                level += step;
                if (level == previousLevel) {
                    System.out.println("Promillage at level " + level + ": " + (promillage * 1000));
                    return level;
                }
                previousLevel = level;
                step /= 2;
            } else if (promillage < target) {
                level -= step;
                if (level == previousLevel) {
                    System.out.println("Promillage at level " + level + ": " + (promillage * 1000));
                    return level;
                }
                previousLevel = level;
                step /= 2;
            } else {
                System.out.println("Promillage at level " + level + ": " + (promillage * 1000));
                return level;
            }
        }
        System.out.println("Promillage at level " + level + ": " + (promillage * 1000));
        return level;
    }

    private static int numberOfHits(PerlinNoise perlinNoise, float level, int count) {
        int hits = 0;
        Random random = new Random(0);
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble() * 256;
            double y = random.nextDouble() * 256;
            double z = random.nextDouble() * 256;
            float noise = perlinNoise.getPerlinNoise(x, y, z);
            if (noise >= level) {
                hits++;
            }
        }
        return hits;
    }
    
    private static final float levels[] = new float[10001];
    private static final CountDownLatch latch = new CountDownLatch(6);
}