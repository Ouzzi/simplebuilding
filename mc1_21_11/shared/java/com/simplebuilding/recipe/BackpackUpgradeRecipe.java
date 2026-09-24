package com.simplebuilding.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.items.custom.BackpackItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
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
 * Rezept ({@code pattern}, {@code key}, {@code result}).
 *
 * <p>MC 1.21.11: {@code ShapedRecipe} nimmt group/category/pattern/result/showNotification noch
 * einzeln entgegen (Recipe.CommonInfo und CraftingRecipe.CraftingBookInfo gibt es erst ab 26.2),
 * das Ergebnis ist ein {@link ItemStack}, und {@code RecipeSerializer} ist noch ein Interface. Der
 * Codec folgt Vanillas {@code ShapedRecipe.Serializer} Feld fuer Feld.
 */
public class BackpackUpgradeRecipe extends ShapedRecipe {
    public static final MapCodec<BackpackUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
    ).apply(i, BackpackUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.group());
                buf.writeEnum(recipe.category());
                ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
                ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                buf.writeBoolean(recipe.showNotification());
            },
            buf -> new BackpackUpgradeRecipe(
                    buf.readUtf(),
                    buf.readEnum(CraftingBookCategory.class),
                    ShapedRecipePattern.STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf),
                    buf.readBoolean()));

    /** Registriert von jedem Loader unter {@code simplebuilding:backpack_upgrade}. */
    public static final RecipeSerializer<BackpackUpgradeRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<BackpackUpgradeRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public BackpackUpgradeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof BackpackItem) {
                return stack.transmuteCopy(this.result.getItem(), this.result.getCount());
            }
        }
        return super.assemble(input, registries);
    }

    @Override
    public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return SERIALIZER;
    }
}
