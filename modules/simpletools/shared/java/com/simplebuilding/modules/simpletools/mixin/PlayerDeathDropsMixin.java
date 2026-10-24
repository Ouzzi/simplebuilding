package com.simplebuilding.modules.simpletools.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import com.simplebuilding.modules.simpletools.vault.DeathVault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Marks the death drops of a player: Soulbound keeps its stacks, the Death Vault records the dropped entities. */
@Mixin(Player.class)
public abstract class PlayerDeathDropsMixin {
    /** One wrapper instead of HEAD/RETURN injects: a throwing {@code dropEquipment} still ends the collection (no stale static state). */
    @WrapMethod(method = "dropEquipment")
    private void simpletools$wrapDeath(ServerLevel level, Operation<Void> original) {
        Player self = (Player) (Object) this;
        Soulbound.beginDeath(self);
        DeathVault.beginDeath(self);
        try {
            original.call(level);
        } finally {
            try {
                DeathVault.endDeath(self);
            } finally {
                Soulbound.endDeath(self);
            }
        }
    }
}
