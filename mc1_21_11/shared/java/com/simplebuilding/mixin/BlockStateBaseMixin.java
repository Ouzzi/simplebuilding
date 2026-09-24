package com.simplebuilding.mixin;

import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.SledgehammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verlangsamt den Vorschlaghammer je Block, den ein Schlag mitnimmt
 * ({@link SledgehammerUtils#miningSpeedDivisor}), und die Spitzhacke mit Strip Miner
 * ({@link MiningUtils#stripMinerSpeedDivisor}). Die beiden schliessen sich aus: ein
 * Vorschlaghammer steht nicht in {@code minecraft:pickaxes}, und nur wenn sein Teiler 1 ist,
 * wird Strip Miner ueberhaupt gefragt - es wird also nie doppelt geteilt.
 *
 * <p>Warum hier und nicht in {@code Player#getDestroySpeed}: die Blockzahl haengt an der
 * Position, und die kennt erst {@code getDestroyProgress}. Ausserdem umgehen NeoForge und Forge
 * {@code Player#getDestroySpeed(BlockState)} beim Abbau (sie rufen die Variante mit Position);
 * {@code BlockStateBase#getDestroyProgress} ist dagegen auf allen Loadern und beiden MC-Linien
 * ungepatcht und wird von Client ({@code MultiPlayerGameMode}) und Server
 * ({@code ServerPlayerGameMode}) gleichermassen gefragt - beide Seiten rechnen also dasselbe.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$slowMiningTools(Player player, BlockGetter level, BlockPos pos,
                                                CallbackInfoReturnable<Float> cir) {
        float divisor = SledgehammerUtils.miningSpeedDivisor(player, pos);
        if (divisor <= 1.0F) {
            divisor = MiningUtils.stripMinerSpeedDivisor(player, (BlockState) (Object) this);
        }
        if (divisor > 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() / divisor);
        }
    }
}
