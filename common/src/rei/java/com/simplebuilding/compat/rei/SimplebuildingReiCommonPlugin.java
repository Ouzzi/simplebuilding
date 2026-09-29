package com.simplebuilding.compat.rei;

import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.SmithingDisplay;
import me.shedaniel.rei.plugin.common.displays.DefaultSmithingDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * REI support, common half: the mod's {@code simplebuilding:count_based_smithing} recipes in REI's
 * smithing category. REI turns recipes into displays on the (integrated or dedicated) server and
 * sends them to the client; its default plugin only knows the vanilla smithing classes, so without
 * this filler these recipes would be missing. The addition slot carries the real count (e.g. four
 * iron ingots for a copper to iron pickaxe), like the JEI {@code CountBasedSmithingExtension}.
 * The display is REI's own {@link DefaultSmithingDisplay}, whose serializer REI registers itself.
 *
 * <p>{@code simplebuilding:easter_smithing} stays hidden on purpose: it is not a
 * {@code SmithingTransformRecipe}, so REI's filler skips it, and its {@code display()} is empty, so
 * REI's client-side fallback (servers without REI) skips it too.
 *
 * <p>Only loaded by REI: {@code rei_common} entrypoint on Fabric, annotated subclass on NeoForge.
 */
public class SimplebuildingReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        registry.beginRecipeFiller(CountBasedSmithingRecipe.class)
                .filterType(RecipeType.SMITHING)
                .fill(SimplebuildingReiCommonPlugin::countBased);
    }

    private static DefaultSmithingDisplay countBased(RecipeHolder<CountBasedSmithingRecipe> holder) {
        CountBasedSmithingRecipe recipe = holder.value();
        EntryIngredient template = recipe.templateIngredient().map(EntryIngredients::ofIngredient).orElse(EntryIngredient.empty());
        EntryIngredient addition = recipe.additionIngredient()
                .map(ingredient -> EntryIngredients.ofItemStacks(ingredient.items()
                        .map(item -> new ItemStack(item, recipe.getAdditionCount()))
                        .toList()))
                .orElse(EntryIngredient.empty());
        return new DefaultSmithingDisplay(
                List.of(template, EntryIngredients.ofIngredient(recipe.baseIngredient()), addition),
                List.of(EntryIngredients.of(recipe.getResultStack())),
                Optional.of(SmithingDisplay.SmithingRecipeType.TRANSFORM),
                Optional.of(holder.id().identifier()));
    }
}
