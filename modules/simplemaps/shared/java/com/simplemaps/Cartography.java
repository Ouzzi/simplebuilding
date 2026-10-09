package com.simplemaps;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Cartography table results with a wayfinder map (owner F4/F5, Feature 1/8). Both input slots accept wayfinder and
 * filled maps, so either order works; the menu still removes one item from each input when the result is taken.
 */
public final class Cartography {
    private Cartography() {}

    public static boolean involves(ItemStack a, ItemStack b) {
        return MapsItems.isWayfinder(a) || MapsItems.isWayfinder(b);
    }

    /**
     * The result for these inputs, or {@link ItemStack#EMPTY} when the combination is not allowed:
     * wayfinder + empty map → two copies with the waypoints; wayfinder + filled map → the wayfinder, taking over the
     * filled map's area on take; wayfinder + wayfinder → the first one, taking over the second one's area and filling
     * its free waypoint slots with the second one's waypoints.
     */
    public static ItemStack result(ItemStack a, ItemStack b, ServerLevel level) {
        boolean wa = MapsItems.isWayfinder(a), wb = MapsItems.isWayfinder(b);
        if (!wa && !wb) return ItemStack.EMPTY;
        if (wa && wb) return combine(a, b, level);
        ItemStack way = wa ? a : b, other = wa ? b : a;
        if (other.is(Items.MAP)) {
            return MapsConfig.allowCopy && wa ? way.copyWithCount(2) : ItemStack.EMPTY;
        }
        if (other.has(DataComponents.MAP_ID)) {
            if (!MapsConfig.allowExtend) return ItemStack.EMPTY;
            MapItemSavedData vanilla = MapItem.getSavedData(other, level);
            if (vanilla == null || !compatible(way, vanilla, level)) return ItemStack.EMPTY;
            ItemStack out = way.copyWithCount(1);
            out.set(MapsComponents.PENDING, new MapsComponents.Pending(MapsComponents.Pending.VANILLA, other.get(DataComponents.MAP_ID).id()));
            return out;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack combine(ItemStack a, ItemStack b, ServerLevel level) {
        if (!MapsConfig.allowCombine || a.getItem() != b.getItem()) return ItemStack.EMPTY;
        MapsComponents.MapRef ra = a.get(MapsComponents.MAP_ID), rb = b.get(MapsComponents.MAP_ID);
        if (rb == null || ra != null && ra.id() == rb.id()) return ItemStack.EMPTY;
        WayfinderData db = WayfinderData.get(level.getServer(), rb.id());
        if (ra != null) {
            WayfinderData da = WayfinderData.get(level.getServer(), ra.id());
            if (da.dimension().isPresent() && db.dimension().isPresent() && !da.dimension().equals(db.dimension())) return ItemStack.EMPTY;
        }
        ItemStack out = a.copyWithCount(1);
        out.set(MapsComponents.PENDING, new MapsComponents.Pending(MapsComponents.Pending.WAYFINDER, rb.id()));
        Waypoints merged = a.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY)
                .mergedWith(b.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY));
        if (!merged.list().isEmpty()) out.set(MapsComponents.WAYPOINTS, merged);
        return out;
    }

    /** A filled map fits when its dimension is the wayfinder's bound dimension, or one this unbound wayfinder accepts. */
    private static boolean compatible(ItemStack way, MapItemSavedData vanilla, ServerLevel level) {
        MapsComponents.MapRef ref = way.get(MapsComponents.MAP_ID);
        if (ref != null) {
            var bound = WayfinderData.get(level.getServer(), ref.id()).dimension();
            if (bound.isPresent()) return bound.get().equals(vanilla.dimension);
        }
        ServerLevel target = level.getServer().getLevel(vanilla.dimension);
        return target != null && ((WayfinderMapItem) way.getItem()).accepts(target);
    }

    /** The table's map slot: Vanilla maps plus wayfinder maps. */
    public static Slot mapSlot(Container container) {
        return new Slot(container, 0, 15, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(DataComponents.MAP_ID) || MapsItems.isWayfinder(stack);
            }
        };
    }

    /** The table's additional slot: paper, empty map, glass pane, plus filled and wayfinder maps. */
    public static Slot additionalSlot(Container container) {
        return new Slot(container, 1, 15, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.GLASS_PANE)
                        || stack.has(DataComponents.MAP_ID) || MapsItems.isWayfinder(stack);
            }
        };
    }
}
