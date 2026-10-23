package com.simplemusic.client;

import java.util.List;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * W24-2 (owner 23.10.: "Kupferkontakte in Linienfarbe"): the contact from a Pocket Jukebox to a line takes the line's
 * colour instead of copper - Vanilla redstone red, SimpleBuilding's Astralit, Nihilit and bridge lines (by id, no import:
 * the module stays playable alone). The contact models {@code pocket_jukebox_link_<side>} carry tintindex 1..4 = north,
 * east, south, west (the world side); {@link #SOURCES} is the list to register for the 17 blocks (index 0 = untinted).
 */
public final class PocketLinkTint implements BlockTintSource {
    public static final int REDSTONE = 0xCC281A;
    public static final int ASTRAL = 0xB06298;
    public static final int NIHIL = 0x589EAA;
    /** The bridge joins both lines: one tint per face, so the middle tone of the two. */
    public static final int BRIDGE = 0x8480A1;
    private static final int WHITE = -1;

    public static final List<BlockTintSource> SOURCES = List.of(new PocketLinkTint(null), new PocketLinkTint(Direction.NORTH),
            new PocketLinkTint(Direction.EAST), new PocketLinkTint(Direction.SOUTH), new PocketLinkTint(Direction.WEST));

    private final @Nullable Direction side;

    private PocketLinkTint(@Nullable Direction side) {
        this.side = side;
    }

    /** The line colour (0xRRGGBB) of a neighbour block state, or -1 if it is no known line. */
    public static int lineColour(BlockState neighbour) {
        var id = BuiltInRegistries.BLOCK.getKey(neighbour.getBlock());
        if ("minecraft".equals(id.getNamespace()) && "redstone_wire".equals(id.getPath())) return REDSTONE;
        if (!"simplebuilding".equals(id.getNamespace())) return -1;
        return switch (id.getPath()) {
            case "astral_redstone" -> ASTRAL;
            case "nihil_redstone" -> NIHIL;
            case "bridge_redstone" -> BRIDGE;
            default -> -1;
        };
    }

    @Override
    public int color(BlockState state) {
        return WHITE;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (this.side == null || level == null || pos == null) return WHITE;
        int colour = lineColour(level.getBlockState(pos.relative(this.side)));
        return colour == -1 ? ARGB.opaque(REDSTONE) : ARGB.opaque(colour);
    }
}
