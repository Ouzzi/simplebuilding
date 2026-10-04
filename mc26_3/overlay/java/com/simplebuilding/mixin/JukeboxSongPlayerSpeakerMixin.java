package com.simplebuilding.mixin;

import com.simplebuilding.util.SpeakerBoost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Musik-Verstärker (2026-10-03): Vanilla schickt Start und Stopp eines Plattenspielers nur an Spieler im
 * Umkreis von 64 Bloecken. Spielt er verstaerkt oder mit einer Kette ({@link SpeakerBoost}), bekommen auch die Spieler
 * in Hoerweite irgendeines Abspielpunkts den Start; den Stopp bekommen alle bis zur groessten Reichweite, die die Config
 * zulaesst ({@link SpeakerBoost#jukeboxStopRange}; ein inzwischen abgebauter Lautsprecher darf kein Stueck weiterlaufen
 * lassen). Je Start bzw. Stopp ein Paket je Spieler.
 */
@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerSpeakerMixin {
    @Shadow
    @Final
    private BlockPos blockPos;

    @Inject(method = "play", at = @At("TAIL"))
    private void simplebuilding$startFarther(LevelAccessor level, Holder<JukeboxSong> song, CallbackInfo ci) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        float multiplier = SpeakerBoost.multiplier(server, blockPos, SpeakerBoost.Source.JUKEBOX);
        java.util.List<BlockPos> chain = SpeakerBoost.cachedChain(server, blockPos, SpeakerBoost.Source.JUKEBOX);
        if (multiplier <= 1.0F && chain.isEmpty()) {
            return;
        }
        int id = server.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(song.value());
        SpeakerBoost.sendBeyondVanilla(server, blockPos, chain, new ClientboundLevelEventPacket(LevelEvent.SOUND_PLAY_JUKEBOX_SONG, blockPos, id, false),
                SpeakerBoost.jukeboxEventRange(multiplier));
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void simplebuilding$stopFarther(LevelAccessor level, BlockState state, CallbackInfo ci) {
        if (level instanceof ServerLevel server) {
            SpeakerBoost.sendBeyondVanilla(server, blockPos, new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, blockPos, 0, false),
                    SpeakerBoost.jukeboxStopRange());
        }
    }
}
