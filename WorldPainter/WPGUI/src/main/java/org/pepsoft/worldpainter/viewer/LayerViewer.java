/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A graphical component which can show a number of layers and scroll them
 * efficiently.
 * 
 * @author pepijn
 */
public class LayerViewer extends JScrollPane implements ChangeListener {
    public LayerViewer() {
        view = new LayeredView();
        setViewportView(view);
        viewport.addChangeListener(this);
    }
    
    public void addLayer(LayerView layer, Point location) {
        view.addLayer(layer, location);
    }

    public void start() {
        view.start();
    }

    public void shutdown() {
        view.shutdown();
    }
    
    // ChangeListener
    
    @Override
    public void stateChanged(ChangeEvent e) {
        view.setVisibleRegion(viewport.getViewRect());
    }
    
    private final LayeredView view;
    
    static class LayeredView extends JPanel {
        LayeredView() {
            setLayout(null);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return getComponentCount() <= 1;
        }
        
        void addLayer(LayerView layer, Point location) {
            add(layer);
            layer.setLocation(location);
            updateBounds();
            if (running) {
                layer.start();
            }
            repaint();
        }
        
        void setVisibleRegion(Rectangle visibleRegion) {
            for (Component component: getComponents()) {
                ((LayerView) component).setVisibleRegion(visibleRegion);
            }
        }
        
        void start() {
            if (running) {
                throw new IllegalStateException("Already running");
            }
            for (Component component: getComponents()) {
                ((LayerView) component).start();
            }
            running = true;
        }
        
        void shutdown() {
            if (! running) {
                throw new IllegalStateException("Not running");
            }
            for (Component component: getComponents()) {
                ((LayerView) component).shutdown();
            }
            running = false;
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
        }
        
        private void updateBounds() {
            Component[] components = getComponents();
            if (components.length == 0) {
                setBounds(0, 0, 0, 0);
            } else {
                Rectangle bounds = components[0].getBounds();
                for (int i = 1; i < components.length; i++) {
                    bounds.add(components[i].getBounds());
                }
                setBounds(bounds);
            }
        }
        
        private boolean running = false;
    }
}