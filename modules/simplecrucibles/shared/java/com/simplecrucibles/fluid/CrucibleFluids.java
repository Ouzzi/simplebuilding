package com.simplecrucibles.fluid;

import com.simplecrucibles.SimpleCrucibles;
import com.simplelib.bucket.Buckets;
import com.simplelib.bucket.ModBucketItem;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

/**
 * Soul lava, the soul lava bucket and the copper buckets (WH26-7: Enderite and ceramic are Simple Tools') (split stage 5b: SimpleBuilding's before, ids {@code simplebuilding:*} migrate
 * through {@code SimpleCrucibles.registerIdMoves}). Fluids register in the FLUID phase ({@link #registerFluids}, Forge
 * unlocks one registry per event), the block with the blocks, the buckets with the items. NeoForge/Forge replace
 * {@link #factory} before registration with subclasses that carry their fluid type.
 */
public final class CrucibleFluids {
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
    // WH26-7: the Enderite and ceramic buckets are Simple Tools' (SimpleLib com.simplelib.bucket.Buckets holds them).

    public static void registerFluids() {
        if (SOUL_LAVA != null) return;
        FLOWING_SOUL_LAVA = Registry.register(BuiltInRegistries.FLUID, id("flowing_soul_lava"), factory.flowing());
        SOUL_LAVA = Registry.register(BuiltInRegistries.FLUID, id("soul_lava"), factory.source());
    }

    public static void registerBlocks() {
        if (SOUL_LAVA_BLOCK != null) return;
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id("soul_lava"));
        // Lava's properties without replaceable(): nothing builds into it but creative players (SoulLavaBlock).
        SOUL_LAVA_BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new SoulLavaBlock(SOUL_LAVA, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN).noCollision().randomTicks().strength(100.0F).lightLevel(state -> 15)
                .pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE).noLootTable().liquid().sound(SoundType.EMPTY).setId(key)));
    }

    public static void registerItems() {
        if (SOUL_LAVA_BUCKET != null) return;
        int lava = SoulLava.LAVA_FUEL_TICKS;
        SOUL_LAVA_BUCKET = item("soul_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.IRON, SOUL_LAVA,
                Buckets.fuel(p.stacksTo(1), SoulLava.fuelTicks())));
        COPPER_BUCKET = item("copper_bucket", p -> new ModBucketItem(ModBucketItem.Kind.COPPER, Fluids.EMPTY, p.stacksTo(16)));
        COPPER_WATER_BUCKET = item("copper_water_bucket", p -> new ModBucketItem(ModBucketItem.Kind.COPPER, Fluids.WATER,
                p.stacksTo(1).craftRemainder(COPPER_BUCKET)));
        COPPER_LAVA_BUCKET = item("copper_lava_bucket", p -> new ModBucketItem(ModBucketItem.Kind.COPPER, Fluids.LAVA,
                Buckets.fuel(p.stacksTo(1), lava)));
        // The shared bucket classes (SimpleLib) read the family from Buckets; with Simple Tools the Enderite soul lava buckets follow.
        Buckets.SOUL_LAVA = SOUL_LAVA;
        Buckets.SOUL_LAVA_BUCKET = SOUL_LAVA_BUCKET;
        Buckets.COPPER_BUCKET = COPPER_BUCKET;
        Buckets.COPPER_WATER_BUCKET = COPPER_WATER_BUCKET;
        Buckets.COPPER_LAVA_BUCKET = COPPER_LAVA_BUCKET;
        Buckets.soulLavaFuelTicks = SoulLava.fuelTicks();
        Buckets.registerEnderiteSoulLava();
    }

    /** The module's own bucket items in creative-tab order (copper, soul lava; empty before registration). */
    public static java.util.List<Item> buckets() {
        if (SOUL_LAVA_BUCKET == null) return java.util.List.of();
        return java.util.List.of(COPPER_BUCKET, COPPER_WATER_BUCKET, COPPER_LAVA_BUCKET, SOUL_LAVA_BUCKET);
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    public static Identifier id(String name) {
        return SimpleCrucibles.id(name);
    }

    private CrucibleFluids() {}
}
