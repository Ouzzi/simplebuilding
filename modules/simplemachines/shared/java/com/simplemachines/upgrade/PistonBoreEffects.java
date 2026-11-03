package com.simplemachines.upgrade;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a piston upgrade destroys a block (the Netherite and Enderite breakers, every block a paid breach removes,
 * {@link PistonBreach}): {@code Level#destroyBlock} (break particles and break sound of the block, world event 2001) plus the
 * own bore sound {@code simplemachines:block.piston.bore}. The removal goes to the players within 64 blocks at once, before the
 * piston's own block event, so a client replaying the extension never pushes a ghost block (audit 2026-09-26 #47).
 */
public final class PistonBoreEffects {
    /** Volume of the bore sound; the block's break sound comes on top. */
    public static final float BORE_VOLUME = 0.7F;

    /** Records which blocks a piston upgrade destroyed (game tests). */
    @FunctionalInterface
    public interface Observer {
        void onBore(ServerLevel level, BlockPos pos, BlockState broken, boolean dropped);
    }

    private static final List<Observer> OBSERVERS = new CopyOnWriteArrayList<>();

    private PistonBoreEffects() {}

    /** Reports every destroyed block to {@code observer} until the returned runnable runs (tests run in parallel: filter yourself). */
    public static Runnable observe(Observer observer) {
        OBSERVERS.add(observer);
        return () -> OBSERVERS.remove(observer);
    }

    /**
     * Destroys the block at {@code pos} with particles, break sound and bore sound.
     *
     * @param drop whether the block drops its normal loot
     * @return whether a block stood there that is gone now
     */
    public static boolean destroy(Level level, BlockPos pos, boolean drop) {
        BlockState broken = level.getBlockState(pos);
        if (broken.isAir() || !(level instanceof ServerLevel server)) {
            return false;
        }
        boolean destroyed = server.destroyBlock(pos, drop);
        if (destroyed) {
            server.getServer().getPlayerList().broadcast(null, pos.getX(), pos.getY(), pos.getZ(),
                    64.0, server.dimension(), new ClientboundBlockUpdatePacket(server, pos));
            float pitch = 0.9F + server.getRandom().nextFloat() * 0.2F;
            server.playSound(null, pos, PistonUpgrades.BORE_SOUND, SoundSource.BLOCKS, BORE_VOLUME, pitch);
            for (Observer observer : OBSERVERS) {
                observer.onBore(server, pos.immutable(), broken, drop);
            }
        }
        return destroyed;
    }
}
