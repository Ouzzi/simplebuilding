package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

/**
 * Wither Wings and Nether Wings (owner 23.10., queue EL24-1, docs/ai/PLAN-A-ELYTRA24-2026-10-23.md): Elytra upgraded at the
 * smithing table with the Strong Upgrade (Simple Beacons; without it the Netherite Upgrade) and the Nether Heart, then once more
 * with the Red Nether Gem. They glide like the Elytra; only the top speed a firework rocket pushes towards is raised
 * ({@link MobsConfig#witherWingsSpeedFactor} x2, {@link MobsConfig#netherWingsSpeedFactor} x4) - and only for a strong rocket
 * (BS25-4, owner 25.10./29.10.): a normal rocket (flight duration 1-3) pushes like Vanilla and so pulls the speed back to the
 * Vanilla Elytra top speed, a rocket of flight duration {@value #STRONG_FLIGHT} or more (Strong Gunpowder, {@link StrongExplosives}) uses the
 * higher cap of the wings. Hitting a wall hurts by the Vanilla formula, so more speed means more risk. Stage 3, the End Wings (WA24-2, x6), and stage 4, the Void Wings (WB24-2, x8), are
 * smithed the same way from the stage below with the Ender Heart and the Ender Gem. Both materials have no loot yet: they will drop
 * from a later End boss (queue WA24-7), until then they are only given by commands, the creative tab and the test centre.
 */
public final class WitherWings {
    public static final String ID = MobsRegistry.ID;
    /** Vanilla rocket push per tick: {@code look * 0.1 + (look * 1.5 - v) * 0.5} = {@code look * 0.85 - v * 0.5}. */
    public static final double VANILLA_PUSH = 0.1 + 1.5 * 0.5;
    public static final float MAX_FACTOR = 8.0f;
    /** A rocket is strong from this flight duration on (Vanilla crafts 1-3): only it lifts the wings above the Vanilla top speed. */
    public static final int STRONG_FLIGHT = 4;
    /** Custom data key the server stamps its current factor under, so the client (which moves the player) uses the server value. */
    public static final String STAMP = "simplemobs:speed_factor";

    public static final ResourceKey<Item> WITHER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "wither_wings"));
    public static final ResourceKey<Item> NETHER_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "nether_wings"));
    public static final ResourceKey<EquipmentAsset> WITHER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ID, "wither_wings"));
    public static final ResourceKey<EquipmentAsset> NETHER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ID, "nether_wings"));
    /** Elytra 432; each stage about the Diamond -> Netherite step (+1/3). Wear per flight time stays Vanilla (1 per second). */
    public static final int WITHER_DURABILITY = 576, NETHER_DURABILITY = 768, END_DURABILITY = 960, VOID_DURABILITY = 1152;
    public static final ResourceKey<Item> END_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "end_wings"));
    public static final ResourceKey<EquipmentAsset> END_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ID, "end_wings"));
    public static final ResourceKey<Item> VOID_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "void_wings"));
    public static final ResourceKey<EquipmentAsset> VOID_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ID, "void_wings"));
    /** Materials of stage 3 and 4 (WB24-2/WB24-3); their look is provisional until the owner picks one, their source is WA24-7. */
    public static final ResourceKey<Item> ENDER_HEART_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "ender_heart"));
    public static final ResourceKey<Item> ENDER_GEM_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "ender_gem"));
    public static final Item WITHER = new Item(wings(com.simplelib.api.SimpleRarity.CURSED, WITHER_DURABILITY, WITHER_ASSET).setId(WITHER_KEY));
    public static final Item NETHER = new Item(wings(com.simplelib.api.SimpleRarity.CURSED, NETHER_DURABILITY, NETHER_ASSET).setId(NETHER_KEY));
    public static final Item END = new Item(wings(com.simplelib.api.SimpleRarity.CURSED, END_DURABILITY, END_ASSET).setId(END_KEY));
    /** Stage 4: the top stage is mythic like the Ender Gem it is made with. */
    public static final Item VOID = new Item(wings(com.simplelib.api.SimpleRarity.MYTHIC, VOID_DURABILITY, VOID_ASSET).setId(VOID_KEY));
    public static final Item ENDER_HEART = new Item(com.simplelib.api.SimpleRarity.CURSED.apply(new Item.Properties()).fireResistant().setId(ENDER_HEART_KEY));
    public static final Item ENDER_GEM = new Item(com.simplelib.api.SimpleRarity.MYTHIC.apply(new Item.Properties()).fireResistant().setId(ENDER_GEM_KEY));

    private WitherWings() {}

    /** Vanilla Elytra components with an own look, repaired with phantom membrane, fire-proof like Netherite. */
    private static Item.Properties wings(com.simplelib.api.SimpleRarity rarity, int durability, ResourceKey<EquipmentAsset> asset) {
        return rarity.apply(new Item.Properties()).durability(durability).fireResistant()
                .component(DataComponents.GLIDER, Unit.INSTANCE)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
                        .setAsset(asset).setDamageOnHurt(false).build())
                .repairable(Items.PHANTOM_MEMBRANE);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(WITHER_KEY, WITHER);
        sink.accept(NETHER_KEY, NETHER);
        sink.accept(END_KEY, END);
        sink.accept(VOID_KEY, VOID);
        sink.accept(ENDER_HEART_KEY, ENDER_HEART);
        sink.accept(ENDER_GEM_KEY, ENDER_GEM);
    }

    /** Whether a firework rocket stack is a strong rocket: flight duration {@value #STRONG_FLIGHT} or more. */
    public static boolean strongRocket(ItemStack rocket) {
        var fireworks = rocket.get(DataComponents.FIREWORKS);
        return fireworks != null && fireworks.flightDuration() >= STRONG_FLIGHT;
    }

    public static boolean isWings(ItemStack stack) {
        return stack.is(WITHER) || stack.is(NETHER) || stack.is(END) || stack.is(VOID);
    }

    /** The server config factor of a stack (1 for anything that is not a pair of wings). */
    public static float configFactor(ItemStack stack) {
        if (stack.is(WITHER)) return clamp(MobsConfig.witherWingsSpeedFactor);
        if (stack.is(NETHER)) return clamp(MobsConfig.netherWingsSpeedFactor);
        if (stack.is(END)) return clamp(MobsConfig.endWingsSpeedFactor);
        if (stack.is(VOID)) return clamp(MobsConfig.voidWingsSpeedFactor);
        return 1.0f;
    }

    /** The factor a stack flies with: the server's stamp when there is one, else the config (defaults on a client). */
    public static float factor(ItemStack stack) {
        if (!isWings(stack)) return 1.0f;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return configFactor(stack);
        return clamp(data.copyTag().getFloatOr(STAMP, configFactor(stack)));
    }

    /** Server: writes the current config factor into the stack when it differs (the item sync carries it to the client). */
    public static void stamp(ItemStack stack) {
        if (!isWings(stack)) return;
        float want = configFactor(stack);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().getFloatOr(STAMP, -1.0f) == want) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(STAMP, want));
    }

    /** The gliding wings a living entity wears (the first glider slot holding wings), or an empty stack. */
    public static ItemStack worn(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (isWings(stack) && LivingEntity.canGlideUsing(stack, slot)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * The rocket push with a raised top speed: Vanilla's result plus {@code look * 0.85 * (f - 1)}. That is exactly Vanilla's
     * push towards {@code 1.5 * f} instead of 1.5 (and {@code 0.1 * f}), so the speed a rocket holds is f times Vanilla's.
     * f = 1 returns the Vanilla result unchanged; gliding without a rocket is never touched.
     */
    public static Vec3 boost(Vec3 vanillaResult, Vec3 look, double factor) {
        if (factor <= 1.0) return vanillaResult;
        return vanillaResult.add(look.scale(VANILLA_PUSH * (factor - 1.0)));
    }

    static float clamp(float f) {
        return Float.isFinite(f) ? Math.max(1.0f, Math.min(MAX_FACTOR, f)) : 1.0f;
    }
}
