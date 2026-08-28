package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.TransmuteResult;
import net.minecraft.world.level.Level;

public class CountBasedSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<CountBasedSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
            Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition),
            TransmuteResult.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            com.mojang.serialization.Codec.INT.fieldOf("addition_count").forGetter(recipe -> recipe.additionCount)
    ).apply(instance, CountBasedSmithingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CountBasedSmithingRecipe> STREAM_CODEC = StreamCodec.of(
            CountBasedSmithingRecipe::write, CountBasedSmithingRecipe::read
    );

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final TransmuteResult result;
    private final int additionCount;

    public CountBasedSmithingRecipe(Ingredient template, Ingredient base, Ingredient addition, TransmuteResult result, int additionCount) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
        this.additionCount = additionCount;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level world) {
        if (!this.template.test(input.template())) return false;
        if (!this.base.test(input.base())) return false;

        ItemStack additionStack = input.addition();
        return this.addition.test(additionStack) && additionStack.getCount() >= this.additionCount;
    }

    // MC 1.21.11: Recipe#assemble bekommt noch die HolderLookup.Provider mitgeliefert, und das
    // Gegenstueck zu ItemStackTemplate (26.2) heisst hier TransmuteResult. TransmuteResult#apply
    // uebernimmt die Komponenten des Basis-Stacks in das Ergebnis -- genau das, was die 26.2-Fassung
    // mit create() + applyComponentsAndValidate(base-Patch) getan hat (JSON-Feldnamen id/count/
    // components sind in beiden Versionen identisch).
    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return this.result.apply(input.base());
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    // --- SmithingRecipe Interface Methoden ---

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(this.template);
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(this.addition);
    }

    // --- IngredientPlacement (Korrigiert für 1.21.2+) ---
    @Override
    public PlacementInfo placementInfo() {
        // Die Methode heißt "forMultipleSlots", nicht "forMultiple"
        return PlacementInfo.createFromOptionals(List.of(
                Optional.of(this.template),
                Optional.of(this.base),
                Optional.of(this.addition)
        ));
    }

    @Override
    public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
        return ModRecipes.COUNT_BASED_SMITHING_SERIALIZER;
    }

    public int getAdditionCount() {
        return additionCount;
    }

    private static void write(RegistryFriendlyByteBuf buf, CountBasedSmithingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.template);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.base);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.addition);
        TransmuteResult.STREAM_CODEC.encode(buf, recipe.result);
        buf.writeInt(recipe.additionCount);
    }

    private static CountBasedSmithingRecipe read(RegistryFriendlyByteBuf buf) {
        Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        Ingredient addition = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        TransmuteResult result = TransmuteResult.STREAM_CODEC.decode(buf);
        int count = buf.readInt();
        return new CountBasedSmithingRecipe(template, base, addition, result, count);
    }
}