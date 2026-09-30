package com.simplebuilding.modules.simpledimensions.mixin;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Vanilla 26.3 GameTestServer intentionally drops datapack dimensions. Restore real registry data only in that test harness. */
@Mixin(GameTestServer.class)
public abstract class DimensionTestWorldMixin {
 @Redirect(method="lambda$create$1",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
 private static WorldDimensions.Complete dimensions(WorldDimensions dimensions,Registry<LevelStem> ignored,LevelSettings settings,WorldLoader.DataLoadContext context){
  return dimensions.bake(context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
 }
}
