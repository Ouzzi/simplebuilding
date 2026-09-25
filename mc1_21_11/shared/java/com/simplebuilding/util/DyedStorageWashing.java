package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LayeredCauldronBlock;

/**
 * Der Wasserkessel waescht gefaerbte Rucksaecke und Buendel ({@link DyedStorage}) wie
 * Lederruestung: er nimmt nur {@code minecraft:dyed_color} weg - Inhalt, Name und Verzauberungen
 * bleiben am selben Stapel - und senkt den Wasserstand um eine Stufe.
 *
 * <p>MC 1.21.11: Vanilla registriert sein Waschen je Item ({@code CauldronInteraction.WATER.map()});
 * auf 26.2 genuegt statt dessen der Tag {@code minecraft:cauldron_can_remove_dye}. Aufgerufen von
 * Fabric ({@code Simplebuilding}) und NeoForge ({@code SimplebuildingNeoForge}).
 */
public final class DyedStorageWashing {
    /** Gleiches Verhalten wie Vanillas {@code CauldronInteraction#dyedItemIteration}. */
    public static final CauldronInteraction WASH = (state, level, pos, player, hand, stack) -> {
        if (!stack.has(DataComponents.DYED_COLOR)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            stack.remove(DataComponents.DYED_COLOR);
            player.awardStat(Stats.CLEAN_ARMOR);
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        }
        return InteractionResult.SUCCESS;
    };

    private DyedStorageWashing() {
    }

    /** Die elf faerbbaren Items: vier Rucksaecke, drei Buendel, vier Koecher. */
    public static Item[] dyeableItems() {
        return new Item[]{ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK,
                ModItems.ENDERITE_BACKPACK, ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE,
                ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_QUIVER};
    }

    public static void register() {
        for (Item item : dyeableItems()) {
            CauldronInteraction.WATER.map().put(item, WASH);
        }
    }
}
