package com.simplebuilding.client;

import com.simplebuilding.util.SpeakerBoost;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Plattenspieler-Stueck mit Musik-Verstärkern (2026-10-04, {@link SpeakerBoost}): ein einziger
 * Klang (keine Ueberlagerung, kein Phasenchaos), der jeden Tick an den Abspielpunkt springt, der dem Spieler am
 * naechsten ist - Quelle oder ein Lautsprecher der Kette. So hoert man das Stueck durch eine ganze Villa, unverzoegert.
 * Seit 2026-10-06 spielt er mit dem Faktor der Verstärker lauter als Vanilla ({@link SpeakerBoost#listenerGain},
 * {@link AmplifiedSound}); vorher war er an der Quelle genau so laut wie ohne Verstärker. Die Kette und der Faktor
 * werden alle {@link #RESCAN_TICKS} Ticks neu ermittelt (Lautsprecher gesetzt oder abgebaut, Chunks geladen).
 * Gestoppt wird wie Vanilla ueber den Plattenspieler.
 */
public final class ChainedJukeboxSound extends SimpleSoundInstance implements TickableSoundInstance, AmplifiedSound {
    /** So oft wird die Kette neu ermittelt. */
    public static final int RESCAN_TICKS = 10;

    private final ClientLevel level;
    private final BlockPos source;
    private List<Vec3> points;
    private int age;
    private final double range;
    private float multiplier;
    private float gain = 1.0F;

    public ChainedJukeboxSound(SoundEvent sound, ClientLevel level, BlockPos source, float volume) {
        super(sound.location(), SoundSource.RECORDS, volume, 1.0F, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, false);
        this.range = sound.getRange(volume);
        this.level = level;
        this.source = source.immutable();
        rescan();
        moveToListener();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public float simplebuilding$gain() {
        return gain;
    }

    @Override
    public void tick() {
        if (++age % RESCAN_TICKS == 0) {
            rescan();
        }
        moveToListener();
    }

    private void rescan() {
        points = SpeakerBoost.points(source, SpeakerBoost.chain(level, source, SpeakerBoost.Source.JUKEBOX));
        multiplier = SpeakerBoost.multiplier(level, source, SpeakerBoost.Source.JUKEBOX);
    }

    private void moveToListener() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Vec3 listener = minecraft.player.getEyePosition();
        Vec3 at = SpeakerBoost.nearest(points, listener);
        float heard = SpeakerBoost.listenerGain(at, listener, range, multiplier);
        // Vanilla clamps the instance volume to 1; the part above 1 goes through AmplifiedSound.
        volume = Math.min(heard, 1.0F);
        gain = Math.max(heard, 1.0F);
        x = at.x;
        y = at.y;
        z = at.z;
    }
}
