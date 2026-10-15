package com.simplebuilding.modules.simplestats.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shearing mobs and the start of an elytra flight. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @WrapMethod(method = "interactOn")
    private InteractionResult simplestats$interact(Entity entity, InteractionHand hand, Vec3 location, Operation<InteractionResult> original) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer player) || !(entity instanceof Shearable shearable)) return original.call(entity, hand, location);
        ItemStack held = self.getItemInHand(hand);
        boolean ready = shearable.readyForShearing();
        InteractionResult result = original.call(entity, hand, location);
        if (ready && result.consumesAction()) StatsRuntime.onShear(player, held, entity);
        return result;
    }

    @Inject(method = "startFallFlying", at = @At("TAIL"))
    private void simplestats$flight(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) StatsRuntime.startFlight(player);
    }
}
