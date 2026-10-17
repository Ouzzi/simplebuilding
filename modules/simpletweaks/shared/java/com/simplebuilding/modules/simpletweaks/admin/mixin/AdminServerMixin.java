package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.Admin;
import com.simplebuilding.modules.simpletweaks.admin.maintenance.Maintenance;
import com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Ticks of the admin features, server list count/sample (vanish) and MOTD (maintenance). */
@Mixin(MinecraftServer.class)
public abstract class AdminServerMixin {
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void simpletweaks$tick(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) { Admin.tick((MinecraftServer) (Object) this); }

    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$players(CallbackInfoReturnable<ServerStatus.Players> cir) {
        cir.setReturnValue(Vanish.status((MinecraftServer) (Object) this, cir.getReturnValue()));
    }

    @Inject(method = "getMotd", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$motd(CallbackInfoReturnable<String> cir) { cir.setReturnValue(Maintenance.motd(cir.getReturnValue())); }
}
