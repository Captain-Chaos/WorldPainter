/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 *
 * @author pepijn
 */
public class ProcessUtils {
    public static void runInBackground(String... args) {
        try {
            Process process = Runtime.getRuntime().exec(args);
            StreamCopier stdoutCopier = new StreamCopier(args[0] + " stdout", process.getInputStream(), System.out);
            StreamCopier stderrCopier = new StreamCopier(args[0] + " stderr", process.getErrorStream(), System.err);
            stdoutCopier.start();
            stderrCopier.start();
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to execute " + Arrays.asList(args), e);
        }
    }
    
    public static int runAndWait(String... args) {
        try {
            Process process = Runtime.getRuntime().exec(args);
            StreamCopier stdoutCopier = new StreamCopier(args[0] + " stdout", process.getInputStream(), System.out);
            StreamCopier stderrCopier = new StreamCopier(args[0] + " stderr", process.getErrorStream(), System.err);
            stdoutCopier.start();
            stderrCopier.start();
            return process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to execute " + Arrays.asList(args), e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted while waiting for " + Arrays.asList(args) + " to finish", e);
        }
    }
    
    static class StreamCopier extends Thread {
        StreamCopier(String name, InputStream in, OutputStream out) {
            super(name);
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[32768];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private final InputStream in;
        private final OutputStream out;
    }
}