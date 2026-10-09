package com.simplebuilding.mixin;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.dummy.ArmorStandSwap;
import com.simplebuilding.dummy.PartialArmorStand;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ruestungsstaender (Besitzer 2026-10-08, docs/ai/PLAN-STAENDER-2026-10-09.md), gilt auch fuer Stroh-Staender,
 * Trainingspuppe und die mittleren/kleinen Staender:
 * <ul>
 *   <li>Arme: ein neu erzeugter Staender hat Arme ({@code server.features.armorStandArms}); geladene und gerufene
 *       Staender lesen danach ihren NBT-Wert {@code ShowArms} (Standard Vanilla: ohne).</li>
 *   <li>Ruestung tauschen: {@link ArmorStandSwap}.</li>
 *   <li>Abbauen: mittlere/kleine Staender werfen ihr eigenes Item statt des Vanilla-Ruestungsstaenders.</li>
 * </ul>
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandStandsMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void simplebuilding$armsByDefault(EntityType<? extends ArmorStand> type, Level level, CallbackInfo ci) {
        if (!level.isClientSide() && ServerTuning.get().features.armorStandArms) {
            ((ArmorStand) (Object) this).setShowArms(true);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$swapArmor(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        ArmorStand self = (ArmorStand) (Object) this;
        if (!ArmorStandSwap.wants(self, player, hand)) {
            return;
        }
        if (self.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        ArmorStandSwap.swap(self, player);
        cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
    }

    @ModifyArg(method = "brokenByPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"),
            index = 2)
    private ItemStack simplebuilding$ownItem(ItemStack stack) {
        if ((Object) this instanceof PartialArmorStand partial && partial.item() != null) {
            return stack.transmuteCopy(partial.item());
        }
        return stack;
    }
}
