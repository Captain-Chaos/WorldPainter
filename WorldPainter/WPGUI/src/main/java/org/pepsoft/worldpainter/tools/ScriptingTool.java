/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.tools.scripts.ScriptingContext;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

/**
 *
 * @author SchmitzP
 */
public class ScriptingTool {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Initialise logging
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s%6$s%n");
        Logger.getLogger("").setLevel(Level.WARNING);
        
        // Load script
        File scriptFile = new File(args[0]);
        if (! scriptFile.isFile()) {
            logger.severe(args[0] + " does not exist or is not a regular file");
            System.exit(1);
        }
        String scriptFileName = scriptFile.getName();
        int p = scriptFileName.lastIndexOf('.');
        if (p == -1) {
            logger.severe("Script file name " + scriptFileName + " has no extension");
            System.exit(1);
        }
        String extension = scriptFileName.substring(p + 1);
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension(extension);
        if (scriptEngine == null) {
            logger.severe("Script file language " + extension + " not supported");
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
        if (! pluginsDir.isDirectory()) {
            pluginsDir.mkdir();
        }
        if (trustedCert != null) {
            PluginManager.loadPlugins(pluginsDir, trustedCert.getPublicKey());
        } else {
            logger.severe("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());
        
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