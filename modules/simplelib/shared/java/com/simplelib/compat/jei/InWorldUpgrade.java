package com.simplelib.compat.jei;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One in-world upgrade as the JEI category {@link InWorldUpgradeCategory} shows it: items that are changed by striking,
 * placing or hitting them in the world instead of a crafting grid (forging a placed trim template, ...). Modules add
 * their entries from their own JEI plugin with {@code registration.addRecipes(InWorldUpgradeCategory.TYPE, entries)}.
 *
 * @param id     unique, lower case, e.g. {@code simpletrims/glowing_trim_template}
 * @param inputs input slots, each a list of alternatives with the count the upgrade takes
 * @param tools  the items that do the work (shown cycling in the tool slot)
 * @param output the result
 * @param notes  lines under the slots (how many strikes, durability, ...)
 */
public record InWorldUpgrade(String id, List<Input> inputs, List<Item> tools, ItemStack output, List<Component> notes) {
    /** An input slot: any of {@code items}, {@code count} of it. */
    public record Input(List<Item> items, int count) {
        public static Input of(Item item, int count) {
            return new Input(List.of(item), count);
        }
    }
}
