/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.jetbrains.annotations.NotNull;
import org.jnbt.ByteArrayTag;
import org.jnbt.IntArrayTag;
import org.jnbt.LongArrayTag;
import org.jnbt.Tag;
import org.pepsoft.util.PluginManager;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.Main;
import org.pepsoft.worldpainter.MouseAdapter;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static javax.swing.SwingConstants.TOP;
import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.FILENAME;

/**
 *
 * @author pepijn
 */
public class MapExplorer {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Load the default platform descriptors so that they don't get blocked
        // by older versions of them which might be contained in the
        // configuration. Do this by loading and initialising (but not
        // instantiating) the DefaultPlugin class
        try {
            Class.forName("org.pepsoft.worldpainter.DefaultPlugin");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Load or initialise configuration
        File configDir = Configuration.getConfigDir();
        if (! configDir.isDirectory()) {
            configDir.mkdirs();
        }
        Configuration config = Configuration.load(); // This will migrate the configuration directory if necessary
        if (config == null) {
            if (! logger.isDebugEnabled()) {
                // If debug logging is on, the Configuration constructor will
                // already log this
                logger.info("Creating new configuration");
            }
            config = new Configuration();
        }
        Configuration.setInstance(config);
        logger.info("Installation ID: " + config.getUuid());

        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(Main.class.getResourceAsStream("/wproot.pem"));
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        }

        // Load the plugins
        if (trustedCert != null) {
            PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey(), FILENAME);
        } else {
            logger.error("Trusted root certificate not available; not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid());

        File defaultDir;
        if (args.length > 0) {
            defaultDir = new File(args[0]);
        } else {
            File minecraftDir = MinecraftUtil.findMinecraftDir();
            if (minecraftDir != null) {
                defaultDir = new File(minecraftDir, "saves");
            } else {
                defaultDir = new File(System.getProperty("user.home"));
            }
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Minecraft Map Explorer");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setLeftComponent(createTreePanel(defaultDir));
            splitPane.setRightComponent(createDetailsPanel());
            frame.getContentPane().add(splitPane, CENTER);
            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static Component createDetailsPanel() {
        JPanel detailsPanel = new JPanel(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        wordSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 4, 64, 1));
        wordSizeSpinner.addChangeListener(e -> updateBinaryData());
        JLabel label = new JLabel("Word size:");
        label.setLabelFor(wordSizeSpinner);
        toolBar.add(label);
        toolBar.add(wordSizeSpinner);
        lineLengthSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 9999, 1));
        lineLengthSpinner.addChangeListener(e -> updateBinaryData());
        label = new JLabel("Line length:");
        label.setLabelFor(lineLengthSpinner);
        toolBar.add(label);
        toolBar.add(lineLengthSpinner);
        // Make the components their smallest size:
        toolBar.add(Box.createHorizontalStrut(Integer.MAX_VALUE));
        detailsPanel.add(toolBar, NORTH);
        detailsArea = new JLabel();
        detailsArea.setVerticalAlignment(TOP);
        detailsPanel.add(new JScrollPane(detailsArea), CENTER);
        return detailsPanel;
    }

    @NotNull
    private static Component createTreePanel(File defaultDir) {
        MapTreeModel treeModel = new MapTreeModel();
        JTree tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new MapTreeCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tree);
        tree.expandPath(treeModel.getPath(defaultDir));
        tree.scrollPathToVisible(treeModel.getPath(defaultDir));
        // Automatically expand any nodes if they only have one child
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                Object node = event.getPath().getLastPathComponent();
                if ((! treeModel.isLeaf(node)) && (treeModel.getChildCount(node) == 1)) {
                    tree.expandPath(event.getPath().pathByAddingChild(treeModel.getChild(node, 0)));
                }
            }

            @Override public void treeCollapsed(TreeExpansionEvent event) {}
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        ((Node) path.getLastPathComponent()).doubleClicked();
                    }
                }
            }
        });
        tree.getSelectionModel().addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path != null) {
                updateDetails((Node) path.getLastPathComponent());
            }
        });
        return scrollPane;
    }

    private static void updateDetails(Node node) {
        if (node instanceof TagNode) {
            // NBT tag
            Tag tag = ((TagNode) node).getTag();
            if (tag instanceof ByteArrayTag) {
                data = ((ByteArrayTag) tag).getValue();
                updateBinaryData();
            } else if (tag instanceof IntArrayTag) {
                data = ((IntArrayTag) tag).getValue();
                updateBinaryData();
            } else if (tag instanceof LongArrayTag) {
                data = ((LongArrayTag) tag).getValue();
                updateBinaryData();
            } else {
                data = null;
                detailsArea.setIcon(null);
                detailsArea.setText("<html><pre>" + tag.getValue() + "</pre></html>");
            }
        } else if (node instanceof FileSystemNode) {
            if (node instanceof NBTFileNode) {
                clearDetails();
                return;
            }
            File file = ((FileSystemNode) node).file;
            if ((! file.isFile()) || (! file.canRead())) {
                clearDetails();
                return;
            }
            String name = file.getName().toLowerCase().trim();
            int p = name.lastIndexOf('.');
            String extension;
            if (p != -1) {
                extension = name.substring(p + 1);
            } else {
                extension = null;
            }
            try {
                byte[] contents = Files.readAllBytes(file.toPath());
                if (SUPPORTED_IMAGE_TYPES.contains(extension)) {
                    // Image
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(contents));
                    data = null;
                    detailsArea.setIcon(new ImageIcon(image));
                    detailsArea.setText(null);
                } else if (TEXT_FILES.contains(extension)) {
                    data = null;
                    detailsArea.setIcon(null);
                    detailsArea.setText("<html><pre>" + new String(contents, "UTF-8") + "</pre></html>");
                } else {
                    // Binary file
                    data = contents;
                    updateBinaryData();
                }
            } catch (IOException e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                data = null;
                detailsArea.setIcon(null);
                detailsArea.setText("<html><pre>" + sw + "</pre></html>");
            }
        } else {
            clearDetails();
        }
    }

    private static void clearDetails() {
        data = null;
        detailsArea.setIcon(null);
        detailsArea.setText(null);
    }

    private static void updateBinaryData() {
        if (data == null) {
            return;
        }
        int wordSize = (int) wordSizeSpinner.getValue();
        int lineLength = (int) lineLengthSpinner.getValue();
        StringBuilder text = new StringBuilder("<html><pre>");
        BitSet bitSet;
        int lengthInWords;
        if (data instanceof byte[]) {
            bitSet = BitSet.valueOf((byte[]) data);
            lengthInWords = ((byte[]) data).length * 8 / wordSize;
        } else if (data instanceof long[]) {
            bitSet = BitSet.valueOf((long[]) data);
            lengthInWords = ((long[]) data).length * 64 / wordSize;
        } else {
            throw new IllegalArgumentException("Don't know how to process data of type " + data.getClass());
        }
        // TODO: special case 2^n bit word sizes for performance
        int wordLength = (int) Math.ceil(wordSize / 4.0);
        String wordFormat = "%" + wordLength + "x";
        int lineCount = (int) Math.ceil((double) lengthInWords / lineLength);
        int lineNumberLength = (int) Math.ceil(Math.log(lineCount + 1) / Math.log(10));
        String lineNumberFormat = "%" + lineNumberLength + "d";
        int wordsOnLine = 0, lineNumber = 1;
        for (int w = 0; w < lengthInWords; w++) {
            if (wordsOnLine == lineLength) {
                text.append('\n');
                wordsOnLine = 0;
                lineNumber++;
            }
            if (wordsOnLine == 0) {
                text.append(String.format(lineNumberFormat, lineNumber));
                text.append(':');
            }
            text.append(' ');
            wordsOnLine++;
            int wordOffset = w * wordSize;
            int word = 0;
            for (int b = 0; b < wordSize; b++) {
                word |= bitSet.get(wordOffset + b) ? 1 << b : 0;
            }
            text.append(String.format(wordFormat, word));
        }
        text.append("</pre></html>");
        detailsArea.setIcon(null);
        detailsArea.setText(text.toString());
    }

    private static JLabel detailsArea;
    private static JSpinner wordSizeSpinner, lineLengthSpinner;
    private static Object data;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = new HashSet<>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
    private static final Set<String> TEXT_FILES = new HashSet<>(Arrays.asList("json", "txt", "xml", "log"));
    private static final Logger logger = LoggerFactory.getLogger(MapExplorer.class);
}