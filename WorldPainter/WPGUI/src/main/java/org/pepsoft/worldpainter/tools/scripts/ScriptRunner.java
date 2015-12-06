/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools.scripts;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldPainterDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pepijn Schmitz
 */
public class ScriptRunner extends WorldPainterDialog {
    /**
     * Creates new form ScriptRunner
     */
    public ScriptRunner(Window parent, World2 world, Dimension dimension) {
        super(parent);
        this.world = world;
        this.dimension = dimension;
        
        initComponents();
        
        Configuration config = Configuration.getInstance();
        recentScriptFiles = (config.getRecentScriptFiles() != null) ? new ArrayList(config.getRecentScriptFiles()) : new ArrayList<>();
        jComboBox1.setModel(new DefaultComboBoxModel<>(recentScriptFiles.toArray(new File[recentScriptFiles.size()])));
        if (jComboBox1.getSelectedItem() != null) {
            setupScript((File) jComboBox1.getSelectedItem());
        }
        setControlStates();
        
        getRootPane().setDefaultButton(jButton2);
        setLocationRelativeTo(parent);
    }

    private void setControlStates() {
        jButton2.setEnabled((jComboBox1.getSelectedItem() != null) && ((File) jComboBox1.getSelectedItem()).isFile());
    }
    
    private void selectFile() {
        Set<String> extensions = new HashSet<>();
        scriptEngineManager.getEngineFactories().forEach(factory -> extensions.addAll(factory.getExtensions()));
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
                sb.append(extensions.stream().map(extension -> "*." + extension).collect(Collectors.joining(", ")));
                sb.append(')');
                return sb.toString();
            }
        });
        if ((script != null) && script.isFile()) {
            recentScriptFiles.remove(script);
            recentScriptFiles.add(0, script);
            jComboBox1.setModel(new DefaultComboBoxModel<>(recentScriptFiles.toArray(new File[recentScriptFiles.size()])));
            jComboBox1.setSelectedItem(script);
            setupScript(script);
        }
    }

    private void setupScript(File script) {
        ScriptDescriptor descriptor = analyseScript(script);
        
        // Remove any previously added fields:
        while (panelDescriptor.getComponentCount() > 2) {
            panelDescriptor.remove(2);
        }
        
        // If there is a descriptor, use it to add fields for the parameters:
        if (descriptor != null) {
            if (descriptor.name != null) {
                labelName.setText(script.getName());
            } else {
                labelName.setText(script.getName());
            }
            if (descriptor.description != null) {
                addRegular(panelDescriptor, new JLabel("Description:"));
                addlastOnLine(panelDescriptor, new JTextArea(descriptor.description));
            }
            for (ParameterDescriptor paramDescriptor: descriptor.parameterDescriptors) {
                JLabel label = new JLabel(paramDescriptor.name + ':');
                JTextField field = new JTextField();
                label.setLabelFor(field);
                if (paramDescriptor.description != null) {
                    label.setToolTipText(paramDescriptor.description);
                }
                addRegular(panelDescriptor, label);
                if (paramDescriptor.description != null) {
                    field.setToolTipText(paramDescriptor.description);
                }
                addlastOnLine(panelDescriptor, field);
            }
        } else {
            labelName.setText(script.getName());
        }
    }
    
    private void addRegular(JPanel panel, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(2, 0, 2, 2);
        panel.add(component, constraints);
    }

    private void addlastOnLine(JPanel panel, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(2, 2, 2, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(component, constraints);
    }
    
    private ScriptDescriptor analyseScript(File script) {
        Properties properties = new Properties();
        try (BufferedReader in = new BufferedReader(new FileReader(script))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                } else if (line.startsWith("#") || line.startsWith("//")) {
                    Matcher matcher = DESCRIPTOR_PATTERN.matcher(line);
                    if (matcher.find()) {
                        properties.put(matcher.group(1), matcher.group(2));
                    }
                } else {
                    // Stop after the first non-comment and non-empty line
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading script " + script);
        }
        if (properties.isEmpty()) {
            return null;
        } else {
            ScriptDescriptor descriptor = new ScriptDescriptor();
            Map<String, ParameterDescriptor> paramMap = new HashMap<>();
            properties.forEach((keyObject, valueObject) -> {
                String key = (String) keyObject, value = (String) valueObject;
                if (key.equals("name")) {
                    descriptor.name = value;
                } else if (key.equals("description")) {
                    descriptor.description = value.replace("\\n", "\n");
                } else if (key.startsWith("param.")) {
                    String[] parts = key.split("\\.");
                    if (parts.length != 3) {
                        logger.warn("Skipping invalid key \"" + key + "\" in script descriptor");
                    }
                    ParameterDescriptor paramDescriptor = paramMap.get(parts[1]);
                    if (paramDescriptor == null) {
                        paramDescriptor = new ParameterDescriptor();
                        paramDescriptor.name = parts[1];
                        paramMap.put(parts[1], paramDescriptor);
                        descriptor.parameterDescriptors.add(paramDescriptor);
                    }
                    if (parts[2].equals("type")) {
                        paramDescriptor.type = parts[2];
                    } else if (parts[2].equals("description")) {
                        paramDescriptor.description = parts[2].replace("\\n", "\n");
                    } else {
                        logger.warn("Skipping unknown key \"" + key + "\" in script descriptor");
                    }
                } else {
                    logger.warn("Skipping unknown key \"" + key + "\" in script descriptor");
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
        new Thread() {
            @Override
            public void run() {
                try {
                    Configuration config = Configuration.getInstance();
                    File scriptFile = (File) jComboBox1.getSelectedItem();
                    String scriptFileName = scriptFile.getName();
                    int p = scriptFileName.lastIndexOf('.');
                    String extension = scriptFileName.substring(p + 1);
                    ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension(extension);
                    scriptEngine.put(ScriptEngine.FILENAME, scriptFileName);
                    config.setRecentScriptFiles(new ArrayList<>(recentScriptFiles));

                    // Initialise script context
                    ScriptingContext context = new ScriptingContext();
                    Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
                    bindings.put("wp", context);
                    bindings.put("argc", 1);
                    String[] parameters = jTextArea1.getText().split("$");
                    String[] argv = new String[parameters.length + 1];
                    argv[0] = scriptFileName;
                    System.arraycopy(parameters, 0, argv, 1, parameters.length);
                    bindings.put("argv", argv);
                    bindings.put("arguments", parameters);
                    if (world != null) {
                        bindings.put("world", world);
                    }
                    if (dimension != null) {
                        bindings.put("dimension", dimension);
                    }

                    // Capture output
                    scriptEngine.getContext().setWriter(new Writer() {
                        @Override
                        public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                            String text = new String(cbuf, off, len);
                            SwingUtilities.invokeLater(() -> jTextArea2.append(text));
                        }

                        @Override public void flush() throws IOException {}
                        @Override public void close() throws IOException {}
                    });

                    // Execute script
                    if (dimension != null) {
                        dimension.setEventsInhibited(true);
                    }
                    try {
                        scriptEngine.eval(new FileReader(scriptFile));

                        // Check that go() was invoked on the last operation:
                        context.checkGoCalled(null);
                    } catch (RuntimeException e) {
                        logger.error(e.getClass().getSimpleName() + " occurred while executing " + scriptFileName, e);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(ScriptRunner.this, e.getClass().getSimpleName() + " occurred (message: " + e.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE));
                    } catch (javax.script.ScriptException e) {
                        logger.error("ScriptException occurred while executing " + scriptFileName, e);
                        StringBuilder sb = new StringBuilder();
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
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(ScriptRunner.this, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE));
                    } catch (FileNotFoundException e) {
                        logger.error("FileNotFoundException occurred while executing " + scriptFileName, e);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(ScriptRunner.this, "File not found while executing " + scriptFileName, "Error", JOptionPane.ERROR_MESSAGE));
                    } finally {
                        if (dimension != null) {
                            dimension.setEventsInhibited(false);
                            dimension.rememberChanges();
                        }
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> {
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

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final World2 world;
    private final Dimension dimension;
    private final ArrayList<File> recentScriptFiles;

    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile("script\\.([.a-zA-Z_0-9]+)=(.+)$");
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);

    static class ScriptDescriptor {
        String name, description;
        List<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
    }

    static class ParameterDescriptor {
        String name, type, description;
    }

    enum ParameterType {STRING, INTEGER, PERCENTAGE, FLOAT, FILE}
}