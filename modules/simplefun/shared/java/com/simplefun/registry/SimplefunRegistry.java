package com.simplefun.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registration triggers. Each loader calls these at the correct time:
 * Fabric directly during mod init, NeoForge during the matching RegisterEvent
 * (the registry is unfrozen during its event, so vanilla Registry.register works on both).
 */
public class SimplefunRegistry {

    public static void registerItems() {
        Registry.register(BuiltInRegistries.ITEM, ModItems.BRICK_SNOWBALL_KEY, ModItems.BRICK_SNOWBALL);
    }

    public static void registerEntities() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, ModEntities.BRICK_PROJECTILE_KEY, ModEntities.BRICK_PROJECTILE);
    }

    public static void registerEffects() {
        Registry.register(BuiltInRegistries.MOB_EFFECT, ModEffects.PIGGY_KEY, ModEffects.PIGGY_EFFECT);
    }
}
