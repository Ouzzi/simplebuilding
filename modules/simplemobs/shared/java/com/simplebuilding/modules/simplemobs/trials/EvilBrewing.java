package com.simplebuilding.modules.simplemobs.trials;

import com.simplebuilding.modules.simplemobs.WitherMobs;
import com.simplebuilding.modules.simplemobs.WitherWings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The recipe of the soul-fuelled Brewing Stand (BS27-5): 1-3 Ominous Bottles below + a Nether Heart above = that many Evil Potions of the
 * bottles' stages (one heart for all bottles, "3 per heart"); an Ender Heart gives Void Potions. Only with a soul charge: the fuel slot
 * also takes Soulfire Powder, which gives {@value #CHARGES} charges; every Evil / Void brew takes one. Vanilla recipes and fuel stay as they are.
 */
public final class EvilBrewing {
    public static final int CHARGES = 20;
    public static final int BREW_TICKS = 400;
    public static final int INGREDIENT_SLOT = 3, FUEL_SLOT = 4;

    private EvilBrewing() {}

    /** The tier the stand would brew, {@code null} when heart and bottle are not both there (the chain's own switches are not asked here). */
    public static @Nullable TrialKind recipe(NonNullList<ItemStack> items) {
        TrialKind kind = heart(items.get(INGREDIENT_SLOT).getItem());
        if (kind == null) return null;
        for (int i = 0; i < 3; i++) if (items.get(i).is(Items.OMINOUS_BOTTLE)) return kind;
        return null;
    }

    public static @Nullable TrialKind heart(Item item) {
        return item == WitherMobs.HEART ? TrialKind.EVIL : item == WitherWings.ENDER_HEART ? TrialKind.VOID : null;
    }

    /** The potion a bottle becomes: the same stage (Ominous Bottle amplifier 0-4 -&gt; stage 1-5). */
    public static ItemStack result(ItemStack bottle, TrialKind kind) {
        OminousBottleAmplifier amplifier = bottle.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
        int stage = amplifier == null ? 1 : Math.max(1, Math.min(Trials.STAGES, amplifier.value() + 1));
        return new ItemStack((kind == TrialKind.VOID ? Trials.VOID_POTIONS : Trials.EVIL_POTIONS)[stage - 1]);
    }

    /** The brew is done: every Ominous Bottle becomes its potion, one heart is used up. */
    public static void finish(Level level, BlockPos pos, NonNullList<ItemStack> items, TrialKind kind) {
        for (int i = 0; i < 3; i++) {
            if (items.get(i).is(Items.OMINOUS_BOTTLE)) items.set(i, result(items.get(i), kind));
        }
        items.get(INGREDIENT_SLOT).shrink(1);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.levelEvent(1035, pos, 0);
    }
}
