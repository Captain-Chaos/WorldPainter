/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.tools.scripts.ScriptingContext;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author SchmitzP
 */
public class ScriptingTool {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Initialise logging
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s%6$s%n");
        Logger.getLogger("").setLevel(Level.WARNING);

        System.err.println("WorldPainter scripting host version " + Version.VERSION + ".\n" +
                "Copyright 2011-2015 pepsoft.org, The Netherlands.\n" +
                "This is free software distributed under the terms of the GPL, version 3, a copy\n" +
                "of which you can find in the installation directory.\n");

        // Check arguments
        if (args.length < 1) {
            System.out.println("Usage:\n" +
                    "\n" +
                    "    wpscript <scriptfile> [<scriptarg> ...]\n" +
                    "\n" +
                    "Where <scriptfile> is the filename, including extension, of the script to\n" +
                    "execute, and [<scriptarg> ...] an optional list of one or more arguments for\n" +
                    "the script, which will be available to the script in the arguments (from index\n" +
                    "0) or argv (from index 1) array.");
            System.exit(1);
        }

        // Load script
        File scriptFile = new File(args[0]);
        if (! scriptFile.isFile()) {
            System.err.println(args[0] + " does not exist or is not a regular file");
            System.exit(1);
        }
        String scriptFileName = scriptFile.getName();
        int p = scriptFileName.lastIndexOf('.');
        if (p == -1) {
            System.err.println("Script file name " + scriptFileName + " has no extension");
            System.exit(1);
        }
        String extension = scriptFileName.substring(p + 1);
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension(extension);
        if (scriptEngine == null) {
            System.err.println("Script file language " + extension + " not supported");
            System.exit(1);
        }
        scriptEngine.put(ScriptEngine.FILENAME, scriptFileName);
        
        // Initialise WorldPainter configuration
        Configuration config = Configuration.load();
        if (config == null) {
            logger.info("Creating new configuration");
            config = new Configuration();
        }
        Configuration.setInstance(config);
        
        // Load trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(ClassLoader.getSystemResourceAsStream("wproot.pem"));
        } catch (CertificateException e) {
            logger.log(Level.SEVERE, "Certificate exception while loading trusted root certificate", e);
        }
        
        // Load the plugins
        File pluginsDir = new File(Configuration.getConfigDir(), "plugins");
        if (pluginsDir.isDirectory()) {
            if (trustedCert != null) {
                PluginManager.loadPlugins(pluginsDir, trustedCert.getPublicKey());
            } else {
                logger.severe("Trusted root certificate not available; not loading plugins");
            }
        }
        WPPluginManager.initialise(config.getUuid());

        if (args.length > 1) {
            System.err.print("Executing script \"" + scriptFileName + "\" with arguments ");
            for (int i = 1; i < args.length; i++) {
                if (i > 1) {
                    System.err.print(", ");
                }
                System.err.print("\"" + args[i] + "\"");
            }
            System.err.println("\n");
        } else {
            System.err.println("Executing script \"" + scriptFileName + "\" with no arguments.\n");
        }

        // Initialise script context
        ScriptingContext context = new ScriptingContext();
        Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("wp", context);
        bindings.put("argc", args.length);
        bindings.put("argv", args);
        String[] scriptArgs = new String[args.length - 1];
        System.arraycopy(args, 1, scriptArgs, 0, scriptArgs.length);
        bindings.put("arguments", scriptArgs);
        
        // Execute script
        try {
            scriptEngine.eval(new FileReader(scriptFile));

            // Check that go() was invoked on the last operation:
            context.checkGoCalled(null);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, e.getClass().getSimpleName() + " occurred while executing " + scriptFileName, e);
            System.exit(2);
        } catch (ScriptException e) {
            logger.log(Level.SEVERE, "ScriptException occurred while executing " + scriptFileName, e);
            System.exit(2);
        }
    }
    
    private static final Logger logger = Logger.getLogger(ScriptingTool.class.getName());
}