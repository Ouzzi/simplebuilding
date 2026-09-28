package com.simplebuilding.loot;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** The mod's loot condition types ({@link CoreChanceCondition}). */
public final class ModLootConditions {

    public static final Identifier CORE_CHANCE_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "core_chance");

    private static boolean registered;

    private ModLootConditions() {
    }

    /**
     * Fabric: from the mod initialiser; NeoForge/Forge: from the RegisterEvent of
     * {@code loot_condition_type}. Registers once.
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, CORE_CHANCE_ID, CoreChanceCondition.TYPE);
    }
}
