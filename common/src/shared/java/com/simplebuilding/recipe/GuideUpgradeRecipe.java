package com.simplebuilding.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.guide.GuideBooks;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** A guide is consumed once, with every existing component preserved in the upgraded result. */
public final class GuideUpgradeRecipe extends ShapelessRecipe {
    public static final MapCodec<GuideUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients),
            Codec.intRange(0, (1 << 20) - 1).fieldOf("chapters").forGetter(r -> r.chapters)
    ).apply(i, GuideUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
            ByteBufCodecs.VAR_INT, r -> r.chapters, GuideUpgradeRecipe::new);
    public static final RecipeSerializer<GuideUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final int chapters;

    public GuideUpgradeRecipe(Recipe.CommonInfo info, CraftingRecipe.CraftingBookInfo bookInfo,
                              ItemStackTemplate result, List<Ingredient> ingredients, int chapters) {
        super(info, bookInfo, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
        this.chapters = chapters;
    }

    private ItemStack base(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof com.simplebuilding.items.custom.GuideBookItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        ItemStack base = base(input);
        if (base.isEmpty()) return false;
        int merged = chapters;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof com.simplebuilding.items.custom.GuideBookItem) {
                if (!stack.is(base.getItem())) return false;
                merged |= GuideBooks.mask(stack);
            }
        }
        if (chapters == 0) {
            for (int i = 0; i < input.size(); i++) if (input.getItem(i).is(base.getItem()) && GuideBooks.mask(input.getItem(i)) != merged) return true;
            return false;
        }
        return (merged & ~GuideBooks.mask(base)) != 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack out = base(input).copyWithCount(1);
        int merged = chapters;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(out.getItem())) merged |= GuideBooks.mask(stack);
        }
        out.set(ModDataComponentTypes.GUIDE_CHAPTERS, merged);
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // Inserts (including flint and steel) are consumed, never duplicated as crafting remainders.
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }
}
