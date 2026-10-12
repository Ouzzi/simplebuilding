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
 * Musik-Verstärker (2026-10-03, N41): Vanilla schickt Start und Stopp eines Plattenspielers nur an Spieler im Umkreis
 * von 64 Bloecken. Hat er eine Funk-Kette ({@link SpeakerBoost}), bekommen auch die Spieler in Vanilla-Hoerweite
 * (64 Bloecke) irgendeines Verstärkers den Start; den Stopp bekommen alle bis {@link SpeakerBoost#jukeboxStopRange}
 * (ein inzwischen abgebauter Verstärker darf kein Stueck weiterlaufen lassen). Je Start bzw. Stopp ein Paket je Spieler.
 * Der Plattenspieler selbst bleibt Vanilla (keine Verstaerkung, N41). Seit 2026-10-10 zeigt jeder Musik-Verstärker der
 * Kette im Takt der Plattenspieler-Noten ebenfalls eine Musiknote ({@link SpeakerBoost#spawnRelayParticles}).
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
        java.util.List<BlockPos> chain = SpeakerBoost.cachedChain(server, blockPos, SpeakerBoost.Source.JUKEBOX);
        if (chain.isEmpty()) {
            return;
        }
        int id = server.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(song.value());
        SpeakerBoost.sendBeyondVanilla(server, blockPos, chain, new ClientboundLevelEventPacket(LevelEvent.SOUND_PLAY_JUKEBOX_SONG, blockPos, id, false),
                SpeakerBoost.VANILLA_EVENT_RANGE);
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void simplebuilding$stopFarther(LevelAccessor level, BlockState state, CallbackInfo ci) {
        if (level instanceof ServerLevel server) {
            SpeakerBoost.sendBeyondVanilla(server, blockPos, new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, blockPos, 0, false),
                    SpeakerBoost.jukeboxStopRange());
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/item/JukeboxSongPlayer;spawnMusicParticles(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void simplebuilding$relayNotes(LevelAccessor level, BlockState state, CallbackInfo ci) {
        if (level instanceof ServerLevel server) {
            SpeakerBoost.spawnRelayParticles(server, blockPos);
        }
    }
}
