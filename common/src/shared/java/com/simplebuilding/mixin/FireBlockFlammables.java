package com.simplebuilding.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Traegt Bloecke der Mod in die Feuer-Tabelle ein (private {@code FireBlock#setFlammable}): Holz-Achtel brennen. */
@Mixin(FireBlock.class)
public interface FireBlockFlammables {
    @Invoker("setFlammable")
    void simplebuilding$setFlammable(Block block, int igniteOdds, int burnOdds);
}
