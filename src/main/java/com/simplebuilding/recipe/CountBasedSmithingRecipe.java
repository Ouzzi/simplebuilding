package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class CountBasedSmithingRecipe implements SmithingRecipe {

    public static final MapCodec<CountBasedSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
            Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.addition),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            com.mojang.serialization.Codec.INT.fieldOf("addition_count").forGetter(recipe -> recipe.additionCount)
    ).apply(instance, CountBasedSmithingRecipe::new));

    public static final PacketCodec<RegistryByteBuf, CountBasedSmithingRecipe> PACKET_CODEC = PacketCodec.ofStatic(
            CountBasedSmithingRecipe::write, CountBasedSmithingRecipe::read
    );

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;
    private final int additionCount;

    public CountBasedSmithingRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStack result, int additionCount) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
        this.additionCount = additionCount;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, World world) {
        // 1. Template prüfen
        if (!this.template.test(input.template())) {
            return false;
        }
        // 2. Basis-Item prüfen
        if (!this.base.test(input.base())) {
            return false;
        }
        // 3. Addition-Item prüfen UND Menge checken
        ItemStack additionStack = input.addition();
        return this.addition.test(additionStack) && additionStack.getCount() >= this.additionCount;
    }

    @Override
    public ItemStack craft(SmithingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack inputStack = input.base();
        ItemStack resultStack = this.result.copy();

        // WICHTIG: Komponenten (Enchantments, Name, etc.) übernehmen
        resultStack.applyComponentsFrom(inputStack.getComponents());

        return resultStack;
    }

    // HINWEIS: 'getResult' wurde in 1.21.2+ aus dem Recipe Interface entfernt und darf nicht mehr überschrieben werden.

    // --- SmithingRecipe Interface Methoden ---

    @Override
    public Optional<Ingredient> template() {
        return Optional.of(this.template);
    }

    @Override
    public Ingredient base() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> addition() {
        return Optional.of(this.addition);
    }

    // --- IngredientPlacement (Korrigiert für 1.21.2+) ---
    @Override
    public IngredientPlacement getIngredientPlacement() {
        // Die Methode heißt "forMultipleSlots", nicht "forMultiple"
        return IngredientPlacement.forMultipleSlots(List.of(
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

    private static void write(RegistryByteBuf buf, CountBasedSmithingRecipe recipe) {
        Ingredient.PACKET_CODEC.encode(buf, recipe.template);
        Ingredient.PACKET_CODEC.encode(buf, recipe.base);
        Ingredient.PACKET_CODEC.encode(buf, recipe.addition);
        ItemStack.PACKET_CODEC.encode(buf, recipe.result);
        buf.writeInt(recipe.additionCount);
    }

    private static CountBasedSmithingRecipe read(RegistryByteBuf buf) {
        Ingredient template = Ingredient.PACKET_CODEC.decode(buf);
        Ingredient base = Ingredient.PACKET_CODEC.decode(buf);
        Ingredient addition = Ingredient.PACKET_CODEC.decode(buf);
        ItemStack result = ItemStack.PACKET_CODEC.decode(buf);
        int count = buf.readInt();
        return new CountBasedSmithingRecipe(template, base, addition, result, count);
    }
}