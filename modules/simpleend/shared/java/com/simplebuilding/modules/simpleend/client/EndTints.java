package com.simplebuilding.modules.simpleend.client;

import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.EndWood;
import java.util.List;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tint = the biome's dry foliage colour blended with the wood's grey-brown: exactly grey-brown in Ash Valley/Dawnlands
 * (their biome JSON), a warmer grey-brown elsewhere - never green. In hand: the Ash Valley tone (Duskwood leaves: the Dawnlands tone).
 */
public final class EndTints {
    public static final int ASH = 0xFFA89C8A;
    public static final int DUSK = 0xFF958673;

    private EndTints() {}

    static BlockTintSource dry(int hand) {
        return new BlockTintSource() {
            @Override public int color(BlockState state) { return hand; }
            @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return blend(BiomeColors.getAverageDryFoliageColor(level, pos), hand);
            }
        };
    }

    /**
     * 60 % own grey-brown, 40 % biome: identical in Ash Valley/Dawnlands (their dry foliage is this grey-brown),
     * and only a hint of the warmer Vanilla dry-leaf tone elsewhere, so End wood never turns orange.
     */
    static int blend(int biome, int own) {
        int r = (((biome >> 16) & 0xFF) * 2 + ((own >> 16) & 0xFF) * 3) / 5;
        int g = (((biome >> 8) & 0xFF) * 2 + ((own >> 8) & 0xFF) * 3) / 5;
        int b = ((biome & 0xFF) * 2 + (own & 0xFF) * 3) / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static void register(BlockColors colors) {
        if (EndRegistry.END_GRASS == null) return;
        colors.register(List.of(dry(ASH)), EndRegistry.END_GRASS, EndRegistry.END_BUSH, EndWood.ASHWOOD.leaves);
        colors.register(List.of(dry(DUSK)), EndWood.DUSKWOOD.leaves);
    }
}
