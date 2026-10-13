package com.simplemobheads.mixin;

import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cat head: creepers keep their distance from the wearer, as from a cat (same goal priority and distance as Vanilla's). */
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
}
