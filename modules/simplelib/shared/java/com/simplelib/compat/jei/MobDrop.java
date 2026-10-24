package com.simplelib.compat.jei;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * One drop of a mob as the JEI tab {@link MobDropCategory} shows it (JEI24): the victim (its spawn egg in the slot), what
 * has to kill it (empty: anything), the items that can come out and the lines underneath. Free of JEI classes, so the
 * modules fill it from their init through {@link com.simplelib.api.RecipeViewer#mobDrops} and the game tests read it.
 *
 * @param id         unique, lower case, e.g. {@code simplemobs/loot/hollow_howler}
 * @param victim     the mob that dies
 * @param victimIcon its spawn egg (empty when it has none)
 * @param killerIcon what kills it, e.g. a creeper egg for a charged creeper (empty: any death)
 * @param results    what can drop
 * @param notes      lines under the slots (at most three)
 */
public record MobDrop(String id, EntityType<?> victim, ItemStack victimIcon, ItemStack killerIcon, List<ItemStack> results,
                      List<Component> notes) {
}
