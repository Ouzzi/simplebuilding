package com.simplebuilding.util;

import com.simplebuilding.items.custom.BackpackItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The entries of the worn backpack that carries an enchantment of SimpleBuilding (Master Builder for the wand and the pick
 * key, Constructor's Touch for the hand refill) - split stage 8c-2, docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md. 26.2: the
 * backpacks are still SimpleBuilding's own ({@link BackpackItem}); the 26.3 twin in mc26_3/overlay reads Simple Storage's
 * worn backpack through SimpleLib {@code CarriedStacks}. Entries are copies; a changed copy goes back through {@link Entry#write}.
 */
public final class WornStorage {
    private WornStorage() {
    }

    /** One stack in the worn backpack: a copy, and how a changed copy (empty = taken out) goes back in. */
    public record Entry(ItemStack stack, Consumer<ItemStack> writeBack) {
        public void write(ItemStack changed) {
            writeBack.accept(changed);
        }
    }

    /** The entries of the worn backpack with {@code key}, in its order; empty without such a backpack. */
    public static List<Entry> entries(Player player, ResourceKey<Enchantment> key) {
        ItemStack backpack = BackpackItem.wornBackpackWith(player, key);
        if (backpack.isEmpty()) return List.of();
        List<ItemStack> stacks = BackpackItem.entryStacks(backpack);
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            final int index = i;
            out.add(new Entry(stacks.get(i), changed -> BackpackItem.replaceEntry(backpack, index, changed)));
        }
        return out;
    }
}
