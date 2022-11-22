package org.pepsoft.worldpainter;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

import static org.pepsoft.util.FileUtils.absolutise;

public class Overlay implements Serializable {
    public Overlay(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        this.file = absolutise(file);
    }

    public File getFile() {
        return file;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        if (scale != this.scale) {
            final float oldScale = this.scale;
            this.scale = scale;
            propertyChangeSupport.firePropertyChange("scale", oldScale, scale);
        }
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        if (transparency != this.transparency) {
            final float oldTransparency = this.transparency;
            this.transparency = transparency;
            propertyChangeSupport.firePropertyChange("transparency", oldTransparency, transparency);
        }
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        if (offsetX != this.offsetX) {
            final int oldOffsetX = this.offsetX;
            this.offsetX = offsetX;
            propertyChangeSupport.firePropertyChange("offsetX", oldOffsetX, offsetX);
        }
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        if (offsetY != this.offsetY) {
            final int oldOffsetY = this.offsetY;
            this.offsetY = offsetY;
            propertyChangeSupport.firePropertyChange("offsetY", oldOffsetY, offsetY);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            propertyChangeSupport.firePropertyChange("enabled", ! enabled, enabled);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        if (! Objects.equals(image, this.image)) {
            final BufferedImage oldImage = this.image;
            this.image = image;
            propertyChangeSupport.firePropertyChange("image", oldImage, image);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    private final File file;
    private float scale = 1.0f, transparency = 0.5f;
    private int offsetX = 0, offsetY = 0;
    private boolean enabled = true;
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private transient BufferedImage image;

    private static final long serialVersionUID = 1L;
}