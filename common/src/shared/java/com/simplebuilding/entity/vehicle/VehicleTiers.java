package com.simplebuilding.entity.vehicle;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.items.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The numbers of the tiered vehicles (Queue N19/N23, docs/ai/PLAN-FAHRZEUG-STUFEN-2026-10-10.md). The tiers are the
 * chest tiers; machine speeds come from {@link ServerTuning} like those of the furnaces and hoppers of the same tier.
 */
public final class VehicleTiers {
    /** Vanilla furnace cart: ticks of fuel per fuel item and the most it holds. */
    public static final int VANILLA_FUEL_PER_ITEM = 3600;
    public static final int VANILLA_MAX_FUEL = 32000;

    private VehicleTiers() {
    }

    /** Tier number of {@link ServerTuning}: 1 reinforced, 2 netherite, 3 enderite. */
    public static int level(ChestTier tier) {
        return tier.ordinal() + 1;
    }

    /** How much longer a fuel item burns than in vanilla's cart: the speed of the furnace of the tier (2/4/8). */
    public static int fuelFactor(ChestTier tier) {
        return 1 + ServerTuning.furnaceExtraTicks(level(tier));
    }

    /**
     * Top speed as a share of a plain cart's (vanilla's furnace cart: 0.5). Enderite reaches a plain cart's top
     * speed, never more - faster carts leave the rails on curves.
     */
    public static double furnaceSpeed(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> 0.625;
            case NETHERITE -> 0.75;
            case ENDERITE -> 1.0;
        };
    }

    /** Items a hopper cart may take in per tick: the speed of the hopper of the tier (2/4/8; vanilla's cart 1). */
    public static int hopperItemsPerTick(ChestTier tier) {
        return ServerTuning.hopperSpeed(level(tier));
    }

    /** Split stage 8b-2: the chest carts and chest boats are Simple Storage's (by id). */
    public static Item chestMinecart(ChestTier tier) {
        return com.simplebuilding.util.StorageRefs.chestMinecart(tier.textureName());
    }

    public static Item furnaceMinecart(ChestTier tier) {
        return pick(tier, com.simplebuilding.util.OvenRefs.item("reinforced_furnace_minecart"), com.simplebuilding.util.OvenRefs.item("netherite_furnace_minecart"), com.simplebuilding.util.OvenRefs.item("enderite_furnace_minecart"));
    }

    public static Item hopperMinecart(ChestTier tier) {
        return pick(tier, ModItems.REINFORCED_HOPPER_MINECART, ModItems.NETHERITE_HOPPER_MINECART, ModItems.ENDERITE_HOPPER_MINECART);
    }

    public static Item chestBoat(ChestTier tier) {
        return com.simplebuilding.util.StorageRefs.chestBoat(tier.textureName());
    }

    public static Block chest(ChestTier tier) {
        // Split stage 8b: on 26.3 the chests are Simple Storage's (by id).
        return com.simplebuilding.util.StorageRefs.chestBlock(tier.textureName());
    }

    public static Block furnace(ChestTier tier) {
        return pick(tier, com.simplebuilding.util.OvenRefs.block("reinforced_furnace"), com.simplebuilding.util.OvenRefs.block("netherite_furnace"), com.simplebuilding.util.OvenRefs.block("enderite_furnace"));
    }

    public static Block hopper(ChestTier tier) {
        return pick(tier, ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER);
    }

    private static <T> T pick(ChestTier tier, T reinforced, T netherite, T enderite) {
        return switch (tier) {
            case REINFORCED -> reinforced;
            case NETHERITE -> netherite;
            case ENDERITE -> enderite;
        };
    }
}
