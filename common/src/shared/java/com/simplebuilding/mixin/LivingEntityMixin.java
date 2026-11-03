package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.simplebuilding.util.DynamicLightHandler;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import com.simplebuilding.util.OwnedLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements OwnedLightHolder, com.simplebuilding.fletching.ArrowRecovery.Holder {

    /** Pfeile von Spielern, die in diesem Wesen stecken (ArrowRecovery, 2026-10-02). */
    @Unique
    private final java.util.List<ItemStack> simplebuilding$stuckArrows = new java.util.ArrayList<>();

    @Override
    public java.util.List<ItemStack> simplebuilding$stuckArrows() {
        return this.simplebuilding$stuckArrows;
    }

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void simplebuilding$dropStuckArrows(ServerLevel level, DamageSource source, CallbackInfo ci) {
        com.simplebuilding.fletching.ArrowRecovery.dropAll((LivingEntity) (Object) this, level);
    }


    /**
     * Marks a player's death drops ({@link com.simplebuilding.util.AttractorFilter}): the attractor
     * of another player leaves them alone. Vanilla sets no thrower on them, so nothing else tells
     * them apart from any other loose item.
     */
    // createItemStackToDrop, nicht drop: 26.3 ruft es aus Inventory#dropAll direkt auf (drop hat dort
    // eine andere Signatur). Nur der Callback, keine Zielparameter.
    @Inject(method = "createItemStackToDrop", at = @At("RETURN"))
    private void simplebuilding$markDeathDrop(CallbackInfoReturnable<net.minecraft.world.entity.item.ItemEntity> cir) {
        com.simplebuilding.util.AttractorFilter.markIfDeathDrop((LivingEntity) (Object) this, cir.getReturnValue());
    }

    // Ein tick-Hook fuer Radiance (emittierende Ruestung). Die Besatz-Faehigkeiten (Rib, Amethyst, Attribute) wirken auf 26.3 in
    // Simple Mobs (ZO9 A / MOB27-3), auf 26.2 in LivingEntityTrimMixin.
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickEffects(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Licht: jeder Traeger ausser Spielern (die haben ihren eigenen Pfad im DynamicLightHandler),
        // also Ruestungsstaender und Mobs; Schimmer clientseitig an jedem Traeger.
        if (entity.level().isClientSide()) {
            DynamicLightHandler.tickGlowMotes(entity, () -> DynamicLightHandler.wornEmission(entity));
            return;
        }
        DynamicLightHandler.tickWearer(entity);
    }

    // Vom Traeger gesetzter Lichtblock (Ruestungsstaender, Mobs), mit der Entity gespeichert.
    @Unique
    private @Nullable BlockPos simplebuilding$ownedLight;

    @Override
    public @Nullable BlockPos simplebuilding$getOwnedLight() {
        return this.simplebuilding$ownedLight;
    }

    @Override
    public void simplebuilding$setOwnedLight(@Nullable BlockPos pos) {
        this.simplebuilding$ownedLight = pos;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$writeOwnedLight(ValueOutput output, CallbackInfo ci) {
        output.storeNullable("SimpleBuildingOwnedLight", BlockPos.CODEC, this.simplebuilding$ownedLight);
        if (!this.simplebuilding$stuckArrows.isEmpty()) {
            output.store(com.simplebuilding.fletching.ArrowRecovery.SAVE_KEY, ItemStack.CODEC.listOf(), this.simplebuilding$stuckArrows);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$readOwnedLight(ValueInput input, CallbackInfo ci) {
        this.simplebuilding$ownedLight = input.read("SimpleBuildingOwnedLight", BlockPos.CODEC).orElse(null);
        this.simplebuilding$stuckArrows.clear();
        input.read(com.simplebuilding.fletching.ArrowRecovery.SAVE_KEY, ItemStack.CODEC.listOf()).ifPresent(list -> list.stream()
                .limit(com.simplebuilding.config.ServerTuning.MAX_ARROWS_PER_MOB).forEach(this.simplebuilding$stuckArrows::add));
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$modifyVoidDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && (Object) this instanceof Player player) {

            int enderitePieces = 0;
            // FIX: Nutze getInventory().armor statt getArmorItems() (da getArmorItems Iterable ist, inventory Liste)
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                ItemStack stack = player.getItemBySlot(slot);
                if (isEnderiteArmor(stack.getItem())) {
                    enderitePieces++;
                }
            }

            if (enderitePieces > 0) {
                int damageInterval = 10;
                if (enderitePieces == 1) damageInterval = 20;
                if (enderitePieces == 2) damageInterval = 40;
                if (enderitePieces == 3) damageInterval = 60;
                if (enderitePieces == 4) damageInterval = 100;

                if (player.tickCount % damageInterval != 0) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Unique
    private boolean isEnderiteArmor(Item item) {
        return item == ModItems.ENDERITE_BOOTS || item == ModItems.ENDERITE_LEGGINGS
                || item == ModItems.ENDERITE_CHESTPLATE || item == ModItems.ENDERITE_HELMET;
    }
}