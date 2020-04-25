/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AboutDialog.java
 *
 * Created on 1-nov-2011, 22:56:34
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.plugins.Plugin;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftUtil;
import org.pepsoft.worldpainter.vo.EventVO;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 *
 * @author pepijn
 */
public class AboutDialog extends javax.swing.JDialog implements WindowListener {
    /** Creates new form AboutDialog */
    public AboutDialog(Window parent, World2 world, WorldPainter view, UndoManager undoManager) {
        super(parent, ModalityType.APPLICATION_MODAL);
        initComponents();
        jTextPane2.setText(loadChangelog());
        jTextPane1.setText(loadCredits());
        jTextPane3.setText(loadTechInfo(world, view, undoManager));

        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("close", new AbstractAction("close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }

            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

        rootPane.setDefaultButton(buttonClose);
        
        setLocationRelativeTo(parent);
        addWindowListener(this);
    }

    // WindowListener
    
    @Override
    public void windowOpened(WindowEvent e) {
        jScrollPane1.getVerticalScrollBar().setValue(0);
        scroller = new Scroller();
        scroller.attach(jScrollPane1.getVerticalScrollBar());
        jScrollPane2.getVerticalScrollBar().setValue(0);
        jScrollPane3.getVerticalScrollBar().setValue(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {
        if (scroller != null) {
            scroller.detach();
            scroller = null;
        }
    }

    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}

    private String loadCredits() {
        Font defaultTextPaneFont = jTextPane2.getFont();
        Color textColour = jTextPane2.getForeground();
        Color linkColour;
        if (textColour.getRed() + textColour.getGreen() + textColour.getBlue() > 384) {
            // Light text colour; use light link colour
            linkColour = Color.CYAN;
        } else {
            linkColour = Color.BLUE;
        }
        String style = String.format("body {font-family: %s; font-size: %dpt; color: #%06x; background-color: #%06x;} a {color: #%06x;}",
            defaultTextPaneFont.getFamily(), defaultTextPaneFont.getSize(), textColour.getRGB() & 0xffffff,
            jTextPane2.getBackground().getRGB() & 0xffffff, linkColour.getRGB() & 0xffffff);
        InputStreamReader in = new InputStreamReader(AboutDialog.class.getResourceAsStream("resources/credits.html"), Charset.forName("UTF-8"));
        try {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            String template = sb.toString();
            MessageFormat formatter = new MessageFormat(template);
            Configuration.DonationStatus donationStatus = Configuration.getInstance().getDonationStatus();
            return formatter.format(new Object[] {
                Version.VERSION,
                (donationStatus != null)
                    ? donationStatus.ordinal()
                    : Configuration.DonationStatus.NO_THANK_YOU.ordinal(),
                style});
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading resource", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("I/O error closing resource", e);
            }
        }
    }
    
    private String loadChangelog() {
        try {
            return FileUtils.load(ClassLoader.getSystemResourceAsStream("CHANGELOG"), Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("I/O error while loading change log from classpath", e);
        }
    }
    
    private void close() {
        dispose();
    }
    
    private void donate() {
        try {
            DesktopUtils.open(new URL("https://www.worldpainter.net/donate/paypal"));
            Configuration config = Configuration.getInstance();
            config.setDonationStatus(Configuration.DonationStatus.DONATED);
            JOptionPane.showMessageDialog(this, strings.getString("the.donation.paypal.page.has.been.opened"), strings.getString("thank.you"), JOptionPane.INFORMATION_MESSAGE);
            config.logEvent(new EventVO(Constants.EVENT_KEY_DONATION_DONATE).addTimestamp());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadTechInfo(World2 world, WorldPainter view, UndoManager undoManager) {
        File installDir = null;
        ClassLoader classLoader = AboutDialog.class.getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            for (URL url: ((URLClassLoader) classLoader).getURLs()) {
                if (url.getPath().endsWith("WorldPainter.jar")) { // NOI18N
                    try {
                        installDir = new File(url.toURI()).getParentFile();
                        if (installDir.getName().equals("app")) { // NOI18N
                            // We're probably on a Mac; the real installation
                            // directory is a few levels higher
                            installDir = installDir.getParentFile().getParentFile().getParentFile();
                        }
                    } catch (URISyntaxException e) {
                        logger.error("URI syntax exception while trying to find install dir", e);
                    }
                    break;
                }
            }
        }
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        File configDir = Configuration.getConfigDir();
        String message = "";
        if (Configuration.getInstance().isSafeMode()) {
            message += "WorldPainter is running in safe mode\n";
        }
        message += MessageFormat.format(strings.getString("worldpainter.version.0.njava.version.1.noperating.system.2.3.version.4.n"), Version.VERSION, Version.BUILD, System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
        if (installDir != null) {
            message += MessageFormat.format(strings.getString("installation.directory.0.n"), installDir);
        }
        message += MessageFormat.format(strings.getString("worldpainter.config.directory.0.ninstallation.id.1.n"), configDir, Configuration.getInstance().getUuid());
        List<Plugin> plugins = WPPluginManager.getInstance().getAllPlugins();
        for (Plugin plugin: plugins) {
            Properties properties = plugin.getProperties();
            String name = properties.getProperty(Plugin.PROPERTY_NAME);
            if (! (name.equals("Default")
                    || plugin.getName().equals("DefaultPlatforms")
                    || plugin.getName().equals("DefaultCustomObjects")
                    || name.equals("DefaultLayerEditorProvider"))) { // NOI18N
                String version = properties.getProperty(Plugin.PROPERTY_VERSION);
                message += MessageFormat.format(strings.getString("0.plugin.version.1.n"), name, version);
            }
        }
        if (minecraftDir != null) {
            message += MessageFormat.format(strings.getString("minecraft.data.directory.0.n"), minecraftDir);
        }
        Runtime runtime = Runtime.getRuntime();
        int dataSize = (world != null) ? world.measureSize() : 0;
        int undoDataSize = (undoManager != null) ? undoManager.getDataSize() : 0;
//        int imageSize = view.getImageSize();
        int overlayImageSize = view.getOverlayImageSize();
        message += MessageFormat.format(strings.getString("available.cpu.cores.0.nmaximum.configured.memory.1.mb.ncurrently.allocated.memory.2.mb.n"), runtime.availableProcessors(), runtime.maxMemory() / 1024 / 1024, runtime.totalMemory() / 1024 / 1024);
        if (dataSize > 0) {
            message += MessageFormat.format(strings.getString("world.data.size.0.kb.n"), dataSize / 1024);
        }
        if ((undoDataSize - dataSize) > 0) {
            message += MessageFormat.format(strings.getString("additional.undo.data.size.0.kb.n"), (undoDataSize - dataSize) / 1024);
        }
//        if (imageSize > 0) {
//            message += MessageFormat.format(strings.getString("world.image.data.size.0.kb.n"), imageSize / 1024);
//        }
        if (overlayImageSize > 0) {
            message += MessageFormat.format(strings.getString("overlay.image.data.size.0.kb.n"), overlayImageSize / 1024);
        }
        return message;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        buttonClose = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPane3 = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("About WorldPainter");
        setResizable(false);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/resources/banner.png"))); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        buttonClose.setText("Close");
        buttonClose.addActionListener(this::buttonCloseActionPerformed);

        jScrollPane1.setBorder(null);

        jTextPane1.setEditable(false);
        jTextPane1.setContentType("text/html"); // NOI18N
        jTextPane1.setText("<html>\n  <head>\n  </head>\n  <body>\n    <p style=\"margin-top: 0\">\nEen paar regels<br>\nom te laten zien<br>\nhoe het er in het echt<br>\nuit zal zien.      \n    </p>\n  </body>\n</html>");
        jTextPane1.addHyperlinkListener(this::jTextPane1HyperlinkUpdate);
        jScrollPane1.setViewportView(jTextPane1);

        jTabbedPane1.addTab("Credits", jScrollPane1);

        jScrollPane2.setBorder(null);

        jTextPane2.setEditable(false);
        jTextPane2.setText("Een paar regels\nom te laten zien\nhoe het er in het echt\nuit zal zien.");
        jScrollPane2.setViewportView(jTextPane2);

        jTabbedPane1.addTab("Change log", jScrollPane2);

        jScrollPane3.setBorder(null);

        jTextPane3.setEditable(false);
        jTextPane3.setText("Een paar regels\nom te laten zien\nhoe het er in het echt\nuit zal zien.");
        jScrollPane3.setViewportView(jTextPane3);

        jTabbedPane1.addTab("Tech Info", jScrollPane3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(buttonClose, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jTabbedPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonClose)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextPane1HyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jTextPane1HyperlinkUpdate
        if (evt.getEventType() == EventType.ACTIVATED) {
            URL url = evt.getURL();
            if (url.getProtocol().equals("action")) { // NOI18N
                String action = url.getPath().toLowerCase().trim();
                if (action.equals("/donate")) { // NOI18N
                    donate();
                }
            } else {
                DesktopUtils.open(url);
            }
        }
    }//GEN-LAST:event_jTextPane1HyperlinkUpdate

    private void buttonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseActionPerformed
        close();
    }//GEN-LAST:event_buttonCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClose;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JTextPane jTextPane3;
    // End of variables declaration//GEN-END:variables

    private Scroller scroller;
    
    private static final int BUFFER_SIZE = 32768;
    private static final ResourceBundle strings = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.strings"); // NOI18N
    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AboutDialog.class);
}