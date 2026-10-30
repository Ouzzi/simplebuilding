package com.simplelib.mixin;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Row and column of a creative tab (final in Vanilla) for {@code CreativeTabPolicy#reorder}. */
@Mixin(CreativeModeTab.class)
public interface CreativeModeTabPosition {
    @Mutable
    @Accessor("row")
    void simplelib$setRow(CreativeModeTab.Row row);

    @Mutable
    @Accessor("column")
    void simplelib$setColumn(int column);
}
