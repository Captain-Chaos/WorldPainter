/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.tools.scripts.ScriptingContext;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.pepsoft.worldpainter.plugins.WPPluginManager.FILENAME;

/**
 *
 * @author SchmitzP
 */
public class ScriptingTool {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Initialise logging
        LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(logContext);
            logContext.reset();
            configurator.doConfigure(ClassLoader.getSystemResourceAsStream("logback-scriptingtool.xml"));
        } catch (JoranException e) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(logContext);

        System.err.println("WorldPainter scripting host version " + Version.VERSION + ".\n" +
                "Copyright 2011-2019 pepsoft.org, The Netherlands.\n" +
                "This is free software distributed under the terms of the GPL, version 3, a copy\n" +
                "of which you can find in the installation directory.\n");

        // Check arguments
        if (args.length < 1) {
            System.err.println("Usage:\n" +
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

        // Load the default platform descriptors so that they don't get blocked
        // by older versions of them which might be contained in the
        // configuration. Do this by loading and initialising (but not
        // instantiating) the DefaultPlugin class
        try {
            Class.forName("org.pepsoft.worldpainter.DefaultPlugin");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

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
            logger.error("Certificate exception while loading trusted root certificate", e);
        }
        
        // Load the plugins
        if (trustedCert != null) {
            File pluginsDir = new File(Configuration.getConfigDir(), "plugins");
            if (pluginsDir.isDirectory()) {
                PluginManager.loadPlugins(pluginsDir, trustedCert.getPublicKey(), FILENAME);
            }
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());

        // Load all the platform descriptors to ensure that when worlds
        // containing older versions of them are loaded later they are replaced
        // with the current versions, rather than the other way around
        for (Platform platform : PlatformManager.getInstance().getAllPlatforms()) {
            logger.info("Available platform: {}", platform.displayName);
        }

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

        // Parse arguments
        List<String> argList = new ArrayList<>();
        Map<String, String> paramMap = new HashMap<>();
        for (String arg: args) {
            if (arg.startsWith("--") && (arg.length() > 2) && (arg.charAt(2) != '-')) {
                p = arg.indexOf('=');
                if (p != -1) {
                    String key = arg.substring(2, p);
                    String value = arg.substring(p + 1);
                    paramMap.put(key, value);
                } else {
                    paramMap.put(arg.substring(2), "true");
                }
            } else if (arg.startsWith("-") && (arg.length() > 1) && (arg.charAt(1) != '-')) {
                for (int i = 1; i < arg.length(); i++) {
                    paramMap.put(arg.substring(i, i + 1), "true");
                }
            } else {
                argList.add(arg);
            }
        }

        // Initialise script context
        ScriptingContext context = new ScriptingContext(true);
        Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("wp", context);
        String[] argArray = argList.toArray(new String[argList.size()]);
        bindings.put("argc", argArray.length);
        bindings.put("argv", argArray);
        String[] scriptArgs = new String[argArray.length - 1];
        System.arraycopy(argArray, 1, scriptArgs, 0, scriptArgs.length);
        bindings.put("arguments", scriptArgs);
        bindings.put("params", paramMap);
        
        // Execute script
        try {
            scriptEngine.eval(new FileReader(scriptFile));

            // Check that go() was invoked on the last operation:
            context.checkGoCalled(null);
        } catch (RuntimeException e) {
            logger.error(e.getClass().getSimpleName() + " occurred while executing " + scriptFileName, e);
            System.exit(2);
        } catch (ScriptException e) {
            logger.error("ScriptException occurred while executing " + scriptFileName, e);
            System.exit(2);
        }
    }
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScriptingTool.class);
}