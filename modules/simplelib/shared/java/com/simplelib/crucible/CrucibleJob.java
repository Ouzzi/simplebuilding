package com.simplelib.crucible;

import com.simplelib.config.LibConfig;
import com.simplelib.registry.LibTags;
import com.simplelib.warm.Warm;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.jetbrains.annotations.Nullable;

/**
 * What a crucible slot does with its item (owner F15 C: all four cooking recipe types, each with
 * its own time). Among the recipes the current heat allows, the fastest wins; if none is allowed,
 * the job carries the heat it would need (slot shows blue). Warmable food without a recipe is
 * warmed instead: the whole stack at once, it stays in its slot.
 */
public record CrucibleJob(int ticks, ItemStack result, float experience, HeatLevel minHeat, boolean warming) {
    private static final List<RecipeType<? extends AbstractCookingRecipe>> TYPES =
            List.of(RecipeType.CAMPFIRE_COOKING, RecipeType.SMOKING, RecipeType.SMELTING, RecipeType.BLASTING);

    public boolean allowedAt(HeatLevel heat) {
        return heat.atLeast(minHeat);
    }

    /** The job for {@code stack} at {@code heat}, or null when the crucible cannot do anything with it. */
    public static @Nullable CrucibleJob find(ServerLevel level, ItemStack stack, HeatLevel heat) {
        if (stack.isEmpty()) return null;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        CrucibleJob allowed = null;
        CrucibleJob lowest = null;
        for (RecipeType<? extends AbstractCookingRecipe> type : TYPES) {
            CrucibleJob job = of(level, type, input);
            if (job == null) continue;
            if (job.allowedAt(heat)) {
                if (allowed == null || job.ticks < allowed.ticks) allowed = job;
            } else if (lowest == null || job.minHeat.ordinal() < lowest.minHeat.ordinal()
                    || job.minHeat == lowest.minHeat && job.ticks < lowest.ticks) {
                lowest = job;
            }
        }
        if (allowed != null) return allowed;
        if (lowest != null) return lowest;
        if (Warm.warmable(stack) && !Warm.isWarm(stack, level)) {
            return new CrucibleJob(LibConfig.warmBaseTicks, stack.copy(), 0.0F, HeatLevel.MEDIUM, true);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable CrucibleJob of(ServerLevel level, RecipeType<? extends AbstractCookingRecipe> type,
                                           SingleRecipeInput input) {
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> found =
                level.recipeAccess().getRecipeFor((RecipeType<AbstractCookingRecipe>) type, input, level);
        if (found.isEmpty()) return null;
        AbstractCookingRecipe recipe = found.get().value();
        ItemStack result = recipe.assemble(input);
        if (result.isEmpty()) return null;
        HeatLevel need = requiredHeat(input.item(), type);
        return new CrucibleJob(Math.max(1, recipe.cookingTime()), result, recipe.experience(), need, false);
    }

    /** Shared by actual cooking and partner recipe displays. */
    public static HeatLevel requiredHeat(ItemStack input, RecipeType<?> type) {
        return input.is(LibTags.NEEDS_EXTREME_HEAT) ? HeatLevel.EXTREME : HeatLevel.required(type);
    }
}
