package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

public final class LegacySpatulaMigration {

    private static final Box WORLD_SCAN_BOX = new Box(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0);

    private LegacySpatulaMigration() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(LegacySpatulaMigration::migrateWorlds);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> migratePlayer(handler.player));
    }

    private static void migrateWorlds(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, WORLD_SCAN_BOX, entity -> true)) {
                ItemStack converted = convertStack(itemEntity.getStack());
                if (converted != itemEntity.getStack()) {
                    itemEntity.setStack(converted);
                }
            }
        }
    }

    private static void migratePlayer(ServerPlayerEntity player) {
        migrateInventory(player.getInventory());

        ScreenHandler handler = player.currentScreenHandler;
        if (handler != null) {
            for (Slot slot : handler.slots) {
                ItemStack stack = slot.getStack();
                ItemStack converted = convertStack(stack);
                if (converted != stack) {
                    slot.setStack(converted);
                }
            }
        }
    }

    private static void migrateInventory(Inventory inventory) {
        for (int slotIndex = 0; slotIndex < inventory.size(); slotIndex++) {
            ItemStack stack = inventory.getStack(slotIndex);
            ItemStack converted = convertStack(stack);
            if (converted != stack) {
                inventory.setStack(slotIndex, converted);
            }
        }
    }

    private static ItemStack convertStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        Item target = getReplacement(stack.getItem());
        if (target == null) {
            return stack;
        }

        ItemStack converted = new ItemStack(target, stack.getCount());
        converted.applyChanges(stack.getComponentChanges());
        return converted;
    }

    private static Item getReplacement(Item item) {
        if (item == ModItems.STONE_SPATULA) return ModItems.STONE_CHISEL;
        if (item == ModItems.COPPER_SPATULA) return ModItems.COPPER_CHISEL;
        if (item == ModItems.IRON_SPATULA) return ModItems.IRON_CHISEL;
        if (item == ModItems.GOLD_SPATULA) return ModItems.GOLD_CHISEL;
        if (item == ModItems.DIAMOND_SPATULA) return ModItems.DIAMOND_CHISEL;
        if (item == ModItems.NETHERITE_SPATULA) return ModItems.NETHERITE_CHISEL;
        return null;
    }
}