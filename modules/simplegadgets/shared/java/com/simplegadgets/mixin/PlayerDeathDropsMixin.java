package com.simplegadgets.mixin;

import com.simplegadgets.attractor.AttractorFilter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player's death loot drops between these two ({@link AttractorFilter}, G3; was SimpleBuilding's PlayerEntityMixin). */
@Mixin(Player.class)
public abstract class PlayerDeathDropsMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void simplegadgets$beginDeathDrops(ServerLevel level, CallbackInfo ci) {
        AttractorFilter.beginDeathDrops((Player) (Object) this);
    }

    @Inject(method = "dropEquipment", at = @At("RETURN"))
    private void simplegadgets$endDeathDrops(ServerLevel level, CallbackInfo ci) {
        AttractorFilter.endDeathDrops();
    }
}
