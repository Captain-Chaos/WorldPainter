package org.pepsoft.worldpainter.dynmap;

import org.dynmap.Color;
import org.dynmap.DynmapWorld;
import org.dynmap.MapTypeState;
import org.dynmap.hdmap.*;
import org.dynmap.markers.impl.MarkerAPIImpl;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.utils.*;

import java.awt.*;
import java.awt.image.*;

/**
 * A dynmap based isometric 3D renderer of HDMap tiles. Largely copied from the
 * IsoHDPerspective class from dynmap. <em>Not</em> thread-safe; the intention
 * is to create an instance per thread.
 *
 * <p>Created by Pepijn Schmitz on 05-06-15.
 */
class DynMapRenderer {
    DynMapRenderer(HDPerspective perspective, HDMap map, int scale, double inclination, double azimuth) {
        this.perspective = perspective;
        this.map = map;
        basemodscale = scale;
        /* Generate transform matrix for world-to-tile coordinate mapping */
        /* First, need to fix basic coordinate mismatches before rotation - we want zero azimuth to have north to top
         * (world -X -> tile +Y) and east to right (world -Z to tile +X), with height being up (world +Y -> tile +Z)
         */
        Matrix3D transform = new Matrix3D(0.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0);
        /* Next, rotate world counterclockwise around Z axis by azimuth angle */
        transform.rotateXY(180-azimuth);
        /* Next, rotate world by (90-inclination) degrees clockwise around +X axis */
        transform.rotateYZ(90.0-inclination);
        /* Finally, shear along Z axis to normalize Z to be height above map plane */
        transform.shearZ(0, Math.tan(Math.toRadians(90.0-inclination)));
        /* And scale Z to be same scale as world coordinates, and scale X and Y based on setting */
        transform.scale(basemodscale, basemodscale, Math.sin(Math.toRadians(inclination)));
        world_to_map = transform;
        /* Now, generate map to world transform, by doing opposite actions in reverse order */
        transform = new Matrix3D();
        transform.scale(1.0/basemodscale, 1.0/basemodscale, 1/Math.sin(Math.toRadians(inclination)));
        transform.shearZ(0, -Math.tan(Math.toRadians(90.0-inclination)));
        transform.rotateYZ(-(90.0-inclination));
        transform.rotateXY(-180+azimuth);
        Matrix3D coordswap = new Matrix3D(0.0, -1.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0);
        transform.multiply(coordswap);
        map_to_world = transform;

        // Create buffers
        argb_buf = new int[TILE_WIDTH * TILE_HEIGHT];
        /* Create integer-base data buffer */
        DataBuffer db = new DataBufferInt (argb_buf, TILE_WIDTH * TILE_HEIGHT);
        /* Create writable raster */
        WritableRaster raster = Raster.createPackedRaster(db, TILE_WIDTH, TILE_HEIGHT, TILE_WIDTH, BAND_MASKS, null);
        /* RGB color model */
        ColorModel color_model = ColorModel.getRGBdefault();
        /* Create buffered image */
        buf_img = new BufferedImage (color_model, raster, false, null);
    }

    /**
     * Render the tile. Note that the returned image is only valid until the
     * next invocation of this method.
     *
     * @param cache The chunk cache from which to retrieve the world data to
     *              render.
     * @param tile The tile to render.
     * @return The rendered tile. <strong>Note:</strong> only valid until the
     * next invocation of this method!
     */
    BufferedImage render(MapChunkCache cache, HDMapTile tile) {
        Color rslt = new Color();
        MapIterator mapiter = cache.getIterator(0, 0, 0);
        DynmapWorld world = tile.getDynmapWorld();
        int scaled = 0;
        if ((tile.boostzoom > 0) && MarkerAPIImpl.testTileForBoostMarkers(cache.getWorld(), perspective, tile.tx * TILE_WIDTH, tile.ty * TILE_HEIGHT, TILE_WIDTH)) {
            scaled = tile.boostzoom;
        }
        int sizescale = 1 << scaled;

        /* Build shader state object for each shader */
        HDShaderState shaderstate = map.getShader().getStateInstance(map, cache, mapiter, sizescale * basemodscale);
        /* Check if nether world */
        boolean isnether = world.isNether();

        // Mark the tiles we're going to render as validated
        MapTypeState mts = world.getMapState(map);
        if (mts != null) {
            mts.validateTile(tile.tx, tile.ty);
        }
        /* Create perspective state object */
        OurPerspectiveState ps = new OurPerspectiveState(mapiter, isnether, scaled);

        ps.top = new Vector3D();
        ps.bottom = new Vector3D();
        ps.direction = new Vector3D();
        double xbase = tile.tx * TILE_WIDTH;
        double ybase = tile.ty * TILE_HEIGHT;
        boolean shaderdone[] = new boolean[1];
        double height = maxheight;
        if(height < 0) {    /* Not set - assume world height - 1 */
            if (isnether)
                height = 127;
            else
                height = tile.getDynmapWorld().worldheight - 1;
        }

        for(int x = 0; x < TILE_WIDTH * sizescale; x++) {
            ps.px = x;
            for(int y = 0; y < TILE_HEIGHT * sizescale; y++) {
                ps.top.x = ps.bottom.x = xbase + ((double)x)/sizescale + 0.5;    /* Start at center of pixel at Y=height+0.5, bottom at Y=-0.5 */
                ps.top.y = ps.bottom.y = ybase + ((double)y)/sizescale + 0.5;
                ps.top.z = height + 0.5; ps.bottom.z = minheight - 0.5;
                map_to_world.transform(ps.top);            /* Transform to world coordinates */
                map_to_world.transform(ps.bottom);
                ps.direction.set(ps.bottom);
                ps.direction.subtract(ps.top);
                ps.py = y / sizescale;
                shaderstate.reset(ps);
                ps.raytrace(cache, shaderstate, shaderdone);
                if(! shaderdone[0]) {
                    shaderstate.rayFinished(ps);
                } else {
                    shaderdone[0] = false;
                }
                shaderstate.getRayColor(rslt, 0);
                int c_argb = rslt.getARGB();
                argb_buf[(TILE_HEIGHT *sizescale-y-1)* TILE_WIDTH *sizescale + x] = c_argb;
            }
        }

        return buf_img;
    }

    public Rectangle getTileCoords(int minx, int miny, int minz, int maxx, int maxy, int maxz) {
        Vector3D blocks[] = new Vector3D[] { new Vector3D(), new Vector3D() };
        blocks[0].x = minx;
        blocks[0].y = miny;
        blocks[0].z = minz;
        blocks[1].x = maxx;
        blocks[1].y = maxy;
        blocks[1].z = maxz;

        Vector3D corner = new Vector3D();
        Vector3D tcorner = new Vector3D();
        int mintilex = Integer.MAX_VALUE;
        int maxtilex = Integer.MIN_VALUE;
        int mintiley = Integer.MAX_VALUE;
        int maxtiley = Integer.MIN_VALUE;
        /* Loop through corners of the prism */
        for(int i = 0; i < 2; i++) {
            corner.x = blocks[i].x;
            for(int j = 0; j < 2; j++) {
                corner.y = blocks[j].y;
                for(int k = 0; k < 2; k++) {
                    corner.z = blocks[k].z;
                    world_to_map.transform(corner, tcorner);  /* Get map coordinate of corner */
                    int tx = fastFloor(tcorner.x/TILE_WIDTH);
                    int ty = fastFloor(tcorner.y/TILE_HEIGHT);
                    if(mintilex > tx) mintilex = tx;
                    if(maxtilex < tx) maxtilex = tx;
                    if(mintiley > ty) mintiley = ty;
                    if(maxtiley < ty) maxtiley = ty;
                }
            }
        }
        /* Not perfect, but it works (some extra tiles on corners possible) */
        return new Rectangle(mintilex, mintiley, maxtilex - mintilex + 1, maxtiley - mintiley + 1);
    }

    private static int fastFloor(double f) {
        return ((int)(f + 1000000000.0)) - 1000000000;
    }

    public final double maxheight = -1;
    public final double minheight = 0;

    private final HDPerspective perspective;
    private final HDMap map;

    /* Coordinate space for tiles consists of a plane (X, Y), corresponding to the projection of each tile on to the
     * plane of the bottom of the world (X positive to the right, Y positive to the top), with Z+ corresponding to the
     * height above this plane on a vector towards the viewer).  Logically, this makes the parallelogram representing the
     * space contributing to the tile have consistent tile-space X,Y coordinate pairs for both the top and bottom faces
     * Note that this is a classic right-hand coordinate system, while minecraft's world coordinates are left handed
     * (X+ is south, Y+ is up, Z+ is east).
     */
    /* Transformation matrix for taking coordinate in world-space (x, y, z) and finding coordinate in tile space (x, y, z) */
    private final Matrix3D world_to_map;
    private final Matrix3D map_to_world;

    /* Scale for default tiles */
    private final int basemodscale;

    private final int[] argb_buf;
    private final BufferedImage buf_img;

    /* dimensions of a map tile */
    public static final int TILE_WIDTH = 128;
    public static final int TILE_HEIGHT = 128;

    private static final BlockStep [] SEMI_STEPS = { BlockStep.Y_PLUS, BlockStep.X_MINUS, BlockStep.X_PLUS, BlockStep.Z_MINUS, BlockStep.Z_PLUS };
    /* ARGB band masks */
    private static final int [] BAND_MASKS = {0xFF0000, 0xFF00, 0xff, 0xff000000};

    class OurPerspectiveState implements HDPerspectiveState {
        int blocktypeid = 0;
        int blockdata = 0;
        int blockrenderdata = -1;
        int lastblocktypeid = 0;
        Vector3D top, bottom, direction;
        int px, py;
        BlockStep laststep = BlockStep.Y_MINUS;

        BlockStep stepx, stepy, stepz;

        /* Scaled models for non-cube blocks */
        private final HDBlockModels.HDScaledBlockModels scalemodels;
        private final int modscale;

        /* Section-level raytrace variables */
        int sx, sy, sz;
        double sdt_dx, sdt_dy, sdt_dz;
        double st_next_x, st_next_y, st_next_z;
        /* Raytrace state variables */
        double dx, dy, dz;
        int x, y, z;
        double dt_dx, dt_dy, dt_dz, t;
        int n;
        int x_inc, y_inc, z_inc;
        double t_next_y, t_next_x, t_next_z;
        boolean nonairhit;
        /* Subblock tracer state */
        int mx, my, mz;
        double xx, yy, zz;
        double mdt_dx;
        double mdt_dy;
        double mdt_dz;
        double togo;
        double mt_next_x, mt_next_y, mt_next_z;
        int subalpha;
        double mt;
        double mtend;
        int mxout, myout, mzout;
        /* Patch state and work variables */
        Vector3D v0 = new Vector3D();
        Vector3D vS = new Vector3D();
        Vector3D d_cross_uv = new Vector3D();
        double patch_t[] = new double[HDBlockModels.getMaxPatchCount()];
        double patch_u[] = new double[HDBlockModels.getMaxPatchCount()];
        double patch_v[] = new double[HDBlockModels.getMaxPatchCount()];
        BlockStep patch_step[] = new BlockStep[HDBlockModels.getMaxPatchCount()];
        int patch_id[] = new int[HDBlockModels.getMaxPatchCount()];
        int cur_patch = -1;
        double cur_patch_u;
        double cur_patch_v;
        double cur_patch_t;

        int[] subblock_xyz = new int[3];
        final MapIterator mapiter;
        final boolean isnether;
        boolean skiptoair;
        final int worldheight;
        final int heightmask;
        final LightLevels llcache[];

        /* Cache for custom model patch lists */
        private final DynLongHashMap custom_meshes;

        public OurPerspectiveState(MapIterator mi, boolean isnether, int scaled) {
            mapiter = mi;
            this.isnether = isnether;
            worldheight = mapiter.getWorldHeight();
            int shift;
            for(shift = 0; (1<<shift) < worldheight; shift++) {}
            heightmask = (1<<shift) - 1;
            llcache = new LightLevels[4];
            for(int i = 0; i < llcache.length; i++)
                llcache[i] = new LightLevels();
            custom_meshes = new DynLongHashMap();
            modscale = basemodscale << scaled;
            scalemodels = HDBlockModels.getModelsForScale(basemodscale << scaled);
        }

        void updateSemitransparentLight(LightLevels ll) {
            int emitted = 0, sky = 0;
            for (BlockStep s: SEMI_STEPS) {
                mapiter.stepPosition(s);
                int v = mapiter.getBlockEmittedLight();
                if (v > emitted) emitted = v;
                v = mapiter.getBlockSkyLight();
                if (v > sky) sky = v;
                mapiter.unstepPosition(s);
            }
            ll.sky = sky;
            ll.emitted = emitted;
        }
        /**
         * Update sky and emitted light
         */
        void updateLightLevel(int blktypeid, LightLevels ll) {
            /* Look up transparency for current block */
            TexturePack.BlockTransparency bt = TexturePack.HDTextureMap.getTransparency(blktypeid);
            switch(bt) {
                case TRANSPARENT:
                    ll.sky = mapiter.getBlockSkyLight();
                    ll.emitted = mapiter.getBlockEmittedLight();
                    break;
                case OPAQUE:
                    if(TexturePack.HDTextureMap.getTransparency(lastblocktypeid) != TexturePack.BlockTransparency.SEMITRANSPARENT) {
                        mapiter.unstepPosition(laststep);  /* Back up to block we entered on */
                        if(mapiter.getY() < worldheight) {
                            ll.sky = mapiter.getBlockSkyLight();
                            ll.emitted = mapiter.getBlockEmittedLight();
                        } else {
                            ll.sky = 15;
                            ll.emitted = 0;
                        }
                        mapiter.stepPosition(laststep);
                    }
                    else {
                        mapiter.unstepPosition(laststep);  /* Back up to block we entered on */
                        updateSemitransparentLight(ll);
                        mapiter.stepPosition(laststep);
                    }
                    break;
                case SEMITRANSPARENT:
                    updateSemitransparentLight(ll);
                    break;
                default:
                    ll.sky = mapiter.getBlockSkyLight();
                    ll.emitted = mapiter.getBlockEmittedLight();
                    break;
            }
        }
        /**
         * Get light level - only available if shader requested it
         */
        public final void getLightLevels(LightLevels ll) {
            updateLightLevel(blocktypeid, ll);
        }
        /**
         * Get sky light level - only available if shader requested it
         */
        public final void getLightLevelsAtStep(BlockStep step, LightLevels ll) {
            if(((step == BlockStep.Y_MINUS) && (y == 0)) ||
                    ((step == BlockStep.Y_PLUS) && (y == worldheight))) {
                getLightLevels(ll);
                return;
            }
            BlockStep blast = laststep;
            mapiter.stepPosition(step);
            laststep = blast;
            updateLightLevel(mapiter.getBlockTypeID(), ll);
            mapiter.unstepPosition(step);
            laststep = blast;
        }
        /**
         * Get current block type ID
         */
        public final int getBlockTypeID() { return blocktypeid; }
        /**
         * Get current block data
         */
        public final int getBlockData() { return blockdata; }
        /**
         * Get current block render data
         */
        public final int getBlockRenderData() { return blockrenderdata; }
        /**
         * Get direction of last block step
         */
        public final BlockStep getLastBlockStep() { return laststep; }
        /**
         * Get perspective scale
         */
        public final double getScale() { return modscale; }
        /**
         * Get start of current ray, in world coordinates
         */
        public final Vector3D getRayStart() { return top; }
        /**
         * Get end of current ray, in world coordinates
         */
        public final Vector3D getRayEnd() { return bottom; }
        /**
         * Get pixel X coordinate
         */
        public final int getPixelX() { return px; }
        /**
         * Get pixel Y coordinate
         */
        public final int getPixelY() { return py; }
        /**
         * Get map iterator
         */
        public final MapIterator getMapIterator() { return mapiter; }
        /**
         * Return submodel alpha value (-1 if no submodel rendered)
         */
        public int getSubmodelAlpha() {
            return subalpha;
        }
        /**
         * Initialize raytrace state variables
         */
        void raytrace_init() {
            /* Compute total delta on each axis */
            dx = Math.abs(direction.x);
            dy = Math.abs(direction.y);
            dz = Math.abs(direction.z);
            /* Compute parametric step (dt) per step on each axis */
            dt_dx = 1.0 / dx;
            dt_dy = 1.0 / dy;
            dt_dz = 1.0 / dz;
            /* Initialize parametric value to 0 (and we're stepping towards 1) */
            t = 0;
            /* Compute number of steps and increments for each */
            n = 1;

            /* Initial section coord */
            sx = fastFloor(top.x/16.0);
            sy = fastFloor(top.y/16.0);
            sz = fastFloor(top.z/16.0);
            /* Compute parametric step (dt) per step on each axis */
            sdt_dx = 16.0 / dx;
            sdt_dy = 16.0 / dy;
            sdt_dz = 16.0 / dz;

            /* If perpendicular to X axis */
            if (dx == 0) {
                x_inc = 0;
                st_next_x = Double.MAX_VALUE;
                stepx = BlockStep.X_PLUS;
                mxout = modscale;
            }
            /* If bottom is right of top */
            else if (bottom.x > top.x) {
                x_inc = 1;
                n += fastFloor(bottom.x) - x;
                st_next_x = (fastFloor(top.x/16.0) + 1 - (top.x/16.0)) * sdt_dx;
                stepx = BlockStep.X_PLUS;
                mxout = modscale;
            }
            /* Top is right of bottom */
            else {
                x_inc = -1;
                n += x - fastFloor(bottom.x);
                st_next_x = ((top.x/16.0) - fastFloor(top.x/16.0)) * sdt_dx;
                stepx = BlockStep.X_MINUS;
                mxout = -1;
            }
            /* If perpendicular to Y axis */
            if (dy == 0) {
                y_inc = 0;
                st_next_y = Double.MAX_VALUE;
                stepy = BlockStep.Y_PLUS;
                myout = modscale;
            }
            /* If bottom is above top */
            else if (bottom.y > top.y) {
                y_inc = 1;
                n += fastFloor(bottom.y) - y;
                st_next_y = (fastFloor(top.y/16.0) + 1 - (top.y/16.0)) * sdt_dy;
                stepy = BlockStep.Y_PLUS;
                myout = modscale;
            }
            /* If top is above bottom */
            else {
                y_inc = -1;
                n += y - fastFloor(bottom.y);
                st_next_y = ((top.y/16.0) - fastFloor(top.y/16.0)) * sdt_dy;
                stepy = BlockStep.Y_MINUS;
                myout = -1;
            }
            /* If perpendicular to Z axis */
            if (dz == 0) {
                z_inc = 0;
                st_next_z = Double.MAX_VALUE;
                stepz = BlockStep.Z_PLUS;
                mzout = modscale;
            }
            /* If bottom right of top */
            else if (bottom.z > top.z) {
                z_inc = 1;
                n += fastFloor(bottom.z) - z;
                st_next_z = (fastFloor(top.z/16.0) + 1 - (top.z/16.0)) * sdt_dz;
                stepz = BlockStep.Z_PLUS;
                mzout = modscale;
            }
            /* If bottom left of top */
            else {
                z_inc = -1;
                n += z - fastFloor(bottom.z);
                st_next_z = ((top.z/16.0) - fastFloor(top.z/16.0)) * sdt_dz;
                stepz = BlockStep.Z_MINUS;
                mzout = -1;
            }
            /* Walk through scene */
            laststep = BlockStep.Y_MINUS; /* Last step is down into map */
            nonairhit = false;
            skiptoair = isnether;
        }

        boolean handleSubModel(short[] model, HDShaderState shaderstate, boolean[] shaderdone) {
            boolean firststep = true;

            while(!raytraceSubblock(model, firststep)) {
                boolean done = true;
                if(!shaderdone[0])
                    shaderdone[0] = shaderstate.processBlock(this);
                done = done && shaderdone[0];
                /* If all are done, we're out */
                if(done)
                    return true;
                nonairhit = true;
                firststep = false;
            }
            return false;
        }

        boolean handlePatches(RenderPatch[] patches, HDShaderState shaderstate, boolean[] shaderdone) {
            int hitcnt = 0;
            /* Loop through patches : compute intercept values for each */
            for (RenderPatch patch: patches) {
                PatchDefinition pd = (PatchDefinition) patch;
                /* Compute origin of patch */
                v0.x = (double) x + pd.x0;
                v0.y = (double) y + pd.y0;
                v0.z = (double) z + pd.z0;
                /* Compute cross product of direction and V vector */
                d_cross_uv.set(direction);
                d_cross_uv.crossProduct(pd.v);
                /* Compute determinant - inner product of this with U */
                double det = pd.u.innerProduct(d_cross_uv);
                /* If parallel to surface, no intercept */
                switch (pd.sidevis) {
                    case TOP:
                        if (det < 0.000001) {
                            continue;
                        }
                        break;
                    case BOTTOM:
                        if (det > -0.000001) {
                            continue;
                        }
                        break;
                    case BOTH:
                    case FLIP:
                        if ((det > -0.000001) && (det < 0.000001)) {
                            continue;
                        }
                        break;
                }
                double inv_det = 1.0 / det; /* Calculate inverse determinant */
                /* Compute distance from patch to ray origin */
                vS.set(top);
                vS.subtract(v0);
                /* Compute u - slope times inner product of offset and cross product */
                double u = inv_det * vS.innerProduct(d_cross_uv);
                if ((u <= pd.umin) || (u >= pd.umax)) {
                    continue;
                }
                /* Compute cross product of offset and U */
                vS.crossProduct(pd.u);
                /* Compute V using slope times inner product of direction and cross product */
                double v = inv_det * direction.innerProduct(vS);
                if ((v <= pd.vmin) || (v >= pd.vmax) || ((u + v) >= pd.uplusvmax)) {
                    continue;
                }
                /* Compute parametric value of intercept */
                double t = inv_det * pd.v.innerProduct(vS);
                if (t > 0.000001) { /* We've got a hit */
                    patch_t[hitcnt] = t;
                    patch_u[hitcnt] = u;
                    patch_v[hitcnt] = v;
                    patch_id[hitcnt] = pd.textureindex;
                    if (det > 0) {
                        patch_step[hitcnt] = pd.step.opposite();
                    } else {
                        if (pd.sidevis == RenderPatchFactory.SideVisible.FLIP) {
                            patch_u[hitcnt] = 1 - u;
                        }
                        patch_step[hitcnt] = pd.step;
                    }
                    hitcnt++;
                }
            }
            /* If no hits, we're done */
            if(hitcnt == 0) {
                return false;
            }
            BlockStep old_laststep = laststep;  /* Save last step */

            for(int i = 0; i < hitcnt; i++) {
                /* Find closest hit (lowest parametric value) */
                double best_t = Double.MAX_VALUE;
                int best_patch = 0;
                for(int j = 0; j < hitcnt; j++) {
                    if(patch_t[j] < best_t) {
                        best_patch = j;
                        best_t = patch_t[j];
                    }
                }
                cur_patch = patch_id[best_patch]; /* Mark this as current patch */
                cur_patch_u = patch_u[best_patch];
                cur_patch_v = patch_v[best_patch];
                laststep = patch_step[best_patch];
                cur_patch_t = best_t;
                /* Process the shaders */
                boolean done = true;
                if(!shaderdone[0])
                    shaderdone[0] = shaderstate.processBlock(this);
                done = done && shaderdone[0];
                cur_patch = -1;
                /* If all are done, we're out */
                if(done) {
                    laststep = old_laststep;
                    return true;
                }
                nonairhit = true;
                /* Now remove patch and repeat */
                patch_t[best_patch] = Double.MAX_VALUE;
            }
            laststep = old_laststep;

            return false;
        }

        /**
         * Process visit of ray to block
         */
        boolean visit_block(HDShaderState shaderstate, boolean[] shaderdone) {
            lastblocktypeid = blocktypeid;
            blocktypeid = mapiter.getBlockTypeID();
            if(skiptoair) {	/* If skipping until we see air */
                if(blocktypeid == 0) {	/* If air, we're done */
                    skiptoair = false;
                }
            }
            else if(nonairhit || (blocktypeid != 0)) {
                blockdata = mapiter.getBlockData();
                blockrenderdata = HDBlockModels.getBlockRenderData(blocktypeid, mapiter);

                RenderPatch[] patches = scalemodels.getPatchModel(blocktypeid,  blockdata,  blockrenderdata);
                /* If no patches, see if custom model */
                if(patches == null) {
                    HDBlockModels.CustomBlockModel cbm = scalemodels.getCustomBlockModel(blocktypeid,  blockdata);
                    if(cbm != null) {   /* If found, see if cached already */
                        patches = this.getCustomMesh();
                        if(patches == null) {
                            patches = cbm.getMeshForBlock(mapiter);
                            this.setCustomMesh(patches);
                        }
                    }
                }
                /* Look up to see if block is modelled */
                if(patches != null) {
                    return handlePatches(patches, shaderstate, shaderdone);
                }
                short[] model = scalemodels.getScaledModel(blocktypeid, blockdata, blockrenderdata);
                if(model != null) {
                    return handleSubModel(model, shaderstate, shaderdone);
                }
                else {
                    boolean done = true;
                    subalpha = -1;
                    if(!shaderdone[0]) {
                        shaderdone[0] = shaderstate.processBlock(this);
                    }
                    done = done && shaderdone[0];
                    /* If all are done, we're out */
                    if(done)
                        return true;
                    nonairhit = true;
                }
            }
            return false;
        }
        /* Skip empty : return false if exited */
        boolean raytraceSkipEmpty(MapChunkCache cache) {
            while(cache.isEmptySection(sx, sy, sz)) {
                /* If Y step is next best */
                if((st_next_y <= st_next_x) && (st_next_y <= st_next_z)) {
                    sy += y_inc;
                    t = st_next_y;
                    st_next_y += sdt_dy;
                    laststep = stepy;
                    if(sy < 0)
                        return false;
                }
                /* If X step is next best */
                else if((st_next_x <= st_next_y) && (st_next_x <= st_next_z)) {
                    sx += x_inc;
                    t = st_next_x;
                    st_next_x += sdt_dx;
                    laststep = stepx;
                }
                /* Else, Z step is next best */
                else {
                    sz += z_inc;
                    t = st_next_z;
                    st_next_z += sdt_dz;
                    laststep = stepz;
                }
            }
            return true;
        }
        /**
         * Step block iterator: false if done
         */
        boolean raytraceStepIterator() {
            /* If Y step is next best */
            if ((t_next_y <= t_next_x) && (t_next_y <= t_next_z)) {
                y += y_inc;
                t = t_next_y;
                t_next_y += dt_dy;
                laststep = stepy;
                mapiter.stepPosition(laststep);
                /* If outside 0-(height-1) range */
                if((y & (~heightmask)) != 0) {
                    return false;
                }
            }
            /* If X step is next best */
            else if ((t_next_x <= t_next_y) && (t_next_x <= t_next_z)) {
                x += x_inc;
                t = t_next_x;
                t_next_x += dt_dx;
                laststep = stepx;
                mapiter.stepPosition(laststep);
            }
            /* Else, Z step is next best */
            else {
                z += z_inc;
                t = t_next_z;
                t_next_z += dt_dz;
                laststep = stepz;
                mapiter.stepPosition(laststep);
            }
            return true;
        }
        /**
         * Trace ray, based on "Voxel Tranversal along a 3D line"
         */
        void raytrace(MapChunkCache cache, HDShaderState shaderstate, boolean[] shaderdone) {
            /* Initialize raytrace state variables */
            raytrace_init();

            /* Skip sections until we hit a non-empty one */
            if (!raytraceSkipEmpty(cache))
                return;

            raytrace_section_init();

            if (y < 0)
                return;

            mapiter.initialize(x, y, z);

            for (; n > 0; --n) {
                if (visit_block(shaderstate, shaderdone)) {
                    return;
                }
                if (!raytraceStepIterator()) {
                    return;
                }
            }
        }

        void raytrace_section_init() {
            t = t - 0.000001;
            double xx = top.x + t * direction.x;
            double yy = top.y + t * direction.y;
            double zz = top.z + t * direction.z;
            x = fastFloor(xx);
            y = fastFloor(yy);
            z = fastFloor(zz);
            t_next_x = st_next_x;
            t_next_y = st_next_y;
            t_next_z = st_next_z;
            n = 1;
            if(t_next_x != Double.MAX_VALUE) {
                if(stepx == BlockStep.X_PLUS) {
                    t_next_x = t + (x + 1 - xx) * dt_dx;
                    n += fastFloor(bottom.x) - x;
                }
                else {
                    t_next_x = t + (xx - x) * dt_dx;
                    n += x - fastFloor(bottom.x);
                }
            }
            if(t_next_y != Double.MAX_VALUE) {
                if(stepy == BlockStep.Y_PLUS) {
                    t_next_y = t + (y + 1 - yy) * dt_dy;
                    n += fastFloor(bottom.y) - y;
                }
                else {
                    t_next_y = t + (yy - y) * dt_dy;
                    n += y - fastFloor(bottom.y);
                }
            }
            if(t_next_z != Double.MAX_VALUE) {
                if(stepz == BlockStep.Z_PLUS) {
                    t_next_z = t + (z + 1 - zz) * dt_dz;
                    n += fastFloor(bottom.z) - z;
                }
                else {
                    t_next_z = t + (zz - z) * dt_dz;
                    n += z - fastFloor(bottom.z);
                }
            }
        }

        boolean raytraceSubblock(short[] model, boolean firsttime) {
            if(firsttime) {
                mt = t + 0.00000001;
                xx = top.x + mt * direction.x;
                yy = top.y + mt * direction.y;
                zz = top.z + mt * direction.z;
                mx = (int)((xx - fastFloor(xx)) * modscale);
                my = (int)((yy - fastFloor(yy)) * modscale);
                mz = (int)((zz - fastFloor(zz)) * modscale);
                mdt_dx = dt_dx / modscale;
                mdt_dy = dt_dy / modscale;
                mdt_dz = dt_dz / modscale;
                mt_next_x = t_next_x;
                mt_next_y = t_next_y;
                mt_next_z = t_next_z;
                if(mt_next_x != Double.MAX_VALUE) {
                    togo = ((t_next_x - t) / mdt_dx);
                    mt_next_x = mt + (togo - fastFloor(togo)) * mdt_dx;
                }
                if(mt_next_y != Double.MAX_VALUE) {
                    togo = ((t_next_y - t) / mdt_dy);
                    mt_next_y = mt + (togo - fastFloor(togo)) * mdt_dy;
                }
                if(mt_next_z != Double.MAX_VALUE) {
                    togo = ((t_next_z - t) / mdt_dz);
                    mt_next_z = mt + (togo - fastFloor(togo)) * mdt_dz;
                }
                mtend = Math.min(t_next_x, Math.min(t_next_y, t_next_z));
            }
            subalpha = -1;
            boolean skip = !firsttime;	/* Skip first block on continue */
            while(mt <= mtend) {
                if(!skip) {
                    try {
                        int blkalpha = model[modscale*modscale*my + modscale*mz + mx];
                        if(blkalpha > 0) {
                            subalpha = blkalpha;
                            return false;
                        }
                    } catch (ArrayIndexOutOfBoundsException aioobx) {	/* We're outside the model, so miss */
                        return true;
                    }
                }
                else {
                    skip = false;
                }

                /* If X step is next best */
                if((mt_next_x <= mt_next_y) && (mt_next_x <= mt_next_z)) {
                    mx += x_inc;
                    mt = mt_next_x;
                    mt_next_x += mdt_dx;
                    laststep = stepx;
                    if(mx == mxout) {
                        return true;
                    }
                }
                /* If Y step is next best */
                else if((mt_next_y <= mt_next_x) && (mt_next_y <= mt_next_z)) {
                    my += y_inc;
                    mt = mt_next_y;
                    mt_next_y += mdt_dy;
                    laststep = stepy;
                    if(my == myout) {
                        return true;
                    }
                }
                /* Else, Z step is next best */
                else {
                    mz += z_inc;
                    mt = mt_next_z;
                    mt_next_z += mdt_dz;
                    laststep = stepz;
                    if(mz == mzout) {
                        return true;
                    }
                }
            }
            return true;
        }
        public final int[] getSubblockCoord() {
            if(cur_patch >= 0) {    /* If patch hit */
                double tt = cur_patch_t;
                double xx = top.x + tt * direction.x;
                double yy = top.y + tt * direction.y;
                double zz = top.z + tt * direction.z;
                subblock_xyz[0] = (int)((xx - fastFloor(xx)) * modscale);
                subblock_xyz[1] = (int)((yy - fastFloor(yy)) * modscale);
                subblock_xyz[2] = (int)((zz - fastFloor(zz)) * modscale);
            }
            else if(subalpha < 0) {
                double tt = t + 0.0000001;
                double xx = top.x + tt * direction.x;
                double yy = top.y + tt * direction.y;
                double zz = top.z + tt * direction.z;
                subblock_xyz[0] = (int)((xx - fastFloor(xx)) * modscale);
                subblock_xyz[1] = (int)((yy - fastFloor(yy)) * modscale);
                subblock_xyz[2] = (int)((zz - fastFloor(zz)) * modscale);
            }
            else {
                subblock_xyz[0] = mx;
                subblock_xyz[1] = my;
                subblock_xyz[2] = mz;
            }
            return subblock_xyz;
        }
        /**
         * Get current texture index
         */
        public int getTextureIndex() {
            return cur_patch;
        }
        /**
         * Get current U of patch intercept
         */
        public double getPatchU() {
            return cur_patch_u;
        }
        /**
         * Get current V of patch intercept
         */
        public double getPatchV() {
            return cur_patch_v;
        }
        /**
         * Light level cache
         * @param idx of light level (0-3)
         */
        public final LightLevels getCachedLightLevels(int idx) {
            return llcache[idx];
        }
        /**
         * Get custom mesh for block, if defined (null if not)
         */
        public final RenderPatch[] getCustomMesh() {
            long key = this.mapiter.getBlockKey();  /* Get key for current block */
            return (RenderPatch[])custom_meshes.get(key);
        }
        /**
         * Save custom mesh for block
         */
        public final void setCustomMesh(RenderPatch[] mesh) {
            long key = this.mapiter.getBlockKey();  /* Get key for current block */
            custom_meshes.put(key,  mesh);
        }
    }
}