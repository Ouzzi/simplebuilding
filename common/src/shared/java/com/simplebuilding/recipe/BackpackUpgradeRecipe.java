package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.items.custom.BackpackItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Geformtes Rezept, das einen Rucksack aufwertet und dabei alles mitnimmt: das Ergebnis ist der
 * Rucksack aus dem Raster mit neuem Item ({@code transmuteCopy}) - Inhalt, Verzauberungen, Name
 * und alle anderen Komponenten bleiben. Ein gewoehnliches {@code crafting_shaped} baut dagegen
 * immer ein frisches Ergebnis und wuerde den Inhalt vernichten.
 *
 * <p>Serializer {@code simplebuilding:backpack_upgrade}; das JSON sieht aus wie ein geformtes
 * Rezept ({@code pattern}, {@code key}, {@code result}). Das Ergebnis wird wie bei Vanilla als
 * {@link ItemStackTemplate} gelesen: das geht auf 26.2 schon beim Datapack-Laden, bevor die
 * Item-Komponenten gebunden sind ({@code ItemStack.CODEC} ginge dort nicht).
 */
public class BackpackUpgradeRecipe extends ShapedRecipe {
    public static final MapCodec<BackpackUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, BackpackUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            BackpackUpgradeRecipe::new);

    /** Registriert von jedem Loader unter {@code simplebuilding:backpack_upgrade}. */
    public static final RecipeSerializer<BackpackUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public BackpackUpgradeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                 ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof BackpackItem) {
                return stack.transmuteCopy(this.result.item().value(), this.result.count());
            }
        }
        return super.assemble(input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }
}
