/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.IconUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.util.BetterAction;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

/**
 *
 * @author pepijn
 */
public class ThreeDeeFrame extends JFrame implements WindowListener {
    public ThreeDeeFrame(Dimension dimension, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Point initialCoords) throws HeadlessException {
        super("WorldPainter - 3D View");
        setIconImage(App.ICON);
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        this.coords = initialCoords;
        
        scrollPane = new JScrollPane();
        
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                previousX = e.getX();
                previousY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - previousX;
                int dy = e.getY() - previousY;
                previousX = e.getX();
                previousY = e.getY();
                JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
                scrollBar.setValue(scrollBar.getValue() - dx);
                scrollBar = scrollPane.getVerticalScrollBar();
                scrollBar.setValue(scrollBar.getValue() - dy);
            }

            @Override public void mouseClicked(MouseEvent e) {}
            @Override public void mouseReleased(MouseEvent e) {}
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
            @Override public void mouseMoved(MouseEvent e) {}
            
            private int previousX, previousY;
        };
        scrollPane.addMouseListener(mouseAdapter);
        scrollPane.addMouseMotionListener(mouseAdapter);
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    if (zoom < MAX_ZOOM) {
                        ZOOM_IN_ACTION.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()));
                    }
                } else {
                    if (zoom > MIN_ZOOM) {
                        ZOOM_OUT_ACTION.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()));
                    }
                }
            }
        });
        
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        
        final JToggleButton alwaysOnTopButton = new JToggleButton(ICON_ALWAYS_ON_TOP);
        alwaysOnTopButton.setToolTipText("Set the 3D view window to be always on top");
        alwaysOnTopButton.addActionListener(e -> {
            if (alwaysOnTopButton.isSelected()) {
                ThreeDeeFrame.this.setAlwaysOnTop(true);
            } else {
                ThreeDeeFrame.this.setAlwaysOnTop(false);
            }
        });
        
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(alwaysOnTopButton);
        toolBar.addSeparator();
        toolBar.add(ROTATE_LEFT_ACTION);
        toolBar.add(ROTATE_RIGHT_ACTION);
        toolBar.addSeparator();
        toolBar.add(ZOOM_OUT_ACTION);
        toolBar.add(RESET_ZOOM_ACTION);
        toolBar.add(ZOOM_IN_ACTION);
        toolBar.addSeparator();
        toolBar.add(EXPORT_IMAGE_ACTION);
        toolBar.addSeparator();
        toolBar.add(MOVE_TO_SPAWN_ACTION);
        toolBar.add(MOVE_TO_ORIGIN_ACTION);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        glassPane = new GlassPane();
        setGlassPane(glassPane);
        getGlassPane().setVisible(true);
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("rotateLeft", ROTATE_LEFT_ACTION);
        actionMap.put("rotateRight", ROTATE_RIGHT_ACTION);
        actionMap.put("zoomIn", ZOOM_IN_ACTION);
        actionMap.put("resetZoom", RESET_ZOOM_ACTION);
        actionMap.put("zoomOut", ZOOM_OUT_ACTION);

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke('l'), "rotateLeft");
        inputMap.put(KeyStroke.getKeyStroke('r'), "rotateRight");
        inputMap.put(KeyStroke.getKeyStroke('-'), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke('0'), "resetZoom");
        inputMap.put(KeyStroke.getKeyStroke('+'), "zoomIn");
        
        setSize(800, 600);
        
        setDimension(dimension);
        
        addWindowListener(this);
    }

    public final Dimension getDimension() {
        return dimension;
    }

    public final void setDimension(Dimension dimension) {
        this.dimension = dimension;
        if (dimension != null) {
            threeDeeView = new ThreeDeeView(dimension, colourScheme, autoBiomeScheme, customBiomeManager, rotation, zoom);
            scrollPane.setViewportView(threeDeeView);
            MOVE_TO_SPAWN_ACTION.setEnabled(dimension.getDim() == DIM_NORMAL);
            glassPane.setRotation(DIRECTIONS[rotation], dimension.getDim() < 0);
        }
    }

    public void moveTo(Point coords) {
        this.coords = coords;
        threeDeeView.moveTo(coords.x, coords.y);
    }

    public void refresh() {
        if (threeDeeView != null) {
            threeDeeView.refresh();
        }
    }
    
    // WindowListener

    @Override
    public void windowOpened(WindowEvent e) {
        moveTo(coords);
    }

    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}
    
    private final Action ROTATE_LEFT_ACTION = new BetterAction("rotate3DViewLeft", "Rotate left", ICON_ROTATE_LEFT) {
        {
            setShortDescription("Rotate the view 90 degrees anticlockwise (l)");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            rotation--;
            if (rotation < 0) {
                rotation = 3;
            }
            Point centreMostTile = threeDeeView.getCentreMostTile();
            threeDeeView = new ThreeDeeView(dimension, colourScheme, autoBiomeScheme, customBiomeManager, rotation, zoom);
            scrollPane.setViewportView(threeDeeView);
//            scrollPane.getViewport().setViewPosition(new Point((threeDeeView.getWidth() - scrollPane.getWidth()) / 2, (threeDeeView.getHeight() - scrollPane.getHeight()) / 2));
            threeDeeView.moveToTile(centreMostTile.x, centreMostTile.y);
            glassPane.setRotation(DIRECTIONS[rotation], dimension.getDim() < 0);
        }
        
        private static final long serialVersionUID = 1L;
    };
    
    private final Action ROTATE_RIGHT_ACTION = new BetterAction("rotate3DViewRight", "Rotate right", ICON_ROTATE_RIGHT) {
        {
            setShortDescription("Rotate the view 90 degrees clockwise (r)");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            rotation++;
            if (rotation > 3) {
                rotation = 0;
            }
            Point centreMostTile = threeDeeView.getCentreMostTile();
            threeDeeView = new ThreeDeeView(dimension, colourScheme, autoBiomeScheme, customBiomeManager, rotation, zoom);
            scrollPane.setViewportView(threeDeeView);
//            scrollPane.getViewport().setViewPosition(new Point((threeDeeView.getWidth() - scrollPane.getWidth()) / 2, (threeDeeView.getHeight() - scrollPane.getHeight()) / 2));
            threeDeeView.moveToTile(centreMostTile.x, centreMostTile.y);
            glassPane.setRotation(DIRECTIONS[rotation], dimension.getDim() < 0);
        }
        
        private static final long serialVersionUID = 1L;
    };

    private final Action EXPORT_IMAGE_ACTION = new BetterAction("export3DViewImage", "Export image", ICON_EXPORT_IMAGE) {
        {
            setShortDescription("Export to an image file");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            final Set<String> extensions = new HashSet<>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
            StringBuilder sb = new StringBuilder("Supported image formats (");
            boolean first = true;
            for (String extension: extensions) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append("*.");
                sb.append(extension);
            }
            sb.append(')');
            final String description = sb.toString();
            String defaultname = dimension.getWorld().getName().replaceAll("\\s", "").toLowerCase() + ((dimension.getDim() == DIM_NORMAL) ? "" : ("_" + dimension.getName().toLowerCase())) + "_3d.png";
            File selectedFile = FileUtils.selectFileForOpen(ThreeDeeFrame.this, "Export as image file", new File(defaultname), new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String filename = f.getName();
                    int p = filename.lastIndexOf('.');
                    if (p != -1) {
                        String extension = filename.substring(p + 1).toLowerCase();
                        return extensions.contains(extension);
                    } else {
                        return false;
                    }
                }

                @Override
                public String getDescription() {
                    return description;
                }
            });
            if (selectedFile != null) {
                final String type;
                int p = selectedFile.getName().lastIndexOf('.');
                if (p != -1) {
                    type = selectedFile.getName().substring(p + 1).toUpperCase();
                } else {
                    type = "PNG";
                    selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
                }
                if (selectedFile.exists()) {
                    if (JOptionPane.showConfirmDialog(ThreeDeeFrame.this, "The file already exists!\nDo you want to overwrite it?", "Overwrite File?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                final File file = selectedFile;
                Boolean result = ProgressDialog.executeTask(ThreeDeeFrame.this, new ProgressTask<Boolean>() {
                        @Override
                        public String getName() {
                            return "Exporting image...";
                        }

                        @Override
                        public Boolean execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                            try {
                                return ImageIO.write(threeDeeView.getImage(progressReceiver), type, file);
                            } catch (IOException e) {
                                throw new RuntimeException("I/O error while exporting image", e);
                            }
                        }
                    });
                if ((result != null) && result.equals(Boolean.FALSE)) {
                    JOptionPane.showMessageDialog(ThreeDeeFrame.this, "Format " + type + " not supported!");
                }
            }
        }
        
        private static final long serialVersionUID = 1L;
    };
    
    private final Action MOVE_TO_SPAWN_ACTION = new BetterAction("move3DViewToSpawn", "Move to spawn", ICON_MOVE_TO_SPAWN) {
        {
            setShortDescription("Move the view to the spawn location");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (dimension.getDim() == DIM_NORMAL) {
                Point spawn = dimension.getWorld().getSpawnPoint();
                threeDeeView.moveTo(spawn.x, spawn.y);
            }
        }
        
        private static final long serialVersionUID = 1L;
    };
    
    private final Action MOVE_TO_ORIGIN_ACTION = new BetterAction("move3DViewToOrigin", "Move to origin", ICON_MOVE_TO_ORIGIN) {
        {
            setShortDescription("Move the view to the origin (coordinates 0,0)");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            threeDeeView.moveTo(0, 0);
        }
        
        private static final long serialVersionUID = 1L;
    };
    
    private final Action ZOOM_IN_ACTION = new BetterAction("zoom3DViewIn", "Zoom in", ICON_ZOOM_IN) {
        {
            setShortDescription("Zoom in");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            final Rectangle visibleRect = threeDeeView.getVisibleRect();
            zoom++;
            threeDeeView.setZoom(zoom);
            visibleRect.x *= 2;
            visibleRect.y *= 2;
            visibleRect.x += visibleRect.width / 2;
            visibleRect.y += visibleRect.height / 2;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    threeDeeView.scrollRectToVisible(visibleRect);
                }
            });
            if (zoom >= MAX_ZOOM) {
                setEnabled(false);
            }
            ZOOM_OUT_ACTION.setEnabled(true);
            RESET_ZOOM_ACTION.setEnabled(zoom != 1);
        }
        
        private static final long serialVersionUID = 1L;
    };
    
    private final Action RESET_ZOOM_ACTION = new BetterAction("reset3DViewZoom", "Reset zoom", ICON_RESET_ZOOM) {
        {
            setShortDescription("Reset the zoom level to 1:1");
            setEnabled(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            final Rectangle visibleRect = threeDeeView.getVisibleRect();
            if (zoom < 1) {
                while (zoom < 1) {
                    zoom++;
                    visibleRect.x *= 2;
                    visibleRect.y *= 2;
                    visibleRect.x += visibleRect.width / 2;
                    visibleRect.y += visibleRect.height / 2;
                }
                threeDeeView.setZoom(zoom);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        threeDeeView.scrollRectToVisible(visibleRect);
                    }
                });
            } else if (zoom > 1) {
                while (zoom > 1) {
                    zoom--;
                    visibleRect.x /= 2;
                    visibleRect.y /= 2;
                    visibleRect.x -= visibleRect.width / 4;
                    visibleRect.y -= visibleRect.height / 4;
                }
                threeDeeView.setZoom(zoom);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        threeDeeView.scrollRectToVisible(visibleRect);
                    }
                });
            }
            ZOOM_IN_ACTION.setEnabled(true);
            ZOOM_OUT_ACTION.setEnabled(true);
            setEnabled(false);
        }
        
        private static final long serialVersionUID = 1L;
    };
    
    private final Action ZOOM_OUT_ACTION = new BetterAction("zoom3DViewOut", "Zoom out", ICON_ZOOM_OUT) {
        {
            setShortDescription("Zoom out");
        }
        
        @Override
        public void performAction(ActionEvent e) {
            final Rectangle visibleRect = threeDeeView.getVisibleRect();
            zoom--;
            threeDeeView.setZoom(zoom);
            visibleRect.x /= 2;
            visibleRect.y /= 2;
            visibleRect.x -= visibleRect.width / 4;
            visibleRect.y -= visibleRect.height / 4;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    threeDeeView.scrollRectToVisible(visibleRect);
                }
            });
            if (zoom <= MIN_ZOOM) {
                setEnabled(false);
            }
            ZOOM_IN_ACTION.setEnabled(true);
            RESET_ZOOM_ACTION.setEnabled(zoom != 1);
        }
        
        private static final long serialVersionUID = 1L;
    };

    private final JScrollPane scrollPane;
    private final GlassPane glassPane;
    private final CustomBiomeManager customBiomeManager;
    private final AutoBiomeScheme autoBiomeScheme = new AutoBiomeScheme(null);
    private Dimension dimension;
    private ThreeDeeView threeDeeView;
    private ColourScheme colourScheme;
    private int rotation = 3, zoom = 1;
    private Point coords;
    
    private static final Direction[] DIRECTIONS = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    
    private static final Icon ICON_ROTATE_LEFT    = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_rotate_anticlockwise.png");
    private static final Icon ICON_ROTATE_RIGHT   = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_rotate_clockwise.png");
    private static final Icon ICON_EXPORT_IMAGE   = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/picture_save.png");
    private static final Icon ICON_MOVE_TO_SPAWN  = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/spawn_red.png");
    private static final Icon ICON_MOVE_TO_ORIGIN = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_in.png");
    private static final Icon ICON_ALWAYS_ON_TOP  = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/lock.png");
    private static final Icon ICON_ZOOM_IN        = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/magnifier_zoom_in.png");
    private static final Icon ICON_RESET_ZOOM     = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/magnifier.png");
    private static final Icon ICON_ZOOM_OUT       = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/magnifier_zoom_out.png");
    
    private static final int MIN_ZOOM = -2;
    private static final int MAX_ZOOM = 4;
    
    private static final long serialVersionUID = 1L;
}