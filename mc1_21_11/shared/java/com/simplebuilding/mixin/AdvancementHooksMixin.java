package com.simplebuilding.mixin;

import com.simplebuilding.advancement.AdvancementChecks;
import com.simplebuilding.advancement.ModCounters;
import com.simplebuilding.advancement.ModTriggers;
import com.simplebuilding.enchantment.ModEnchantments;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player-side hooks of the advancement tree:
 * <ul>
 *   <li>the counts behind the counter advancements ({@link ModCounters}), saved with the player and
 *       carried over on respawn and dimension change ({@code restoreFrom} with either flag);</li>
 *   <li>the once-a-second state checks ({@link AdvancementChecks});</li>
 *   <li>Kinetic Protection at work: damage of the {@code simplebuilding:kinetic_damage} tag that hit a
 *       player wearing the enchantment on any armor piece reports {@link ModTriggers#KINETIC_PROTECTION}.</li>
 * </ul>
 */
@Mixin(ServerPlayer.class)
public abstract class AdvancementHooksMixin implements ModCounters.Holder {

    @Unique
    private final Map<String, Long> simplebuilding$counters = new HashMap<>();

    @Override
    public Map<String, Long> simplebuilding$counters() {
        return this.simplebuilding$counters;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$advancementChecks(CallbackInfo ci) {
        AdvancementChecks.tick((ServerPlayer) (Object) this);
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void simplebuilding$kineticProtection(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !source.is(ModEnchantments.KINETIC_DAMAGE_TAG)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        var kinetic = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantments.KINETIC_PROTECTION);
        if (kinetic.isEmpty()) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (EnchantmentHelper.getItemEnchantmentLevel(kinetic.get(), player.getItemBySlot(slot)) > 0) {
                ModTriggers.feature(player, ModTriggers.KINETIC_PROTECTION);
                return;
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$writeCounters(ValueOutput output, CallbackInfo ci) {
        if (!this.simplebuilding$counters.isEmpty()) {
            output.store(ModCounters.NBT_KEY, CompoundTag.CODEC, ModCounters.write(this.simplebuilding$counters));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$readCounters(ValueInput input, CallbackInfo ci) {
        input.read(ModCounters.NBT_KEY, CompoundTag.CODEC).ifPresent(tag -> ModCounters.read(tag, this.simplebuilding$counters));
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void simplebuilding$keepCounters(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        if (oldPlayer instanceof ModCounters.Holder old) {
            this.simplebuilding$counters.clear();
            this.simplebuilding$counters.putAll(old.simplebuilding$counters());
        }
    }
}
