package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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
    /** Wie viele Stueck des Nebenhand-Materials ein Schlag verbraucht (ausserhalb des Kreativmodus). */
    public static final int CATALYST_COST = 1;

    private SledgehammerEntityInteraction() {
    }

    /** Nebenhand-Material -> Ergebnis, in fester Reihenfolge. */
    public static Map<Item, Item> trimUpgrades() {
        Map<Item, Item> upgrades = new LinkedHashMap<>();
        upgrades.put(Items.GLOW_INK_SAC, ModItems.GLOWING_TRIM_TEMPLATE);
        upgrades.put(Items.GLOWSTONE_DUST, ModItems.EMITTING_TRIM_TEMPLATE);
        return upgrades;
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
