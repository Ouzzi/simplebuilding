package com.simplebuilding.modules.simpleend.client;

import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.EndWood;
import java.util.List;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tint = the biome's dry foliage colour: grey-brown in Ash Valley/Dawnlands (their biome JSON), the Vanilla dry
 * foliage tone elsewhere - never green. In hand: the Ash Valley tone (Duskwood leaves: the Dawnlands tone).
 */
public final class EndTints {
    public static final int ASH = 0xFFA89C8A;
    public static final int DUSK = 0xFF958673;

    private EndTints() {}

    static BlockTintSource dry(int hand) {
        return new BlockTintSource() {
            @Override public int color(BlockState state) { return hand; }
            @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return BiomeColors.getAverageDryFoliageColor(level, pos);
            }
        };
    }

    public static void register(BlockColors colors) {
        if (EndRegistry.END_GRASS == null) return;
        colors.register(List.of(dry(ASH)), EndRegistry.END_GRASS, EndRegistry.END_BUSH, EndWood.ASHWOOD.leaves);
        colors.register(List.of(dry(DUSK)), EndWood.DUSKWOOD.leaves);
    }
}
