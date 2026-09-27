package com.simplebuilding.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.tweaks.spawn.SpawnRules;
import java.util.Set;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code /tp} und {@code /execute in <dim> run tp} gehen an der Dimensionssperre vorbei (Audit
 * 2026-09-26 #51): die Sperre gilt Portalen und Gegenstaenden, nicht einem Operator, der per Befehl
 * teleportiert. Der Befehl setzt fuer die Dauer seines {@code teleportTo} eine Freigabe, die
 * {@link SpawnRules#blocksDimensionChange} liest.
 */
@Mixin(TeleportCommand.class)
public abstract class TweaksTeleportCommandMixin {

    @WrapOperation(method = "performTeleport", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"))
    private static boolean simplebuilding$commandTeleportPassesTheDimensionLock(Entity victim, ServerLevel level, double x, double y, double z,
                                                                                Set<Relative> relatives, float yRot, float xRot, boolean resetCamera,
                                                                                Operation<Boolean> original) {
        return SpawnRules.asCommandTeleport(() -> original.call(victim, level, x, y, z, relatives, yRot, xRot, resetCamera));
    }
}
