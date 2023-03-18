/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools.scripts;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldPainterDialog;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.FileFilter;
import org.pepsoft.worldpainter.util.FileUtils;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static org.pepsoft.util.AwtUtils.doLaterOnEventThread;
import static org.pepsoft.util.swing.MessageUtils.beepAndShowError;
import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_SCRIPT_FILENAME;
import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_SCRIPT_NAME;
import static org.pepsoft.worldpainter.ExceptionHandler.doWithoutExceptionReporting;

/**
 *
 * @author Pepijn Schmitz
 */
public class ScriptRunner extends WorldPainterDialog {
    /**
     * Creates new form ScriptRunner
     */
    public ScriptRunner(Window parent, World2 world, Dimension dimension, Collection<UndoManager> undoManagers) {
        super(parent);
        this.world = world;
        this.dimension = dimension;
        this.undoManagers = undoManagers;

        initComponents();
        
        Configuration config = Configuration.getInstance();
        recentScriptFiles = (config.getRecentScriptFiles() != null) ? new ArrayList<>(config.getRecentScriptFiles()) : new ArrayList<>();
        recentScriptFiles.removeIf(file -> !file.isFile());
        jComboBox1.setModel(new DefaultComboBoxModel<>(recentScriptFiles.toArray(new File[recentScriptFiles.size()])));
        if ((jComboBox1.getSelectedItem() != null) && ((File) jComboBox1.getSelectedItem()).isFile()) {
            setupScript((File) jComboBox1.getSelectedItem());
        }
        setControlStates();
        
        getRootPane().setDefaultButton(jButton2);
        scaleToUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void setControlStates() {
        jButton2.setEnabled((jComboBox1.getSelectedItem() != null)
                && ((File) jComboBox1.getSelectedItem()).isFile()
                && ((scriptDescriptor == null) || scriptDescriptor.isValid()));
    }
    
    private void selectFile() {
        Set<String> extensions = new HashSet<>();
        SCRIPT_ENGINE_MANAGER.getEngineFactories().forEach(factory -> extensions.addAll(factory.getExtensions()));
        File script = FileUtils.selectFileForOpen(this, "Select Script", (File) jComboBox1.getSelectedItem(), new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String name = f.getName();
                    int p = name.lastIndexOf('.');
                    if (p >= 0) {
                        return extensions.contains(name.substring(p + 1));
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public String getDescription() {
                StringBuilder sb = new StringBuilder();
                sb.append("Script files (");
                sb.append(extensions.stream().map(extension -> "*." + extension).collect(joining(", ")));
                sb.append(')');
                return sb.toString();
            }

            @Override
            public String getExtensions() {
                return String.join(";", extensions);
            }
        });
        if ((script != null) && script.isFile()) {
            recentScriptFiles.remove(script);
            recentScriptFiles.add(0, script);
            jComboBox1.setModel(new DefaultComboBoxModel<>(recentScriptFiles.toArray(new File[recentScriptFiles.size()])));
            jComboBox1.setSelectedItem(script);
            setupScript(script);
            setControlStates();
        }
    }

    private void setupScript(File script) {
        scriptDescriptor = analyseScript(script);
        
        // Remove any previously added fields:
        while (panelDescriptor.getComponentCount() > 2) {
            panelDescriptor.remove(2);
        }
        
        // If there is a descriptor, use it to add fields for the parameters:
        if (scriptDescriptor != null) {
            if (scriptDescriptor.name != null) {
                labelName.setText(scriptDescriptor.name);
            } else {
                labelName.setText(script.getName());
            }
            if (scriptDescriptor.description != null) {
                addRegular(panelDescriptor, new JLabel("Description:"));
                JTextArea textArea = new JTextArea(scriptDescriptor.description);
                textArea.setEditable(false);
                textArea.setOpaque(false);
                addlastOnLine(panelDescriptor, textArea);
            }
            boolean allFieldsOptional = true;
            for (ParameterDescriptor paramDescriptor: scriptDescriptor.parameterDescriptors) {
                boolean showAsMandatory = (! paramDescriptor.optional) && ((paramDescriptor instanceof FileParameterDescriptor) || (paramDescriptor instanceof FloatParameterDescriptor) || (paramDescriptor instanceof StringParameterDescriptor));
                JLabel label = new JLabel(((paramDescriptor.displayName != null) ? paramDescriptor.displayName : paramDescriptor.name) + (showAsMandatory ? "*:" : ":"));
                allFieldsOptional &= ! showAsMandatory;
                JComponent editor = paramDescriptor.getEditor();
                label.setLabelFor(editor);
                if (paramDescriptor.description != null) {
                    label.setToolTipText(paramDescriptor.description);
                }
                addRegular(panelDescriptor, label);
                if (paramDescriptor.description != null) {
                    editor.setToolTipText(paramDescriptor.description);
                }
                addlastOnLine(panelDescriptor, editor);
                paramDescriptor.setChangeListener(e -> setControlStates());
            }
            if (! allFieldsOptional) {
                addlastOnLine(panelDescriptor, new JLabel("* mandatory parameter"));
            }

            jLabel2.setVisible(! scriptDescriptor.hideCmdLineParams);
            jLabel3.setVisible(! scriptDescriptor.hideCmdLineParams);
            jScrollPane1.setVisible(! scriptDescriptor.hideCmdLineParams);
        } else {
            labelName.setText(script.getName());
            jLabel2.setVisible(true);
            jLabel3.setVisible(true);
            jScrollPane1.setVisible(true);
        }

        pack();
    }
    
    private void addRegular(JPanel panel, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(3, 0, 3, 3);
        panel.add(component, constraints);
    }

    private void addlastOnLine(JPanel panel, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(3, 3, 3, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(component, constraints);
    }
    
    private ScriptDescriptor analyseScript(File script) {
        if (! script.isFile()) {
            return null;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        try (BufferedReader in = new BufferedReader(new FileReader(script))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                } else if (line.startsWith("#") || line.startsWith("//")) {
                    if (((line.startsWith("#")) && (line.substring(1).trim().startsWith("--")))
                        || ((line.startsWith("//")) && (line.substring(2).trim().startsWith("--")))) {
                        // Script descriptor comment
                        continue;
                    } else {
                        Matcher matcher = DESCRIPTOR_PATTERN.matcher(line);
                        if (matcher.find()) {
                            properties.put(matcher.group(1), matcher.group(2));
                        }
                    }
                } else {
                    // Stop after the first non-comment and non-empty line
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading script " + script, e);
        }
        if (properties.isEmpty()) {
            return null;
        } else {
            ScriptDescriptor descriptor = new ScriptDescriptor();
            Map<String, ParameterDescriptor> paramMap = new LinkedHashMap<>();
            properties.forEach((key, value) -> {
                if (key.equals("name")) {
                    descriptor.name = value.trim();
                } else if (key.equals("description")) {
                    descriptor.description = value.trim().replace("\\n", "\n");
                } else if (key.startsWith("param.")) {
                    String[] parts = key.split("\\.");
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid key \"" + key + "\" in script descriptor");
                    }
                    ParameterDescriptor paramDescriptor = paramMap.get(parts[1]);
                    switch (parts[2]) {
                        case "type":
                            if (paramDescriptor == null) {
                                switch (value.toLowerCase().trim()) {
                                    case "string":
                                        paramDescriptor = new StringParameterDescriptor();
                                        break;
                                    case "integer":
                                        paramDescriptor = new IntegerParameterDescriptor();
                                        break;
                                    case "percentage":
                                        paramDescriptor = new PercentageParameterDescriptor();
                                        break;
                                    case "float":
                                        paramDescriptor = new FloatParameterDescriptor();
                                        break;
                                    case "file":
                                        paramDescriptor = new FileParameterDescriptor();
                                        break;
                                    case "boolean":
                                        paramDescriptor = new BooleanParameterDescriptor();
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Invalid type \"" + value + "\" specified for parameter " + parts[1]);
                                }
                                paramDescriptor.name = parts[1];
                                paramMap.put(parts[1], paramDescriptor);
                                descriptor.parameterDescriptors.add(paramDescriptor);
                            } else {
                                throw new IllegalArgumentException("Type specified more than once for parameter " + parts[1]);
                            }
                            break;
                        case "description":
                            paramDescriptor.description = value.replace("\\n", "\n");
                            break;
                        case "optional":
                            paramDescriptor.optional = value.trim().isEmpty() || Boolean.parseBoolean(value.toLowerCase().trim());
                            break;
                        case "default":
                            paramDescriptor.defaultValue = paramDescriptor.toObject(value.trim());
                            break;
                        case "displayName":
                            paramDescriptor.displayName = value.trim();
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid key \"" + key + "\" in script descriptor");
                    }
                } else if (key.equals("hideCmdLineParams")) {
                    descriptor.hideCmdLineParams = "true".equalsIgnoreCase(value.trim());
                } else {
                    throw new IllegalArgumentException("Invalid key \"" + key + "\" in script descriptor");
                }
            });
            return descriptor;
        }
    }

    private void run() {
        jComboBox1.setEnabled(false);
        jButton1.setEnabled(false);
        jTextArea1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
        jTextArea2.setText(null);
        final File scriptFile = (File) jComboBox1.getSelectedItem();
        final String scriptFilePath = scriptFile.getParentFile().getAbsolutePath();
        final String scriptFileName = scriptFile.getName(), scriptName;
        final Map<String, Object> params;
        if (scriptDescriptor != null) {
            params = scriptDescriptor.getValues();
            if (scriptDescriptor.name != null) {
                scriptName = scriptDescriptor.name;
            } else {
                scriptName = scriptFileName;
            }
        } else {
            params = null;
            scriptName = scriptFileName;
        }
        new Thread(scriptFileName) {
            @Override
            public void run() {
                try {
                    final Configuration config = Configuration.getInstance();
                    final int p = scriptFileName.lastIndexOf('.');
                    final String extension = scriptFileName.substring(p + 1);
                    ScriptEngine scriptEngine;
                    synchronized (SCRIPT_ENGINES) {
                        if (SCRIPT_ENGINES.containsKey(extension)) {
                            scriptEngine = SCRIPT_ENGINES.get(extension);
                        } else {
                            scriptEngine = SCRIPT_ENGINE_MANAGER.getEngineByExtension(extension);
                            if (scriptEngine == null) {
                                logger.error("No script engine found for extension \"" + extension + "\"");
                                doLaterOnEventThread(() -> beepAndShowError(ScriptRunner.this, "No script engine installed for extension \"" + extension + "\"", "Error"));
                                return;
                            }
                            SCRIPT_ENGINES.put(extension, scriptEngine);
                            logger.info("Using script engine {} version {} for scripts of type {}", scriptEngine.getFactory().getEngineName(), scriptEngine.getFactory().getEngineVersion(), extension);
                        }
                    }

                    scriptEngine.put(ScriptEngine.FILENAME, scriptFileName);
                    config.setRecentScriptFiles(new ArrayList<>(recentScriptFiles));

                    // Initialise script context
                    final Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
                    final ScriptingContext context = new ScriptingContext(false);
                    bindings.put("wp", context);
                    final String[] parameters = jTextArea1.getText().isEmpty() ? new String[0] : jTextArea1.getText().split("\\R");
                    bindings.put("argc", parameters.length + 1);
                    final String[] argv = new String[parameters.length + 1];
                    argv[0] = scriptFileName;
                    System.arraycopy(parameters, 0, argv, 1, parameters.length);
                    bindings.put("argv", argv);
                    bindings.put("arguments", parameters);
                    if (params != null) {
                        bindings.put("params", params);
                    }
                    if (world != null) {
                        bindings.put("world", world);
                    }
                    if (dimension != null) {
                        bindings.put("dimension", dimension);
                    }
                    final Map<String, Layer.DataSize> dataSizes = new HashMap<>();
                    for (Layer.DataSize dataSize: Layer.DataSize.values()) {
                        dataSizes.put(dataSize.name(), dataSize);
                    }
                    bindings.put("DataSize", dataSizes);
                    bindings.put("scriptDir", scriptFilePath);

                    // Capture output
                    final List<String> textQueue = new LinkedList<>();
                    final boolean[] textUpdateScheduled = new boolean[] {false};
                    Writer writer = new Writer() {
                        @Override
                        public void write(char @NotNull [] cbuf, int off, int len) {
                            synchronized (textQueue) {
                                textQueue.add(new String(cbuf, off, len));
                                if (! textUpdateScheduled[0]) {
                                    doLaterOnEventThread(() -> {
                                        synchronized (textQueue) {
                                            // Join the fragments first so that
                                            // only one string need be appended
                                            // to the text area's document
                                            jTextArea2.append(String.join("", textQueue));
                                            textQueue.clear();
                                            textUpdateScheduled[0] = false;
                                        }
                                    });
                                    textUpdateScheduled[0] = true;
                                }
                            }
                        }

                        @Override public void flush() {}
                        @Override public void close() {}
                    };
                    scriptEngine.getContext().setWriter(writer);
                    scriptEngine.getContext().setErrorWriter(writer);

                    // Log the execution
                    config.logEvent(new EventVO("script.execute").addTimestamp()
                            .setAttribute(ATTRIBUTE_KEY_SCRIPT_NAME, scriptName)
                            .setAttribute(ATTRIBUTE_KEY_SCRIPT_FILENAME, scriptFileName));
                    logger.info("Executing script {} from file {}", scriptName, scriptFileName);
                    // TODO add an event to the world history (after it has succeeded, and first make that history
                    //  undoable)

                    // Execute script
                    if (dimension != null) {
                        dimension.setEventsInhibited(true);
                    }
                    try {
                        // Load the script
                        final String script = org.pepsoft.util.FileUtils.load(scriptFile, Charset.defaultCharset());

                        // Compile the script, if the engine supports it, and run it
                        final long start;
                        if (scriptEngine instanceof Compilable) {
                            final CompiledScript compiledScript;
                            if (COMPILED_SCRIPTS.containsKey(script)) {
                                compiledScript = COMPILED_SCRIPTS.get(script);
                            } else {
                                logger.info("Compiling script {}", scriptName);
                                compiledScript = ((Compilable) scriptEngine).compile(new FileReader(scriptFile));
                                COMPILED_SCRIPTS.put(script, compiledScript);
                            }
                            start = System.currentTimeMillis();
                            compiledScript.eval();
                        } else {
                            start = System.currentTimeMillis();
                            scriptEngine.eval(script);
                        }
                        logger.debug("Running script {} took {} ms", scriptName, System.currentTimeMillis() - start);

                        // Check that go() was invoked on the last operation:
                        context.checkGoCalled(null);
                    } catch (RuntimeException e) {
                        logger.error(e.getClass().getSimpleName() + " occurred while executing " + scriptFileName, e);
                        doLaterOnEventThread(() -> beepAndShowError(ScriptRunner.this, e.getClass().getSimpleName() + " occurred (message: " + e.getMessage() + ")", "Error"));
                    } catch (javax.script.ScriptException e) {
                        logger.error("ScriptException occurred while executing " + scriptFileName, e);
                        final StringBuilder sb = new StringBuilder();
                        sb.append(e.getMessage());
                        if (e.getLineNumber() != -1) {
                            sb.append(" (");
                            sb.append(e.getLineNumber());
                            if (e.getColumnNumber() != -1) {
                                sb.append(':');
                                sb.append(e.getColumnNumber());
                            }
                            sb.append(')');
                        }
                        doLaterOnEventThread(() -> beepAndShowError(ScriptRunner.this, sb.toString(), "Error"));
                    } catch (IOException e) {
                        logger.error("I/O error occurred while executing " + scriptFileName, e);
                        doLaterOnEventThread(() -> beepAndShowError(ScriptRunner.this, "I/O error while executing " + scriptFileName, "Error"));
                    } finally {
                        if (dimension != null) {
                            dimension.setEventsInhibited(false);
                        }
                        if (undoManagers != null) {
                            undoManagers.forEach(UndoManager::armSavePoint);
                        }
                    }
                } finally {
                    doLaterOnEventThread(() -> {
                        jComboBox1.setEnabled(true);
                        jButton1.setEnabled(true);
                        jTextArea1.setEnabled(true);
                        jButton2.setEnabled(true);
                        jButton3.setText("Close");
                        jButton3.setEnabled(true);
                    });
                }
            }
        }.start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jButton3 = new javax.swing.JButton();
        panelDescriptor = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        labelName = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Run Script");

        jLabel1.setText("Script:");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Parameters:");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel3.setText("(one per line)");

        jLabel4.setText("Output:");

        jButton2.setText("Run");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextArea2.setEditable(false);
        jTextArea2.setColumns(20);
        jTextArea2.setLineWrap(true);
        jTextArea2.setRows(5);
        jTextArea2.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jTextArea2);

        jButton3.setText("Cancel");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        panelDescriptor.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 0, 2);
        panelDescriptor.add(jLabel5, gridBagConstraints);

        labelName.setText("jLabel6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        panelDescriptor.add(labelName, gridBagConstraints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(panelDescriptor, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addComponent(panelDescriptor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        selectFile();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        run();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        setupScript((File) jComboBox1.getSelectedItem());
        setControlStates();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        cancel();
    }//GEN-LAST:event_jButton3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JLabel labelName;
    private javax.swing.JPanel panelDescriptor;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final Dimension dimension;
    private final ArrayList<File> recentScriptFiles;
    private final Collection<UndoManager> undoManagers;
    private ScriptDescriptor scriptDescriptor;

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile("script\\.([.a-zA-Z_0-9]+)=(.+)$");
    private static final Map<String, ScriptEngine> SCRIPT_ENGINES = new HashMap<>();
    private static final Map<String, CompiledScript> COMPILED_SCRIPTS = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);

    @SuppressWarnings("Convert2MethodRef") // This is shorter
    static class ScriptDescriptor {
        boolean isValid() {
            return parameterDescriptors.stream().allMatch(p -> p.isEditorValid());
        }

        Map<String, Object> getValues() {
            Map<String, Object> values = new HashMap<>();
            parameterDescriptors.forEach(p -> {
                Object value = p.getValue();
                if (value != null) {
                    values.put(p.name, value);
                }
            });
            return values;
        }

        String name, description;
        List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        boolean hideCmdLineParams;
    }

    abstract static class ParameterDescriptor<T, E extends JComponent> {
        E getEditor() {
            if (editor == null) {
                editor = createEditor();
            }
            if (defaultValue != null) {
                setValue(defaultValue);
            }
            return editor;
        }

        boolean isEditorValid() {
            return true;
        }

        abstract T getValue();

        abstract void setValue(T value);

        abstract T toObject(String str);

        ChangeListener getChangeListener() {
            return changeListener;
        }

        void setChangeListener(ChangeListener changeListener) {
            this.changeListener = changeListener;
        }

        protected abstract E createEditor();

        protected void notifyChangeListener() {
            if (changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }

        String name, description, displayName;
        boolean optional;
        E editor;
        T defaultValue;

        private ChangeListener changeListener;
    }

    static class StringParameterDescriptor extends ParameterDescriptor<String, JTextField> {
        @Override
        protected JTextField createEditor() {
            JTextField field = new JTextField(defaultValue);
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    notifyChangeListener();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    notifyChangeListener();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    notifyChangeListener();
                }
            });
            return field;
        }

        @Override
        String toObject(String str) {
            return str;
        }

        @Override
        boolean isEditorValid() {
            return optional || (! editor.getText().trim().isEmpty());
        }

        @Override
        String getValue() {
            String text = editor.getText();
            return text.trim().isEmpty() ? null : text.trim();
        }

        @Override
        void setValue(String value) {
            editor.setText(value);
        }
    }

    static class IntegerParameterDescriptor extends ParameterDescriptor<Integer, JSpinner> {
        @Override
        protected JSpinner createEditor() {
            JSpinner spinner = new JSpinner();
            spinner.addChangeListener(e -> notifyChangeListener());
            return spinner;
        }

        @Override
        Integer toObject(String str) {
            return Integer.valueOf(str);
        }

        @Override
        Integer getValue() {
            return (Integer) editor.getValue();
        }

        @Override
        void setValue(Integer value) {
            editor.setValue(value);
        }
    }

    static class PercentageParameterDescriptor extends ParameterDescriptor<Integer, JPanel> {
        @Override
        protected JPanel createEditor() {
            JPanel panel = new JPanel(new GridBagLayout()) {
                @Override
                public int getBaseline(int width, int height) {
                    return getComponent(0).getBaseline(width, height);
                }
            };
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
            JSpinner spinner = new JSpinner(spinnerModel);
            spinner.setToolTipText(description);
            spinner.addChangeListener(e -> notifyChangeListener());
            panel.add(spinner, constraints);
            constraints.weightx = 0.0;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            JLabel label = new JLabel("%");
            label.setToolTipText(description);
            panel.add(label, constraints);
            return panel;
        }

        @Override
        Integer toObject(String str) {
            if (str.endsWith("%")) {
                return Integer.valueOf(str.substring(0, str.length() - 1).trim());
            } else {
                return Integer.valueOf(str.trim());
            }
        }

        @Override
        Integer getValue() {
            return (Integer) ((JSpinner) editor.getComponent(0)).getValue();
        }

        @Override
        void setValue(Integer value) {
            ((JSpinner) editor.getComponent(0)).setValue(value);
        }
    }

    static class FloatParameterDescriptor extends ParameterDescriptor<Float, JFormattedTextField> {
        @Override
        protected JFormattedTextField createEditor() {
            JFormattedTextField field = new JFormattedTextField(NumberFormat.getNumberInstance());
            field.setHorizontalAlignment(SwingConstants.TRAILING);
            field.addPropertyChangeListener("value", e -> notifyChangeListener());
            return field;
        }

        @Override
        Float toObject(String str) {
            return Float.valueOf(str);
        }

        @Override
        boolean isEditorValid() {
            try {
                editor.commitEdit();
                return optional || (editor.getValue() != null);
            } catch (ParseException e) {
                return optional;
            }
        }

        @Override
        Float getValue() {
            Number nr = (Number) editor.getValue();
            return (nr != null) ? nr.floatValue() : null;
        }

        @Override
        void setValue(Float value) {
            editor.setValue(value);
        }
    }

    static class FileParameterDescriptor extends ParameterDescriptor<File, JPanel> {
        @Override
        protected JPanel createEditor() {
            JPanel panel = new JPanel(new GridBagLayout()) {
                @Override
                public int getBaseline(int width, int height) {
                    return getComponent(0).getBaseline(width, height);
                }
            };
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JTextField field = new JTextField();
            field.setToolTipText(description);
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    notifyChangeListener();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    notifyChangeListener();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    notifyChangeListener();
                }
            });
            panel.add(field, constraints);
            constraints.weightx = 0.0;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(0, 3, 0, 0);
            JButton button = new JButton("...");
            button.setToolTipText(description);
            button.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                if (! field.getText().trim().isEmpty()) {
                    fileChooser.setSelectedFile(new File(field.getText().trim()));
                }
                if (doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(panel)) == JFileChooser.APPROVE_OPTION) {
                    field.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            });
            panel.add(button, constraints);
            return panel;
        }

        @Override
        File toObject(String str) {
            return new File(str);
        }

        @Override
        boolean isEditorValid() {
            if (optional) {
                return true;
            } else {
                String text = ((JTextField) editor.getComponent(0)).getText();
                return (! text.trim().isEmpty()) && new File(text.trim()).isFile();
            }
        }

        @Override
        File getValue() {
            String text = ((JTextField) editor.getComponent(0)).getText();
            return text.trim().isEmpty() ? null : new File(text.trim());
        }

        @Override
        void setValue(File value) {
            ((JTextField) editor.getComponent(0)).setText(value.getAbsolutePath());
        }
    }

    static class BooleanParameterDescriptor extends ParameterDescriptor<Boolean, JCheckBox> {
        @Override
        protected JCheckBox createEditor() {
            JCheckBox checkBox = new JCheckBox(" ");
            checkBox.addChangeListener(e -> notifyChangeListener());
            return checkBox;
        }

        @Override
        Boolean toObject(String str) {
            return Boolean.valueOf(str);
        }

        @Override
        Boolean getValue() {
            return editor.isSelected();
        }

        @Override
        void setValue(Boolean value) {
            editor.setSelected(value);
        }
    }
}