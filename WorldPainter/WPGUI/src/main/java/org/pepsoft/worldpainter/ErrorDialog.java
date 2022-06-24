/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ErrorDialog.java
 *
 * Created on Apr 17, 2011, 8:22:55 PM
 */

package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;
import org.pepsoft.worldpainter.vo.EventVO;
import org.pepsoft.worldpainter.vo.ExceptionVO;
import org.pepsoft.worldpainter.vo.UsageVO;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.pepsoft.util.AwtUtils.doOnEventThread;
import static org.pepsoft.util.GUIUtils.scaleToUI;
import static org.pepsoft.util.mdc.MDCUtils.gatherMdcContext;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class ErrorDialog extends javax.swing.JDialog {
    /** Creates new form ErrorDialog */
    public ErrorDialog(Dialog parent) {
        super(parent, true);
        init(parent);
    }

    /** Creates new form ErrorDialog */
    public ErrorDialog(Frame parent) {
        super(parent, true);
        init(parent);
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend") // Readability
    public void setException(Throwable exception) {
        UUID uuid = UUID.randomUUID();
        logger.error("[" + uuid + "] " + exception.getClass().getSimpleName() + ": " + exception.getMessage(), exception);

        event = new EventVO(EVENT_KEY_EXCEPTION);
        event.addTimestamp();
        event.setAttribute(ATTRIBUTE_KEY_EXCEPTION, new ExceptionVO(exception));
        event.setAttribute(ATTRIBUTE_KEY_UUID, uuid.toString());

        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        Configuration config = Configuration.getInstance();
        if (rootCause instanceof OutOfMemoryError) {
            setTitle("Out of Memory");
            jTextArea1.setText("Not enough memory available for that operation!\n\n"
                + "WorldPainter is already using the recommended maximum\n"
                + "amount of memory, so it is not recommended to give it\n"
                + "more. To be able to perform the operation you should\n"
                + "install more memory (and reinstall WorldPainter).");
            jButton1.setEnabled(false);
            jButton1.setToolTipText("Not necessary to send details of out of memory errors");
            jButton3.setEnabled(false);
        } else {
            String message = rootCause.getMessage();
            if ((message != null) && (message.length() > 250)) {
                message = message.substring(0, 247) + "...";
            }
            String requestedActionLine;
            if (Main.privateContext != null) {
                // We can submit the error
                if ((config != null) && TRUE.equals(config.getPingAllowed())) {
                    // Automatic submission is allowed; submit it automatically
                    mode = Mode.SEND_AUTOMATICALLY;
                    requestedActionLine = "The details of this error are being automatically submitted to the creator of this program.\n\n";
                } else {
                    mode = Mode.SEND_MANUALLY;
                    requestedActionLine = "Please help debug the problem by using the Send Report button below to send the details of this error to the creator of this program.\n\n";
                }
            } else {
                jButton1.setText("Email Details...");
                if ((! Desktop.isDesktopSupported()) || (! Desktop.getDesktop().isSupported(Desktop.Action.MAIL))) {
                    jButton1.setToolTipText("Emailing not supported on this system; please use the \"copy to clipboard\" button and mail the details to worldpainter@pepsoft.org.");
                } else {
                    jButton1.setEnabled(true);
                }
                mode = Mode.REPORTING_DISABLED;
                requestedActionLine = "Please help debug the problem by using the button below to email the details of this error to the creator of this program.\n\n";
            }
            String text = "An unexpected error has occurred.\n\n"
                + "Type: " + rootCause.getClass().getName() + "\n"
                + "Message: " + message + "\n\n"
                + requestedActionLine
                + "The program may now be in an unstable state. It is recommended to restart it as soon as possible.";
            jTextArea1.setText(text);
        }
        pack();

        StringBuilder sb = new StringBuilder();
        String eol = System.getProperty("line.separator");
        sb.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append(eol);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
            sb.append("\tat " + stackTrace[i].getClassName() + '.' + stackTrace[i].getMethodName() + '(' + stackTrace[i].getFileName() + ':' + stackTrace[i].getLineNumber() + ')' + eol);
        }
        sb.append(eol);
        if (rootCause != exception) {
            sb.append("Root cause:" + eol);
            sb.append(rootCause.getClass().getName() + ": " + rootCause.getMessage() + eol);
            stackTrace = rootCause.getStackTrace();
            for (int i = 0; i < Math.min(stackTrace.length, 5); i++) {
                sb.append("\tat " + stackTrace[i].getClassName() + '.' + stackTrace[i].getMethodName() + '(' + stackTrace[i].getFileName() + ':' + stackTrace[i].getLineNumber() + ')' + eol);
            }
            sb.append(eol);
        }

        Map<String, String> mdcContextMap = gatherMdcContext(exception);
        if (! mdcContextMap.isEmpty()) {
            sb.append("Diagnostic context:" + eol);
            mdcContextMap.forEach((key, value) -> sb.append("\t" + key + ": " + value + eol));
            sb.append(eol);

            mdcContextMap.forEach((key, value) -> event.setAttribute(new AttributeKeyVO<>(ATTRIBUTE_KEY_MDC_ENTRY + '.' + key), value));
        }

        sb.append("WorldPainter version: " + Version.VERSION + " (" + Version.BUILD + ")" + eol);
        event.setAttribute(ATTRIBUTE_KEY_VERSION, Version.VERSION);
        event.setAttribute(ATTRIBUTE_KEY_BUILD, Version.BUILD);
        sb.append(eol);
        for (String propertyName: SYSTEM_PROPERTIES) {
            sb.append(propertyName + ": " + System.getProperty(propertyName) + eol);
            event.setAttribute(new AttributeKeyVO<>(ATTRIBUTE_KEY_SYSTEM_PROPERTY + '.' + propertyName), System.getProperty(propertyName));
        }
        sb.append(eol);
        Runtime runtime = Runtime.getRuntime();
        sb.append("Free memory: " + runtime.freeMemory() + " bytes" + eol);
        sb.append("Total memory size: " + runtime.totalMemory() + " bytes" + eol);
        sb.append("Max memory size: " + runtime.maxMemory() + " bytes" + eol);
        event.setAttribute(ATTRIBUTE_KEY_FREE_MEMORY, runtime.freeMemory());
        event.setAttribute(ATTRIBUTE_KEY_TOTAL_MEMORY, runtime.totalMemory());
        event.setAttribute(ATTRIBUTE_KEY_MAX_MEMORY, runtime.maxMemory());

        // The app may be in an unstable state, so if an exception occurs while
        // interrogating it, swallow it to prevent endless loops
        try {
            App app = App.getInstanceIfExists();
            World2 world = (app != null) ? app.getWorld() : null;
            Dimension dimension = (app != null) ? app.getDimension() : null;
            if ((world != null) && (dimension != null)) {
                sb.append(eol);
                sb.append("World name: " + world.getName() + eol);
                sb.append("Platform: " + world.getPlatform().displayName + " (" + world.getPlatform().id + ')' + eol);
                sb.append("Seed: " + dimension.getSeed() + eol);
                sb.append("Bounds: " + dimension.getLowestX() + ", " + dimension.getLowestY() + " => " + dimension.getHighestX() + ", " + dimension.getHighestY() + eol);
                sb.append("Height: " + world.getMaxHeight() + eol);
                sb.append("Number of tiles: " + dimension.getTiles().size() + eol);
                Set<Layer> layers = new HashSet<>();
                for (Tile tile : dimension.getTiles()) {
                    layers.addAll(tile.getLayers());
                }
                sb.append("Layers in use: ");
                boolean first = true;
                for (Layer layer : layers) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(layer.getName());
                }
                sb.append(eol);
                sb.append("Border: " + dimension.getBorder() + " @ " + dimension.getBorderLevel() + eol);
                sb.append("Sub surface material: " + dimension.getSubsurfaceMaterial() + eol);
                TileFactory tileFactory = dimension.getTileFactory();
                if (tileFactory instanceof HeightMapTileFactory) {
                    HeightMapTileFactory heightMapTileFactory = (HeightMapTileFactory) tileFactory;
                    sb.append("Water height: " + heightMapTileFactory.getWaterHeight() + eol);
                }
                if (world.getImportedFrom() != null) {
                    sb.append("World imported from " + world.getImportedFrom() + eol);
                }
                if (!world.isAllowMerging()) {
                    sb.append("World created in old coordinate system" + eol);
                }
            }
            if (app != null) {
                sb.append(eol);
                sb.append("Operation: " + app.getActiveOperation() + eol);
                sb.append("Radius: " + app.getRadius() + eol);
                //        sb.append("Brush shape: " + app.getBrushShape() + "/" + app.getToolBrushShape() + eol);
                sb.append("Brush: " + app.getBrush() + "/" + app.getToolBrush() + eol);
                sb.append("Level: " + app.getLevel() + "/" + app.getToolLevel() + eol);
                sb.append("Zoom: " + app.getZoom() + eol);
                sb.append("Hidden layers: " + app.getHiddenLayers());
            }
        } catch (Throwable t) {
            logger.error("Secondary exception occurred while interrogating app for exception report", t);
        }

        body = sb.toString();

        if (! "true".equals(System.getProperty("org.pepsoft.worldpainter.devMode"))) {
            logger.error(body);
        }

        if (mode == Mode.SEND_AUTOMATICALLY) {
            submitInBackground(config);
        }
    }

    private void init(Window parent) {
        initComponents();

        getRootPane().setDefaultButton(jButton2);

        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
            
            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

        scaleToUI(this);
        setLocationRelativeTo(parent);
    }

    private void close() {
        dispose();
    }

    private void email() {
        try {
            URI uri = new URI("mailto", "worldpainter@pepsoft.org?subject=WorldPainter error report&body=" + body, null);
            Desktop desktop = Desktop.getDesktop();
            desktop.mail(uri);
            JOptionPane.showMessageDialog(this, "A new email message should have been opened now for you to send.\nIf it did not work, please use the \"copy to clipboard\" button\nand manually mail the information to worldpainter@pepsoft.org.", "Email Created", JOptionPane.INFORMATION_MESSAGE);
        } catch (URISyntaxException e) {
            logger.error("URI syntax error while trying to send email", e);
            JOptionPane.showMessageDialog(this, "Could not create email message with error details!\nPlease use the \"copy to clipboard\" button and mail\nthe information to worldpainter@pepsoft.org.", "Could Not Create Email", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            logger.error("I/O error while trying to send email", e);
            JOptionPane.showMessageDialog(this, "Could not create email message with error details!\nPlease use the \"copy to clipboard\" button and mail\nthe information to worldpainter@pepsoft.org.", "Could Not Create Email", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void copyToClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection data = new StringSelection(body);
        clipboard.setContents(data, data);
        JOptionPane.showMessageDialog(this, "The information has been copied to the clipboard. Please paste\nit in a new email and send it to worldpainter@pepsoft.org.", "Information Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void submitInBackground(Configuration config) {
        jButton1.setText("Sending...");
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        new Thread("Exception Submitter") {
            @Override
            public void run() {
                try {
                    UsageVO usageVO = new UsageVO();
                    usageVO.setEvents(singletonList(event));
                    usageVO.setLaunchCount(config.getLaunchCount());
                    usageVO.setInstall(config.getUuid());
                    usageVO.setWPVersion(Version.VERSION);
                    Main.privateContext.submitUsageData(usageVO, true);
                    doOnEventThread(() -> {
                        jButton1.setText("Report Sent");
                        jButton2.setEnabled(true);
                    });
                } catch (RuntimeException e) {
                    logger.error("{} while trying to submit exception report to server (message: {})", e.getClass().getSimpleName(), e.getMessage(), e);
                    doOnEventThread(() -> {
                        JOptionPane.showMessageDialog(ErrorDialog.this, "Submitting error report failed.\nPlease use the \"Email Details...\" button below\nto email the report.");
                        jButton1.setText("Email Details...");
                        if ((! Desktop.isDesktopSupported()) || (! Desktop.getDesktop().isSupported(Desktop.Action.MAIL))) {
                            jButton1.setToolTipText("Emailing not supported on this system; please use the \"copy to clipboard\" button and mail the details to worldpainter@pepsoft.org.");
                        } else {
                            jButton1.setEnabled(true);
                        }
                        mode = Mode.REPORTING_DISABLED;
                        jButton2.setEnabled(true);
                    });
                }
            }
        }.start();
    }
    
    public static void main(String[] args) {
        ErrorDialog dialog = new ErrorDialog((Frame) null);
        dialog.setException(new OutOfMemoryError("test"));
        dialog.setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextArea1 = new javax.swing.JTextArea();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Unexpected Error");

        jButton1.setText("Send Report");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Close");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextArea1.setEditable(false);
        jTextArea1.setFont(getFont());
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(10);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setOpaque(false);

        jButton3.setText("Copy details to clipboard");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextArea1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextArea1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        switch (mode) {
            case REPORTING_DISABLED:
                email();
                break;
            case SEND_MANUALLY:
                submitInBackground(Configuration.getInstance());
                break;
            default:
                throw new IllegalStateException("mode " + mode);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        close();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        copyToClipboard();
    }//GEN-LAST:event_jButton3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables

    private String body;
    private EventVO event;
    private Mode mode = Mode.REPORTING_UNNECESSARY;

    private static final double GB = 1024.0 * 1024.0 * 1024.0;
    
    private static final String[] SYSTEM_PROPERTIES = {
        "java.version",
        "java.vendor",
        "java.vm.version",
        "java.vm.vendor",
        "java.vm.name",
        "os.name",
        "os.arch",
        "os.version",
        "user.home",
        "user.dir",
        "user.country",
        "user.language",
    };

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ErrorDialog.class);
    private static final long serialVersionUID = 1L;

    enum Mode { REPORTING_UNNECESSARY, SEND_AUTOMATICALLY, SEND_MANUALLY, REPORTING_DISABLED }
}