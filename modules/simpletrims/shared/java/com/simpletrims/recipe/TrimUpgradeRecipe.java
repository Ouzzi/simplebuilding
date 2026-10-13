package com.simpletrims.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simpletrims.TrimUpgrades;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * {@code simpletrims:trim_upgrade}: template + armor + material at the smithing table give the same armor with the
 * upgrade ({@link TrimUpgrades}). A real smithing recipe instead of SimpleBuilding's placeholder recipe plus menu mixin:
 * Vanilla's smithing table, the auto smither and every recipe display use it as it is. It does not match once the
 * armor is at its cap (Glowing once, Radiance 5, Pulsating once).
 */
public class TrimUpgradeRecipe implements SmithingRecipe {
    public static final MapCodec<TrimUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
            Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(r -> r.addition)
    ).apply(i, TrimUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrimUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.template,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
            TrimUpgradeRecipe::new);
    public static final RecipeSerializer<TrimUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;

    public TrimUpgradeRecipe(Ingredient template, Ingredient base, Ingredient addition) {
        this.template = template;
        this.base = base;
        this.addition = addition;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!template.test(input.template()) || !base.test(input.base()) || !addition.test(input.addition())) return false;
        ItemStack result = TrimUpgrades.result(input.template(), input.base(), input.addition());
        return result != null && !result.isEmpty();
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack result = TrimUpgrades.result(input.template(), input.base(), input.addition());
        return result == null ? ItemStack.EMPTY : result;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(template);
    }

    @Override
    public Ingredient baseIngredient() {
        return base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(addition);
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(List.of(Optional.of(template), Optional.of(base), Optional.of(addition)));
    }

    /** Like Vanilla's trim display: the result slot shows the armor (it keeps its look, the upgrade is invisible here). */
    @Override
    public List<RecipeDisplay> display() {
        return List.of(new SmithingRecipeDisplay(template.display(), base.display(), addition.display(), base.display(),
                new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)));
    }
}
