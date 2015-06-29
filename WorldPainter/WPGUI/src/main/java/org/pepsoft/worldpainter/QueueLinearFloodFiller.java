package org.pepsoft.worldpainter;

//Original algorithm by J. Dunlap http://www.codeproject.com/KB/GDI-plus/queuelinearfloodfill.aspx
//Java port by Owen Kaluza
// Adapted for WorldPainter by Pepijn Schmitz on 28-3-2011
import java.awt.Window;
import java.util.BitSet;
import java.util.Queue;
import java.util.LinkedList;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import static org.pepsoft.worldpainter.Constants.*;

public class QueueLinearFloodFiller {
    // Dimension to flood

    private final Dimension dimension;
    // Level to flood to
    private final int waterLevel;
    // Whether to flood with lava instead of water
    private final boolean floodWithLava;
    // Whether to remove a layer of material instead of adding it
    private final boolean undo;
    //cached image properties
    private final int width, height, offsetX, offsetY;
    //internal, initialized per fill
    protected BitSet blocksChecked;
    //Queue of floodfill ranges
    protected Queue<FloodFillRange> ranges;

    public QueueLinearFloodFiller(Dimension dimension, int waterLevel, boolean floodWithLava, boolean undo) {
        this.dimension = dimension;
        this.waterLevel = waterLevel;
        this.floodWithLava = floodWithLava;
        this.undo = undo;
        width = dimension.getWidth() * TILE_SIZE;
        height = dimension.getHeight() * TILE_SIZE;
        offsetX = dimension.getLowestX() * TILE_SIZE;
        offsetY = dimension.getLowestY() * TILE_SIZE;
    }

    public Dimension getDimension() {
        return dimension;
    }

    protected void prepare() {
        //Called before starting flood-fill
        blocksChecked = new BitSet(width * height);
        ranges = new LinkedList<>();
    }

    // Fills the specified point on the bitmap with the currently selected fill color.
    // int x, int y: The starting coords for the fill
    public boolean floodFill(int x, int y, Window parent) {
        // Normalise coordinates (the algorithm needs them to always be
        // positive)
        x -= offsetX;
        y -= offsetY;
        
        //Setup
        prepare();

        long start = System.currentTimeMillis();

        //***Do first call to floodfill.
        linearFill(x, y);

        //***Call floodfill routine while floodfill ranges still exist on the queue
        FloodFillRange range;
        while (ranges.size() > 0) {
            //**Get Next Range Off the Queue
            range = ranges.remove();
            processRange(range);

            long lap = System.currentTimeMillis();
            if ((lap - start) > 2000) {
                // We're taking more than two seconds. Do the rest in the
                // background and show a progress dialog so the user can cancel
                // the operation
                if (ProgressDialog.executeTask(parent, new ProgressTask<Dimension>() {
                    @Override
                    public String getName() {
                        return undo ? "Draining" : "Flooding";
                    }

                    @Override
                    public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                        synchronized (dimension) {
                            //***Call floodfill routine while floodfill ranges still exist on the queue
                            FloodFillRange range;
                            while (ranges.size() > 0) {
                                //**Get Next Range Off the Queue
                                range = ranges.remove();
                                processRange(range);
                                progressReceiver.checkForCancellation();
                            }
                            return dimension;
                        }
                    }
                }) == null) {
                    // Operation cancelled
                    return false;
                }
                return true;
            }
        }

        return true;
    }

    private void processRange(FloodFillRange range) {
        //**Check Above and Below Each block in the Floodfill Range
        int downPxIdx = (width * (range.Y + 1)) + range.startX;
        int upPxIdx = (width * (range.Y - 1)) + range.startX;
        int upY = range.Y - 1;//so we can pass the y coord by ref
        int downY = range.Y + 1;
        for (int i = range.startX; i <= range.endX; i++) {
            //*Start Fill Upwards
            //if we're not above the top of the bitmap and the block above this one is within the color tolerance
            if (range.Y > 0 && (!blocksChecked.get(upPxIdx)) && checkBlock(upPxIdx)) {
                linearFill(i, upY);
            }

            //*Start Fill Downwards
            //if we're not below the bottom of the bitmap and the block below this one is within the color tolerance
            if (range.Y < (height - 1) && (!blocksChecked.get(downPxIdx)) && checkBlock(downPxIdx)) {
                linearFill(i, downY);
            }
            downPxIdx++;
            upPxIdx++;
        }
    }

    // Finds the furthermost left and right boundaries of the fill area
    // on a given y coordinate, starting from a given x coordinate, filling as it goes.
    // Adds the resulting horizontal range to the queue of floodfill ranges,
    // to be processed in the main loop.
    //
    // int x, int y: The starting coords
    protected void linearFill(int x, int y) {
        //***Find Left Edge of Color Area
        int lFillLoc = x; //the location to check/fill on the left
        int pxIdx = (width * y) + x;
        int origPxIdx = pxIdx;
        while (true) {
            if (undo) {
                //**remove a layer of material
                dimension.setWaterLevelAt(offsetX + x + pxIdx - origPxIdx, offsetY + y, waterLevel - 1);
            } else {
                //**flood
                dimension.setWaterLevelAt(offsetX + x + pxIdx - origPxIdx, offsetY + y, waterLevel);
                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, offsetX + x + pxIdx - origPxIdx, offsetY + y, floodWithLava);
            }
            //**indicate that this block has already been checked and filled
            blocksChecked.set(pxIdx);
            //**de-increment
            lFillLoc--;     //de-increment counter
            pxIdx--;        //de-increment block index
            //**exit loop if we're at edge of bitmap or color area
            if (lFillLoc < 0 || blocksChecked.get(pxIdx) || !checkBlock(pxIdx)) {
                break;
            }
        }
        lFillLoc++;

        //***Find Right Edge of Color Area
        int rFillLoc = x; //the location to check/fill on the left
        pxIdx = (width * y) + x;
        origPxIdx = pxIdx;
        while (true) {
            if (undo) {
                //**remove a layer of material
                dimension.setWaterLevelAt(offsetX + x + pxIdx - origPxIdx, offsetY + y, waterLevel - 1);
            } else {
                //**flood
                dimension.setWaterLevelAt(offsetX + x + pxIdx - origPxIdx, offsetY + y, waterLevel);
                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, offsetX + x + pxIdx - origPxIdx, offsetY + y, floodWithLava);
            }
            //**indicate that this block has already been checked and filled
            blocksChecked.set(pxIdx);
            //**increment
            rFillLoc++;     //increment counter
            pxIdx++;        //increment block index
            //**exit loop if we're at edge of bitmap or color area
            if (rFillLoc >= width || blocksChecked.get(pxIdx) || !checkBlock(pxIdx)) {
                break;
            }
        }
        rFillLoc--;

        //add range to queue
        FloodFillRange r = new FloodFillRange(lFillLoc, rFillLoc, y);
        ranges.offer(r);
    }

    //Sees if a block should be flooded (or unflooded)
    protected boolean checkBlock(int px) {
        int y = px / width;
        int x = px % width;
        if (dimension.getBitLayerValueAt(org.pepsoft.worldpainter.layers.Void.INSTANCE, offsetX + x, offsetY + y)) {
            return false;
        } else {
            int height = dimension.getIntHeightAt(offsetX + x, offsetY + y);
            if (undo) {
                return (height != -1)
                    && (dimension.getWaterLevelAt(offsetX + x, offsetY + y) >= waterLevel)
                    && (height < waterLevel);
            } else {
                return (height != -1)
                    && (waterLevel > height)
                    && ((waterLevel > dimension.getWaterLevelAt(offsetX + x, offsetY + y))
                        || (floodWithLava != dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, offsetX + x, offsetY + y)));
            }
        }
    }

    // Represents a linear range to be filled and branched from.
    protected class FloodFillRange {

        public int startX;
        public int endX;
        public int Y;

        public FloodFillRange(int startX, int endX, int y) {
            this.startX = startX;
            this.endX = endX;
            this.Y = y;
        }
    }
}