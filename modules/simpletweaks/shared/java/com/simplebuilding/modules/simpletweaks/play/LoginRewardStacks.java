package com.simplebuilding.modules.simpletweaks.play;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Item lines {@code "namespace:id count"} of the reward lists (T20). Unknown items are skipped at use time. */
public final class LoginRewardStacks {
    public static boolean valid(String line) {
        var parts = line.trim().split("\\s+");
        if (parts.length < 1 || parts.length > 2 || Identifier.tryParse(parts[0]) == null) return false;
        if (parts.length == 2) { try { int n = Integer.parseInt(parts[1]); return n >= 1 && n <= 64; } catch (NumberFormatException e) { return false; } }
        return true;
    }
    /** The stack of one line, empty when the item does not exist (e.g. a partner mod is missing). */
    public static ItemStack stack(String line) {
        if (!valid(line)) return ItemStack.EMPTY;
        var parts = line.trim().split("\\s+");
        var id = Identifier.tryParse(parts[0]);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.getValue(id);
        int count = parts.length == 2 ? Integer.parseInt(parts[1]) : 1;
        return new ItemStack(item, Math.min(count, new ItemStack(item).getMaxStackSize()));
    }
    private LoginRewardStacks() {}
}
