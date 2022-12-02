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

import org.pepsoft.util.mdc.MDCWrappingException;
import org.pepsoft.util.mdc.MDCWrappingRuntimeException;
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
import java.util.*;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.pepsoft.util.AwtUtils.doOnEventThread;
import static org.pepsoft.util.GUIUtils.scaleToUI;
import static org.pepsoft.util.mdc.MDCUtils.gatherMdcContext;
import static org.pepsoft.util.swing.MessageUtils.showInfo;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"unused", "Convert2Lambda", "Anonymous2MethodRef"}) // Managed by NetBeans
public class ErrorDialog extends javax.swing.JDialog {
    /** Creates new form ErrorDialog */
    public ErrorDialog(Window parent) {
        super(parent, APPLICATION_MODAL);
        init(parent);
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend") // Readability
    public void setException(Throwable exception) {
        SortedMap<String, String> mdcContextMap = new TreeMap<>(gatherMdcContext(exception));
        if ((exception instanceof MDCWrappingException) || (exception instanceof MDCWrappingRuntimeException)) {
            exception = exception.getCause();
        }

        final UUID uuid = UUID.randomUUID();
        logger.error("[" + uuid + "] " + exception.getClass().getSimpleName() + ": " + exception.getMessage(), exception);

        event = new EventVO(EVENT_KEY_EXCEPTION);
        event.addTimestamp();
        event.setAttribute(ATTRIBUTE_KEY_EXCEPTION, new ExceptionVO(exception));
        event.setAttribute(ATTRIBUTE_KEY_UUID, uuid.toString());

        final Set<Class<? extends Throwable>> exceptionTypes = new HashSet<>();
        exceptionTypes.add(exception.getClass());
        Throwable rootCause = exception;
        String ioExceptionMessage = (exception instanceof IOException) ? exception.getMessage() : null;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
            exceptionTypes.add(rootCause.getClass());
            if (rootCause instanceof IOException) {
                ioExceptionMessage = rootCause.getMessage();
            }
        }
        final boolean ioException = ioExceptionMessage != null;

        if (exceptionTypes.contains(OutOfMemoryError.class)) {
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
            String message = (ioExceptionMessage != null) ? ioExceptionMessage : rootCause.getMessage();
            if ((message != null) && (message.length() > 250)) {
                message = message.substring(0, 247) + "...";
            }
            final String requestedActionLine;
            if (Main.privateContext != null) {
                // We can submit the error
                final Configuration config = Configuration.getInstance();
                if ((config != null) && TRUE.equals(config.getPingAllowed()) && (! ioException)) {
                    // Automatic submission is allowed; submit it automatically
                    mode = Mode.SEND_AUTOMATICALLY;
                    requestedActionLine = "The details of this error are being automatically submitted to the creator of this program.";
                } else {
                    mode = Mode.SEND_MANUALLY;
                    if (ioException) {
                        requestedActionLine = "If you think this is a bug then please use the Send Report button below to send the details of this error to the creator of this program.";
                    } else {
                        requestedActionLine = "Please help debug the problem by using the Send Report button below to send the details of this error to the creator of this program.";
                    }
                }
            } else {
                jButton1.setText("Email Details...");
                if ((! Desktop.isDesktopSupported()) || (! Desktop.getDesktop().isSupported(Desktop.Action.MAIL))) {
                    jButton1.setToolTipText("Emailing not supported on this system; please use the \"copy to clipboard\" button and mail the details to worldpainter@pepsoft.org.");
                } else {
                    jButton1.setEnabled(true);
                }
                mode = Mode.REPORTING_DISABLED;
                if (ioException) {
                    requestedActionLine = "If you think this is a bug then please use the button below to email the details of this error to the creator of this program.";
                } else {
                    requestedActionLine = "Please help debug the problem by using the button below to email the details of this error to the creator of this program.";
                }
            }
            final String text;
            if (ioException) {
                text = "A read or write error has occurred.\n\n"
                    + "Message: " + message + "\n\n"
                    + requestedActionLine;
            } else {
                text = "An unexpected error has occurred.\n\n"
                    + "Type: " + rootCause.getClass().getName() + "\n"
                    + "Message: " + message + "\n\n"
                    + requestedActionLine + "\n\n"
                    + "The program may now be in an unstable state. It is recommended to restart it as soon as possible.";
            }
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
                sb.append("Number of tiles: " + dimension.getTileCount() + eol);
                Set<Layer> layers = dimension.getAllLayers(false);
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
            submitInBackground();
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
            showInfo(this, "A new email message should have been opened now for you to send.\nIf it did not work, please use the \"copy to clipboard\" button\nand manually mail the information to worldpainter@pepsoft.org.", "Email Created");
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
        showInfo(this, "The information has been copied to the clipboard. Please paste\nit in a new email and send it to worldpainter@pepsoft.org.", "Information Copied");
    }

    private void submitInBackground() {
        jButton1.setText("Sending...");
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        new Thread("Exception Submitter") {
            @Override
            public void run() {
                try {
                    UsageVO usageVO = new UsageVO();
                    usageVO.setEvents(singletonList(event));
                    final Configuration config = Configuration.getInstance();
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
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
                submitInBackground();
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