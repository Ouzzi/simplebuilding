package com.simpleshops.logic;

import com.simpleshops.block.EarningsStore;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlock;
import com.simpleshops.block.SalesStorageBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The earnings chain of a shop: the sales block, then every sales storage that faces it (SP22-7: any side), and storages
 * facing those (chains of older worlds, which hung below each other), as long as ALL of them have the same (non-null) owner
 * and sit in loaded chunks. Everything runs on the server thread and changes both sides of a move in one go.
 */
public final class ShopStorage {
    /** Most storages of one sales block (placement refuses more; the chain never looks further). */
    public static final int MAX_CHAIN = 16;

    /** The store at {@code pos} if it is a live earnings store in a loaded chunk, else {@code null}. */
    public static @Nullable EarningsStore storeAt(Level level, BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SalesBlockEntity s && !s.isRemoved() && !s.isServerShop()) return s;
        if (be instanceof SalesStorageBlockEntity s && !s.isRemoved()) return s;
        return null;
    }

    /** The position a storage pulls from: the block it faces. */
    public static BlockPos source(SalesStorageBlockEntity storage) {
        return storage.getBlockPos().relative(storage.getBlockState().getValueOrElse(SalesStorageBlock.FACING, Direction.UP));
    }

    /**
     * The sales block's store first, then its storages in rings (the ones facing it, then the ones facing those) that share its
     * owner; a gap or a foreign block ends that branch.
     */
    public static List<EarningsStore> chain(SalesBlockEntity sales) {
        List<EarningsStore> out = new ArrayList<>();
        out.add(sales);
        UUID owner = sales.owner();
        Level level = sales.getLevel();
        if (owner == null || level == null || sales.isServerShop()) return out;
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(sales.getBlockPos());
        while (!open.isEmpty() && out.size() <= MAX_CHAIN) {
            BlockPos at = open.poll();
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (!(storeAt(level, next) instanceof SalesStorageBlockEntity s) || !owner.equals(s.owner()) || !source(s).equals(at) || out.contains(s)) continue;
                if (out.size() > MAX_CHAIN) break;
                out.add(s);
                open.add(next);
            }
        }
        return out;
    }

    /** Hopper-like step: the first stack of the store this storage faces moves into {@code self}, if the owners are the same. */
    public static boolean pull(SalesStorageBlockEntity self) {
        Level level = self.getLevel();
        if (level == null || self.isRemoved() || self.owner() == null) return false;
        EarningsStore from0 = storeAt(level, source(self));
        if (from0 == null || !self.owner().equals(from0.owner())) return false;
        NonNullList<ItemStack> from = from0.earnings();
        for (int i = 0; i < from.size(); i++) {
            ItemStack stack = from.get(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = insert(List.of(self.earnings()), stack.copy());
            if (rest.getCount() == stack.getCount()) continue;
            from.set(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
            from0.changed();
            self.changed();
            return true;
        }
        return false;
    }

    /**
     * Storages saved before SP22-7 had a horizontal look-only facing and only ever pulled from above. Once per block entity: when
     * the block above is a store of its owner, it turns to face up (the chain keeps working exactly as it did).
     */
    public static void aimLegacy(SalesStorageBlockEntity self) {
        Level level = self.getLevel();
        if (level == null || self.owner() == null) return;
        BlockState state = self.getBlockState();
        if (!state.hasProperty(SalesStorageBlock.FACING)) return;
        EarningsStore above = storeAt(level, self.getBlockPos().above());
        if (above != null && self.owner().equals(above.owner()) && state.getValue(SalesStorageBlock.FACING) != Direction.UP) level.setBlock(self.getBlockPos(), state.setValue(SalesStorageBlock.FACING, Direction.UP), 3);
    }

    /** Merges {@code stack} into the lists (same item first over all of them, then empty slots); returns what did not fit. */
    public static ItemStack insert(List<NonNullList<ItemStack>> lists, ItemStack stack) {
        ItemStack rest = stack.copy();
        int max = rest.getMaxStackSize();
        for (NonNullList<ItemStack> list : lists) {
            for (int i = 0; i < list.size() && !rest.isEmpty(); i++) {
                ItemStack at = list.get(i);
                if (at.isEmpty() || !ItemStack.isSameItemSameComponents(at, rest) || at.getCount() >= max) continue;
                int moved = Math.min(max - at.getCount(), rest.getCount());
                at.grow(moved);
                rest.shrink(moved);
            }
        }
        for (NonNullList<ItemStack> list : lists) {
            for (int i = 0; i < list.size() && !rest.isEmpty(); i++) {
                if (!list.get(i).isEmpty()) continue;
                int moved = Math.min(max, rest.getCount());
                list.set(i, rest.copyWithCount(moved));
                rest.shrink(moved);
            }
        }
        return rest;
    }

    private ShopStorage() {}
}
