package com.simplebuilding.machines;

import com.simplebuilding.config.ServerTuning;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Double Piston and Signal Dampener of the Simple Machines tab (IG31-12, b-mach32 31.10.: moved from the former module
 * Simple Redstone into the core, next to the piston upgrades; docs/ai/PLAN-B-MACH32-2026-10-31.md). Registered from 26.3 on
 * ({@code McVersion.MACHINE_PARTS}, like the Simple Doors); {@code ModBlocks} and {@code ModItems} hold the registered objects,
 * the old {@code simpleredstone:} ids move here through the 26.3 {@code IdMovesHook}.
 */
public final class MachineParts {
    /** Namespace of the former module; its ids move to {@code simplebuilding} (IdMovesHook). */
    public static final String OLD_NAMESPACE = "simpleredstone";
    /** Items (and recipes, recipe advancements under recipes/redstone/) that moved from {@link #OLD_NAMESPACE}. */
    public static final List<String> MOVED_ITEMS = List.of("double_piston", "signal_dampener");
    /** Blocks that moved from {@link #OLD_NAMESPACE} under the same path (the 1x2 barrel became air). */
    public static final List<String> MOVED_BLOCKS = List.of("double_piston", "double_piston_head", "double_piston_arm", "signal_dampener");

    private MachineParts() {}

    /** Server switch {@code server.features.doublePiston}: off = the Double Piston keeps still (placed ones stay). */
    public static boolean doublePistonOn() {
        return ServerTuning.get().features.doublePiston;
    }

    /** Server switch {@code server.features.signalDampener}: off = the Dampener outputs nothing. */
    public static boolean signalDampenerOn() {
        return ServerTuning.get().features.signalDampener;
    }

    public static Block signalDampener() {
        ResourceKey<Block> key = key("signal_dampener");
        return Registry.register(BuiltInRegistries.BLOCK, key, new SignalDampenerBlock(BlockBehaviour.Properties.of()
                .setId(key).instabreak().sound(SoundType.STONE).pushReaction(com.simplebuilding.version.McVersion.PUSH_DESTROYS)));
    }

    /** The base block of a variant; head and arm are registered with it ({@link DoublePistonBlock#head()}, {@link DoublePistonBlock#arm()}). */
    public static Block doublePiston(DoublePistonVariant variant) {
        DoublePistonHeadBlock head = (DoublePistonHeadBlock) part(variant.headId(), p -> new DoublePistonHeadBlock(variant, p.noLootTable()));
        DoublePistonArmBlock arm = (DoublePistonArmBlock) part(variant.armId(), p -> new DoublePistonArmBlock(variant, p.noLootTable()));
        return part(variant.id(), p -> new DoublePistonBlock(variant, head, arm, p));
    }

    public static Block doublePiston() {
        return doublePiston(DoublePistonVariant.BASE);
    }

    public static Block head(Block base) {
        return ((DoublePistonBlock) base).head();
    }

    public static Block arm(Block base) {
        return ((DoublePistonBlock) base).arm();
    }

    public static Item item(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key)));
    }

    private static ResourceKey<Block> key(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("simplebuilding", path));
    }

    /**
     * A piston part like Vanilla's piston (strength 1.5, stone sound, no conductor); immovable for pistons and machines
     * (a part torn off would leave a broken piston, PLAN decision 4) and without occlusion (parts are partial blocks). Head and
     * arm have no loot table, like Vanilla's piston head: the base drops the piston.
     */
    private static Block part(String path, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = key(path);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).strength(1.5F)
                .sound(SoundType.STONE).pushReaction(com.simplebuilding.version.McVersion.PUSH_BLOCKED).isRedstoneConductor((state, level, pos) -> false)
                .noOcclusion();
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties));
    }
}
