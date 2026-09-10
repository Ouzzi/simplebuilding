package com.simplebuilding.neoforge.clienttest.mixin;

import com.simplebuilding.clientgametest.PayloadCounter;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.SpaceKeyPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds {@link PayloadCounter} from the mod's own space key handler. Test source set only; the
 * shipped jar never sees it. See the counter for why a count is the only observation that can
 * tell "sent on change" from "sent every tick".
 */
@Mixin(ModMessageHandlers.class)
public abstract class SpaceKeyHandlerMixin {

    @Inject(method = "handleSpaceKey", at = @At("HEAD"), remap = false)
    private static void simplebuilding$countSpaceKey(SpaceKeyPayload payload, ServerPlayer player,
                                                     CallbackInfo ci) {
        PayloadCounter.spaceKeyArrived();
    }
}
