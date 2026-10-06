package com.simplebuilding.fluid;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.version.McVersion;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

/**
 * Soul lava and the crucible buckets (McVersion.CRUCIBLE, 26.3). Fluids register in the FLUID phase
 * ({@link #registerFluids}, Forge unlocks one registry per event), the block with the blocks, the
 * buckets with the items. NeoForge/Forge replace {@link #factory} before registration with subclasses
 * that carry their fluid type.
 */
public final class ModFluids {
    /** Creates the two soul lava fluids; loaders with fluid types swap it for their subclasses. */
    public interface Factory {
        FlowingFluid source();

        FlowingFluid flowing();
    }

    public static Factory factory = new Factory() {
        @Override
        public FlowingFluid source() {
            return new SoulLavaFluid.Source();
        }

        @Override
        public FlowingFluid flowing() {
            return new SoulLavaFluid.Flowing();
        }
    };

    public static FlowingFluid SOUL_LAVA;
    public static FlowingFluid FLOWING_SOUL_LAVA;
    public static Block SOUL_LAVA_BLOCK;
    public static Item SOUL_LAVA_BUCKET;
    public static Item COPPER_BUCKET, COPPER_WATER_BUCKET, COPPER_LAVA_BUCKET;
    public static Item ENDERITE_BUCKET, ENDERITE_WATER_BUCKET, ENDERITE_LAVA_BUCKET, ENDERITE_SOUL_LAVA_BUCKET;
    /** Ceramic bucket (owner addition 11): 3 clay -> raw, fired in a furnace or crucible; water and (N12) lava, 32 uses. */
    public static Item RAW_CERAMIC_BUCKET, CERAMIC_BUCKET, CERAMIC_WATER_BUCKET, CERAMIC_LAVA_BUCKET;

    public static void registerFluids() {
        if (!McVersion.CRUCIBLE || SOUL_LAVA != null) return;
        FLOWING_SOUL_LAVA = Registry.register(BuiltInRegistries.FLUID, id("flowing_soul_lava"), factory.flowing());
        SOUL_LAVA = Registry.register(BuiltInRegistries.FLUID, id("soul_lava"), factory.source());
    }

    public static void registerBlocks() {
        if (!McVersion.CRUCIBLE || SOUL_LAVA_BLOCK != null) return;
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id("soul_lava"));
        // Lava's properties without replaceable(): nothing builds into it but creative players (SoulLavaBlock).
        SOUL_LAVA_BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new SoulLavaBlock(SOUL_LAVA, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN).noCollision().randomTicks().strength(100.0F).lightLevel(state -> 15)
                .pushReaction(McVersion.immovable()).noLootTable().liquid().sound(SoundType.EMPTY).setId(key)));
    }

    public static void registerItems() {
        if (!McVersion.CRUCIBLE || SOUL_LAVA_BUCKET != null) return;
        int lava = SoulLava.LAVA_FUEL_TICKS;
        SOUL_LAVA_BUCKET = item("soul_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.IRON, SOUL_LAVA,
                McVersion.cookingFuel(p.stacksTo(1), SoulLava.fuelTicks())));
        COPPER_BUCKET = item("copper_bucket", p -> new ModBucketItem(ModBucketItem.Kind.COPPER, Fluids.EMPTY, p.stacksTo(16)));
        COPPER_WATER_BUCKET = item("copper_water_bucket", p -> new ModBucketItem(ModBucketItem.Kind.COPPER, Fluids.WATER,
                p.stacksTo(1).craftRemainder(COPPER_BUCKET)));
        COPPER_LAVA_BUCKET = item("copper_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.COPPER, Fluids.LAVA,
                McVersion.cookingFuel(p.stacksTo(1), lava)));
        ENDERITE_BUCKET = item("enderite_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.EMPTY,
                p.stacksTo(16).fireResistant().rarity(Rarity.EPIC)));
        ENDERITE_WATER_BUCKET = item("enderite_water_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.WATER,
                p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_BUCKET)));
        ENDERITE_LAVA_BUCKET = item("enderite_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, Fluids.LAVA,
                McVersion.cookingFuel(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_BUCKET), lava)));
        ENDERITE_SOUL_LAVA_BUCKET = item("enderite_soul_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.ENDERITE, SOUL_LAVA,
                McVersion.cookingFuel(p.stacksTo(1).fireResistant().rarity(Rarity.EPIC).craftRemainder(ENDERITE_BUCKET), SoulLava.fuelTicks())));
        // No crafting remainder on the ceramic water bucket: a fresh bucket back would repair it for free.
        RAW_CERAMIC_BUCKET = item("raw_ceramic_bucket", p -> new Item(p.stacksTo(16)));
        CERAMIC_BUCKET = item("ceramic_bucket", p -> new ModBucketItem(ModBucketItem.Kind.CERAMIC, Fluids.EMPTY,
                p.durability(ModBucketItem.CERAMIC_USES)));
        CERAMIC_WATER_BUCKET = item("ceramic_water_bucket", p -> new ModBucketItem(ModBucketItem.Kind.CERAMIC, Fluids.WATER,
                p.durability(ModBucketItem.CERAMIC_USES)));
        // Owner N12: lava too. Fuel like a lava bucket, but no remainder - the clay bucket burns up with its lava
        // (a fresh empty bucket back would also undo its wear).
        CERAMIC_LAVA_BUCKET = item("ceramic_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.CERAMIC, Fluids.LAVA,
                McVersion.cookingFuel(p.durability(ModBucketItem.CERAMIC_USES), lava)));
    }

    /** All bucket items of this file in creative-tab order, the raw ceramic bucket included (empty on 26.2). */
    public static java.util.List<Item> buckets() {
        if (SOUL_LAVA_BUCKET == null) return java.util.List.of();
        return java.util.List.of(COPPER_BUCKET, COPPER_WATER_BUCKET, COPPER_LAVA_BUCKET, SOUL_LAVA_BUCKET,
                ENDERITE_BUCKET, ENDERITE_WATER_BUCKET, ENDERITE_LAVA_BUCKET, ENDERITE_SOUL_LAVA_BUCKET,
                RAW_CERAMIC_BUCKET, CERAMIC_BUCKET, CERAMIC_WATER_BUCKET, CERAMIC_LAVA_BUCKET);
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
    }

    private ModFluids() {}
}
