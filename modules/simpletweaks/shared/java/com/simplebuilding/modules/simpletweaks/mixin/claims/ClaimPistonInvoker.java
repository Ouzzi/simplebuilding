package com.simplebuilding.modules.simpletweaks.mixin.claims;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(net.minecraft.world.level.block.piston.PistonBaseBlock.class)
public interface ClaimPistonInvoker { @Invoker("moveBlocks") boolean claims$move(Level level,BlockPos pos,Direction direction,boolean extending); }
