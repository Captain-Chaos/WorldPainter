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
import org.pepsoft.worldpainter.layers.GardenCategory;
import org.pepsoft.worldpainter.util.GeometryUtil;

/**
 * A seed for planting in a {@link Garden} for creating complex random
 * structures that follow rules. A seed has a germination time, which is the
 * number of ticks after planting when it will try to sprout. When it germinates
 * it checks whether there is room to sprout and if so does so, optionally
 * planting new seeds in the process.
 *
 * <p>Seeds may have parents and form a hierarchy, in which case parents will
 * always sprout before children (and therefore children will not sprout if
 * their parent hasn't), and parents will also be exported before their
 * children during the export process.
 *
 * @author pepijn
 */
public abstract class Seed implements Serializable, org.pepsoft.util.undo.Cloneable<Seed> {
    /**
     * Create a new seed.
     *
     * @param garden The garden in which the seed will be planted.
     * @param seed The random seed which it may use for seeding pseudo random
     *             number generators.
     * @param parent The parent of this seed, if any. May be <code>null</code>.
     * @param location The location of the seed. The z coordinate may be -1,
     *                 meaning "on the surface", or it may be zero or higher to
     *                 indicate a specific height.
     * @param germinationTime The number of ticks after planting when the seed
     *                        will germinate. If positive a random deviation
     *                        will be applied. If negative it will be made
     *                        positive <em>without</em> applying a random
     *                        deviation.
     * @param category The category of seed, as one of the
     *                 <code>CATEGORY_*</code> constants in the {@link
     *                 GardenCategory} class.
     */
    public Seed(Garden garden, long seed, Seed parent, Point3i location, int germinationTime, int category) {
        this.id = nextId++;
        this.garden = garden;
        this.parent = parent;
        this.location = location;
        if (germinationTime == 0) {
            this.germinationTime = 0;
            sprouted = true;
        } else if (germinationTime < 0) {
            this.germinationTime = -germinationTime;
        } else {
            this.germinationTime = Math.max(1, (int) (germinationTime * (staticRandom.nextDouble() + 0.6)));
        }
        this.category = category;
        this.seed = seed;
    }

    /**
     * Get the garden in which the seed is planted.
     *
     * @return The garden in which the seed is planted.
     */
    public final Garden getGarden() {
        return garden;
    }

    /**
     * Get this seed's parent, if any,
     *
     * @return This seed's parent, or <code>null</code> if it has none.
     */
    public final Seed getParent() {
        return parent;
    }

    /**
     * Get this seed's location. The z coordinate may be -1, meaning "on the
     * surface", or it may be zero or higher to indicate a specific height.
     *
     * @return This seed's location.
     */
    public final Point3i getLocation() {
        return location;
    }

    /**
     * Tick the seed over. This involves determining whether it is time to
     * sprout yet, and if so, attempt to sprout. It is time to sprout when the
     * germination time has passed, and the parent (if it exits) has sprouted.
     * If sprouting fails, the seed is removed from the garden.
     */
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
        } else if (germinationTime > 0) {
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

    /**
     * Determine whether the seed is "alive", i.e. has not tried to sprout yet.
     *
     * @return <code>true</code> if the seed is dead, i.e. it has attempted to
     *     sprout, which either succeeded or failed.
     */
    public final boolean isFinished() {
        return germinationTime <= 0;
    }

    /**
     * Determine whether the seed has successfully sprouted.
     *
     * @return <code>true</code> if the seed has successfully sprouted.
     */
    public final boolean isSprouted() {
        return sprouted;
    }

    /**
     * "Kill" the seed, i.e. stop if from ever germinating.
     */
    public final void neutralise() {
        germinationTime = 0;
    }

    /**
     * Perform the first export pass. The seed may make what changes it wishes
     * to the passed in Minecraft map. At this point the terrain has been
     * rendered, all custom layers have been exported, and if the seed has a
     * parent, its first (but not second) export pass has been executed.
     *
     * <p>For buildings it may be advantageous to export the exteriors in the
     * first pass.
     *
     * @param dimension The dimension which is being exported.
     * @param tile The tile which is being exported. Note that the seed does
     *             <em>not</em> have to constrain its changes to the area of the
     *             tile.
     * @param minecraftWorld The Minecraft map to which the seed should export
     *                       itself.
     */
    public void buildFirstPass(Dimension dimension, Tile tile, MinecraftWorld minecraftWorld) {
        // Do nothing
    }
    
    /**
     * Perform the second export pass. The seed may make what changes it wishes
     * to the passed in Minecraft map. At this point the terrain has been
     * rendered, all custom layers have been exported, the first pass of all
     * seeds has been executed, and if the seed has a parent, its second export
     * pass has been executed.
     *
     * <p>For buildings it may be advantageous to export the interiors in the
     * second pass.
     *
     * @param dimension The dimension which is being exported.
     * @param tile The tile which is being exported. Note that the seed does
     *             <em>not</em> have to constrain its changes to the area of the
     *             tile.
     * @param minecraftWorld The Minecraft map to which the seed should export
     *                       itself.
     */
    public void buildSecondPass(Dimension dimension, Tile tile, MinecraftWorld minecraftWorld) {
        // Do nothing
    }

    /**
     * Transform the seed's location and rotation according to some coordinate
     * transform.
     *
     * @param rotation The coordinate transform to apply to the seed's location
     *                 and rotation.
     */
    public void transform(CoordinateTransform rotation) {
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

    /**
     * Utility method for setting the seed category on the garden along a
     * straight line.
     *
     * @param location1 The start location of the line.
     * @param location2 The end location of the line.
     * @param diameter The thickness of the line.
     * @param stopWhenOccupied Whether to stop drawing the line upon reaching a
     *                         location which is already occupied.
     * @param category The seed category with which to mark the line, as one of
     *                 the <code>CATEGORY_*</code> constants in the
     *                 {@link GardenCategory} class.
     */
    protected final void drawLine(Point location1, Point location2, int diameter, boolean stopWhenOccupied, int category) {
        int length = Integer.MAX_VALUE;
        if (stopWhenOccupied) {
            length = scanLine(location1.x, location1.y, location2.x, location2.y);
        }
        drawLine(location1, location2, diameter, length, category);
    }
    
    /**
     * Utility method for setting the seed category on the garden along a
     * straight line.
     *
     * @param location1 The start location of the line.
     * @param location2 The end location of the line.
     * @param diameter The thickness of the line.
     * @param stopWhenOccupied Whether to stop drawing the line upon reaching a
     *                         location which is already occupied.
     * @param category The seed category with which to mark the line, as one of
     *                 the <code>CATEGORY_*</code> constants in the
     *                 {@link GardenCategory} class.
     */
    protected final void drawLine(Point3i location1, Point3i location2, int diameter, boolean stopWhenOccupied, int category) {
        int length = Integer.MAX_VALUE;
        if (stopWhenOccupied) {
            length = scanLine(location1.x, location1.y, location2.x, location2.y);
        }
        drawLine(location1, location2, diameter, length, category);
    }
    
    /**
     * Utility method for setting the seed category on the garden along a
     * straight line.
     *
     * @param location1 The start location of the line.
     * @param location2 The end location of the line.
     * @param diameter The thickness of the line.
     * @param maxLength The maximum lengh of the line. If this is shorter than
     *                  the distance between <code>location1</code> and
     *                  <code>location2</code>, only the first
     *                  <code>maxLength</code> blocks of the line will be
     *                  painted.
     * @param category The seed category with which to mark the line, as one of
     *                 the <code>CATEGORY_*</code> constants in the
     *                 {@link GardenCategory} class.
     */
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
    
    /**
     * Utility method for setting the seed category on the garden along a
     * straight line.
     *
     * @param location1 The start location of the line.
     * @param location2 The end location of the line.
     * @param diameter The thickness of the line.
     * @param maxLength The maximum lengh of the line. If this is shorter than
     *                  the distance between <code>location1</code> and
     *                  <code>location2</code>, only the first
     *                  <code>maxLength</code> blocks of the line will be
     *                  painted.
     * @param category The seed category with which to mark the line, as one of
     *                 the <code>CATEGORY_*</code> constants in the
     *                 {@link GardenCategory} class.
     */
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
    
    /**
     * Utility method for testing how much of a straight line is unoccupied.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @return The number of blocks along the line, from the start, that are
     *     unoccupied. Equal to the length of the line if it is entirely
     *     unoccupied. <strong>Note</strong> that if the <em>start</em> of the
     *     line is already occupied, that location is not counted as occupied,
     *     nor are any subsequent occupied locations until an unoccupied
     *     location is encountered.
     */
    protected final int scanLine(int x1, int y1, int x2, int y2) {
        if ((x1 == x2) && (y1 == y2)) {
            return 1;
        }
        boolean[] armed = {false};
        int[] distance = {0};
        GeometryUtil.visitLine(x1, y1, x2, y2, (x, y, d) -> {
            if (armed[0] && garden.isOccupied(x, y)) {
                return false;
            } else {
                armed[0] = true;
            }
            distance[0] = (int) d;
            return true;
        });
        return distance[0];
    }
    
    /**
     * Utility method for setting the seed category on the garden along a
     * straight line.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param maxLength The maximum lengh of the line. If this is shorter than
     *                  the distance between <code>location1</code> and
     *                  <code>location2</code>, only the first
     *                  <code>maxLength</code> blocks of the line will be
     *                  painted.
     * @param category The seed category with which to mark the line, as one of
     *                 the <code>CATEGORY_*</code> constants in the
     *                 {@link GardenCategory} class.
     */
    protected final void drawLine(int x1, int y1, int x2, int y2, int maxLength, int category) {
        GeometryUtil.visitLine(x1, y1, x2, y2, (x, y, d) -> {
            if (d <= maxLength) {
                garden.setCategory(x, y, category);
                return true;
            } else {
                return false;
            }
        });
    }
    
    /**
     * Utility method for setting the seed category on the garden along a
     * straight line, regardless of what is already there.
     *
     * @param location1 The start location of the line.
     * @param location2 The end location of the line.
     * @param category The seed category with which to mark the line, as one of
     *                 the <code>CATEGORY_*</code> constants in the
     *                 {@link GardenCategory} class.
     */
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
    
    /**
     * Utility method for performing some arbitrary task along a straight line,
     * regardless of whether it is occupied or not.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param task The task to perform at each loction along the specified line.
     */
    protected final void doAlongLine(int x1, int y1, int x2, int y2, Task task) {
        GeometryUtil.visitLine(x1, y1, x2, y2, (x, y, d) -> task.perform(x, y));
    }
    
    /**
     * Utility method for performing some arbitrary task at some interval along
     * a straight line, regardless of whether it is occupied or not.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param task The task to perform at each specified interval along the
     *             specified line.
     * @param every The interval between performances of the specified task.
     */
    protected final void doAlongLine(int x1, int y1, int x2, int y2, Task task, int every) {
        GeometryUtil.visitLine(x1, y1, x2, y2, every, (x, y, d) -> task.perform(x, y));
    }
    
    /**
     * Utility method for performing some arbitrary task along a straight line.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param stopWhenOccupied Whether to stop performing the task upon reaching
     *                         a location which is occupied.
     * @param task The task to perform at each location along the specified line.
     */
    protected final void doAlongLine(int x1, int y1, int x2, int y2, boolean stopWhenOccupied, Task task) {
        doAlongLine(x1, y1, x2, y2, stopWhenOccupied, task, 1);
    }
    
    /**
     * Utility method for performing some arbitrary task at some interval along
     * a straight line.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param stopWhenOccupied Whether to stop performing the task upon reaching
     *                         a location which is occupied.
     * @param task The task to perform at each specified interval along the
     *             specified line.
     * @param every The interval between performances of the specified task.
     */
    protected final void doAlongLine(int x1, int y1, int x2, int y2, boolean stopWhenOccupied, Task task, int every) {
        int length = stopWhenOccupied ? scanLine(x1, y1, x2, y2) : Integer.MAX_VALUE;
        doAlongLine(x1, y1, x2, y2, length, task, every);
    }
    
    /**
     * Utility method for performing some arbitrary task along a straight line.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param maxLength The maximum length for which to perform the task. If
     *                  this is shorter than the distance between
     *                  <code>location1</code> and <code>location2</code>, the
     *                  task will only be performed for the first
     *                  <code>maxLength</code> blocks of the line.
     * @param task The task to perform at each location along the specified
     *             line.
     */
    protected final void doAlongLine(int x1, int y1, int x2, int y2, int maxLength, Task task) {
        GeometryUtil.visitLine(x1, y1, x2, y2, (x, y, d) -> (d <= maxLength) && task.perform(x, y));
    }
    
    /**
     * Utility method for performing some arbitrary task at some interval along
     * a straight line.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param maxLength The maximum length for which to perform the task. If
     *                  this is shorter than the distance between
     *                  <code>location1</code> and <code>location2</code>, the
     *                  task will only be performed for the first
     *                  <code>maxLength</code> blocks of the line.
     * @param task The task to perform at each specified interval along the
     *             specified line.
     * @param every The interval between performances of the specified task.
     */
    protected final void doAlongLine(int x1, int y1, int x2, int y2, int maxLength, Task task, int every) {
        GeometryUtil.visitLine(x1, y1, x2, y2, every, (x, y, d) -> (d <= maxLength) && task.perform(x, y));
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

    @FunctionalInterface
    public interface Task {
        boolean perform(int x, int y);
    }
}