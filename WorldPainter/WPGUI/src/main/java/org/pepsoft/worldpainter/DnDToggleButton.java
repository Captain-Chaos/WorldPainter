/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.TransferHandler;

/**
 *
 * @author SchmitzP
 */
public class DnDToggleButton extends JToggleButton implements Transferable, DragSourceListener, DragGestureListener {
    public DnDToggleButton() {
        init();
    }

    public DnDToggleButton(Icon icon) {
        super(icon);
        init();
    }

    public DnDToggleButton(Icon icon, boolean selected) {
        super(icon, selected);
        init();
    }

    public DnDToggleButton(String text) {
        super(text);
        init();
    }

    public DnDToggleButton(String text, boolean selected) {
        super(text, selected);
        init();
    }

    public DnDToggleButton(Action a) {
        super(a);
        init();
    }

    public DnDToggleButton(String text, Icon icon) {
        super(text, icon);
        init();
    }

    public DnDToggleButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        init();
    }
    
    // Transferable
    
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {new DataFlavor(DnDToggleButton.class, "JToggleButton")};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return true;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        return this;
    }

    // DragSourceListener
    
    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        repaint();
    }

    @Override public void dragEnter(DragSourceDragEvent dsde) {}
    @Override public void dragOver(DragSourceDragEvent dsde) {}
    @Override public void dropActionChanged(DragSourceDragEvent dsde) {}
    @Override public void dragExit(DragSourceEvent dse) {}

    // DragGestureListener
    
    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        source.startDrag(dge, DragSource.DefaultMoveDrop, new DnDToggleButton("Text"), this);
    }
    
    private void init() {
        transferHandler = new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                return new DnDToggleButton(getText(), getIcon());
            }
            
        };
        setTransferHandler(transferHandler);
        
        source = new DragSource();
        source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);
    }
    
    private DragSource source;
    private TransferHandler transferHandler;
}