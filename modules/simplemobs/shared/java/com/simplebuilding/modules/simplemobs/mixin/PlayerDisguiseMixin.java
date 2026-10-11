package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.Disguise;
import com.simplebuilding.modules.simplemobs.DisguiseHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cloak of deception: synced disguise data on every player, right-click to imitate, a hit ends the disguise. */
@Mixin(Player.class)
public abstract class PlayerDisguiseMixin implements DisguiseHolder {
    @Unique private static final EntityDataAccessor<String> SIMPLEMOBS_SPEC = SynchedEntityData.defineId(Player.class, EntityDataSerializers.STRING);
    @Unique private static final EntityDataAccessor<ItemStack> SIMPLEMOBS_GEAR_0 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
    @Unique private static final EntityDataAccessor<ItemStack> SIMPLEMOBS_GEAR_1 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
    @Unique private static final EntityDataAccessor<ItemStack> SIMPLEMOBS_GEAR_2 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
    @Unique private static final EntityDataAccessor<ItemStack> SIMPLEMOBS_GEAR_3 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
    @Unique private static final EntityDataAccessor<ItemStack> SIMPLEMOBS_GEAR_4 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
    @Unique private static final EntityDataAccessor<ItemStack> SIMPLEMOBS_GEAR_5 = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);

    @Unique private long simplemobs$revealedUntil = Long.MIN_VALUE;
    @Unique private final Map<UUID, Long> simplemobs$spotted = new HashMap<>();

    @Unique
    private static EntityDataAccessor<ItemStack> simplemobs$gearKey(int slot) {
        return switch (slot) {
            case 0 -> SIMPLEMOBS_GEAR_0;
            case 1 -> SIMPLEMOBS_GEAR_1;
            case 2 -> SIMPLEMOBS_GEAR_2;
            case 3 -> SIMPLEMOBS_GEAR_3;
            case 4 -> SIMPLEMOBS_GEAR_4;
            default -> SIMPLEMOBS_GEAR_5;
        };
    }

    @Unique
    private SynchedEntityData simplemobs$data() {
        return ((Player) (Object) this).getEntityData();
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void simplemobs$defineDisguise(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SIMPLEMOBS_SPEC, "");
        for (int i = 0; i < 6; i++) builder.define(simplemobs$gearKey(i), ItemStack.EMPTY);
    }

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

    @Override public String simplemobs$spec() { return simplemobs$data().get(SIMPLEMOBS_SPEC); }
    @Override public void simplemobs$setSpec(String spec) { simplemobs$data().set(SIMPLEMOBS_SPEC, spec); }
    @Override public ItemStack simplemobs$gear(int slot) { return simplemobs$data().get(simplemobs$gearKey(slot)); }
    @Override public void simplemobs$setGear(int slot, ItemStack stack) { simplemobs$data().set(simplemobs$gearKey(slot), stack); }
    @Override public long simplemobs$revealedUntil() { return simplemobs$revealedUntil; }
    @Override public void simplemobs$setRevealedUntil(long gameTime) { simplemobs$revealedUntil = gameTime; }
    @Override public Map<UUID, Long> simplemobs$spotted() { return simplemobs$spotted; }
    @Override public boolean simplemobs$isSpecKey(Object accessor) { return SIMPLEMOBS_SPEC.equals(accessor); }
}
