package com.simplebuilding.client.render;

import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Farbe des abgestellten gefaerbten Rucksacks: Ebene 0 (die Leder-Ebene von
 * {@code block/template_backpack_dyed}) nimmt die Farbe der Block-Entity, alles andere bleibt weiss.
 * Registriert je Loader fuer die vier Rucksack-Bloecke (Fabric {@code BlockColorRegistry}, NeoForge
 * {@code RegisterColorHandlersEvent.BlockTintSources}, Forge {@code RegisterColorHandlersEvent.Block}).
 *
 * <p>Die Section-Kopie, aus der der Chunk-Mesh gebaut wird, fuehrt die Block-Entities mit; die
 * Farbe kommt dort per {@code BackpackBlockEntity#getUpdatePacket} an.
 */
public final class BackpackBlockTint implements BlockTintSource {
    public static final BackpackBlockTint INSTANCE = new BackpackBlockTint();

    private BackpackBlockTint() {
    }

    /** Die Farbe (ARGB) des Rucksacks an {@code pos}, oder weiss, wenn dort keiner gefaerbt steht. */
    public static int colorAt(BlockAndTintGetter level, BlockPos pos) {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack
                && backpack.dyeColor() != DyedStorage.UNDYED) {
            return ARGB.opaque(backpack.dyeColor());
        }
        return -1;
    }

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return colorAt(level, pos);
    }
}
