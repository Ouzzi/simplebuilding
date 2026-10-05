package com.simplebuilding.crucible;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 26.2 twin of the crucible bridge: SimpleLib is not built for 26.2, so there are no crucible parts
 * ({@code McVersion.CRUCIBLE} is false); every call is a no-op with the same signature as on 26.3.
 */
public final class CrucibleCompat {
    public static final int HAMMER_DAMAGE_PER_STRIKE = 2;

    public static void registerBlocks() {}

    public static void registerItems() {}

    public static void init() {}

    public static @Nullable Block enderiteCrucible() { return null; }

    public static @Nullable Block enderiteBarrel() { return null; }

    public static @Nullable Block reinforcedCauldron() { return null; }

    public static BlockState reinforcedCauldron(String content) {
        throw new UnsupportedOperationException("no reinforced cauldron on 26.2");
    }

    public static @Nullable String cauldronContent(BlockState state) { return null; }

    public static String heatAt(Level level, BlockPos crucible) { return "none"; }

    public static int crucibleSlots(Block block) { return 0; }

    public static int stackMultiplier(Block block) { return 0; }

    public static boolean isAttached(BlockState state) { return false; }

    public static boolean upgradesInPlace(Block block) { return false; }

    public static void upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {}

    public static boolean axeWaysEnabled() { return true; }

    public static @Nullable InteractionResult hammerUse(UseOnContext context) { return null; }

    private CrucibleCompat() {}
}
