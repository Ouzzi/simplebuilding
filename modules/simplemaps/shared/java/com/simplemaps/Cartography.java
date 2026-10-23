package com.simplemaps;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
     * wayfinder + empty map → one copy with the waypoints, the original stays (maps do not stack, owner 21.10.;
     * {@link #resultSlot}); wayfinder + filled map → the wayfinder, taking over the
     * filled map's area on take; wayfinder + wayfinder → the first one, taking over the second one's area and filling
     * its free waypoint slots with the second one's waypoints.
     */
    public static ItemStack result(ItemStack a, ItemStack b, ServerLevel level) {
        boolean wa = MapsItems.isWayfinder(a), wb = MapsItems.isWayfinder(b);
        if (!wa && !wb) return ItemStack.EMPTY;
        if (wa && wb) return a.getItem() instanceof MegaWayfinderMapItem || b.getItem() instanceof MegaWayfinderMapItem ? ItemStack.EMPTY : combine(a, b, level);
        ItemStack way = wa ? a : b, other = wa ? b : a;
        // The mega map holds several dimensions: it is only copied, never extended or combined (owner B23-K1).
        if ((a.getItem() instanceof MegaWayfinderMapItem || b.getItem() instanceof MegaWayfinderMapItem) && !other.is(Items.MAP)) return ItemStack.EMPTY;
        if (other.is(Items.MAP)) {
            return MapsConfig.allowCopy && wa ? way.copyWithCount(1) : ItemStack.EMPTY;
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

    /** Copying: a wayfinder map on top, an empty map below. */
    public static boolean isCopy(ItemStack map, ItemStack additional) {
        return MapsItems.isWayfinder(map) && additional.is(Items.MAP);
    }

    /**
     * The table's result slot: taking a copy uses up only the empty map, the original stays on top (like a blueprint
     * copy); everything else is Vanilla's. Wraps the slot, so later wrappers (blueprint, sheet music) keep working.
     */
    public static Slot resultSlot(Slot original, AbstractContainerMenu menu, ContainerLevelAccess access) {
        Slot slot = new Slot(original.container, original.getContainerSlot(), original.x, original.y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                if (!isCopy(menu.getSlot(0).getItem(), menu.getSlot(1).getItem())) {
                    original.onTake(player, carried);
                    return;
                }
                menu.getSlot(1).remove(1);
                carried.getItem().onCraftedBy(carried, player);
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F));
                super.onTake(player, carried);
            }

            @Override
            public ItemStack safeClone(Player player) {
                return original.safeClone(player);
            }
        };
        slot.index = original.index;
        return slot;
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
