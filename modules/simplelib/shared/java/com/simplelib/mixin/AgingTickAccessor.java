package com.simplelib.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** DW04B-1: lets {@link com.simplelib.api.aging.Aging} switch on random ticks for a Vanilla base block of an aging chain. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public interface AgingTickAccessor {
    @Accessor("isRandomlyTicking")
    void simplelib$setRandomlyTicking(boolean ticking);
}
