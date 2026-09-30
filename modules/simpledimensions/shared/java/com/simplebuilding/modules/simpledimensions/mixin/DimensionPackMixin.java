package com.simplebuilding.modules.simpledimensions.mixin;
import net.minecraft.server.packs.repository.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;
@Mixin(BuiltInPackSource.class)
public abstract class DimensionPackMixin {
 @Inject(method="loadPacks",at=@At("TAIL"))
 private void dimensions(Consumer<Pack> consumer,CallbackInfo ci){
  if((Object)this instanceof ServerPacksSource)com.simplebuilding.modules.simpledimensions.GeneratedPack.load(consumer);
 }
}
