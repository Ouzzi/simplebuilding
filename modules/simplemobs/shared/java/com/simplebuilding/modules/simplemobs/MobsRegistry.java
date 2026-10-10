package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** Entity type and items of Simple Mobs. Loaders register through {@link #entity()} / {@link #items()}. */
public final class MobsRegistry {
    public static final String ID = "simplemobs";

    public static final ResourceKey<EntityType<?>> DECEIVER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "deceiver"));
    public static final EntityType<DeceiverEntity> DECEIVER =
            EntityType.Builder.<DeceiverEntity>of(DeceiverEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.95f)
                    .eyeHeight(0.8f)
                    .clientTrackingRange(10)
                    .build(DECEIVER_KEY);

    public static final ResourceKey<Item> CLOTH_KEY = itemKey("deceiver_cloth");
    public static final ResourceKey<Item> EGG_KEY = itemKey("deceiver_spawn_egg");
    public static final Item CLOTH = new Item(new Item.Properties().setId(CLOTH_KEY));
    public static final Item EGG = new SpawnEggItem(new Item.Properties().spawnEgg(DECEIVER).setId(EGG_KEY));

    private MobsRegistry() {}

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, DECEIVER_KEY, DECEIVER);
        Registry.register(BuiltInRegistries.ITEM, CLOTH_KEY, CLOTH);
        Registry.register(BuiltInRegistries.ITEM, EGG_KEY, EGG);
    }
}
