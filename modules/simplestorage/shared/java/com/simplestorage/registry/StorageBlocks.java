package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.crate.CrateBlock;
import com.simplestorage.crate.CrateWood;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestBlock;
import com.simplestorage.chest.TieredShulkerBoxBlock;
import com.simplestorage.chest.TieredTrappedChestBlock;
import com.simplestorage.chest.TrappedCopperChestBlock;
import com.simplestorage.table.StorageCraftingTableBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.material.MapColor;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The blocks, with the properties SimpleBuilding gave them before split stage 8a. */
public final class StorageBlocks {
    /** Crates in creative-tab order (Vanilla woods, then the End woods with Simple End). */
    public static final Map<CrateWood, CrateBlock> CRATES = new EnumMap<>(CrateWood.class);
    public static Block STORAGE_CRAFTING_TABLE;
    // Split stage 8b: the tiered (trapped) chests, the trapped copper chests and the tier shulker boxes, with the
    // properties SimpleBuilding gave them (hardness of the machines of the same tier, pickaxe like the copper chest).
    public static Block REINFORCED_CHEST, NETHERITE_CHEST, ENDERITE_CHEST;
    public static Block REINFORCED_TRAPPED_CHEST, NETHERITE_TRAPPED_CHEST, ENDERITE_TRAPPED_CHEST;
    public static Block TRAPPED_COPPER_CHEST, EXPOSED_TRAPPED_COPPER_CHEST, WEATHERED_TRAPPED_COPPER_CHEST, OXIDIZED_TRAPPED_COPPER_CHEST,
            WAXED_TRAPPED_COPPER_CHEST, WAXED_EXPOSED_TRAPPED_COPPER_CHEST, WAXED_WEATHERED_TRAPPED_COPPER_CHEST, WAXED_OXIDIZED_TRAPPED_COPPER_CHEST;
    public static Block REINFORCED_SHULKER_BOX, NETHERITE_SHULKER_BOX, ENDERITE_SHULKER_BOX;
    /** Split stage 8c-1: a bundle set down on a top face (no item; the bundle lives in the block entity). */
    public static Block PLACED_BUNDLE;
    /** ST27-1: the four pipes in tier order. */
    public static final Map<com.simplestorage.pipe.PipeTier, Block> PIPES = new EnumMap<>(com.simplestorage.pipe.PipeTier.class);
    /** BS29b-3: the chest network - copper cable, interface plate and the four controller chests in step order. */
    public static Block COPPER_CABLE, CHEST_INTERFACE;
    public static final Map<com.simplestorage.network.ControllerLevel, Block> CONTROLLERS = new EnumMap<>(com.simplestorage.network.ControllerLevel.class);
    /** Split stage 8c-2: the four placed backpacks in tier order (Basic, Reinforced, Netherite, Enderite). */
    public static final Map<com.simplestorage.backpack.BackpackTier, Block> BACKPACKS = new EnumMap<>(com.simplestorage.backpack.BackpackTier.class);

    /** {@code simpleEnd}: Simple End is loaded (only then the Ashwood and Duskwood crates exist, as before the move). */
    public static void register(boolean simpleEnd) {
        com.simplestorage.vehicle.BoatWoods.useEndWoods(simpleEnd);
        for (CrateWood wood : CrateWood.values()) {
            if (wood.external() && !simpleEnd) continue;
            CRATES.put(wood, block(wood.crateId(), wood.template(),
                    p -> new CrateBlock(wood, (wood.external() ? p.mapColor(wood.endColor()) : p).strength(1.5F).noOcclusion())));
        }
        STORAGE_CRAFTING_TABLE = block(SimpleStorage.STORAGE_CRAFTING_TABLE, Blocks.CRAFTING_TABLE, StorageCraftingTableBlock::new);
        registerChests();
        // Soft like wool, popped by pistons, no loot table: the block entity gives the bundle back (getDrops).
        PLACED_BUNDLE = Registry.register(BuiltInRegistries.BLOCK, key("placed_bundle"), new com.simplestorage.bundle.PlacedBundleBlock(
                BlockBehaviour.Properties.of().setId(key("placed_bundle")).strength(0.3F).sound(net.minecraft.world.level.block.SoundType.WOOL)
                        .noLootTable().mapColor(net.minecraft.world.level.material.MapColor.COLOR_BROWN)
                        .pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)));
        registerBackpacks();
        registerPipes();
        registerNetwork();
    }

    /** ST27-1: strength and sound like the chests of the tier; thin shapes need {@code noOcclusion}. */
    private static void registerPipes() {
        PIPES.put(com.simplestorage.pipe.PipeTier.BASIC, block("pipe", Blocks.IRON_BLOCK, p -> new com.simplestorage.pipe.ItemPipeBlock(
                com.simplestorage.pipe.PipeTier.BASIC, p.strength(2.0F, 4.0F).sound(SoundType.COPPER).mapColor(MapColor.COLOR_ORANGE).noOcclusion())));
        PIPES.put(com.simplestorage.pipe.PipeTier.REINFORCED, block("reinforced_pipe", Blocks.IRON_BLOCK, p -> new com.simplestorage.pipe.ItemPipeBlock(
                com.simplestorage.pipe.PipeTier.REINFORCED, p.strength(3.5F, 6.0F).sound(SoundType.COPPER).mapColor(MapColor.METAL).noOcclusion())));
        PIPES.put(com.simplestorage.pipe.PipeTier.NETHERITE, block("netherite_pipe", Blocks.IRON_BLOCK, p -> new com.simplestorage.pipe.ItemPipeBlock(
                com.simplestorage.pipe.PipeTier.NETHERITE, p.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_BLACK).noOcclusion())));
        PIPES.put(com.simplestorage.pipe.PipeTier.ENDERITE, block("enderite_pipe", Blocks.IRON_BLOCK, p -> new com.simplestorage.pipe.ItemPipeBlock(
                com.simplestorage.pipe.PipeTier.ENDERITE, p.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_PURPLE).noOcclusion())));
    }

    /** BS29b-3: cable and interface are soft copper parts; the controllers copy the chest of their step. */
    private static void registerNetwork() {
        COPPER_CABLE = block("copper_cable", Blocks.IRON_BLOCK, p -> new com.simplestorage.network.CableBlock(
                p.strength(1.0F, 3.0F).sound(SoundType.COPPER).mapColor(MapColor.COLOR_ORANGE).noOcclusion()));
        CHEST_INTERFACE = block("chest_interface", Blocks.IRON_BLOCK, p -> new com.simplestorage.network.InterfaceBlock(
                p.strength(1.0F, 3.0F).sound(SoundType.COPPER).mapColor(MapColor.GOLD).noOcclusion()));
        CONTROLLERS.put(com.simplestorage.network.ControllerLevel.WOODEN, block("wooden_controller_chest", Blocks.CHEST, p ->
                new com.simplestorage.network.ControllerChestBlock(com.simplestorage.network.ControllerLevel.WOODEN, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, p)));
        CONTROLLERS.put(com.simplestorage.network.ControllerLevel.REINFORCED, block("reinforced_controller_chest", REINFORCED_CHEST, p ->
                new com.simplestorage.network.ControllerChestBlock(com.simplestorage.network.ControllerLevel.REINFORCED, SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, p)));
        CONTROLLERS.put(com.simplestorage.network.ControllerLevel.NETHERITE, block("netherite_controller_chest", NETHERITE_CHEST, p ->
                new com.simplestorage.network.ControllerChestBlock(com.simplestorage.network.ControllerLevel.NETHERITE, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, p)));
        CONTROLLERS.put(com.simplestorage.network.ControllerLevel.ENDERITE, block("enderite_controller_chest", ENDERITE_CHEST, p ->
                new com.simplestorage.network.ControllerChestBlock(com.simplestorage.network.ControllerLevel.ENDERITE, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, p)));
    }

    /** The cable, the interface and the controller chests (the network blocks of the config switch). */
    public static Block[] networkBlocks() {
        java.util.List<Block> out = new java.util.ArrayList<>(java.util.List.of(COPPER_CABLE, CHEST_INTERFACE));
        out.addAll(CONTROLLERS.values());
        return out.toArray(Block[]::new);
    }

    /** Ids of the four backpacks (item and block) in tier order. */
    public static final java.util.List<String> BACKPACK_IDS = java.util.List.of("backpack", "reinforced_backpack", "netherite_backpack", "enderite_backpack");

    /** Like wool, popped by pistons; Netherite and Enderite blast-proof (as in SimpleBuilding's {@code backpackProperties}). */
    private static void registerBackpacks() {
        for (com.simplestorage.backpack.BackpackTier tier : com.simplestorage.backpack.BackpackTier.values()) {
            String path = BACKPACK_IDS.get(tier.ordinal());
            boolean blastProof = tier.ordinal() >= com.simplestorage.backpack.BackpackTier.NETHERITE.ordinal();
            BACKPACKS.put(tier, Registry.register(BuiltInRegistries.BLOCK, key(path), new com.simplestorage.backpack.BackpackBlock(tier,
                    BlockBehaviour.Properties.of().setId(key(path)).strength(0.8F, blastProof ? 1200.0F : 0.8F)
                            .sound(net.minecraft.world.level.block.SoundType.WOOL).mapColor(net.minecraft.world.level.material.MapColor.COLOR_BROWN)
                            .pushReaction(net.minecraft.world.level.material.PushReaction.POPPED))));
        }
    }

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, SimpleStorage.id(name));
    }

    private static void registerChests() {
        REINFORCED_CHEST = block("reinforced_chest", Blocks.IRON_BLOCK, p -> new TieredChestBlock(ChestTier.REINFORCED,
                SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, p.strength(3.5F, 6.0F).sound(SoundType.COPPER).mapColor(MapColor.METAL)));
        NETHERITE_CHEST = block("netherite_chest", Blocks.IRON_BLOCK, p -> new TieredChestBlock(ChestTier.NETHERITE,
                p.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_BLACK)));
        ENDERITE_CHEST = block("enderite_chest", Blocks.IRON_BLOCK, p -> new TieredChestBlock(ChestTier.ENDERITE,
                p.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_PURPLE)));
        REINFORCED_TRAPPED_CHEST = block("reinforced_trapped_chest", REINFORCED_CHEST, p -> new TieredTrappedChestBlock((TieredChestBlock) REINFORCED_CHEST, p));
        NETHERITE_TRAPPED_CHEST = block("netherite_trapped_chest", NETHERITE_CHEST, p -> new TieredTrappedChestBlock((TieredChestBlock) NETHERITE_CHEST, p));
        ENDERITE_TRAPPED_CHEST = block("enderite_trapped_chest", ENDERITE_CHEST, p -> new TieredTrappedChestBlock((TieredChestBlock) ENDERITE_CHEST, p));
        TRAPPED_COPPER_CHEST = copperChest("trapped_copper_chest", WeatheringCopper.WeatherState.UNAFFECTED, false, MapColor.COLOR_ORANGE);
        EXPOSED_TRAPPED_COPPER_CHEST = copperChest("exposed_trapped_copper_chest", WeatheringCopper.WeatherState.EXPOSED, false, MapColor.TERRACOTTA_LIGHT_GRAY);
        WEATHERED_TRAPPED_COPPER_CHEST = copperChest("weathered_trapped_copper_chest", WeatheringCopper.WeatherState.WEATHERED, false, MapColor.WARPED_STEM);
        OXIDIZED_TRAPPED_COPPER_CHEST = copperChest("oxidized_trapped_copper_chest", WeatheringCopper.WeatherState.OXIDIZED, false, MapColor.WARPED_NYLIUM);
        WAXED_TRAPPED_COPPER_CHEST = copperChest("waxed_trapped_copper_chest", WeatheringCopper.WeatherState.UNAFFECTED, true, MapColor.COLOR_ORANGE);
        WAXED_EXPOSED_TRAPPED_COPPER_CHEST = copperChest("waxed_exposed_trapped_copper_chest", WeatheringCopper.WeatherState.EXPOSED, true, MapColor.TERRACOTTA_LIGHT_GRAY);
        WAXED_WEATHERED_TRAPPED_COPPER_CHEST = copperChest("waxed_weathered_trapped_copper_chest", WeatheringCopper.WeatherState.WEATHERED, true, MapColor.WARPED_STEM);
        WAXED_OXIDIZED_TRAPPED_COPPER_CHEST = copperChest("waxed_oxidized_trapped_copper_chest", WeatheringCopper.WeatherState.OXIDIZED, true, MapColor.WARPED_NYLIUM);
        // Copied from Vanilla's shulker box (dynamic shape, no occlusion, broken by pistons, no tool needed), harder and
        // blast-proof like the chests; an open box does not suffocate.
        REINFORCED_SHULKER_BOX = block("reinforced_shulker_box", Blocks.SHULKER_BOX, p -> new TieredShulkerBoxBlock(ChestTier.REINFORCED,
                shulker(p).strength(3.0F, 6.0F).mapColor(MapColor.METAL)));
        NETHERITE_SHULKER_BOX = block("netherite_shulker_box", Blocks.SHULKER_BOX, p -> new TieredShulkerBoxBlock(ChestTier.NETHERITE,
                shulker(p).strength(4.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_BLACK)));
        ENDERITE_SHULKER_BOX = block("enderite_shulker_box", Blocks.SHULKER_BOX, p -> new TieredShulkerBoxBlock(ChestTier.ENDERITE,
                shulker(p).strength(5.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_PURPLE)));
    }

    private static Block copperChest(String name, WeatheringCopper.WeatherState state, boolean waxed, MapColor color) {
        return block(name, Blocks.IRON_BLOCK, p -> new TrappedCopperChestBlock(state, waxed,
                p.mapColor(color).strength(3.0F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops()));
    }

    private static BlockBehaviour.Properties shulker(BlockBehaviour.Properties p) {
        return p.isSuffocating(TieredShulkerBoxBlock::isClosedAt);
    }

    /** The blocks of the tiered chest block entity: the three tiers and their trapped twins. */
    public static Block[] tieredChests() {
        return new Block[]{REINFORCED_CHEST, NETHERITE_CHEST, ENDERITE_CHEST, REINFORCED_TRAPPED_CHEST, NETHERITE_TRAPPED_CHEST, ENDERITE_TRAPPED_CHEST};
    }

    /** The eight trapped copper chests (oxidation order, then waxed). */
    public static Block[] trappedCopperChests() {
        return new Block[]{TRAPPED_COPPER_CHEST, EXPOSED_TRAPPED_COPPER_CHEST, WEATHERED_TRAPPED_COPPER_CHEST, OXIDIZED_TRAPPED_COPPER_CHEST,
                WAXED_TRAPPED_COPPER_CHEST, WAXED_EXPOSED_TRAPPED_COPPER_CHEST, WAXED_WEATHERED_TRAPPED_COPPER_CHEST, WAXED_OXIDIZED_TRAPPED_COPPER_CHEST};
    }

    /** The tiered chest of {@code tier} (the chest carts and chest boats show it). */
    public static Block chest(com.simplestorage.chest.ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> REINFORCED_CHEST;
            case NETHERITE -> NETHERITE_CHEST;
            case ENDERITE -> ENDERITE_CHEST;
        };
    }

    public static Block[] shulkerBoxes() {
        return new Block[]{REINFORCED_SHULKER_BOX, NETHERITE_SHULKER_BOX, ENDERITE_SHULKER_BOX};
    }

    public static Block[] crates() {
        return CRATES.values().toArray(Block[]::new);
    }

    /** Crates burn like planks (not the Nether woods); call once after registration. */
    public static void registerFlammability() {
        com.simplestorage.mixin.FireBlockInvoker fire = (com.simplestorage.mixin.FireBlockInvoker) Blocks.FIRE;
        CRATES.forEach((wood, crate) -> {
            if (!wood.nether()) fire.simplestorage$setFlammable(crate, 5, 20);
        });
    }

    private static <B extends Block> B block(String name, Block base, Function<BlockBehaviour.Properties, B> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleStorage.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key)));
    }

    private StorageBlocks() {}
}
