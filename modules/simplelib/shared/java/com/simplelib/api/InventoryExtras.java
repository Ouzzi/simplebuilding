package com.simplelib.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Extra slots a mod adds to its player inventory (split stage 8c-2, docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md): a menu that
 * rebuilds the player inventory on its own - Simple Storage's backpack menu - asks here for them, so SimpleBuilding's extra
 * head slot (owner N49) is there too without either mod importing the other. Server and client build the menu from the
 * same list (register on both sides, in common init). Client drawing: {@link com.simplelib.api.client.InventoryExtrasClient}.
 */
public final class InventoryExtras {
    private static final List<ExtraSlot> SLOTS = new CopyOnWriteArrayList<>();

    private InventoryExtras() {}

    /** One extra slot of the player inventory. */
    public interface ExtraSlot {
        /** The slot, placed relative to the top left ({@code x}, {@code y}) of the 176 px wide player inventory panel. */
        Slot create(Player player, int x, int y);

        /** Whether a shift-clicked {@code stack} from the inventory goes into this slot first while it is empty. */
        default boolean takesOnQuickMove(Player player, ItemStack stack) {
            return false;
        }
    }

    public static void register(ExtraSlot slot) {
        SLOTS.add(slot);
    }

    public static void unregister(ExtraSlot slot) {
        SLOTS.remove(slot);
    }

    /** Every registered extra slot, in registration order (the menu adds them in this order). */
    public static List<ExtraSlot> all() {
        return List.copyOf(SLOTS);
    }
}
