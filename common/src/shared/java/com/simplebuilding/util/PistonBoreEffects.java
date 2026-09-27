package com.simplebuilding.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
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
 * <p>Die Entfernung geht sofort als Block-Update an die Spieler in 64 Bloecken (dieselbe Reichweite
 * wie das Block-Ereignis-Paket des Kolbens), also noch vor dessen Ausfahr-Ereignis. Der Client
 * bricht selbst nie; er sieht beim Nachspielen des Ausfahrens schon Luft.
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
            return false;
        }
        boolean destroyed = server.destroyBlock(pos, drop);
        if (destroyed) {
            // Sofort an die Clients, nicht erst mit dem naechsten Chunk-Abgleich: der Kolben reiht
            // gleich sein Ausfahr-Ereignis ein, und der Client spielt es mit moveBlocks nach. Laege
            // der Block dort noch, schoebe er ihn als Geisterblock mit (audit 2026-09-26 #47).
            server.getServer().getPlayerList().broadcast(null, pos.getX(), pos.getY(), pos.getZ(),
                    64.0, server.dimension(), new ClientboundBlockUpdatePacket(server, pos));
            float pitch = 0.9F + server.getRandom().nextFloat() * 0.2F;
            server.playSound(null, pos, ModSounds.PISTON_BORE, SoundSource.BLOCKS, BORE_VOLUME, pitch);
            for (Observer observer : OBSERVERS) {
                observer.onBore(server, pos.immutable(), broken, drop);
            }
        }
        return destroyed;
    }
}
