package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * Blighted Wither and Biome Brush (concept N26, docs/ai/KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md): the stronger Wither,
 * the Wither Heart it drops and the Biome Brush made of it. Both in Simple Mobs so the module stays playable alone.
 */
public final class WitherMobs {
    public static final String ID = MobsRegistry.ID;

    public static final ResourceKey<EntityType<?>> BLIGHTED_WITHER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "blighted_wither"));
    public static final EntityType<BlightedWither> BLIGHTED_WITHER =
            EntityType.Builder.<BlightedWither>of(BlightedWither::new, MobCategory.MONSTER)
                    .fireImmune().immuneTo(BlockTags.WITHER_IMMUNE_TO).sized(0.9f, 3.5f).clientTrackingRange(10)
                    .notInPeaceful().dontTrackDeltas()
                    .build(BLIGHTED_WITHER_KEY);
    public static final ResourceKey<EntityType<?>> ABYSSAL_WITHER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "abyssal_wither"));
    public static final EntityType<AbyssalWither> ABYSSAL_WITHER =
            EntityType.Builder.<AbyssalWither>of(AbyssalWither::new, MobCategory.MONSTER)
                    .fireImmune().immuneTo(BlockTags.WITHER_IMMUNE_TO).sized(0.9f, 3.5f).clientTrackingRange(10)
                    .notInPeaceful().dontTrackDeltas()
                    .build(ABYSSAL_WITHER_KEY);
    /** Helmets that summon the Abyssal Wither (optional Enderite Helmets of other mods; empty when none is installed). */
    public static final TagKey<Item> ABYSSAL_CROWNS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "abyssal_wither_crowns"));

    public static final ResourceKey<Item> HEART_KEY = itemKey("wither_heart");
    public static final ResourceKey<Item> BRUSH_KEY = itemKey("biome_brush");
    public static final Item HEART = new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant().setId(HEART_KEY));
    public static final Item BRUSH = new BiomeBrushItem(new Item.Properties().durability(BiomeBrushItem.DURABILITY)
            .repairable(Items.NETHERITE_INGOT).enchantable(15).rarity(Rarity.EPIC).fireResistant().setId(BRUSH_KEY));

    private WitherMobs() {}

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(BLIGHTED_WITHER_KEY, BLIGHTED_WITHER);
        sink.accept(ABYSSAL_WITHER_KEY, ABYSSAL_WITHER);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(HEART_KEY, HEART);
        sink.accept(BRUSH_KEY, BRUSH);
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }
}
