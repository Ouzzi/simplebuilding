package com.simplebuilding.modules.simpleinterfaces.sort;

import com.simplebuilding.modules.simpleinterfaces.SimpleInterfaces;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory sorting (WE25-3). The client only asks; this runs on the server. Region: the player's main inventory
 * (slots 9..35, no hotbar, no armor) or the whole container of a chest, barrel, ender chest or shulker box. Machines
 * and every other menu are refused. Order: item registry id (Vanilla registers in creative tab order), equal stacks merged.
 */
public final class ContainerSort {
    private ContainerSort() {}

    /** Menus the sort applies to; everything else (furnaces, anvils, mod machines) is left alone. */
    public static boolean eligible(AbstractContainerMenu menu) {
        return menu instanceof InventoryMenu || menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
    }

    private static boolean plain(Slot s) {
        return s.getClass() == Slot.class || s instanceof net.minecraft.world.inventory.ShulkerBoxSlot;
    }

    /** The slots one sort acts on when the player points at menu slot {@code index}; empty = not sortable. */
    public static List<Slot> region(AbstractContainerMenu menu, int index, Inventory inv) {
        if (!eligible(menu) || index < 0 || index >= menu.slots.size()) return List.of();
        Slot hit = menu.slots.get(index);
        boolean own = hit.container == inv;
        List<Slot> out = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (s.container != hit.container || !plain(s)) continue;
            if (own && (s.getContainerSlot() < 9 || s.getContainerSlot() > 35)) continue;
            out.add(s);
        }
        if (own) return hit.getContainerSlot() >= 9 && hit.getContainerSlot() <= 35 ? out : List.of();
        return menu instanceof InventoryMenu ? List.of() : out;
    }

    /** Server entry: sorts the region under {@code slotIndex} of the player's open menu. */
    public static boolean sort(ServerPlayer player, int containerId, int slotIndex) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!SimpleInterfaces.active() || menu.containerId != containerId || !menu.stillValid(player)) return false;
        List<Slot> region = region(menu, slotIndex, player.getInventory());
        if (region.isEmpty()) return false;
        List<ItemStack> in = new ArrayList<>();
        for (Slot s : region) {
            if (!s.getItem().isEmpty() && !s.mayPickup(player)) return false;
            in.add(s.getItem());
        }
        List<ItemStack> out = sorted(in, region.get(0).container.getMaxStackSize());
        for (int i = 0; i < region.size(); i++) {
            if (!out.get(i).isEmpty() && !region.get(i).mayPlace(out.get(i))) return false;
        }
        for (int i = 0; i < region.size(); i++) region.get(i).setByPlayer(out.get(i));
        menu.broadcastChanges();
        return true;
    }

    /** Pure sort: same size, empties last, equal stacks merged up to the stack limit, ordered by item id. Inputs stay untouched. */
    public static List<ItemStack> sorted(List<ItemStack> in, int containerLimit) {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack s : in) if (!s.isEmpty()) all.add(s.copy());
        all.sort(Comparator.<ItemStack>comparingInt(s -> BuiltInRegistries.ITEM.getId(s.getItem()))
                .thenComparingInt(s -> s.getComponentsPatch().hashCode()));
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack s : all) {
            int max = Math.min(s.getMaxStackSize(), containerLimit);
            for (ItemStack m : merged) {
                if (s.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(m, s) || m.getCount() >= max) continue;
                int moved = Math.min(max - m.getCount(), s.getCount());
                m.grow(moved);
                s.shrink(moved);
            }
            if (!s.isEmpty()) merged.add(s);
        }
        while (merged.size() < in.size()) merged.add(ItemStack.EMPTY);
        return merged;
    }
}
