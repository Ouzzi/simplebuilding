package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements TrimBenefitUser, ISpaceKeyTracker {

    // Konstruktor ist notwendig, da wir von LivingEntity erben
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Unique private boolean simplebuilding$trimBenefitsEnabled = true;
    @Unique private boolean simplebuilding$spacePressed = false;
    @Shadow public abstract Inventory getInventory();

    @Override public boolean simplebuilding$areTrimBenefitsEnabled() { return this.simplebuilding$trimBenefitsEnabled; }
    @Override public void simplebuilding$setTrimBenefitsEnabled(boolean enabled) { this.simplebuilding$trimBenefitsEnabled = enabled; }
    @Override public boolean simplebuilding$isSpacePressed() { return this.simplebuilding$spacePressed; }
    @Override public void simplebuilding$setSpacePressed(boolean pressed) { this.simplebuilding$spacePressed = pressed; }

    // --- TICK LOGIK ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickLogic(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // 1. Trim Effekte (Stasis, Astralit Jump Boost)
        TrimEffectUtil.tick(player);

        // 2. Nihilith Gravity (Client & Server für prediction)
        TrimEffectUtil.handleNihilithGravity(player);

        // 3. Enderite Slow Fall (Server-Side)
        if (!this.level().isClientSide()) {
            int enderiteCount = 0;

            if (isEnderite(this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET))) enderiteCount++;
            if (isEnderite(this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS))) enderiteCount++;
            if (isEnderite(this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))) enderiteCount++;
            if (isEnderite(this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD))) enderiteCount++;

            // Logik: Mindestens 2 Teile UND Spieler fällt UND Leertaste gedrückt
            if (enderiteCount >= 2 && !this.onGround() && this.getDeltaMovement().y < -0.1) {
                if (this.simplebuilding$isSpacePressed()) {
                    this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2, 0, false, false, false));
                }
            }
        }
    }

    @Unique
    private boolean isEnderite(ItemStack stack) {
        return stack.getItem() == ModItems.ENDERITE_BOOTS ||
                stack.getItem() == ModItems.ENDERITE_LEGGINGS ||
                stack.getItem() == ModItems.ENDERITE_CHESTPLATE ||
                stack.getItem() == ModItems.ENDERITE_HELMET;
    }

    // --- BESTEHENDE MIXINS ---

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void modifyMiningSpeedForStripMiner(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ItemTags.PICKAXES) || !stack.getItem().isCorrectToolForDrops(stack, state)) return;

        var registry = player.level().registryAccess();
        var enchantLookup = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var stripMinerKey = enchantLookup.get(ModEnchantments.STRIP_MINER);

        if (stripMinerKey.isEmpty()) return;

        int level = EnchantmentHelper.getItemEnchantmentLevel(stripMinerKey.get(), stack);

        if (level > 0) {
            float originalSpeed = cir.getReturnValue();
            float divisor = 1.0f;


            switch (level) {
                case 1 -> divisor = 2.0f;
                case 2 -> divisor = 3.0f;
                case 3 -> divisor = 4.0f;
            }

            cir.setReturnValue(originalSpeed / divisor);
        }
    }

    // --- HUNGER / EXHAUSTION ---
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float simplebuilding$reduceExhaustion(float exhaustion) {
        Player player = (Player) (Object) this;
        if (player.isSprinting()) {
            // Nutzt jetzt den zentralen Rechner mit Multiplikator
            float reductionPct = TrimEffectUtil.getExhaustionReduction(player);
            if (reductionPct > 0) {
                return exhaustion * (1.0f - reductionPct);
            }
        }
        return exhaustion;
    }

    // --- XP BOOST ---
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int simplebuilding$modifyXpGain(int experience) {
        Player player = (Player) (Object) this;
        if (experience <= 0) return experience;

        float multiplier = TrimEffectUtil.getXPMultiplier(player);
        if (multiplier > 1.0f) {
            return Math.round(experience * multiplier);
        }
        return experience;
    }

    // --- LUCK ---
    @Inject(method = "getLuck", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$modifyLuck(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        float bonus = TrimEffectUtil.getLuckBonus(player);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValue() + bonus);
        }
    }

    // --- MOVEMENT SPEED (Bolt / Redstone) ---
    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$modifyWalkSpeed(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        // Nur an Land anwenden, Schwimmen ist separat im LivingEntityMixin
        if (!player.isSwimming() && !player.isFallFlying()) {
            float mult = TrimEffectUtil.getLandSpeedMultiplier(player);
            if (mult > 1.0f) {
                cir.setReturnValue(cir.getReturnValue() * mult);
            }
        }
    }

}