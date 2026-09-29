package com.simplebuilding.loot;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/**
 * Der Faktor {@code server.loot.globalLootMultiplier} auf alle Beute-Pools der Mod (2026-09-28):
 * jeder Pool geht so oft an den Loader, wie der ganzzahlige Teil sagt, und einmal mehr mit der
 * Wahrscheinlichkeit des Rests ({@code 1.5} = einmal sicher, einmal mit 50 %). Im Mittel waechst die
 * Mod-Beute also genau um den Faktor, und 0 laesst sie ganz weg. Fertig gebaute Pools kann man nicht
 * mehr an eine Bedingung haengen; sie bekommen den Rest gerundet. Faktor 1 gibt den Editor unveraendert
 * zurueck - beide Wege bleiben wie bisher.
 */
public final class TunedLootEditor implements ModLootTableModifications.Editor {

    private final ModLootTableModifications.Editor target;
    private final int whole;
    private final float rest;

    private TunedLootEditor(ModLootTableModifications.Editor target, double multiplier) {
        this.target = target;
        this.whole = (int) Math.floor(multiplier);
        this.rest = (float) (multiplier - whole);
    }

    public static ModLootTableModifications.Editor wrap(ModLootTableModifications.Editor target, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier == 1.0) {
            return target;
        }
        return new TunedLootEditor(target, Math.max(0.0, multiplier));
    }

    @Override
    public void addPool(LootPool.Builder pool) {
        // Die Loader bauen den Pool sofort (LootTable.Builder#withPool, addPool(pool.build())), eine
        // spaeter angehaengte Bedingung trifft also nur die letzte Kopie.
        for (int i = 0; i < whole; i++) {
            target.addPool(pool);
        }
        if (rest > 0.0f) {
            target.addPool(pool.when(LootItemRandomChanceCondition.randomChance(rest)));
        }
    }

    @Override
    public void addBuiltPool(LootPool pool) {
        int copies = whole + (rest >= 0.5f ? 1 : 0);
        for (int i = 0; i < copies; i++) {
            target.addBuiltPool(pool);
        }
    }
}
