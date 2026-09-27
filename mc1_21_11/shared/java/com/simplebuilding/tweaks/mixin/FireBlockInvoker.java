package com.simplebuilding.tweaks.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Zuendwert eines Blocks aus der Feuer-Tabelle ({@code FireBlock#setFlammable}): die Amethystlinse
 * zuendet nur, was auch Feuer fangen wuerde. Die Methode ist in Vanilla und NeoForge privat.
 */
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("getIgniteOdds")
    int simplebuilding$getIgniteOdds(BlockState state);
}
