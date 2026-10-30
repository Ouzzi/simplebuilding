package com.simplebuilding.modules.simplechallenges.skyline;

import com.simplebuilding.modules.simplechallenges.lucky.BlockCatalog;
import com.simplebuilding.modules.simplechallenges.preset.SkylineMode;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The Skyline of one challenge: for each of the three dimensions the sequence of blocks, where it starts and how it is laid out. Pure
 * arithmetic over the sequence, so the generator asks "which block stands at x, y, z" without storing anything. The Nether line
 * has one more place than blocks: the seal (Bedrock) at its end, which opens the End portal (S6).
 */
public final class SkylinePlans {
    public static final int EGG_DISTANCE = 96;
    /** The End's main island lies round 0,0: its eggs stand farther out, beyond the pillars. */
    public static final int END_EGG_DISTANCE = 190;

    /** The line of one dimension. */
    public static final class Dim {
        public final BlockCatalog.Dim kind;
        public final ResourceKey<Level> key;
        public final Layout layout;
        public final int ox, oy, oz;
        public final List<Block> blocks;
        public final boolean seal;
        public final int eggDistance;

        public Dim(BlockCatalog.Dim kind, ResourceKey<Level> key, Layout layout, int ox, int oy, int oz, List<Block> blocks, boolean seal, int eggDistance) {
            this.kind = kind;
            this.key = key;
            this.layout = layout;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.blocks = blocks;
            this.seal = seal;
            this.eggDistance = eggDistance;
        }

        public int size() {
            return blocks.size() + (seal ? 1 : 0);
        }

        public boolean isSeal(int index) {
            return seal && index == blocks.size();
        }

        public Block blockAt(int index) {
            return index < blocks.size() ? blocks.get(index) : Blocks.BEDROCK;
        }

        /** The sequence number of the block that stands at this position, or -1 when none belongs there. */
        public int indexAt(int x, int y, int z) {
            if (y != oy) return -1;
            int dx = x - ox, dz = z - oz;
            if (((dx | dz) & 1) != 0) return -1;
            int i = layout.index(dx >> 1, dz >> 1);
            return i < size() ? i : -1;
        }

        public BlockPos posOf(int index) {
            int[] c = layout.cell(index);
            return new BlockPos(ox + 2 * c[0], oy, oz + 2 * c[1]);
        }

        /** Where a player arriving in this dimension is put: on the first block. */
        public BlockPos start() {
            return posOf(0);
        }
    }

    public final SkylineMode mode;
    private final Dim overworld, nether, end;

    public SkylinePlans(SkylineMode mode, Dim overworld, Dim nether, Dim end) {
        this.mode = mode;
        this.overworld = overworld;
        this.nether = nether;
        this.end = end;
    }

    /** Builds the three lines from the registry. Positions: Overworld and Nether start at 0,64,0, the End line starts where Vanilla puts its arrival platform (100,48,0). */
    public static SkylinePlans build(SkylineMode mode) {
        BlockPos ov = SkylineService.originOverride;
        BlockPos o = ov != null ? ov : new BlockPos(0, 64, 0), e = ov != null ? ov : new BlockPos(100, 48, 0);
        return new SkylinePlans(mode,
                new Dim(BlockCatalog.Dim.OVERWORLD, Level.OVERWORLD, mode.layout(), o.getX(), o.getY(), o.getZ(), BlockCatalog.sequence(BlockCatalog.Dim.OVERWORLD), false, EGG_DISTANCE),
                new Dim(BlockCatalog.Dim.NETHER, Level.NETHER, mode.layout(), o.getX(), o.getY(), o.getZ(), BlockCatalog.sequence(BlockCatalog.Dim.NETHER), true, EGG_DISTANCE),
                new Dim(BlockCatalog.Dim.END, Level.END, mode.layout(), e.getX(), e.getY(), e.getZ(), BlockCatalog.sequence(BlockCatalog.Dim.END), false, END_EGG_DISTANCE));
    }

    public Dim overworld() {
        return overworld;
    }

    public Dim nether() {
        return nether;
    }

    public Dim end() {
        return end;
    }

    /** The line of a dimension, or null for any other dimension (the mod leaves it alone). */
    public Dim of(ResourceKey<Level> key) {
        if (key == Level.OVERWORLD) return overworld;
        if (key == Level.NETHER) return nether;
        if (key == Level.END) return end;
        return null;
    }
}
