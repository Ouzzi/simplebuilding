package com.simplebuilding.modules.simpletweaks.deed;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Copying a deed like Vanilla copies books and maps: a bound original plus an empty deed give a second deed bound to
 * the same owner (generation 1); the original stays. A copy cannot be copied again. The recipe does not know the
 * player, so anybody may copy a deed they hold; the copy acts only for the owner or a manager (the server checks).
 */
public class DeedCloningRecipe extends CustomRecipe {
    public static final RecipeSerializer<DeedCloningRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(DeedCloningRecipe::new), StreamCodec.<RegistryFriendlyByteBuf, DeedCloningRecipe>unit(new DeedCloningRecipe()));

    private static boolean deed(ItemStack stack) { return stack.is(ClaimDeedItem.item()); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) return false;
        boolean source = false, blank = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!deed(stack)) return false;
            DeedData data = stack.get(DeedData.TYPE);
            if (data == null) {
                if (blank) return false;
                blank = true;
            } else {
                if (source || !data.copyable()) return false;
                source = true;
            }
        }
        return source && blank;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            DeedData data = deed(stack) ? stack.get(DeedData.TYPE) : null;
            if (data == null || !data.copyable()) continue;
            ItemStack out = stack.copyWithCount(1);
            out.set(DeedData.TYPE, data.copy());
            return out;
        }
        return ItemStack.EMPTY;
    }

    /** The original stays in the grid (one of it), the empty deed is used up. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (deed(stack) && stack.has(DeedData.TYPE)) {
                result.set(slot, stack.copyWithCount(1));
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<DeedCloningRecipe> getSerializer() { return SERIALIZER; }
}
