package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import com.simplebuilding.modules.simplemobs.DisguiseHolder;
import com.simplebuilding.modules.simplemobs.DisguiseMarker;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cloak of deception: disguise state on every player (clients see it through {@link DisguiseMarker}), right-click to imitate, a hit ends the disguise. */
@Mixin(Player.class)
public abstract class PlayerDisguiseMixin implements DisguiseHolder {
    @Unique private String simplemobs$spec = "";
    @Unique private final ItemStack[] simplemobs$gear = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    @Unique private @Nullable DisguiseMarker simplemobs$marker;
    @Unique private long simplemobs$revealedUntil = Long.MIN_VALUE;
    @Unique private final Map<UUID, Long> simplemobs$spotted = new HashMap<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplemobs$tickDisguise(CallbackInfo ci) {
        Disguise.tick((Player) (Object) this);
    }

    /** Wearing the cloak, an empty-handed right-click on a mob or player takes its shape (items in hand keep vanilla use). */
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void simplemobs$imitate(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (hand != InteractionHand.MAIN_HAND || !self.getMainHandItem().isEmpty() || self.isSpectator()) return;
        if (!Disguise.wearsCloak(self) || !(entity instanceof LivingEntity living) || !Disguise.canImitate(entity)) return;
        if (!self.level().isClientSide()) Disguise.morph(self, living);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    /** Owner: hitting ends the disguise; hitting a living thing also reveals the wearer around it. */
    @Inject(method = "attack", at = @At("HEAD"))
    private void simplemobs$attackEndsDisguise(Entity entity, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return;
        if (entity instanceof LivingEntity living && Disguise.wearsCloak(self)) Disguise.reveal(self, living);
        else Disguise.revert(self);
    }

    @Override public String simplemobs$spec() { return simplemobs$spec; }
    @Override public void simplemobs$setSpec(String spec) { simplemobs$spec = spec; }
    @Override public ItemStack[] simplemobs$gear() { return simplemobs$gear; }
    @Override public @Nullable DisguiseMarker simplemobs$marker() { return simplemobs$marker; }
    @Override public void simplemobs$setMarker(@Nullable DisguiseMarker marker) { simplemobs$marker = marker; }
    @Override public long simplemobs$revealedUntil() { return simplemobs$revealedUntil; }
    @Override public void simplemobs$setRevealedUntil(long gameTime) { simplemobs$revealedUntil = gameTime; }
    @Override public Map<UUID, Long> simplemobs$spotted() { return simplemobs$spotted; }
}
