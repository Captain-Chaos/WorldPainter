/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Random;
import javax.vecmath.Point3i;
import org.pepsoft.worldpainter.CoordinateTransform;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 *
 * @author pepijn
 */
public abstract class Seed implements Serializable, org.pepsoft.util.undo.Cloneable<Seed> {
    public Seed(Garden garden, long seed, Seed parent, Point3i location, int germinationTime, int category) {
        this.id = nextId++;
        this.garden = garden;
        this.parent = parent;
        this.location = location;
        if (germinationTime < 0) {
            this.germinationTime = Math.max(1, -germinationTime);
        } else {
            this.germinationTime = Math.max(1, (int) (germinationTime * (staticRandom.nextDouble() + 0.6)));
        }
        this.category = category;
        this.seed = seed;
    }

    public final Garden getGarden() {
        return garden;
    }

    public final Seed getParent() {
        return parent;
    }

    public final Point3i getLocation() {
        return location;
    }
    
    public final void tick() {
        if ((parent != null) && (! parent.sprouted)) {
            if (! parent.isFinished()) {
                // Parent is alive and not yet sprouted. Wait until it has
                // sprouted before sprouting ourselves
                return;
            } else {
                // Parent has died without sprouting. Don't sprout ourselves
                germinationTime = 0;
                return;
            }
        } else {
            // We have no parent, or our parent has sprouted
            germinationTime--;
            if (germinationTime == 0) {
                sprouted = sprout();
                if (! sprouted) {
                    garden.removeSeed(this);
                }
            }
        }
    }

    public final boolean isFinished() {
        return germinationTime <= 0;
    }

    public final boolean isSprouted() {
        return sprouted;
    }
    
    public final void neutralise() {
        germinationTime = 0;
    }
    
    public void buildFirstPass(Dimension dimension, Tile tile, MinecraftWorld minecraftWorld) {
        // Do nothing
    }
    
    public void buildSecondPass(Dimension dimension, Tile tile, MinecraftWorld minecraftWorld) {
        // Do nothing
    }
    
    public void rotate(CoordinateTransform rotation) {
        rotation.transformInPlace(location);
    }

    @Override
    public Seed clone() {
        try {
            return (Seed) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Seed)
            && (id == ((Seed) obj).id);
    }

    @Override
    public int hashCode() {
        // Note that the ID changes whenever this class is deserialized. This
        // should be no problem, because (in this codebase) the hash code is
        // only used by the collection classes, and their serialized format is
        // independent of the hash codes of their contents
        return (int) (this.id ^ (this.id >>> 32));
    }
    
    /**
     * Try to sprout the seed. This may fail if not all conditions are met, such
     * as if there is no space for it to sprout. Will only be invoked once,
     * after the parent seed (if any) has sprouted and the germination time has
     * elapsed. If it returns <code>false</code>, the seed will be considered to
     * have died, and be removed from the garden automatically.
     * 
     * @return <code>true</code> if the seed sprouted successfully,
     *     <code>false</code> if it could not sprout for whatever reason.
     */
    protected abstract boolean sprout();
    
//    protected final void reset(int germinationTime) {
//        this.germinationTime = Math.max(1, (int) (germinationTime * (staticRandom.nextDouble() + 0.6)));
//    }
    
    protected final void drawLine(Point location1, Point location2, int diameter, boolean stopWhenOccupied, int category) {
        int length = Integer.MAX_VALUE;
        if (stopWhenOccupied) {
            length = scanLine(location1.x, location1.y, location2.x, location2.y);
        }
        drawLine(location1, location2, diameter, length, category);
    }
    
    protected final void drawLine(Point3i location1, Point3i location2, int diameter, boolean stopWhenOccupied, int category) {
        int length = Integer.MAX_VALUE;
        if (stopWhenOccupied) {
            length = scanLine(location1.x, location1.y, location2.x, location2.y);
        }
        drawLine(location1, location2, diameter, length, category);
    }
    
    protected final void drawLine(Point location1, Point location2, int diameter, int maxLength, int category) {
        if (diameter > 1) {
            int offset = diameter / 2;
            for (int dx = 0; dx < diameter; dx++) {
                for (int dy = 0; dy < diameter; dy++) {
                    if ((dx != offset) || (dy != offset)) {
                        drawLine(location1.x + dx - offset, location1.y + dy - offset, location2.x + dx - offset, location2.y + dy - offset, maxLength, category);
                    }
                }
            }
        } else {
            drawLine(location1.x, location1.y, location2.x, location2.y, maxLength, category);
        }
    }
    
    protected final void drawLine(Point3i location1, Point3i location2, int diameter, int maxLength, int category) {
        if (diameter > 1) {
            int offset = diameter / 2;
            for (int dx = 0; dx < diameter; dx++) {
                for (int dy = 0; dy < diameter; dy++) {
                    if ((dx != offset) || (dy != offset)) {
                        drawLine(location1.x + dx - offset, location1.y + dy - offset, location2.x + dx - offset, location2.y + dy - offset, maxLength, category);
                    }
                }
            }
        } else {
            drawLine(location1.x, location1.y, location2.x, location2.y, maxLength, category);
        }
    }
    
    protected final int scanLine(int x1, int y1, int x2, int y2) {
        if ((x1 == x2) && (y1 == y2)) {
            return 1;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        boolean armed = false;
        if (Math.abs(dx) > Math.abs(dy)) {
            float y = y1, offset = (float) dy / Math.abs(dx);
            dx = (dx < 0) ? -1 : 1;
            for (int x = x1; x != x2; x += dx) {
                if (armed && garden.isOccupied(x, (int) y)) {
                    return Math.abs(x - x1);
                } else {
                    armed = true;
                }
                y += offset;
            }
            return Math.abs(x2 - x1);
        } else {
            float x = x1, offset = (float) dx / Math.abs(dy);
            dy = (dy < 0) ? -1 : 1;
            for (int y = y1; y != y2; y += dy) {
                if (armed && garden.isOccupied((int) x, y)) {
                    return Math.abs(y - y1);
                } else {
                    armed = true;
                }
                x += offset;
            }
            return Math.abs(y2 - y1);
        }
    }
    
    protected final void drawLine(int x1, int y1, int x2, int y2, int maxLength, int category) {
        if ((x1 == x2) && (y1 == y2)) {
            garden.setCategory(x1, y1, category);
            return;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        if (Math.abs(dx) > Math.abs(dy)) {
            float y = y1, offset = (float) dy / Math.abs(dx);
            dx = (dx < 0) ? -1 : 1;
            for (int x = x1; (x != x2) && (maxLength > 0); x += dx) {
                garden.setCategory(x, (int) y, category);
                y += offset;
                maxLength--;
            }
        } else {
            float x = x1, offset = (float) dx / Math.abs(dy);
            dy = (dy < 0) ? -1 : 1;
            for (int y = y1; (y != y2) && (maxLength > 0); y += dy) {
                garden.setCategory((int) x, y, category);
                maxLength--;
                x += offset;
            }
        }
    }
    
    protected final void drawLine(Point location1, Point location2, int category) {
        drawLine(location1.x, location1.y, location2.x, location2.y, Integer.MAX_VALUE, category);
    }
    
    protected final void fill(int x1, int y1, int width, int height, int category) {
        for (int x = x1; x < x1 + width; x++) {
            for (int y = y1; y < y1 + height; y++) {
                garden.setCategory(x, y, category);
            }
        }
    }
    
    protected final void doAlongLine(int x1, int y1, int x2, int y2, Task task) {
        doAlongLine(x1, y1, x2, y2, task, 1);
    }
    
    protected final void doAlongLine(int x1, int y1, int x2, int y2, Task task, int every) {
        doAlongLine(x1, y1, x2, y2, Integer.MAX_VALUE, task, every);
    }
    
    protected final void doAlongLine(int x1, int y1, int x2, int y2, boolean stopWhenOccupied, Task task) {
        doAlongLine(x1, y1, x2, y2, stopWhenOccupied, task, 1);
    }
    
    protected final void doAlongLine(int x1, int y1, int x2, int y2, boolean stopWhenOccupied, Task task, int every) {
        int length = stopWhenOccupied ? scanLine(x1, y1, x2, y2) : Integer.MAX_VALUE;
        doAlongLine(x1, y1, x2, y2, length, task, every);
    }
    
    protected final void doAlongLine(int x1, int y1, int x2, int y2, int maxLength, Task task) {
        doAlongLine(x1, y1, x2, y2, maxLength, task, 1);
    }
    
    protected final void doAlongLine(int x1, int y1, int x2, int y2, int maxLength, Task task, int every) {
        if ((x1 == x2) && (y1 == y2)) {
            task.perform(x1, y1);
            return;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        int count = every / 2;
        if (Math.abs(dx) > Math.abs(dy)) {
            float y = y1, offset = (float) dy / Math.abs(dx);
            dx = (dx < 0) ? -1 : 1;
            for (int x = x1; (x != x2) && (maxLength > 0); x += dx) {
                if (((count % every) == 0) && (! task.perform(x, (int) y))) {
                    return;
                }
                y += offset;
                maxLength--;
                count++;
            }
        } else {
            float x = x1, offset = (float) dx / Math.abs(dy);
            dy = (dy < 0) ? -1 : 1;
            for (int y = y1; (y != y2) && (maxLength > 0); y += dy) {
                if (((count % every) == 0) && (! task.perform((int) x, y))) {
                    return;
                }
                x += offset;
                maxLength--;
                count++;
            }
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Reassign unique ID. Don't store it, because that might cause ID
        // clashes when adding seeds to a world with existing seeds. This will
        // cause the hash code to change, but that should be no problem because
        // the collection classes' serialized formats are independent of the
        // hash codes of their contents
        id = nextId++;
    }
    
    public final Point3i location;
    public transient Garden garden;
    public final Seed parent;
    public final int category;
    public final long seed;
    
    /**
     * Unique ID field used to keep equals() and hashcode() working even when
     * the instance is cloned. Looking at the location and possibly the type
     * instead seems like it should be good enough, but it is hard to predict
     * whether in the future there might be a reason to have multiple seeds at
     * the same location, possibly even of the same type.
     */
    private transient long id;
    private int germinationTime;
    private boolean sprouted = false;
    
    private static long nextId = 1;
    
    private static final Random staticRandom = new Random();
    
    private static final long serialVersionUID = 1L;
    
    public interface Task {
        boolean perform(int x, int y);
    }
}