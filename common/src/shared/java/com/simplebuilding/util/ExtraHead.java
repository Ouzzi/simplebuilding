package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The extra head slot of the player inventory (owner N49, 2026-10-12): above the off-hand slot, level with the
 * helmet slot. It takes one mob head (every item of {@code #minecraft:skulls} - Vanilla's and the mod heads - plus
 * other mods' heads through {@code #simplebuilding:extra_head_slot}) or a carved pumpkin. The head works as if worn:
 * a carved pumpkin keeps endermen calm, a mob head lowers that mob's detection range like in the helmet slot, the
 * SimpleBuilding head abilities apply; it is drawn on the player and a helmet sits over it.
 *
 * <p>The stack lives in the player's synced entity data ({@code PlayerExtraHeadMixin}): saved with the player, sent
 * to every client that sees the player (rendering), dropped on death unless {@code keepInventory}. Every menu slot
 * reads and writes that one value through {@link #container}, so two open menus cannot hold two copies.
 */
public final class ExtraHead {
    /** Further heads of other mods (optional; {@code #minecraft:skulls} already holds Vanilla's and SimpleBuilding's). */
    public static final TagKey<Item> EXTRA_HEAD_ITEMS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "extra_head_slot"));
    public static final String SAVE_KEY = "SimpleBuildingExtraHead";
    /** Empty-slot icon: a skull silhouette (tools/textures/resonance_heart_2026_10_09.py). */
    public static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "container/slot/extra_head");

    private ExtraHead() {
    }

    /** Implemented by {@code Player} through {@code PlayerExtraHeadMixin}. */
    public interface Holder {
        ItemStack simplebuilding$getExtraHead();

        void simplebuilding$setExtraHead(ItemStack stack);
    }

    /** Whether {@code stack} may go into the extra head slot. */
    public static boolean fits(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.SKULLS) || stack.is(Items.CARVED_PUMPKIN) || stack.is(EXTRA_HEAD_ITEMS));
    }

    /** The head in the extra slot of {@code entity}, or an empty stack (not a player, nothing in it). */
    public static ItemStack get(LivingEntity entity) {
        return entity instanceof Holder holder ? holder.simplebuilding$getExtraHead() : ItemStack.EMPTY;
    }

    public static void set(Player player, ItemStack stack) {
        if (player instanceof Holder holder) {
            holder.simplebuilding$setExtraHead(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    /** Whether {@code entity} wears {@code item} on its head: in the helmet slot or in the extra head slot. */
    public static boolean wears(LivingEntity entity, Item item) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(item) || get(entity).is(item);
    }

    /** A one-slot view of the player's extra head; every write goes back into the synced entity data. */
    public static Container container(Player player) {
        return new Container() {
            @Override
            public int getContainerSize() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return get(player).isEmpty();
            }

            @Override
            public ItemStack getItem(int slot) {
                return slot == 0 ? get(player) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack removeItem(int slot, int count) {
                ItemStack current = getItem(slot);
                if (current.isEmpty() || count <= 0) {
                    return ItemStack.EMPTY;
                }
                ItemStack copy = current.copy();
                ItemStack taken = copy.split(count);
                set(player, copy);
                return taken;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                ItemStack current = getItem(slot);
                set(player, ItemStack.EMPTY);
                return current;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                if (slot == 0) {
                    set(player, stack);
                }
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                // a stack changed in place (count, components): store a fresh copy so the change reaches the clients
                ItemStack current = get(player);
                set(player, current.isEmpty() ? ItemStack.EMPTY : current.copy());
            }

            @Override
            public boolean stillValid(Player other) {
                return other == player;
            }

            @Override
            public void clearContent() {
                set(player, ItemStack.EMPTY);
            }
        };
    }

    /** The menu slot of the extra head: one head or carved pumpkin, the skull silhouette when empty. */
    public static final class HeadSlot extends Slot {
        public HeadSlot(Player player, int x, int y) {
            super(container(player), 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return fits(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_ICON;
        }
    }

    /**
     * How a helmet fits over the head in the extra slot (owner N49: per head, visible, no z-fighting): the helmet is
     * scaled by {@code helmetScale} around the neck pivot so it encloses the head; {@code onTop} heads (the tiny
     * silverfish head) are drawn on top of the helmet instead, the helmet stays as it is.
     */
    public record Fit(float helmetScale, boolean onTop) {
        /** Mob skulls: 8 px x 1.1875 = 9.5 px from the neck up; a helmet (10 px, 1 px below the neck) scaled 1.1. */
        public static final Fit SKULL = new Fit(1.1F, false);
    }

    /** The fit for this head item (by its registry path, so mod heads of other mods get the default). */
    public static Fit fit(ItemStack head) {
        if (head.is(Items.CARVED_PUMPKIN)) {
            return new Fit(1.12F, false); // the pumpkin block is drawn 10 px wide like the helmet
        }
        if (head.is(Items.PIGLIN_HEAD)) {
            return new Fit(1.3F, false); // 10 px wide snout head
        }
        if (head.is(Items.DRAGON_HEAD)) {
            return new Fit(1.75F, false);
        }
        String path = BuiltInRegistries.ITEM.getKey(head.getItem()).getPath();
        if (path.equals("silverfish_head")) {
            return new Fit(1.0F, true); // 4 x 3 x 4 px: inside the player's head, so it sits on top of the helmet
        }
        if (path.equals("shulker_head") || path.equals("breeze_head")) {
            return new Fit(1.3F, false);
        }
        return Fit.SKULL;
    }
}
