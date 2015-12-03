/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools.scripts;

import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
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
        }
    }

    private void run() {
        jComboBox1.setEnabled(false);
        jButton1.setEnabled(false);
        jTextArea1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
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
                        public void write(char[] cbuf, int off, int len) throws IOException {
                            SwingUtilities.invokeLater(() -> jTextArea2.append(new String(cbuf, off, len)));
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    // End of variables declaration//GEN-END:variables

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final World2 world;
    private final Dimension dimension;
    private final ArrayList<File> recentScriptFiles;
    
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);
}