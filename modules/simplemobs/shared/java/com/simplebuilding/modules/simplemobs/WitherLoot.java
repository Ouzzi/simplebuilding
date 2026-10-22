package com.simplebuilding.modules.simplemobs;

import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Owner 21.10.: the Wither's new loot (Nether Heart, Nether Gem) appears in its placed form once the death has ended -
 * under the Wither when there is ground, else as an item. Pure placement rules, used by {@link BlightedWither} and the
 * GameTests.
 */
public final class WitherLoot {
    /** How far below the Wither the ground is searched (blocks). */
    public static final int SEARCH_DEPTH = 24;
    /** Columns tried for a second block: the spot itself first, then its neighbours. */
    private static final int[][] COLUMNS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};

    /** The id the Nether Heart had as Wither Heart (before 21.10.); old worlds find the Nether Heart under it. */
    public static final Identifier OLD_HEART = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "wither_heart");
    public static final Identifier HEART = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "nether_heart");

    private WitherLoot() {}

    /**
     * Old worlds: the Wither Heart becomes the Nether Heart (SimpleLib IdMoves). Called first in the mod constructor /
     * initializer, before any registry event; kept out of {@link WitherMobs} so it creates no registry object early.
     */
    public static void idMoves() {
        IdMoves.move(Registries.ITEM, OLD_HEART, HEART);
    }

    /**
     * Places every stack whose item is a loot block under {@code at} (one block each, side by side), drops what found no
     * spot - and every other stack - as long-lived items. Returns the positions that got a block.
     */
    public static List<BlockPos> place(ServerLevel level, Vec3 at, List<ItemStack> stacks) {
        List<BlockPos> placed = new ArrayList<>();
        BlockPos top = BlockPos.containing(at.x, at.y + 0.5, at.z);
        for (ItemStack stack : stacks) {
            BlockPos spot = stack.getItem() instanceof BlockItem item && item.getBlock() instanceof WitherLootBlock ? spot(level, top, placed) : null;
            if (spot != null) {
                BlockState state = ((WitherLootBlock) ((BlockItem) stack.getItem()).getBlock()).lootState(level, spot);
                level.setBlock(spot, state, 3);
                level.playSound(null, spot, state.getSoundType().getPlaceSound(), SoundSource.HOSTILE, 1.0F, 0.8F);
                placed.add(spot);
                if (stack.getCount() > 1) drop(level, at, stack.copyWithCount(stack.getCount() - 1));
            } else {
                drop(level, at, stack);
            }
        }
        return placed;
    }

    /** A free spot on ground below {@code top} (or next to it), not one of {@code taken}; null without ground. */
    public static @Nullable BlockPos spot(ServerLevel level, BlockPos top, List<BlockPos> taken) {
        for (int[] c : COLUMNS) {
            BlockPos found = ground(level, top.offset(c[0], 0, c[1]));
            if (found != null && !taken.contains(found)) return found;
        }
        return null;
    }

    /** The first replaceable, dry spot over a sturdy top face, searched downward from {@code from}; null when none. */
    public static @Nullable BlockPos ground(ServerLevel level, BlockPos from) {
        BlockPos.MutableBlockPos p = from.mutable();
        for (int i = 0; i <= SEARCH_DEPTH && p.getY() > level.getMinY(); i++, p.move(Direction.DOWN)) {
            BlockState here = level.getBlockState(p);
            if (!here.canBeReplaced() || !here.getFluidState().isEmpty()) {
                if (!here.isAir() && !here.canBeReplaced()) return null;
                continue;
            }
            BlockPos below = p.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return p.immutable();
        }
        return null;
    }

    private static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
        item.setDefaultPickUpDelay();
        item.setExtendedLifetime();
        level.addFreshEntity(item);
    }
}
