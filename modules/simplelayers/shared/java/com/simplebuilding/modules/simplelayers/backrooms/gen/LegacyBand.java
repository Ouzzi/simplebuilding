package com.simplebuilding.modules.simplelayers.backrooms.gen;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandView;
import com.simplebuilding.modules.simplelayers.backrooms.api.CorePiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import com.simplebuilding.modules.simplelayers.world.BackroomsBlocks;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import com.simplebuilding.modules.simplelayers.world.gen.Piece;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Start content of the Lobby, Poolrooms, Warehouse and Pipes (BACK22 core §7, deviation 3 of the plan): the BACK18/20 generators
 * (64 high) as a level - y 0..63 become ly 0..63, ly 64..95 is hull; their bedrock becomes the level's hull, rifts and the old
 * maintenance doors become hull or air (fg11 a: no rifts). The level branches replace this in their package.
 */
public class LegacyBand implements Band {
    private final String id;
    private final BackroomsLevel legacy;
    private final DoorRule rule;
    private final BandView view;
    private final Supplier<BlockState> hull;
    private final boolean loot;
    private final BandLayout layout;

    public LegacyBand(String id, BackroomsLevel legacy, DoorRule rule, int fog, Supplier<BlockState> hull, boolean loot) {
        this.id = id;
        this.legacy = legacy;
        this.rule = rule;
        this.view = new BandView(fog);
        this.hull = hull;
        this.loot = loot;
        this.layout = new Layout(legacy);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public BandLayout layout() {
        return layout;
    }

    @Override
    public BlockState state(BandPiece piece) {
        if (piece instanceof Piece p) return BackroomsBlocks.state(p);
        return piece == CorePiece.HULL ? hull.get() : Blocks.AIR.defaultBlockState();
    }

    @Override
    public DoorRule doorRule() {
        return rule;
    }

    @Override
    public @Nullable ResourceKey<LootTable> loot() {
        return loot ? ResourceKey.create(Registries.LOOT_TABLE, SimpleLayers.id("chests/backrooms_" + legacy.id)) : null;
    }

    @Override
    public BandView view() {
        return view;
    }

    /** The adapter around the old generator. */
    public record Layout(BackroomsLevel legacy) implements BandLayout {
        public static final int OLD_HEIGHT = 64;

        @Override
        public BandPiece at(long seed, int x, int ly, int z) {
            if (ly >= OLD_HEIGHT || ly < 0) return CorePiece.HULL;
            Piece p = legacy.at(seed, x, ly, z);
            return switch (p) {
                case BEDROCK, GLITCHED_BEDROCK, LEVEL_DOOR_LOWER, LEVEL_DOOR_UPPER, SEALED_DOOR_LOWER, SEALED_DOOR_UPPER -> CorePiece.HULL;
                case RIFT -> ly <= 1 ? CorePiece.HULL : CorePiece.AIR;
                default -> p;
            };
        }

        @Override
        public int[] arrival(long seed, int x, int z) {
            return legacy.arrival(seed, x, z);
        }

        @Override
        public boolean hullSection(long seed, int chunkX, int chunkZ, int section) {
            return section * 16 >= OLD_HEIGHT;
        }

        @Override
        public void containers(long seed, int chunkX, int chunkZ, List<int[]> out) {
            out.addAll(legacy.caches(seed, chunkX * 16, chunkZ * 16, 16));
        }
    }
}
