package com.simpletrims.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simpletrims.TemplateTools;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/**
 * {@code simpletrims:tool_shapeless}: a shapeless recipe with one tool ingredient ({@code #simpletrims:template_tools})
 * that follows the tool rule ({@link TemplateTools}): only the axe while no hammer partner is loaded, only a hammer
 * once one is. The tool stays in the grid and loses {@link #TOOL_DAMAGE} durability (a tool on its last point breaks).
 * JSON like {@code crafting_shapeless}.
 */
public class ToolShapelessRecipe extends ShapelessRecipe {
    /** Durability a tool loses per craft (same as one hit on a placed template). */
    public static final int TOOL_DAMAGE = 1;

    public static final MapCodec<ToolShapelessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients)
    ).apply(i, ToolShapelessRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
            ToolShapelessRecipe::new);
    public static final RecipeSerializer<ToolShapelessRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;

    public ToolShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
                               List<Ingredient> ingredients) {
        super(commonInfo, bookInfo, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
    }

    /** A tool in the grid: an item of {@code #simpletrims:template_tools} (axe or hammer). */
    public static boolean isToolCandidate(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TemplateTools.TOOLS);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (isToolCandidate(stack) && !TemplateTools.isTemplateTool(stack)) return false;
        }
        return true;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rest = CraftingRecipe.defaultCraftingReminder(input);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (isToolCandidate(stack)) rest.set(slot, damaged(stack));
        }
        return rest;
    }

    /** The tool after one craft; empty when it breaks. */
    public static ItemStack damaged(ItemStack tool) {
        ItemStack copy = tool.copyWithCount(1);
        if (!copy.isDamageableItem() || copy.has(DataComponents.UNBREAKABLE)) return copy;
        int damage = copy.getDamageValue() + TOOL_DAMAGE;
        if (damage >= copy.getMaxDamage()) return ItemStack.EMPTY;
        copy.setDamageValue(damage);
        return copy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    /** The tool slot shows the tools of the current mode (axes alone, hammers with a hammer partner). */
    @Override
    public List<RecipeDisplay> display() {
        List<SlotDisplay> slots = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            boolean tool = ingredient.items().anyMatch(holder -> holder.is(TemplateTools.TOOLS));
            slots.add(tool ? new SlotDisplay.TagSlotDisplay(TemplateTools.currentTag()) : ingredient.display());
        }
        return List.of(new ShapelessCraftingRecipeDisplay(slots, new SlotDisplay.ItemStackSlotDisplay(result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
