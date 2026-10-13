package com.simplebuilding.modules.simplemobs;

import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jspecify.annotations.Nullable;

/**
 * Shellker (End gateway guard) and Soulfire Blaze: entity types, items, the High Gravity effect and its potions.
 * Kept apart from {@link MobsRegistry} so the mobs can be worked on independently; the loaders call
 * {@link #registerAll()} (Fabric) or the per-registry sinks (NeoForge/Forge).
 */
public final class EndNetherMobs {
    public static final String ID = MobsRegistry.ID;

    public static final ResourceKey<EntityType<?>> SHELLKER_KEY = entityKey("shellker");
    public static final EntityType<ShellkerEntity> SHELLKER =
            EntityType.Builder.<ShellkerEntity>of(ShellkerEntity::new, MobCategory.MONSTER)
                    .fireImmune().canSpawnFarFromPlayer().sized(1.0f, 1.0f).eyeHeight(0.5f).clientTrackingRange(10)
                    .build(SHELLKER_KEY);
    public static final ResourceKey<EntityType<?>> SHELLKER_BULLET_KEY = entityKey("shellker_bullet");
    public static final EntityType<ShellkerBullet> SHELLKER_BULLET =
            EntityType.Builder.<ShellkerBullet>of(ShellkerBullet::new, MobCategory.MISC)
                    .noLootTable().sized(0.3125f, 0.3125f).clientTrackingRange(8)
                    .build(SHELLKER_BULLET_KEY);
    public static final ResourceKey<EntityType<?>> SOULFIRE_BLAZE_KEY = entityKey("soulfire_blaze");
    public static final EntityType<SoulfireBlaze> SOULFIRE_BLAZE =
            EntityType.Builder.<SoulfireBlaze>of(SoulfireBlaze::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.6f, 1.8f).clientTrackingRange(8).notInPeaceful()
                    .build(SOULFIRE_BLAZE_KEY);
    public static final ResourceKey<EntityType<?>> SOUL_FIREBALL_KEY = entityKey("soul_fireball");
    public static final EntityType<SoulFireball> SOUL_FIREBALL =
            EntityType.Builder.<SoulFireball>of(SoulFireball::new, MobCategory.MISC)
                    .noLootTable().sized(0.3125f, 0.3125f).clientTrackingRange(4).updateInterval(10)
                    .build(SOUL_FIREBALL_KEY);

    /** Shellker helmet: own armor material (helmet 3, toughness 1, knockback resistance 10 %, iron durability). */
    public static final ResourceKey<EquipmentAsset> SHELLKER_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ID, "shellker"));
    public static final TagKey<Item> REPAIRS_SHELLKER_HELMET =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "repairs_shellker_helmet"));
    public static final ArmorMaterial SHELLKER_MATERIAL = new ArmorMaterial(EndNetherLogic.HELMET_DURABILITY,
            Map.of(ArmorType.HELMET, EndNetherLogic.HELMET_ARMOR), 9, SoundEvents.ARMOR_EQUIP_TURTLE,
            EndNetherLogic.HELMET_TOUGHNESS, EndNetherLogic.HELMET_KNOCKBACK, REPAIRS_SHELLKER_HELMET, SHELLKER_ASSET);

    public static final ResourceKey<Item> SHELL_KEY = itemKey("shellker_shell");
    public static final ResourceKey<Item> HELMET_KEY = itemKey("shellker_helmet");
    public static final ResourceKey<Item> SHELLKER_EGG_KEY = itemKey("shellker_spawn_egg");
    public static final ResourceKey<Item> ROD_KEY = itemKey("soulfire_rod");
    public static final ResourceKey<Item> SOULFIRE_EGG_KEY = itemKey("soulfire_blaze_spawn_egg");
    public static final ResourceKey<Item> POWDER_KEY = itemKey("soulfire_powder");
    public static final ResourceKey<Item> SOUL_FIRE_CHARGE_KEY = itemKey("soul_fire_charge");
    public static final Item SHELL = new ShellkerShellItem(new Item.Properties().setId(SHELL_KEY));
    public static final Item HELMET = new Item(new Item.Properties().humanoidArmor(SHELLKER_MATERIAL, ArmorType.HELMET)
            .attributes(SHELLKER_MATERIAL.createAttributes(ArmorType.HELMET).withModifierAdded(Attributes.FALL_DAMAGE_MULTIPLIER,
                    new AttributeModifier(Identifier.fromNamespaceAndPath(ID, "shellker_helmet_fall"), -EndNetherLogic.HELMET_FALL_REDUCTION,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD))
            .setId(HELMET_KEY));
    public static final Item SHELLKER_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(SHELLKER).setId(SHELLKER_EGG_KEY));
    public static final Item ROD = new Item(new Item.Properties().fireResistant().setId(ROD_KEY));
    public static final Item SOULFIRE_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(SOULFIRE_BLAZE).setId(SOULFIRE_EGG_KEY));
    /** Soulfire Powder (from the rod, like blaze powder) and the Soulfire Charge made of it (dispensable like a fire charge). */
    public static final Item POWDER = new Item(new Item.Properties().fireResistant().setId(POWDER_KEY));
    public static final Item SOUL_FIRE_CHARGE = new SoulFireChargeItem(new Item.Properties().setId(SOUL_FIRE_CHARGE_KEY));

    public static final ResourceKey<MobEffect> GRAVITY_KEY = ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ID, "high_gravity"));
    public static final MobEffect GRAVITY = new HighGravityEffect();
    public static final ResourceKey<Potion> GRAVITY_POTION_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(ID, "high_gravity"));
    public static final ResourceKey<Potion> LONG_GRAVITY_POTION_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(ID, "long_high_gravity"));
    private static @Nullable Potion gravityPotion, longGravityPotion;
    private static @Nullable Holder<MobEffect> gravityHolder;

    private EndNetherMobs() {}

    private static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    /** The registered High Gravity effect (call after the effect registry was filled). */
    public static Holder<MobEffect> gravity() {
        if (gravityHolder == null) gravityHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(GRAVITY);
        return gravityHolder;
    }

    public static Potion gravityPotion() {
        if (gravityPotion == null) gravityPotion = new Potion("high_gravity", new MobEffectInstance(gravity(), EndNetherLogic.POTION_TICKS));
        return gravityPotion;
    }

    public static Potion longGravityPotion() {
        if (longGravityPotion == null) longGravityPotion = new Potion("high_gravity", new MobEffectInstance(gravity(), EndNetherLogic.LONG_POTION_TICKS));
        return longGravityPotion;
    }

    /** Whether the entity wears the Shellker helmet (immune to High Gravity). */
    public static boolean wearsHelmet(LivingEntity living) {
        return living.getItemBySlot(EquipmentSlot.HEAD).is(HELMET);
    }

    /** High Gravity pulls this entity down (has the effect and no helmet). False before registration. */
    public static boolean heavy(LivingEntity living) {
        return gravityHolder != null && living.hasEffect(gravityHolder) && !wearsHelmet(living);
    }

    public static boolean isGravity(Holder<MobEffect> effect) {
        return effect.value() == GRAVITY;
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(SHELLKER_KEY, SHELLKER);
        sink.accept(SHELLKER_BULLET_KEY, SHELLKER_BULLET);
        sink.accept(SOULFIRE_BLAZE_KEY, SOULFIRE_BLAZE);
        sink.accept(SOUL_FIREBALL_KEY, SOUL_FIREBALL);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(SHELL_KEY, SHELL);
        sink.accept(HELMET_KEY, HELMET);
        sink.accept(SHELLKER_EGG_KEY, SHELLKER_EGG);
        sink.accept(ROD_KEY, ROD);
        sink.accept(SOULFIRE_EGG_KEY, SOULFIRE_EGG);
        sink.accept(POWDER_KEY, POWDER);
        sink.accept(SOUL_FIRE_CHARGE_KEY, SOUL_FIRE_CHARGE);
        net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(SOUL_FIRE_CHARGE);
    }

    public static void effects(BiConsumer<ResourceKey<MobEffect>, MobEffect> sink) {
        sink.accept(GRAVITY_KEY, GRAVITY);
    }

    public static void potions(BiConsumer<ResourceKey<Potion>, Potion> sink) {
        sink.accept(GRAVITY_POTION_KEY, gravityPotion());
        sink.accept(LONG_GRAVITY_POTION_KEY, longGravityPotion());
    }

    /** Fabric: direct registration (call after {@link MobsRegistry#registerAll()}). */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
        gravityHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, GRAVITY_KEY, GRAVITY);
        potions((k, v) -> Registry.register(BuiltInRegistries.POTION, k, v));
    }
}
