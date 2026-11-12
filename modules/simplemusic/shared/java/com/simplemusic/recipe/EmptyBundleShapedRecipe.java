package com.simplemusic.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * Geformtes Werkbank-Rezept, das nur <b>leere</b> Buendel nimmt ({@code simplemusic:empty_bundle_shaped}; F8=B, Besitzer
 * 12.11.): die Taschen-Jukebox und ihre 16 Farbvarianten. {@code Ingredient} vergleicht keine Komponenten, ein gewoehnliches
 * {@code crafting_shaped} nahm deshalb auch ein gefuelltes Buendel und der Inhalt verschwand. Die Komponenten-Zutaten der
 * Loader ({@code fabric:components}, {@code neoforge:components}) schreiben verschiedenes JSON; dieses Rezept ist auf allen
 * Loadern dasselbe (Vorbild {@code EnchantedShapelessRecipe} der Pads).
 *
 * <p>JSON wie {@code crafting_shaped} ({@code category}, {@code group}, {@code pattern}, {@code key}, {@code result}); dazu
 * verlangt {@link #matches}, dass jeder Stapel im Raster mit {@code minecraft:bundle_contents} leer ist. Rezeptbuch, Handbuch
 * und JEI zeigen das Rezept wie das geformte (Zutat: das Buendel).
 */
public class EmptyBundleShapedRecipe extends ShapedRecipe {

    public static final MapCodec<EmptyBundleShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, EmptyBundleShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EmptyBundleShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            EmptyBundleShapedRecipe::new);

    /** Registered under {@code simplemusic:empty_bundle_shaped} ({@code MusicRegistry#registerRecipeSerializers}). */
    public static final RecipeSerializer<EmptyBundleShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public EmptyBundleShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
                                   ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) {
            return false;
        }
        for (int slot = 0; slot < input.size(); slot++) {
            if (!isEmptyOrNoBundle(input.getItem(slot))) {
                return false;
            }
        }
        return true;
    }

    /** True for a stack without bundle contents or with empty ones; a filled bundle (anything inside) is false. */
    public static boolean isEmptyOrNoBundle(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null || contents.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }
}
