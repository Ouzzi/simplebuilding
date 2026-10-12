package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Boats are placed through use(), not useOn(); a refused footprint behaves like a blocked spot (FAIL, nothing consumed). */
@Mixin(BoatItem.class)
public abstract class ClaimBoatMixin {
 @ModifyExpressionValue(method="use",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/BoatItem;getBoat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;"))
 private AbstractBoat claims$boat(AbstractBoat boat,@com.llamalad7.mixinextras.sugar.Local(argsOnly=true) Player player){
  return boat!=null&&player instanceof ServerPlayer actor&&!Claims.allowEntity(actor,boat)?null:boat;
 }
}
