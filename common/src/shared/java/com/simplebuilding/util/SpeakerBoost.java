package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.SpeakerBlock;
import com.simplebuilding.config.ServerTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;

/**
 * Lautsprecher (Besitzer 2026-10-03, {@code McVersion.MUSIC_DISCS}): ein Astralit-Lautsprecher direkt an einem
 * Plattenspieler, ein Nihilit-Lautsprecher direkt an einem Notenblock macht ihn lauter und weiter hoerbar - wie ein
 * Lautsprecher ohne Verzoegerung: dieselbe Wiedergabe, nur mit hoeherer Lautstaerke, kein zweiter Klang, kein Echo.
 *
 * <p><b>Was zaehlt:</b> die sechs direkten Nachbarn (wie Vanilla-Redstone-Nachbarn, billig und eindeutig), nur
 * Lautsprecher der passenden Art, hoechstens {@code server.speakers.maxSpeakers} (Standard 2, hart 3). Faktor =
 * 1 + Anzahl x {@code boostPercent} (Standard 50 %, hart 50 %), also hoechstens 2,5-fach.
 *
 * <p><b>Wie es wirkt:</b> Lautstaerken ueber 1 heben in Minecraft nicht den Pegel, sondern die Reichweite der linearen
 * Abschwaechung (16 Bloecke je Lautstaerke-Einheit): beim Spieler in derselben Entfernung ist es lauter, und man hoert
 * es weiter. Notenblock: der Server spielt den Ton mit 3,0 x Faktor ({@code NoteBlockSpeakerMixin}, bis 120 Bloecke).
 * Plattenspieler: der Client startet das Stueck mit 4,0 x Faktor ({@code LevelEventHandlerSpeakerMixin}, bis 160 Bloecke,
 * die Config kommt vom Server), und der Server schickt Start und Stopp zusaetzlich an Spieler jenseits der Vanilla-64
 * ({@code JukeboxSongPlayerSpeakerMixin}) - je Start/Stopp genau ein Paket je Spieler. Gezaehlt wird beim Start eines
 * Stuecks bzw. bei jedem Notenschlag.
 */
public final class SpeakerBoost {
    /** Welche Klangquelle ein Lautsprecher verstaerkt. */
    public enum Source {
        /** Plattenspieler (Astralit-Lautsprecher). */
        JUKEBOX,
        /** Notenblock (Nihilit-Lautsprecher). */
        NOTE_BLOCK
    }

    /** Vanillas Lautstaerke eines Plattenspieler-Stuecks ({@code SimpleSoundInstance#forJukeboxSong}). */
    public static final float JUKEBOX_VOLUME = 4.0F;
    /** Vanillas Lautstaerke eines Notenblock-Tons ({@code NoteBlock#triggerEvent}). */
    public static final float NOTE_BLOCK_VOLUME = 3.0F;
    /** Bloecke Hoerweite je Lautstaerke-Einheit (Vanilla-Standard der Klang-Definition). */
    public static final int BLOCKS_PER_VOLUME = 16;
    /** Bis hierhin schickt Vanilla Start und Stopp eines Plattenspielers ({@code ServerLevel#levelEvent}). */
    public static final double VANILLA_EVENT_RANGE = 64.0;

    private SpeakerBoost() {
    }

    /** Direkt angrenzende Lautsprecher der Art {@code source} (0 bis 6, ohne Obergrenze). */
    public static int adjacentSpeakers(BlockGetter level, BlockPos pos, Source source) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() instanceof SpeakerBlock speaker && speaker.source() == source) {
                count++;
            }
        }
        return count;
    }

    /** Faktor fuer {@code speakers} angrenzende Lautsprecher: gedeckelt durch die Server-Config. */
    public static float multiplier(int speakers) {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            return 1.0F;
        }
        return 1.0F + Math.min(Math.max(speakers, 0), ServerTuning.maxSpeakers()) * ServerTuning.speakerBoost();
    }

    /** Faktor fuer die Quelle bei {@code pos}: 1 ohne passende Lautsprecher. */
    public static float multiplier(BlockGetter level, BlockPos pos, Source source) {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            return 1.0F;
        }
        return multiplier(adjacentSpeakers(level, pos, source));
    }

    /** Hoechster Faktor, den die Server-Config gerade zulaesst. */
    public static float maxMultiplier() {
        return multiplier(ServerTuning.maxSpeakers());
    }

    /** Hoerweite in Bloecken fuer eine Lautstaerke (Vanilla: hoechstens eine Einheit zaehlt nach unten). */
    public static double reach(float volume) {
        return Math.max(volume, 1.0F) * BLOCKS_PER_VOLUME;
    }

    /** Lautstaerke des Notenblocks bei {@code pos} (Vanilla 3,0). */
    public static float noteBlockVolume(BlockGetter level, BlockPos pos) {
        return NOTE_BLOCK_VOLUME * multiplier(level, pos, Source.NOTE_BLOCK);
    }

    /** Bis hierhin muessen Start und Stopp eines Plattenspielers mit diesem Faktor reichen. */
    public static double jukeboxEventRange(float multiplier) {
        return Math.max(VANILLA_EVENT_RANGE, reach(JUKEBOX_VOLUME * multiplier));
    }

    /**
     * Schickt {@code packet} an jeden Spieler dieser Dimension, der weiter als Vanillas 64 Bloecke, aber naeher als
     * {@code range} am Block ist (die Naeheren hat Vanilla schon bedient). Liefert die Zahl der Empfaenger.
     */
    public static int sendBeyondVanilla(ServerLevel level, BlockPos pos, Packet<?> packet, double range) {
        if (range <= VANILLA_EVENT_RANGE) {
            return 0;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double near = VANILLA_EVENT_RANGE * VANILLA_EVENT_RANGE;
        double far = range * range;
        int sent = 0;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(x, y, z);
            if (distance >= near && distance < far) {
                player.connection.send(packet);
                sent++;
            }
        }
        return sent;
    }
}
