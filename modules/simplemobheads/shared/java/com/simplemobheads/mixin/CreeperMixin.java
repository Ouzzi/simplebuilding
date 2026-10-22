package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplemobheads.ability.HeadAbilities;
import com.simplemobheads.easter.Giants;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cat head: creepers keep their distance from the wearer, as from a cat (same goal priority and distance as Vanilla's).
 * Giants (EH21): a charged creeper's explosion gives a giant its giant head instead of the normal head roll.
 */
@Mixin(Creeper.class)
public abstract class CreeperMixin extends Monster {
    protected CreeperMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void simplemobheads$avoidCatHeads(CallbackInfo ci) {
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, HeadAbilities::scaresCreepers,
                HeadAbilities.CAT_AVOID_DISTANCE, 1.0, 1.2, net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR));
    }

    @WrapOperation(method = "killedEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V"))
    private void simplemobheads$giantHead(LivingEntity victim, ServerLevel level, DamageSource source, boolean playerKilled,
                                          ResourceKey<LootTable> table, Consumer<ItemStack> drop, Operation<Void> original) {
        ItemStack head = Giants.creeperHead(victim);
        if (head != null) {
            drop.accept(head);
        } else {
            original.call(victim, level, source, playerKilled, table, drop);
        }
    }
}
