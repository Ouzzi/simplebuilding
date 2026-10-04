package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import com.simplebuilding.version.McVersion;

/**
 * Die Besatz-Aufwertung mit dem Vorschlaghammer: Leuchttintenbeutel in der Nebenhand ergibt die leuchtende,
 * Glowstonestaub die strahlende Besatzvorlage. Die Tabelle ({@link #trimUpgrades()}), die Vorlagen-Regel
 * ({@link #isTrimTemplate}) und die Kosten stehen hier einmal; geschlagen wird nur die abgelegte Vorlage in der Welt
 * ({@link PlacedTemplates}). Der fruehere Weg ueber eine Vorlage im Rahmen ist entfallen (Backlog Q3, 2026-10-02:
 * Umwandlungen nur in der Welt). Das Wiki und der JEI-Katalog lesen die Tabelle ueber {@link InWorldTransformations}.
 */
public final class SledgehammerEntityInteraction {
    /** Haltbarkeit, die ein Schlag den Hammer kostet (ausserhalb des Kreativmodus). */
    public static final int HAMMER_DAMAGE = 1;
    /** Nebenhand-Material pro fertiger Aufwertung (ausserhalb des Kreativmodus). */
    public static final int CATALYST_COST = McVersion.EXPENSIVE_TEMPLATES ? 2 : 1;

    private SledgehammerEntityInteraction() {
    }

    /** Nebenhand-Material -> Ergebnis, in fester Reihenfolge. */
    public static Map<Item, Item> trimUpgrades() {
        Map<Item, Item> upgrades = new LinkedHashMap<>();
        upgrades.put(Items.GLOW_INK_SAC, ModItems.GLOWING_TRIM_TEMPLATE);
        upgrades.put(Items.GLOWSTONE_DUST, ModItems.EMITTING_TRIM_TEMPLATE);
        return upgrades;
    }

    /** Additional inventory materials; the catalyst remains in the off hand. */
    public static Map<Item, Integer> extraMaterials(Item catalyst) {
        Map<Item, Integer> materials = new LinkedHashMap<>();
        if (McVersion.EXPENSIVE_TEMPLATES && trimUpgrades().containsKey(catalyst)) {
            materials.put(Items.DIAMOND, 4);
            materials.put(catalyst == Items.GLOW_INK_SAC ? Items.GLOWSTONE : Items.BLAZE_POWDER, 2);
        }
        return materials;
    }

    public static boolean hasMaterials(Player player) {
        if (player.isCreative()) return true;
        if (player.getOffhandItem().getCount() < CATALYST_COST) return false;
        for (var material : extraMaterials(player.getOffhandItem().getItem()).entrySet()) {
            if (player.getInventory().countItem(material.getKey()) < material.getValue()) return false;
        }
        return true;
    }

    /** Called only after checking all materials on the server's final hit. */
    public static void consumeMaterials(Player player) {
        if (player.isCreative()) return;
        for (var material : extraMaterials(player.getOffhandItem().getItem()).entrySet()) {
            int remaining = material.getValue();
            for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack.is(material.getKey())) {
                    int count = Math.min(remaining, stack.getCount());
                    stack.shrink(count);
                    remaining -= count;
                }
            }
        }
        player.getOffhandItem().shrink(CATALYST_COST);
        player.getInventory().setChanged();
    }

    /**
     * Namensregel: jedes Item, dessen registrierter Name {@code trim_smithing_template} enthaelt -
     * alle Vanilla-Besatzvorlagen. Die mod-eigenen Ergebnisse heissen {@code *_trim_template} und
     * fallen nicht darunter, eine aufgewertete Vorlage laesst sich also nicht noch einmal aufwerten.
     */
    public static boolean isTrimTemplate(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath().contains("trim_smithing_template");
    }
}
