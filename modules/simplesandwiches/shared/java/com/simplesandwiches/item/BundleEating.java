package com.simplesandwiches.item;

import com.simplesandwiches.config.SandwichConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

/**
 * Owner decision F10 (2026-10-04): instead of a food basket, any Vanilla bundle lets its holder eat
 * the top item directly. "Top" is the item the bundle hands out next ({@code removeOne}): the
 * selected item, otherwise the first one. It must be food ({@code FOOD} + {@code CONSUMABLE}) without a
 * use remainder (stews, honey bottles stay out). Eating uses that item's own consume time, animation,
 * sound, particles, nutrition and effects; one piece leaves the bundle. Anything else on top keeps the
 * Vanilla behavior. Config {@code bundleEating}.
 */
public final class BundleEating {
    public static ItemStack top(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return ItemStack.EMPTY;
        int selected = contents.getSelectedItemIndex();
        int index = selected >= 0 && selected < contents.size() ? selected : 0;
        return contents.items().get(index).create();
    }

    public static boolean edible(ItemStack food) {
        return !food.isEmpty() && food.has(DataComponents.FOOD) && food.has(DataComponents.CONSUMABLE)
                && !food.has(DataComponents.USE_REMAINDER) && !(food.getItem() instanceof BundleItem)
                && !(food.getItem() instanceof net.minecraft.world.item.BucketItem)
                && !food.has(DataComponents.BUNDLE_CONTENTS);
    }

    /** The consumable to eat from this bundle, or null when the bundle should act like Vanilla. */
    public static Consumable eating(ItemStack bundle) {
        if (!SandwichConfig.bundleEating || !(bundle.getItem() instanceof BundleItem)) return null;
        ItemStack top = top(bundle);
        return edible(top) ? top.get(DataComponents.CONSUMABLE) : null;
    }

    public static InteractionResult use(Level level, Player player, InteractionHand hand, ItemStack bundle, Consumable consumable) {
        ItemStack top = top(bundle);
        if (!consumable.canConsume(player, top)) return InteractionResult.FAIL;
        if (consumable.consumeTicks() <= 0) {
            finish(level, player, bundle);
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    public static void tick(LivingEntity entity, ItemStack bundle, Consumable consumable, int ticksRemaining) {
        if (consumable.shouldEmitParticlesAndSounds(ticksRemaining)) {
            consumable.emitParticlesAndSounds(entity.getRandom(), entity, top(bundle), 5);
        }
    }

    /** Eats one piece of the top item and removes it from the bundle (not in creative, like Vanilla food). */
    public static ItemStack finish(Level level, LivingEntity entity, ItemStack bundle) {
        ItemStack top = top(bundle);
        if (!edible(top)) return bundle;
        Consumable consumable = top.get(DataComponents.CONSUMABLE);
        ItemStack piece = top.copyWithCount(1);
        consumable.onConsume(level, entity, piece);
        if (entity.hasInfiniteMaterials()) return bundle;
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return bundle;
        boolean selected = contents.getSelectedItemIndex() >= 0;
        BundleContents.Mutable mutable = contents.asMutable();
        ItemStack rest = mutable.removeOne();
        if (rest != null) {
            rest.shrink(1);
            if (!rest.isEmpty()) {
                mutable.tryInsert(rest);
                // The rest now sits first; keep it on top if it was the selected item.
                if (selected) mutable.toggleSelectedItem(0);
            }
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        return bundle;
    }

    private BundleEating() {}
}
