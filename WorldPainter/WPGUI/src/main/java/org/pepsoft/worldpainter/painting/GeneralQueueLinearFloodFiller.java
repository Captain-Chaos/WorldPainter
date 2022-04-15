package org.pepsoft.worldpainter.painting;

//Original algorithm by J. Dunlap http://www.codeproject.com/KB/GDI-plus/queuelinearfloodfill.aspx
//Java port by Owen Kaluza
// Adapted for WorldPainter by Pepijn Schmitz on 28-3-2011

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;

import java.awt.*;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;

public class GeneralQueueLinearFloodFiller {
    // Dimension to flood

    //cached image properties
    private int width, height, offsetX, offsetY;
    //internal, initialized per fill
    private BitSet blocksChecked;
    //Queue of floodfill ranges
    private Queue<FloodFillRange> ranges;
    private boolean boundsTooLarge, boundsHit;
    /**
     * The actual logic for determining what should be filled, and what it should be filled with.
     */
    private final FillMethod fillMethod;

    private static final int MAX_INT_SQUARE_SIDE = 46340;

    public GeneralQueueLinearFloodFiller(FillMethod fillMethod) {
        this.fillMethod = fillMethod;
    }

    // Fills the specified point on the bitmap with the currently selected fill color.
    // int x, int y: The starting coords for the fill
    public boolean floodFill(int x, int y, Window parent) {
        final Rectangle largestPossibleFloodArea = new Rectangle(x - MAX_INT_SQUARE_SIDE / 2, y - MAX_INT_SQUARE_SIDE / 2, MAX_INT_SQUARE_SIDE, MAX_INT_SQUARE_SIDE);
        final Rectangle fillBounds = fillMethod.getBounds();
        if (! largestPossibleFloodArea.contains(fillBounds)) {
            boundsTooLarge = true;
            final Rectangle floodArea = largestPossibleFloodArea.intersection(fillBounds);
            width = floodArea.width;
            height = floodArea.height;
            offsetX = floodArea.x;
            offsetY = floodArea.y;
        } else {
            width = fillBounds.width;
            height = fillBounds.height;
            offsetX = fillBounds.x;
            offsetY = fillBounds.y;
        }

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
                return ProgressDialog.executeTask(parent, new ProgressTask<FillMethod>() {
                    @Override
                    public String getName() {
                        return fillMethod.getDescription();
                    }

                    @Override
                    public FillMethod execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                        //***Call floodfill routine while floodfill ranges still exist on the queue
                        FloodFillRange range;
                        while (ranges.size() > 0) {
                            //**Get Next Range Off the Queue
                            range = ranges.remove();
                            processRange(range);
                            progressReceiver.checkForCancellation();
                        }
                        return fillMethod;
                    }
                }) != null;
            }
        }

        return true;
    }

    /**
     * Indicates whether the fill operation <em>may</em> have been incomplete, because the bounds specified by the
     * {@link FillMethod} exceeded the bounds that this class can flood-fill due to a limitation of Java, and the fill
     * operation hit the intersection of those bounds.
     *
     * @return {@code true} if the fill operation may have been incomplete.
     */
    public boolean isBoundsHit() {
        return boundsTooLarge && boundsHit;
    }

    private synchronized void prepare() {
        //Called before starting flood-fill
        blocksChecked = new BitSet(width * height);
        ranges = new LinkedList<>();
    }

    private synchronized void processRange(FloodFillRange range) {
        //**Check Above and Below Each block in the Floodfill Range
        int downPxIdx = (width * (range.Y + 1)) + range.startX;
        int upPxIdx = (width * (range.Y - 1)) + range.startX;
        int upY = range.Y - 1;//so we can pass the y coord by ref
        int downY = range.Y + 1;
        for (int i = range.startX; i <= range.endX; i++) {
            //*Start Fill Upwards
            //if we're not above the top of the bitmap and the block above this one is not filled
            if (range.Y > 0) {
                if ((! blocksChecked.get(upPxIdx)) && ! fillMethod.isBoundary(offsetX + i, offsetY + upY)) {
                    linearFill(i, upY);
                }
            } else {
                boundsHit = true;
            }

            //*Start Fill Downwards
            //if we're not below the bottom of the bitmap and the block below this one is not filled
            if (range.Y < (height - 1)) {
                if ((! blocksChecked.get(downPxIdx)) && ! fillMethod.isBoundary(offsetX + i, offsetY + downY)) {
                    linearFill(i, downY);
                }
            } else {
                boundsHit = true;
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
    private synchronized void linearFill(int x, int y) {
        //***Find Left Edge of Color Area
        int lFillLoc = x; //the location to check/fill on the left
        int pxIdx = (width * y) + x;
        do {
            fillMethod.fill(offsetX + lFillLoc, offsetY + y);
            //**indicate that this block has already been checked and filled
            blocksChecked.set(pxIdx);
            //**de-increment
            lFillLoc--;     //de-increment counter
            pxIdx--;        //de-increment block index
            //**exit loop if we're at edge of bitmap or color area
        } while ((lFillLoc >= 0) && (! blocksChecked.get(pxIdx)) && (! fillMethod.isBoundary(offsetX + lFillLoc, offsetY + y)));
        if (lFillLoc == 0) {
            boundsHit = true;
        }
        lFillLoc++;

        //***Find Right Edge of Color Area
        int rFillLoc = x; //the location to check/fill on the left
        pxIdx = (width * y) + x;
        do {
            fillMethod.fill(offsetX + rFillLoc, offsetY + y);
            //**indicate that this block has already been checked and filled
            blocksChecked.set(pxIdx);
            //**increment
            rFillLoc++;     //increment counter
            pxIdx++;        //increment block index
            //**exit loop if we're at edge of bitmap or color area
        } while ((rFillLoc < width) && (! blocksChecked.get(pxIdx)) && (! fillMethod.isBoundary(offsetX + rFillLoc, offsetY + y)));
        if (rFillLoc == width - 1) {
            boundsHit = true;
        }
        rFillLoc--;

        //add range to queue
        FloodFillRange r = new FloodFillRange(lFillLoc, rFillLoc, y);
        ranges.offer(r);
    }

    // Represents a linear range to be filled and branched from.
    static class FloodFillRange {

        public int startX;
        public int endX;
        public int Y;

        public FloodFillRange(int startX, int endX, int y) {
            this.startX = startX;
            this.endX = endX;
            this.Y = y;
        }
    }

    public interface FillMethod {
        /**
         * Get a short human readable description of the operation. May be shown to the user if the operation takes a
         * long time.
         *
         * @return A short human readable description of the operation.
         */
        String getDescription();

        /**
         * Indicates the area to which the fill operation should be constrained.
         *
         * @return The area to which the fill operation should be constrained.
         */
        Rectangle getBounds();

        /**
         * Indicates whether the specified coordinates are a boundary beyond which the fill operation should not
         * proceed, for example because it is already "filled", whatever that means.
         *
         * @param x The X coordinate to check.
         * @param y The Y coordinate to check.
         * @return {@code true} if the specified coordinates are a boundary beyond which the fill operation should
         * not proceed.
         */
        boolean isBoundary(int x, int y);

        /**
         * "Fills" the specified coordinates, whatever that means.
         *
         * @param x The X coordinate to "fill".
         * @param y The Y coordinate to "fill".
         */
        void fill(int x, int y);
    }
}