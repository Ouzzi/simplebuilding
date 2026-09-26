package com.simplebuilding.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wie ein Kolben der Mod einen Block zerstoert: das normale Brechen des Netherit- und
 * Enderitkolbens und jeder Block, den ein bezahlter Durchbruch loescht ({@link PistonBreach}).
 *
 * <p>Auf dem Server:
 * <ul>
 *   <li>{@code Level#destroyBlock} entfernt den Block (mit oder ohne seine Beute) und sendet dabei
 *       das Weltereignis 2001 an alle Spieler in der Naehe. Das Ereignis zeigt die Bruchpartikel
 *       dieses Blocks und spielt seinen normalen Abbauklang ({@code SoundType#getBreakSound}), genau
 *       wie beim Abbauen von Hand.</li>
 *   <li>Dazu spielt der eigene Klang {@code simplebuilding:block.piston.bore} ({@link ModSounds}) an
 *       der Stelle des Blocks.</li>
 * </ul>
 *
 * <p>Auf dem Client spielt der Kolben dasselbe Block-Ereignis noch einmal ab, damit das Ausfahren
 * richtig aussieht. Dort wird der Block nur still entfernt: Partikel und Klaenge kommen schon vom
 * Server, ein zweites {@code destroyBlock} zeigte sie doppelt.
 */
public final class PistonBoreEffects {

    /** Lautstaerke des Bohrklangs; der Abbauklang des Blocks kommt dazu. */
    public static final float BORE_VOLUME = 0.7F;

    /** Wer auf dem Server mitschreibt, welche Bloecke ein Kolben der Mod zerstoert hat (Spieltests). */
    @FunctionalInterface
    public interface Observer {
        void onBore(ServerLevel level, BlockPos pos, BlockState broken, boolean dropped);
    }

    private static final List<Observer> OBSERVERS = new CopyOnWriteArrayList<>();

    private PistonBoreEffects() {
    }

    /**
     * Meldet jeden zerstoerten Block an {@code observer}, bis das zurueckgegebene Runnable laeuft.
     * Spieltests laufen parallel: ein Beobachter filtert selbst nach seiner Welt und seinem Bereich.
     */
    public static Runnable observe(Observer observer) {
        OBSERVERS.add(observer);
        return () -> OBSERVERS.remove(observer);
    }

    /**
     * Zerstoert den Block an {@code pos} mit Partikeln, Abbauklang und Bohrklang.
     *
     * @param drop ob der Block seine normale Beute fallen laesst
     * @return ob dort ein Block stand, der jetzt weg ist
     */
    public static boolean destroy(Level level, BlockPos pos, boolean drop) {
        BlockState broken = level.getBlockState(pos);
        if (broken.isAir()) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            return level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 3);
        }
        boolean destroyed = server.destroyBlock(pos, drop);
        if (destroyed) {
            float pitch = 0.9F + server.getRandom().nextFloat() * 0.2F;
            server.playSound(null, pos, ModSounds.PISTON_BORE, SoundSource.BLOCKS, BORE_VOLUME, pitch);
            for (Observer observer : OBSERVERS) {
                observer.onBore(server, pos.immutable(), broken, drop);
            }
        }
        return destroyed;
    }
}
