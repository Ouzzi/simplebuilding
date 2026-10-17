package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.simplebuilding.modules.simpletweaks.admin.AdminCodecs;
import java.util.List;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Undoes logged actions, newest first: placed blocks are removed (only while the same block is still there), broken
 * blocks come back (only onto air or replaceable blocks, with their block entity data), items taken from a container
 * go back in, deposited items are taken out again. Server thread.
 */
public final class Rollback {
    public record Result(int undone, int skipped) {}
    /** Placed/removed without drops and without the block entity's own clean-up (no item duplication). */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    public static Result apply(MinecraftServer server, List<BlockLog.Entry> newestFirst) {
        int done = 0, skipped = 0;
        for (var e : newestFirst) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(e.dimension())));
            if (level == null) { skipped++; continue; }
            boolean ok = switch (e.action()) {
                case 'P' -> unplace(level, e);
                case 'B' -> unbreak(level, e);
                case 'T' -> move(level, e, true);
                case 'D' -> move(level, e, false);
                default -> false;
            };
            if (ok) done++; else skipped++;
        }
        return new Result(done, skipped);
    }
    private static boolean unplace(ServerLevel level, BlockLog.Entry e) {
        var state = level.getBlockState(e.pos());
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(e.id())) return false;
        return level.setBlock(e.pos(), Blocks.AIR.defaultBlockState(), FLAGS | Block.UPDATE_NEIGHBORS);
    }
    private static boolean unbreak(ServerLevel level, BlockLog.Entry e) {
        var current = level.getBlockState(e.pos());
        if (!current.isAir() && !current.canBeReplaced()) return false;
        String extra = e.extra();
        int cut = extra.indexOf('\u0001');
        String stateText = cut < 0 ? extra : extra.substring(0, cut);
        try {
            var state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, stateText.isEmpty() ? e.id() : stateText, false).blockState();
            if (!level.setBlock(e.pos(), state, Block.UPDATE_ALL)) return false;
            if (cut >= 0) {
                var be = level.getBlockEntity(e.pos());
                if (be != null) {
                    be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), TagParser.parseCompoundFully(extra.substring(cut + 1))));
                    be.setChanged();
                    level.sendBlockUpdated(e.pos(), state, state, Block.UPDATE_CLIENTS);
                }
            }
            return true;
        } catch (Exception ex) { return false; }
    }
    private static boolean move(ServerLevel level, BlockLog.Entry e, boolean putBack) {
        if (!(level.getBlockEntity(e.pos()) instanceof Container container)) return false;
        ItemStack proto = e.extra().isEmpty() ? new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(e.id()))) : AdminCodecs.fromSnbt(level.registryAccess(), e.extra());
        if (proto.isEmpty()) return false;
        int left = e.count();
        if (putBack) {
            for (int pass = 0; pass < 2 && left > 0; pass++) for (int i = 0; i < container.getContainerSize() && left > 0; i++) {
                var in = container.getItem(i);
                if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, proto) && container.canPlaceItem(i, proto)) {
                    int n = Math.min(left, Math.min(in.getMaxStackSize(), container.getMaxStackSize()) - in.getCount());
                    if (n > 0) { in.grow(n); left -= n; }
                } else if (pass == 1 && in.isEmpty() && container.canPlaceItem(i, proto)) {
                    int n = Math.min(left, Math.min(proto.getMaxStackSize(), container.getMaxStackSize()));
                    container.setItem(i, proto.copyWithCount(n)); left -= n;
                }
            }
        } else {
            for (int i = 0; i < container.getContainerSize() && left > 0; i++) {
                var in = container.getItem(i);
                if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, proto)) { int n = Math.min(left, in.getCount()); in.shrink(n); left -= n; if (in.isEmpty()) container.setItem(i, ItemStack.EMPTY); }
            }
        }
        container.setChanged();
        return left < e.count();
    }
    private Rollback() {}
}
