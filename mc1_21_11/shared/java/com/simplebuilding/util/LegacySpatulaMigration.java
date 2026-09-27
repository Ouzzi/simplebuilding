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

/**
 * Turns the pre-rename "spatula" items into the matching chisels.
 *
 * <p>What gets converted, and when:
 * <ul>
 *   <li>on every server start, every <em>loaded</em> item entity in every dimension - over the
 *       dimension's own build height plus {@link #SCAN_MARGIN_Y} above and below. Until
 *       2026-09-27 (audit #39) the box was a fixed -64..320, so items flying above the world or
 *       lying in a data-pack dimension with another height were never converted;</li>
 *   <li>on every join, the player's inventory and the menu they have open.</li>
 * </ul>
 *
 * <p><b>Storage limit (by design):</b> spatulas inside containers - chests, barrels, shulker boxes,
 * ender chests, bundles, item frames, unloaded chunks - are not searched. They stay spatulas until
 * a player picks them up and rejoins, or opens the container while joining. A spatula still works
 * as a backward chisel in the meantime, and it has a name and a model, so nothing is lost; the
 * wiki says so under the chisel. Converting on every stack load would need a DataFixer-style hook
 * into every item codec for six legacy items, which is not worth it.
 */
public final class LegacySpatulaMigration {

    /** How far above the build limit and below the floor the start-up scan still looks for item entities. */
    static final double SCAN_MARGIN_Y = 4096.0;

    private LegacySpatulaMigration() {
    }

    public static void migrateWorlds(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            // Build height plus a generous margin: item entities fly above and fall below the world.
            AABB box = new AABB(-30000000.0, world.getMinY() - SCAN_MARGIN_Y, -30000000.0,
                    30000000.0, world.getMaxY() + 1.0 + SCAN_MARGIN_Y, 30000000.0);
            for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, box, entity -> true)) {
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
