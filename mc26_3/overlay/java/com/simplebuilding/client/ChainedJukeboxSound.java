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
 * Plattenspieler-Stueck mit Musik-Verstärkern (2026-10-04, {@link SpeakerBoost}): ein einziger Klang (keine Ueberlagerung,
 * kein Phasenchaos), der jeden Tick an den Abspielpunkt springt, der dem Spieler am naechsten ist - der Plattenspieler
 * oder ein Verstärker der Funk-Kette. So hoert man das Stueck unverzoegert auch vom Verstärker aus.
 * <p>Besitzer N41: der Plattenspieler bleibt exakt Vanilla - Lautstaerke 4, lineare Abschwaechung ueber 64 Bloecke
 * ({@code SimpleSoundInstance#forJukeboxSong}), keine Verstaerkung. Steht man dem Plattenspieler am naechsten, klingt es
 * also genau wie Vanilla; jeder Verstärker ist ein gleich lauter weiterer Abspielpunkt. Die Kette wird alle
 * {@link #RESCAN_TICKS} Ticks neu ermittelt (Verstärker gesetzt oder abgebaut, Chunks geladen). Gestoppt wird wie
 * Vanilla ueber den Plattenspieler.
 */
public final class ChainedJukeboxSound extends SimpleSoundInstance implements TickableSoundInstance {
    /** So oft wird die Kette neu ermittelt. */
    public static final int RESCAN_TICKS = 10;

    private final ClientLevel level;
    private final BlockPos source;
    private List<Vec3> points;
    private int age;

    public ChainedJukeboxSound(SoundEvent sound, ClientLevel level, BlockPos source) {
        super(sound.location(), SoundSource.RECORDS, SpeakerBoost.JUKEBOX_VOLUME, 1.0F, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.LINEAR, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, false);
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
    public void tick() {
        if (++age % RESCAN_TICKS == 0) {
            rescan();
        }
        moveToListener();
    }

    private void rescan() {
        points = SpeakerBoost.points(source, SpeakerBoost.chain(level, source, SpeakerBoost.Source.JUKEBOX));
    }

    private void moveToListener() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Vec3 at = SpeakerBoost.nearest(points, minecraft.player.getEyePosition());
        x = at.x;
        y = at.y;
        z = at.z;
    }
}
