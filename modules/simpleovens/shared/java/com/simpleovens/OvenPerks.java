package com.simpleovens;

import com.simpleovens.block.OvenBlock;
import com.simpleovens.config.OvensConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What the tiers change (moved from SimpleBuilding's {@code FurnaceTierPerks}): extra cooking ticks per game tick, and
 * double experience for Netherite and Enderite. Output counts always come from the recipe. PLAN40 simpleovens-2 (owner A):
 * the fuel burns along with the extra ticks, so one fuel item smelts as many items as in the Vanilla machine (like the
 * Vanilla blast furnace: faster, not cheaper); {@code fuelEfficiency} in the config brings back the old saving.
 * Inputs in {@code #simpleovens:furnace_bonus_excluded} (cracked diamonds: a lossless crafting cycle would farm
 * experience) get no extra experience.
 */
public final class OvenPerks {
    public static final TagKey<Item> FURNACE_BONUS_EXCLUDED = TagKey.create(Registries.ITEM, SimpleOvens.id("furnace_bonus_excluded"));

    private OvenPerks() {
    }

    public static @Nullable OvenTier tierOf(BlockState state) {
        return state.getBlock() instanceof OvenBlock oven ? oven.tier() : null;
    }

    /** Whether this finished smelt counts twice (double experience). */
    public static boolean doublesExperience(BlockState state, @Nullable RecipeHolder<?> recipe) {
        OvenTier tier = tierOf(state);
        return recipe != null && tier != null && tier.upper() && SimpleOvens.on() && !inputTouches(recipe, FURNACE_BONUS_EXCLUDED);
    }

    /** Speed against the Vanilla machine (shipped defaults 2, 4, 8); 1 for any other block. */
    public static int speedFactor(BlockState state) {
        OvenTier tier = tierOf(state);
        return tier == null ? 1 : tier.speed();
    }

    /**
     * After the Vanilla server tick: while the furnace burns and cooks, adds the tier's extra ticks to the cooking
     * progress, never reaching the full cook time (Vanilla finishes the item on its own tick), and burns as many extra fuel
     * ticks (unless {@code fuelEfficiency}); a furnace whose fuel runs out this way lights the next fuel on the next tick.
     */
    public static void boost(BlockState state, ContainerData data) {
        OvenTier tier = tierOf(state);
        if (tier == null || !SimpleOvens.on()) return;
        int cookTime = data.get(2);
        int totalTime = data.get(3);
        if (data.get(0) <= 0 || cookTime <= 0 || totalTime <= 0) return;
        int extraTicks = tier.extraCookTicks();
        if (extraTicks <= 0) return;
        int boosted = Math.min(cookTime + extraTicks, totalTime - 1);
        data.set(2, boosted);
        if (!OvensConfig.fuelEfficiency && boosted > cookTime) data.set(0, Math.max(0, data.get(0) - (boosted - cookTime)));
    }

    @SuppressWarnings("deprecation")
    private static boolean inputTouches(RecipeHolder<?> recipe, TagKey<Item> tag) {
        if (!(recipe.value() instanceof AbstractCookingRecipe cooking)) return false;
        return cooking.input().items().anyMatch((Holder<Item> holder) -> holder.is(tag));
    }
}
