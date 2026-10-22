package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

/**
 * The extra head slot of the player inventory (owner N49, 2026-10-12): above the off-hand slot, level with the
 * helmet slot. It takes one mob head (every item of {@code #minecraft:skulls} - Vanilla's and the mod heads - plus
 * other mods' heads through {@code #simplebuilding:extra_head_slot}) or a carved pumpkin. The head works as if worn:
 * a carved pumpkin keeps endermen calm, a mob head lowers that mob's detection range like in the helmet slot, the
 * SimpleBuilding head abilities apply, so do the head item's attribute modifiers and its freeze immunity
 * ({@link ExtraHeadEffects}, {@code LivingEntityExtraHeadFreezeMixin}); putting it in plays the equip sound
 * ({@code PlayerExtraHeadMixin}); it is drawn on the player like a worn head and a helmet sits over it. With a head in
 * the helmet slot as well, both heads' effects apply and only the extra head's model is drawn (owner N55, {@link #heads}).
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

        /** The attribute modifiers {@link ExtraHeadEffects} has put on the player for the extra head. */
        List<ExtraHeadEffects.Applied> simplebuilding$getAppliedHeadModifiers();

        void simplebuilding$setAppliedHeadModifiers(List<ExtraHeadEffects.Applied> applied);
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

    /**
     * Owner SP22-11: a right-click into the air with a mob head (not a carved pumpkin) puts it into the extra head slot
     * like Vanilla puts on a helmet ({@code Equippable#swapWithEquipmentSlot}): an empty slot takes it, a filled one swaps
     * (from a stack one goes in, the old head into the inventory or dropped), the same head or a bound one refuses. The
     * equip sound comes from {@code PlayerExtraHeadMixin}. A click on a block places the head as in Vanilla before
     * ({@code useOn}). Null for any other item: Vanilla goes on ({@code ItemUseExtraHeadMixin}).
     */
    public static net.minecraft.world.InteractionResult useInAir(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        if (!(player instanceof Holder) || !fits(inHand) || inHand.is(Items.CARVED_PUMPKIN) || player.isSpectator()) {
            return null;
        }
        ItemStack worn = get(player);
        if ((net.minecraft.world.item.enchantment.EnchantmentHelper.has(worn,
                net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && !player.isCreative())
                || ItemStack.isSameItemSameComponents(inHand, worn)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (!player.level().isClientSide()) {
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(inHand.getItem()));
        }
        if (inHand.getCount() <= 1) {
            ItemStack toHand = worn.isEmpty() ? inHand : worn.copy();
            set(player, player.isCreative() ? inHand.copy() : inHand.copyAndClear());
            return net.minecraft.world.InteractionResult.SUCCESS.heldItemTransformedTo(toHand);
        }
        ItemStack old = worn.copy();
        set(player, inHand.consumeAndReturn(1, player));
        if (!old.isEmpty() && !player.getInventory().add(old)) {
            com.simplebuilding.version.McVersion.drop(player, old, false, false);
        }
        return net.minecraft.world.InteractionResult.SUCCESS.heldItemTransformedTo(inHand);
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
     * scaled by {@code helmetScale} around the neck pivot so it encloses the head. Mod heads replace the wearer's head
     * like in the helmet slot (owner 2026-10-01), so even the tiny ones need no special place any more.
     */
    public record Fit(float helmetScale) {
        /** Vanilla mob skulls: 8 px x 1.1875 = 9.5 px from the neck up; a helmet (10 px, 1 px below the neck) scaled 1.1. */
        public static final Fit SKULL = new Fit(1.1F);
    }

    /**
     * The fit for this head item. Mod heads (Simple Mobheads) are sized by the worn edge their look reports through
     * {@link HeadLooks#skullEdge} (owner N55: as big as on the mob), the helmet then clears that edge by 0.5 px per side.
     */
    public static Fit fit(ItemStack head) {
        if (head.is(Items.CARVED_PUMPKIN)) {
            return new Fit(1.12F); // the pumpkin block is drawn 10 px wide like the helmet
        }
        if (head.is(Items.PIGLIN_HEAD)) {
            return new Fit(1.3F); // 10 px wide snout head
        }
        if (head.is(Items.DRAGON_HEAD)) {
            return new Fit(1.75F);
        }
        if (head.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skull) {
            float edge = HeadLooks.skullEdge(skull.getType());
            if (edge > 0.0F) {
                return new Fit(Math.max(1.0F, (edge + 1.0F) / 10.0F));
            }
        }
        return Fit.SKULL;
    }

    /** Whether the skull of {@code head} is a mod skull type: it replaces the wearer's head instead of sitting over it. */
    public static boolean replacesHead(ItemStack head) {
        return head.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractSkullBlock skull
                && !(skull.getType() instanceof SkullBlock.Types);
    }

    /**
     * The heads {@code entity} wears in the order they are drawn (owner N55): the extra head slot first - its model is
     * the one drawn, it wins over a head in the helmet slot - then a head in the helmet slot. The effects of every
     * entry apply; only the first is shown.
     */
    public static List<ItemStack> heads(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>(2);
        ItemStack extra = get(entity);
        if (!extra.isEmpty()) {
            out.add(extra);
        }
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && (helmet.is(ItemTags.SKULLS) || helmet.is(Items.CARVED_PUMPKIN) || helmet.is(EXTRA_HEAD_ITEMS))) {
            out.add(helmet);
        }
        return out;
    }
}
