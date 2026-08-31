package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class LegacySpatulaMigration {

    private static final AABB WORLD_SCAN_BOX = new AABB(-30000000.0, -64.0, -30000000.0, 30000000.0, 320.0, 30000000.0);

    private LegacySpatulaMigration() {
    }

    public static void migrateWorlds(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, WORLD_SCAN_BOX, entity -> true)) {
                ItemStack converted = convertStack(itemEntity.getItem());
                if (converted != itemEntity.getItem()) {
                    itemEntity.setItem(converted);
                }
            }
        }
    }

    public static void migratePlayer(ServerPlayer player) {
        migrateInventory(player.getInventory());

        AbstractContainerMenu handler = player.containerMenu;
        if (handler != null) {
            for (Slot slot : handler.slots) {
                ItemStack stack = slot.getItem();
                ItemStack converted = convertStack(stack);
                if (converted != stack) {
                    slot.setByPlayer(converted);
                }
            }
        }
    }

    private static void migrateInventory(Container inventory) {
        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            ItemStack stack = inventory.getItem(slotIndex);
            ItemStack converted = convertStack(stack);
            if (converted != stack) {
                inventory.setItem(slotIndex, converted);
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
        converted.applyComponentsAndValidate(stack.getComponentsPatch());
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
