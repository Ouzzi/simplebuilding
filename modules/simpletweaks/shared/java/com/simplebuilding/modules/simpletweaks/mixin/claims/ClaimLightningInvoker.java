package com.simplebuilding.modules.simpletweaks.mixin.claims;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LightningBolt.class)
public interface ClaimLightningInvoker {
 @Invoker("spawnFire") void claims$fire(int additionalSources);
 @Invoker("clearCopperOnLightningStrike") static void claims$clean(Level level,BlockPos pos){throw new AssertionError();}
}
