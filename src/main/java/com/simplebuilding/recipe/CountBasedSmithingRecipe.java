package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

public class CountBasedSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<CountBasedSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
            Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            com.mojang.serialization.Codec.INT.fieldOf("addition_count").forGetter(recipe -> recipe.additionCount)
    ).apply(instance, CountBasedSmithingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CountBasedSmithingRecipe> STREAM_CODEC = StreamCodec.of(
            CountBasedSmithingRecipe::write, CountBasedSmithingRecipe::read
    );

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStackTemplate result;
    private final int additionCount;

    public CountBasedSmithingRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStackTemplate result, int additionCount) {
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

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack inputStack = input.base();
        ItemStack resultStack = this.result.create();

        resultStack.applyComponentsAndValidate(inputStack.getComponentsPatch());

        return resultStack;
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
        ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
        buf.writeInt(recipe.additionCount);
    }

    private static CountBasedSmithingRecipe read(RegistryFriendlyByteBuf buf) {
        Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        Ingredient addition = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
        ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buf);
        int count = buf.readInt();
        return new CountBasedSmithingRecipe(template, base, addition, result, count);
    }
}