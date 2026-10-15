package com.simpleshops.logic;

import com.simpleshops.block.EarningsStore;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The earnings chain of a shop: the sales block, then every sales storage directly below it, as long as ALL of them have the
 * same (non-null) owner and sit in loaded chunks. Everything runs on the server thread and changes both sides of a move in one go.
 */
public final class ShopStorage {
    /** Most storages below one sales block (placement refuses more; the chain never looks further). */
    public static final int MAX_CHAIN = 16;

    /** The store at {@code pos} if it is a live earnings store in a loaded chunk, else {@code null}. */
    public static @Nullable EarningsStore storeAt(Level level, BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SalesBlockEntity s && !s.isRemoved() && !s.isServerShop()) return s;
        if (be instanceof SalesStorageBlockEntity s && !s.isRemoved()) return s;
        return null;
    }

    /** The sales block's store first, then the storages below it that share its owner (stops at the first gap or foreign block). */
    public static List<EarningsStore> chain(SalesBlockEntity sales) {
        List<EarningsStore> out = new ArrayList<>();
        out.add(sales);
        UUID owner = sales.owner();
        Level level = sales.getLevel();
        if (owner == null || level == null || sales.isServerShop()) return out;
        BlockPos pos = sales.getBlockPos();
        for (int i = 0; i < MAX_CHAIN; i++) {
            pos = pos.below();
            if (!(storeAt(level, pos) instanceof SalesStorageBlockEntity s) || !owner.equals(s.owner())) break;
            out.add(s);
        }
        return out;
    }

    /** How many storages directly above {@code pos} (to the sales block) form an unbroken run with {@code owner}; -1 if there is no sales block at its top. */
    public static int storagesAbove(Level level, BlockPos pos, UUID owner) {
        int n = 0;
        BlockPos p = pos.above();
        for (int i = 0; i <= MAX_CHAIN + 1; i++) {
            EarningsStore s = storeAt(level, p);
            if (s == null || !owner.equals(s.owner())) return -1;
            if (s instanceof SalesBlockEntity) return n;
            n++;
            p = p.above();
        }
        return -1;
    }

    /** Hopper-like step: the first stack of the store directly above moves into {@code self}, if the owners are the same. */
    public static boolean pull(SalesStorageBlockEntity self) {
        Level level = self.getLevel();
        if (level == null || self.isRemoved() || self.owner() == null) return false;
        EarningsStore above = storeAt(level, self.getBlockPos().above());
        if (above == null || !self.owner().equals(above.owner())) return false;
        NonNullList<ItemStack> from = above.earnings();
        for (int i = 0; i < from.size(); i++) {
            ItemStack stack = from.get(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = insert(List.of(self.earnings()), stack.copy());
            if (rest.getCount() == stack.getCount()) continue;
            from.set(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
            above.changed();
            self.changed();
            return true;
        }
        return false;
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
