package com.simplebuilding.fletching;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * Ein Rezept des Befiederungstischs ({@code simplebuilding:fletching}): genau eine Teile-Kombination aus
 * {@link ArrowParts}. Es existiert fuer das Vanilla-Rezeptbuch (Anzeige, Herstellbar-Pruefung, Platzierung); das
 * Ergebnis rechnet das {@link FletchingMenu} weiterhin selbst aus den Slots aus. Die Anzeige nutzt Vanillas
 * {@link SmithingRecipeDisplay} (drei Eingaben + Ergebnis), also keinen eigenen Anzeige-Typ.
 *
 * <p>Eingabe ist {@link SmithingRecipeInput} in der Reihenfolge Spitze, Schaft, Befiederung.
 */
public record FletchingRecipe(ArrowParts.Tip tip, ArrowParts.Shaft shaft, ArrowParts.Fletching fletching)
        implements Recipe<SmithingRecipeInput> {

    public static final MapCodec<FletchingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            StringRepresentable.fromEnum(ArrowParts.Tip::values).fieldOf("tip").forGetter(FletchingRecipe::tip),
            StringRepresentable.fromEnum(ArrowParts.Shaft::values).fieldOf("shaft").forGetter(FletchingRecipe::shaft),
            StringRepresentable.fromEnum(ArrowParts.Fletching::values).fieldOf("fletching").forGetter(FletchingRecipe::fletching)
    ).apply(i, FletchingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FletchingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> ArrowParts.Tip.values()[i], ArrowParts.Tip::ordinal), FletchingRecipe::tip,
            ByteBufCodecs.idMapper(i -> ArrowParts.Shaft.values()[i], ArrowParts.Shaft::ordinal), FletchingRecipe::shaft,
            ByteBufCodecs.idMapper(i -> ArrowParts.Fletching.values()[i], ArrowParts.Fletching::ordinal), FletchingRecipe::fletching,
            FletchingRecipe::new);

    /** Registriert von jedem Loader unter {@code simplebuilding:fletching}. */
    public static final RecipeSerializer<FletchingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public FletchingRecipe(ArrowParts.Parts parts) {
        this(parts.tip(), parts.shaft(), parts.fletching());
    }

    public ArrowParts.Parts parts() {
        return new ArrowParts.Parts(tip, shaft, fletching);
    }

    /** Rezept-ID-Pfad, z. B. {@code fletching/iron_blaze_rod_feather}. */
    public String idPath() {
        return "fletching/" + tip.getSerializedName() + "_" + shaft.getSerializedName() + "_" + fletching.getSerializedName();
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.template().is(tip.input()) && input.base().is(shaft.input()) && input.addition().is(fletching.input());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return result();
    }

    public ItemStack result() {
        return ArrowParts.stack(parts(), ArrowParts.ARROWS_PER_CRAFT);
    }

    /** Keine Toasts: alle Befiederungsrezepte werden beim Oeffnen des Tisches auf einmal freigeschaltet. */
    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<FletchingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<FletchingRecipe> getType() {
        return FletchingRecipes.TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of(Ingredient.of(tip.input()), Ingredient.of(shaft.input()), Ingredient.of(fletching.input())));
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new SmithingRecipeDisplay(
                new SlotDisplay.ItemSlotDisplay(tip.input()),
                new SlotDisplay.ItemSlotDisplay(shaft.input()),
                new SlotDisplay.ItemSlotDisplay(fletching.input()),
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(result())),
                new SlotDisplay.ItemSlotDisplay(Items.FLETCHING_TABLE)));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return FletchingRecipes.CATEGORY;
    }
}
