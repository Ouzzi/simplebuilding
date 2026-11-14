package com.simpleores.circuit;

import com.simpleores.OresRegistry;
import com.simpleores.line.LineBlocks;
import com.simpleores.line.LineSounds;
import com.simpleores.line.SignalDustBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * N46 "Simple Circuits" (owner answer CIRC46, 13.11.2026: lives in Simple Ores; plan docs/ai/PLAN-B-CIRC46-2026-11-14.md):
 * Astral and Bridge Redstone on walls and ceilings (F4), the Astral Relay (F3) and the Nihil Core with connected textures (F2).
 * Registered from {@link LineBlocks#init} (the dust items need the wall blocks). Models: tools/circuit_models.py.
 */
public final class CircuitBlocks {
    public static final Block ASTRAL_WALL_REDSTONE = OresRegistry.putBlock("astral_wall_redstone", Blocks.GLASS,
            s -> new WallSignalDustBlock(SignalDustBlock.Line.ASTRAL, LineBlocks.ASTRAL_REDSTONE, wallDust(s, LineBlocks.ASTRAL_REDSTONE, LineSounds.ASTRAL_DUST)));
    public static final Block BRIDGE_WALL_REDSTONE = OresRegistry.putBlock("bridge_wall_redstone", Blocks.GLASS,
            s -> new WallSignalDustBlock(SignalDustBlock.Line.BRIDGE, LineBlocks.BRIDGE_REDSTONE, wallDust(s, LineBlocks.BRIDGE_REDSTONE, LineSounds.BRIDGE_DUST)));
    /** Copper bulb strength and light, a real signal source (F3: 15 to all sides while lit). */
    public static final Block ASTRAL_RELAY = OresRegistry.putBlock("astral_relay", Blocks.REDSTONE_LAMP,
            s -> new AstralRelayBlock(s.strength(3.0F, 6.0F).mapColor(MapColor.COLOR_PINK).sound(LineSounds.ASTRAL_LAMP).isRedstoneConductor((state, level, pos) -> false)
                    .lightLevel(state -> state.getValue(AstralRelayBlock.LIT) ? 15 : 0)));
    /** A casing-like block: full, opaque, glows faintly while lit; a group of touching cores switches as one (F2). */
    public static final Block NIHIL_CORE = OresRegistry.putBlock("nihil_core", Blocks.REDSTONE_LAMP,
            s -> new NihilCoreBlock(s.strength(3.0F, 6.0F).mapColor(MapColor.COLOR_CYAN).sound(SoundType.AMETHYST).isRedstoneConductor((state, level, pos) -> false)
                    .lightLevel(state -> state.getValue(NihilCoreBlock.LIT) ? 9 : 0)));

    /** Creative order (Redstone tab, behind the line). */
    public static final String[] REDSTONE = {"astral_relay", "nihil_core"};

    private CircuitBlocks() {}

    /** The block items of the relay and the core (the wall dust is placed by the dust items, LineDustItem). */
    public static void items() {
        for (String path : REDSTONE) OresRegistry.putItem(path, p -> new BlockItem(OresRegistry.BLOCKS.get(path), p.useBlockDescriptionPrefix()));
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties wallDust(net.minecraft.world.level.block.state.BlockBehaviour.Properties s, Block floor, SoundType sound) {
        return s.noCollision().instabreak().pushReaction(PushReaction.POPPED).noOcclusion().sound(sound)
                .isRedstoneConductor((state, world, pos) -> false)
                .lightLevel(state -> state.getValue(WallSignalDustBlock.POWER) > 0 ? 3 : 0)
                .overrideLootTable(floor.getLootTable()).overrideDescription(floor.getDescriptionId());
    }
}
