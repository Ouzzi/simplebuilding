package com.simplebuilding.client.render;

import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Farbe des abgestellten gefaerbten Rucksacks: Ebene 0 (die Leder-Ebene von
 * {@code block/template_backpack_dyed}) nimmt die Farbe der Block-Entity, alles andere bleibt weiss.
 *
 * <p>MC 1.21.11: ein {@link BlockColor} (auf 26.2 statt dessen eine {@code BlockTintSource}),
 * registriert per Fabric {@code ColorProviderRegistry.BLOCK} bzw. NeoForge
 * {@code RegisterColorHandlersEvent.Block}.
 */
public final class BackpackBlockTint implements BlockColor {
    public static final BackpackBlockTint INSTANCE = new BackpackBlockTint();

    private BackpackBlockTint() {
    }

    /** Die Farbe (ARGB) des Rucksacks an {@code pos}, oder weiss, wenn dort keiner gefaerbt steht. */
    public static int colorAt(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack
                && backpack.dyeColor() != DyedStorage.UNDYED) {
            return ARGB.opaque(backpack.dyeColor());
        }
        return -1;
    }

    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        return tintIndex == 0 ? colorAt(level, pos) : -1;
    }
}
