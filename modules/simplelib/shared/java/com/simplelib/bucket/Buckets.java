package com.simplelib.bucket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The bucket family shared by Simple Crucibles (copper bucket, soul lava bucket, soul lava) and Simple Tools (Enderite and
 * ceramic buckets, WH26-7). Each module registers its part and fills these fields; the bucket classes of this package read
 * them, so neither module imports the other. A field stays null while its module is missing.
 */
public final class Buckets {
    /** Vanilla lava bucket burn time. */
    public static final int LAVA_FUEL_TICKS = 20000;
    /** Simple Crucibles: soul lava and its iron bucket, the copper buckets, the copper components. */
    public static @Nullable Fluid SOUL_LAVA;
    public static Item SOUL_LAVA_BUCKET;
    public static Item COPPER_BUCKET, COPPER_WATER_BUCKET, COPPER_LAVA_BUCKET;
    public static DataComponentType<Integer> OXIDATION;
    public static DataComponentType<Boolean> WAXED, WAX_WORN;
    /** Burn time of a soul lava bucket (Simple Crucibles config), set before {@link #registerEnderiteSoulLava}. */
    public static int soulLavaFuelTicks = 10 * LAVA_FUEL_TICKS;

    /** Simple Tools: the Enderite buckets (half = one bucket, full = two, owner N21/N28) and milk (N32). */
    public static Item ENDERITE_BUCKET, ENDERITE_WATER_BUCKET, ENDERITE_LAVA_BUCKET, ENDERITE_SOUL_LAVA_BUCKET;
    public static Item FULL_ENDERITE_WATER_BUCKET, FULL_ENDERITE_LAVA_BUCKET, FULL_ENDERITE_SOUL_LAVA_BUCKET;
    public static Item ENDERITE_MILK_BUCKET, FULL_ENDERITE_MILK_BUCKET;
    /** Simple Tools: ceramic bucket, 3 clay -> raw, fired; one item per wear stage (index) and filling. */
    public static Item RAW_CERAMIC_BUCKET, CERAMIC_BUCKET, CERAMIC_WATER_BUCKET, CERAMIC_LAVA_BUCKET;
    public static final Item[] CERAMIC_EMPTY = new Item[ModBucketItem.CERAMIC_STAGES], CERAMIC_WATER = new Item[ModBucketItem.CERAMIC_STAGES],
            CERAMIC_LAVA = new Item[ModBucketItem.CERAMIC_STAGES];
    /** Name prefix of each ceramic wear stage. */
    public static final String[] CERAMIC_STAGE_NAMES = {"", "chipped_", "cracked_", "brittle_"};
    /** Ceramic bucket pours so far (0..3); the wear stage is the item itself. */
    public static DataComponentType<Integer> CERAMIC_USES;
    /** Namespace the Enderite buckets were registered under (the soul lava ones follow it). */
    private static String enderiteNamespace;

    private Buckets() {}


    /** Simple Tools: the Enderite and ceramic buckets under {@code namespace}; the soul lava ones via {@link #registerEnderiteSoulLava}. */
    public static void registerEnderiteAndCeramic(String namespace) {
        if (ENDERITE_BUCKET != null) return;
        enderiteNamespace = namespace;
        int lava = LAVA_FUEL_TICKS;
        ENDERITE_BUCKET = item(namespace, "enderite_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.EMPTY,
                p.stacksTo(16).fireResistant().rarity(Rarity.EPIC)));
        ENDERITE_WATER_BUCKET = item(namespace, "enderite_water_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.WATER,
                p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_BUCKET)));
        ENDERITE_LAVA_BUCKET = item(namespace, "enderite_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.LAVA,
                fuel(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_BUCKET), lava)));
        // Full (two buckets): burning or crafting one bucket away leaves the half bucket of the same fluid.
        FULL_ENDERITE_WATER_BUCKET = item(namespace, "enderite_water_bucket_full", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.WATER,
                p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_WATER_BUCKET)));
        FULL_ENDERITE_LAVA_BUCKET = item(namespace, "enderite_lava_bucket_full", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.LAVA,
                fuel(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_LAVA_BUCKET), lava)));
        // Milk (N32): drinking or crafting with it leaves the next smaller bucket (usingConvertsTo as Vanilla's milk bucket).
        ENDERITE_MILK_BUCKET = item(namespace, "enderite_milk_bucket", p -> new EnderiteMilkBucketItem(false, p.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                .craftRemainder(ENDERITE_BUCKET).component(net.minecraft.core.component.DataComponents.CONSUMABLE,
                        net.minecraft.world.item.component.Consumables.MILK_BUCKET).usingConvertsTo(ENDERITE_BUCKET)));
        FULL_ENDERITE_MILK_BUCKET = item(namespace, "enderite_milk_bucket_full", p -> new EnderiteMilkBucketItem(true, p.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
                .craftRemainder(ENDERITE_MILK_BUCKET).component(net.minecraft.core.component.DataComponents.CONSUMABLE,
                        net.minecraft.world.item.component.Consumables.MILK_BUCKET).usingConvertsTo(ENDERITE_MILK_BUCKET)));
        // No crafting remainder on the ceramic water bucket: a fresh bucket back would repair it for free.
        RAW_CERAMIC_BUCKET = item(namespace, "raw_ceramic_bucket", p -> new Item(p.stacksTo(16)));
        // Wear stages as items (like the copper bucket's oxidation, but visible in the name), no durability. Lava (owner N12) is
        // fuel like a lava bucket, but leaves no remainder - the clay bucket burns up with its lava.
        for (int stage = 0; stage < ModBucketItem.CERAMIC_STAGES; stage++) {
            int s = stage;
            String prefix = CERAMIC_STAGE_NAMES[stage];
            CERAMIC_EMPTY[stage] = item(namespace, prefix + "ceramic_bucket", p -> new ModBucketItem(ModBucketItem.Kind.CERAMIC, Fluids.EMPTY, s, p.stacksTo(16)));
            CERAMIC_WATER[stage] = item(namespace, prefix + "ceramic_water_bucket", p -> new ModBucketItem(ModBucketItem.Kind.CERAMIC, Fluids.WATER, s, p.stacksTo(1)));
            CERAMIC_LAVA[stage] = item(namespace, prefix + "ceramic_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.CERAMIC, Fluids.LAVA, s,
                    fuel(p.stacksTo(1), lava)));
        }
        CERAMIC_BUCKET = CERAMIC_EMPTY[0];
        CERAMIC_WATER_BUCKET = CERAMIC_WATER[0];
        CERAMIC_LAVA_BUCKET = CERAMIC_LAVA[0];
        registerEnderiteSoulLava();
    }

    /**
     * The Enderite soul lava buckets (half and full) need both modules: Simple Tools' Enderite bucket and Simple Crucibles' soul
     * lava. Both call this after their items; the second call registers them (whichever loads first), later calls do nothing.
     */
    public static synchronized void registerEnderiteSoulLava() {
        if (ENDERITE_SOUL_LAVA_BUCKET != null || ENDERITE_BUCKET == null || SOUL_LAVA == null) return;
        Fluid soul = SOUL_LAVA;
        int ticks = soulLavaFuelTicks;
        ENDERITE_SOUL_LAVA_BUCKET = item(enderiteNamespace, "enderite_soul_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, soul,
                fuel(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_BUCKET), ticks)));
        FULL_ENDERITE_SOUL_LAVA_BUCKET = item(enderiteNamespace, "enderite_soul_lava_bucket_full", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, soul,
                fuel(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_SOUL_LAVA_BUCKET), ticks)));
        BucketDispensing.register(List.of(ENDERITE_SOUL_LAVA_BUCKET, FULL_ENDERITE_SOUL_LAVA_BUCKET));
    }

    /**
     * Simple Tools' buckets in creative-tab order (the raw ceramic bucket included), then the worn ceramic stages and the full
     * Enderite buckets; the soul lava ones only with Simple Crucibles. Empty before registration.
     */
    public static List<Item> toolsBuckets() {
        if (ENDERITE_BUCKET == null) return List.of();
        List<Item> out = new ArrayList<>();
        for (Item item : new Item[] {ENDERITE_BUCKET, ENDERITE_WATER_BUCKET, ENDERITE_LAVA_BUCKET, ENDERITE_SOUL_LAVA_BUCKET, ENDERITE_MILK_BUCKET,
                RAW_CERAMIC_BUCKET, CERAMIC_EMPTY[0], CERAMIC_WATER[0], CERAMIC_LAVA[0]}) {
            if (item != null) out.add(item);
        }
        out.addAll(wornCeramicBuckets());
        out.addAll(fullEnderiteBuckets());
        return out;
    }

    /** {@link #toolsBuckets()} without the worn ceramic stages and the full Enderite buckets: what the creative tabs offer. */
    public static List<Item> creativeToolsBuckets() {
        List<Item> out = new ArrayList<>(toolsBuckets());
        out.removeAll(wornCeramicBuckets());
        out.removeAll(fullEnderiteBuckets());
        return out;
    }

    /** The full Enderite buckets (two buckets each): only reached by scooping twice, so not in the creative tab. */
    public static List<Item> fullEnderiteBuckets() {
        if (FULL_ENDERITE_WATER_BUCKET == null) return List.of();
        List<Item> out = new ArrayList<>();
        for (Item item : new Item[] {FULL_ENDERITE_WATER_BUCKET, FULL_ENDERITE_LAVA_BUCKET, FULL_ENDERITE_SOUL_LAVA_BUCKET, FULL_ENDERITE_MILK_BUCKET}) {
            if (item != null) out.add(item);
        }
        return out;
    }

    /** The worn ceramic buckets (stage 1..3, every filling): only reached by pouring, so not in the creative tab. */
    public static List<Item> wornCeramicBuckets() {
        if (CERAMIC_EMPTY[0] == null) return List.of();
        List<Item> out = new ArrayList<>();
        for (int stage = 1; stage < ModBucketItem.CERAMIC_STAGES; stage++) {
            out.add(CERAMIC_EMPTY[stage]);
            out.add(CERAMIC_WATER[stage]);
            out.add(CERAMIC_LAVA[stage]);
        }
        return out;
    }

    /** Half Enderite bucket (one bucket) -> the full one of the same fluid; null for anything else. */
    public static @Nullable Item fullEnderite(Item half) {
        if (half == null || FULL_ENDERITE_WATER_BUCKET == null) return null;
        if (half == ENDERITE_WATER_BUCKET) return FULL_ENDERITE_WATER_BUCKET;
        if (half == ENDERITE_LAVA_BUCKET) return FULL_ENDERITE_LAVA_BUCKET;
        if (half == ENDERITE_SOUL_LAVA_BUCKET) return FULL_ENDERITE_SOUL_LAVA_BUCKET;
        return null;
    }

    /** Full Enderite bucket (two buckets) -> the half one of the same fluid; null for anything else. */
    public static @Nullable Item halfEnderite(Item full) {
        if (full == null || FULL_ENDERITE_WATER_BUCKET == null) return null;
        if (full == FULL_ENDERITE_WATER_BUCKET) return ENDERITE_WATER_BUCKET;
        if (full == FULL_ENDERITE_LAVA_BUCKET) return ENDERITE_LAVA_BUCKET;
        if (full == FULL_ENDERITE_SOUL_LAVA_BUCKET) return ENDERITE_SOUL_LAVA_BUCKET;
        return null;
    }

    /** The ceramic bucket of wear {@code stage} holding {@code content} (empty, water, lava), or null. */
    public static @Nullable Item ceramic(Fluid content, int stage) {
        if (CERAMIC_EMPTY[0] == null || stage < 0 || stage >= ModBucketItem.CERAMIC_STAGES) return null;
        if (content == Fluids.EMPTY) return CERAMIC_EMPTY[stage];
        if (content.isSame(Fluids.WATER)) return CERAMIC_WATER[stage];
        if (content.isSame(Fluids.LAVA)) return CERAMIC_LAVA[stage];
        return null;
    }

    /** Whether {@code fluid} is soul lava (false without Simple Crucibles). */
    public static boolean isSoulLava(Fluid fluid) {
        return SOUL_LAVA != null && fluid.isSame(SOUL_LAVA);
    }

    /** Furnace fuel of an item: the cooking_fuel component with a fixed burn time. */
    public static Item.Properties fuel(Item.Properties properties, int ticks) {
        return properties.component(net.minecraft.core.component.DataComponents.COOKING_FUEL,
                new net.minecraft.world.item.component.CookingFuel(new net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant(ticks),
                        new net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat.Constant(1.0F)));
    }

    /** Registers an item {@code namespace:name} built from fresh properties with its id set. */
    public static Item item(String namespace, String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
