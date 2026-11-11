package com.simplebuilding.modules.simpledimensions.mixin;
import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * WD25-14: a world saved with the old mod id lists its dimensions as {@code simpledimension:<id>}. Re-keyed to
 * {@code simpledimensions:<id>} before baking, otherwise the old key would open a second level next to the new one
 * (see {@link com.simplebuilding.modules.simpledimensions.LegacyIds}). Untouched when no old key is saved.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMigrationMixin {
 @Inject(method="bake",at=@At("HEAD"),cancellable=true)
 private void simpledimensions$rekey(Registry<LevelStem> base,CallbackInfoReturnable<WorldDimensions.Complete> cir){
  var now=com.simplebuilding.modules.simpledimensions.LegacyIds.rekey(((WorldDimensions)(Object)this).dimensions());
  if(now!=null)cir.setReturnValue(new WorldDimensions(now).bake(base));
 }
}
