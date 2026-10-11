package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Deceiver stage 2 (owner 11.10.): the cloak of deception, the trick mirror and the veiled (particle-free) invisibility
 * potions. Kept apart from {@link MobsRegistry} so the stage-2 content stays in one place.
 */
public final class DisguiseRegistry {
    public static final ResourceKey<Item> CLOAK_KEY = key("deceiver_cloak");
    public static final ResourceKey<Item> MIRROR_KEY = key("trick_mirror");
    public static final ResourceKey<EquipmentAsset> CLOAK_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "deceiver_cloak"));
    /** Mobs that see through the cloak and cannot be imitated: bosses, the Warden, the Hollow Howler. */
    public static final TagKey<EntityType<?>> SEES_THROUGH =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "sees_through_disguise"));

    public static final Item CLOAK = new Item(new Item.Properties()
            .durability(CloakLogic.DURABILITY)
            .rarity(Rarity.UNCOMMON)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                    .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                    .setAsset(CLOAK_ASSET)
                    .setDamageOnHurt(false)
                    .build())
            .repairable(MobsRegistry.CLOTH)
            .setId(CLOAK_KEY));
    public static final Item MIRROR = new TrickMirrorItem(new Item.Properties().durability(64).rarity(Rarity.UNCOMMON).setId(MIRROR_KEY));

    public static final ResourceKey<Potion> VEILED_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "veiled_invisibility"));
    public static final ResourceKey<Potion> LONG_VEILED_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "long_veiled_invisibility"));
    /** Vanilla invisibility without swirl particles (ambient off, particles off, icon on). */
    public static final Potion VEILED = new Potion("veiled_invisibility", new MobEffectInstance(MobEffects.INVISIBILITY, 3600, 0, false, false, true));
    public static final Potion LONG_VEILED = new Potion("veiled_invisibility", new MobEffectInstance(MobEffects.INVISIBILITY, 9600, 0, false, false, true));

    private DisguiseRegistry() {}

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MobsRegistry.ID, path));
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> out) {
        out.accept(CLOAK_KEY, CLOAK);
        out.accept(MIRROR_KEY, MIRROR);
    }

    public static void potions(BiConsumer<ResourceKey<Potion>, Potion> out) {
        out.accept(VEILED_KEY, VEILED);
        out.accept(LONG_VEILED_KEY, LONG_VEILED);
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
        potions((k, v) -> Registry.register(BuiltInRegistries.POTION, k, v));
    }
}
