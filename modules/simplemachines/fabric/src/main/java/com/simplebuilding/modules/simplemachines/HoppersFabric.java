package com.simplebuilding.modules.simplemachines;

import com.simplemachines.hopper.HopperAutomation;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Fabric side of the mod hoppers (stage 3): a hopper whose front is no Vanilla {@code Container} inserts through Fabric's transfer
 * API ({@code ItemStorage.SIDED}), like SimpleBuilding's {@code FabricItemAutomation}. Nothing is registered for the hoppers
 * themselves: the transfer API wraps every {@code Container} block entity (sided for a {@code WorldlyContainer}) on its own.
 */
final class HoppersFabric {
    private HoppersFabric() {}

    static void init() {
        HopperAutomation.install((level, pos, side, stack) -> {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
            if (storage == null) return HopperAutomation.NO_HANDLER;
            // A nested transaction when another mod's transfer runs on this thread (openOuter would throw).
            TransactionContext current = Transaction.getCurrentUnsafe();
            try (Transaction transaction = current == null ? Transaction.openOuter() : Transaction.openNested(current)) {
                long moved = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                transaction.commit();
                return (int) moved;
            }
        });
    }
}
