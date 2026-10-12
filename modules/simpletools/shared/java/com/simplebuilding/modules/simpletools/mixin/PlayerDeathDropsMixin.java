package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import com.simplebuilding.modules.simpletools.vault.DeathVault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the death drops of a player: Soulbound keeps its stacks, the Death Vault records the dropped entities. */
@Mixin(Player.class)
public abstract class PlayerDeathDropsMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void simpletools$beginDeath(ServerLevel level, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        Soulbound.beginDeath(self);
        DeathVault.beginDeath(self);
    }

    @Inject(method = "dropEquipment", at = @At("RETURN"))
    private void simpletools$endDeath(ServerLevel level, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        try {
            DeathVault.endDeath(self);
        } finally {
            Soulbound.endDeath(self);
        }
    }
}
