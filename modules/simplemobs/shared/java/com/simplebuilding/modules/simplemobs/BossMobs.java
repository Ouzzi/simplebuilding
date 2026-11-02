package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

/**
 * WP-19 (owner 01.11.): the two End bosses - Void Runner and Brimguard - their creative spawn eggs, and the brood
 * hollow (K1): mob essence, the blank egg and the invisible brood marker. Plan docs/ai/PLAN-B-BOSS01-2026-11-02.md.
 */
public final class BossMobs {
    public static final String ID = MobsRegistry.ID;

    public static final ResourceKey<EntityType<?>> RUNNER_KEY = entityKey("void_runner");
    public static final EntityType<VoidRunnerEntity> VOID_RUNNER =
            EntityType.Builder.<VoidRunnerEntity>of(VoidRunnerEntity::new, MobCategory.MONSTER)
                    .sized(BossLogic.RUNNER_WIDTH, BossLogic.RUNNER_HEIGHT).eyeHeight(1.7f).clientTrackingRange(10).notInPeaceful()
                    .build(RUNNER_KEY);
    public static final ResourceKey<EntityType<?>> BRIMGUARD_KEY = entityKey("brimguard");
    public static final EntityType<BrimguardEntity> BRIMGUARD =
            EntityType.Builder.<BrimguardEntity>of(BrimguardEntity::new, MobCategory.MONSTER)
                    .sized(BossLogic.BRIM_WIDTH, BossLogic.BRIM_HEIGHT).eyeHeight(1.05f).clientTrackingRange(10).notInPeaceful().fireImmune()
                    .build(BRIMGUARD_KEY);
    public static final ResourceKey<EntityType<?>> BROOD_KEY = entityKey("brood_ritual");
    public static final EntityType<BroodRitualEntity> BROOD_RITUAL =
            EntityType.Builder.<BroodRitualEntity>of(BroodRitualEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).noSummon().clientTrackingRange(4)
                    .build(BROOD_KEY);

    public static final ResourceKey<Item> RUNNER_EGG_KEY = itemKey("void_runner_spawn_egg");
    public static final ResourceKey<Item> BRIMGUARD_EGG_KEY = itemKey("brimguard_spawn_egg");
    public static final ResourceKey<Item> ESSENCE_KEY = itemKey("mob_essence");
    public static final ResourceKey<Item> EGG_BLANK_KEY = itemKey("egg_blank");
    public static final Item RUNNER_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(VOID_RUNNER).setId(RUNNER_EGG_KEY));
    public static final Item BRIMGUARD_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(BRIMGUARD).setId(BRIMGUARD_EGG_KEY));
    public static final Item ESSENCE = new MobEssenceItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).setId(ESSENCE_KEY));
    public static final Item EGG_BLANK = new Item(new Item.Properties().stacksTo(16).setId(EGG_BLANK_KEY));

    private BossMobs() {}

    private static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(RUNNER_KEY, VOID_RUNNER);
        sink.accept(BRIMGUARD_KEY, BRIMGUARD);
        sink.accept(BROOD_KEY, BROOD_RITUAL);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(ESSENCE_KEY, ESSENCE);
        sink.accept(EGG_BLANK_KEY, EGG_BLANK);
        sink.accept(RUNNER_EGG_KEY, RUNNER_EGG);
        sink.accept(BRIMGUARD_EGG_KEY, BRIMGUARD_EGG);
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }
}
