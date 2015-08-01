package org.pepsoft.worldpainter.layers.bo2;

import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;

import javax.vecmath.Point3i;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by pepijn on 25-7-15.
 */
public class Bo3BlockSpec extends Bo2BlockSpec {
    public Bo3BlockSpec(Point3i coords, Material material, TileEntity tileEntity) {
        super(coords, material, null);
        this.tileEntity = tileEntity;
        if (tileEntity != null) {
            tileEntity.setX(coords.x);
            tileEntity.setY(coords.z);
            tileEntity.setZ(coords.y);
        }
        randomBlocks = null;
    }

    public Bo3BlockSpec(Point3i coords, RandomBlock[] randomBlocks) {
        super(coords, null, null);
        tileEntity = null;
        this.randomBlocks = randomBlocks;
        Arrays.stream(randomBlocks)
            .filter(b -> b.tileEntity != null)
            .forEach(b -> {
                b.tileEntity.setX(coords.x);
                b.tileEntity.setY(coords.z);
                b.tileEntity.setZ(coords.y);
            });
    }

    /**
     * Gets all tile entities contained in this block. Note that since bo3
     * objects can contain random blocks, which can be tile entities with
     * different contents, this collection can contain multiple overlapping
     * tile entities with the same coordinates. Hopefully WorldPainter's post
     * processing will deal with that correctly. TODO: does it?
     *
     * @return All tile entities contained in this object, or an empty
     *     collection if there are none.
     */
    public Set<TileEntity> getTileEntities() {
        if (randomBlocks != null) {
            return Arrays.stream(randomBlocks)
                .filter(b -> b.tileEntity != null)
                .map(b -> b.tileEntity)
                .collect(Collectors.toSet());
        } else if (tileEntity != null) {
            return Collections.singleton(tileEntity);
        } else {
            return Collections.emptySet();
        }
    }

    // Bo2BlockSpec

    @Override
    public Material getMaterial() {
        if (randomBlocks != null) {
            for (RandomBlock randomBlock: randomBlocks) {
                if ((randomBlock.chance >= 100) || (RANDOM.nextInt(100) < randomBlock.chance)) {
                    return randomBlock.material;
                }
            }
            // The spec is not really clear what to do if this happens:
            return randomBlocks[randomBlocks.length - 1].material;
        } else {
            return super.getMaterial();
        }
    }

    private final TileEntity tileEntity;
    private final RandomBlock[] randomBlocks;

    private static final Random RANDOM = new Random();

    private static final long serialVersionUID = 1L;

    public static class RandomBlock {
        RandomBlock(Material material, TileEntity tileEntity, int chance) {
            this.material = material;
            this.tileEntity = tileEntity;
            this.chance = chance;
        }

        final Material material;
        final TileEntity tileEntity;
        final int chance;
    }
}
