package com.simplebuilding.platform;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric's side of {@link ItemAutomation}. Nothing is registered for the mod's machines: Fabric's
 * transfer API wraps every block entity that is a {@code Container} (sided for a
 * {@code WorldlyContainer}) on its own, and the game tests hold it to that.
 */
public final class FabricItemAutomation implements ItemAutomation {

    @Override
    public int insert(ServerLevel level, BlockPos pos, Direction side, ItemStack stack) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        if (storage == null) {
            return NO_HANDLER;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return (int) moved;
        }
    }

    @Override
    public int extract(ServerLevel level, BlockPos pos, Direction side, Item item, int amount) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        if (storage == null) {
            return NO_HANDLER;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = storage.extract(ItemVariant.of(item), amount, transaction);
            transaction.commit();
            return (int) moved;
        }
    }
}
