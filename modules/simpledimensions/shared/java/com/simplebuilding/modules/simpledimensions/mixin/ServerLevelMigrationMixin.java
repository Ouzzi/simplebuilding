package com.simplebuilding.modules.simpledimensions.mixin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * WD25-14: a level asked for under the old mod id ({@code simpledimension:<id>} - saved player position, respawn point,
 * ender pearls) is the level of the new id; the next save writes the new key. Only a missed lookup is redirected.
 */
@Mixin(MinecraftServer.class)
public abstract class ServerLevelMigrationMixin {
 @Inject(method="getLevel",at=@At("RETURN"),cancellable=true)
 private void simpledimensions$oldKey(ResourceKey<Level> dimension,CallbackInfoReturnable<ServerLevel> cir){
  if(cir.getReturnValue()!=null||dimension==null)return;
  var now=com.simplebuilding.modules.simpledimensions.LegacyIds.migrate(dimension.identifier());
  if(now!=null)cir.setReturnValue(((MinecraftServer)(Object)this).getLevel(ResourceKey.create(Registries.DIMENSION,now)));
 }
}
