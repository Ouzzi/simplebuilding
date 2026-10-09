package com.simplebuilding.entity;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Die Entity-Typen der Mod. Aufgebaut wie {@code ModBlocks} und {@code ModItems}:
 * die Registrierung passiert im statischen Initialisierer, und
 * {@link #registerModEntities()} erzwingt ihn zu einem Zeitpunkt, an dem die
 * Registry noch offen ist – auf Fabric aus dem Einstiegspunkt, auf NeoForge und
 * Forge aus deren {@code RegisterEvent}.
 */
public final class ModEntities {

    /**
     * Der aufsteigende Block. Die Bauwerte spiegeln Vanillas {@code falling_block}:
     * dieselbe Trefferbox (0,98), damit {@code blockPosition()} beim Anstoßen an
     * eine Decke genau die freie Zelle darunter trifft, und {@code noLootTable()},
     * weil die Mod keine Entity-Loot-Tabelle mitliefert.
     */
    public static final EntityType<LevitatingBlockEntity> LEVITATING_BLOCK = register("levitating_block");

    /** Pfeil vom Befiederungstisch; Masse wie Vanillas Pfeil. */
    public static final EntityType<com.simplebuilding.fletching.CraftedArrow> CRAFTED_ARROW = com.simplebuilding.version.McVersion.FLETCHING
            ? registerArrow("crafted_arrow") : null;

    /** Stroh-Ruestungsstaender (2026-10-02): Masse wie Vanillas Ruestungsstaender. */
    public static final EntityType<com.simplebuilding.dummy.TrainingDummy> STRAW_ARMOR_STAND = com.simplebuilding.version.McVersion.TRAINING_DUMMY
            ? registerDummy("straw_armor_stand") : null;

    /** Trainingspuppe: Stroh-Ruestungsstaender mit geschnitztem Kuerbis (2026-10-02). */
    public static final EntityType<com.simplebuilding.dummy.TrainingDummy> TRAINING_DUMMY = com.simplebuilding.version.McVersion.TRAINING_DUMMY
            ? registerDummy("training_dummy") : null;

    /**
     * Kleiner Ruestungsstaender (Nachtrag 29, 2026-10-09): Pfosten mit Querholz auf einer Steinplatte, genau ein
     * Ruestungsteil oder eine Tier-Ruestung. Der fruehere mittlere Staender ist ein Alias hierauf ({@code LegacyItemIds}).
     */
    public static final EntityType<com.simplebuilding.dummy.SmallArmorStand> SMALL_ARMOR_STAND = com.simplebuilding.version.McVersion.TRAINING_DUMMY
            ? registerPartialStand("small_armor_stand", 1.0F) : null;

    private ModEntities() {
    }

    private static EntityType<com.simplebuilding.dummy.TrainingDummy> registerDummy(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        EntityType<com.simplebuilding.dummy.TrainingDummy> type = EntityType.Builder
                .<com.simplebuilding.dummy.TrainingDummy>of(com.simplebuilding.dummy.TrainingDummy::new, MobCategory.MISC)
                .noLootTable()
                .sized(0.5F, 1.975F)
                .eyeHeight(1.7775F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    private static EntityType<com.simplebuilding.dummy.SmallArmorStand> registerPartialStand(String name, float height) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        EntityType<com.simplebuilding.dummy.SmallArmorStand> type = EntityType.Builder
                .<com.simplebuilding.dummy.SmallArmorStand>of(com.simplebuilding.dummy.SmallArmorStand::new, MobCategory.MISC)
                .noLootTable()
                .sized(0.5F, height)
                .eyeHeight(height * 0.9F)
                .clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    private static EntityType<LevitatingBlockEntity> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        EntityType<LevitatingBlockEntity> type = EntityType.Builder
                .<LevitatingBlockEntity>of(LevitatingBlockEntity::new, MobCategory.MISC)
                .noLootTable()
                .sized(0.98F, 0.98F)
                .clientTrackingRange(10)
                .updateInterval(20)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    private static EntityType<com.simplebuilding.fletching.CraftedArrow> registerArrow(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        EntityType<com.simplebuilding.fletching.CraftedArrow> type = EntityType.Builder
                .<com.simplebuilding.fletching.CraftedArrow>of(com.simplebuilding.fletching.CraftedArrow::new, MobCategory.MISC)
                .noLootTable()
                .sized(0.5F, 0.5F)
                .eyeHeight(0.13F)
                .clientTrackingRange(4)
                .updateInterval(20)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    public static void registerModEntities() {
        Simplebuilding.LOGGER.info("Registering Mod Entities for " + Simplebuilding.MOD_ID);
    }
}
