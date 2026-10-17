package com.simplebuilding.modules.simpletweaks.protect.mixin;

import com.simplebuilding.modules.simpletweaks.protect.ActionChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Action checks: the client's own break actions (START/STOP) are measured against the player's server-side reach and
 * Vanilla's own break progress. Server-side breaks ({@code destroyBlock} from hammers etc.) never pass here.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ProtectGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;
    @Shadow private BlockPos destroyPos;
    @Shadow private int destroyProgressStart;
    @Shadow private int gameTicks;
    @Shadow private boolean isDestroyingBlock;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void simpletweaks$check(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && action != ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) return;
        ActionChecks.block(player, pos, action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK ? "start break" : "stop break");
        if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK && isDestroyingBlock && pos.equals(destroyPos) && !player.getAbilities().instabuild) {
            var state = level.getBlockState(pos);
            if (!state.isAir()) ActionChecks.stop(player, pos, state.getDestroyProgress(player, level, pos), gameTicks - destroyProgressStart);
        }
    }
}
