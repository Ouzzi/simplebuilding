package com.simplemobheads.ritual;

import com.simplemobheads.SimpleMobHeads;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The haunting after the Herobrine ritual (owner KONZ20 H2 b): five minutes, only for the player who lit the shrine. Far away
 * (40-60 blocks, on the ground, loaded chunks only) a dark figure with two white eyes stands for a few seconds and is gone as
 * soon as the player comes closer than 24 blocks or looks straight at it; now and then footsteps behind the player or a quiet
 * torch hiss. Nothing is placed in the world and no entity exists: the server only times it and sends particles and sounds
 * to that one player's connection (no packet reaches anyone else).
 */
public final class HerobrineHaunt {
    public static final int DURATION = 6000;
    public static final double FIGURE_MIN = 40.0;
    public static final double FIGURE_MAX = 60.0;
    public static final double VANISH_DISTANCE = 24.0;
    /** Looking within this angle of the figure makes it vanish (cosine of 8 degrees). */
    public static final double VANISH_LOOK = Math.cos(Math.toRadians(8.0));
    private static final ParticleOptions BODY = new DustParticleOptions(0x141414, 2.0F);

    private static final Map<UUID, State> HAUNTED = new ConcurrentHashMap<>();

    private HerobrineHaunt() {
    }

    private static final class State {
        final long end;
        long nextFigure;
        long figureUntil;
        long nextSound;
        @Nullable Vec3 figure;

        State(long now, RandomSource random) {
            end = now + DURATION;
            nextFigure = now + 200 + random.nextInt(400);
            nextSound = now + 300 + random.nextInt(600);
        }
    }

    public static void start(ServerPlayer player) {
        HAUNTED.put(player.getUUID(), new State(player.level().getGameTime(), player.getRandom()));
    }

    public static boolean haunted(ServerPlayer player) {
        return HAUNTED.containsKey(player.getUUID());
    }

    /** Where the figure stands now, or null. */
    public static @Nullable Vec3 figure(ServerPlayer player) {
        State state = HAUNTED.get(player.getUUID());
        return state == null ? null : state.figure;
    }

    public static void stop(ServerPlayer player) {
        HAUNTED.remove(player.getUUID());
    }

    /** Every server tick of a player ({@code ServerPlayerTickMixin}); a map lookup for everyone who is not haunted. */
    public static void tick(ServerPlayer player) {
        State state = HAUNTED.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || !SimpleMobHeads.on() || !SimpleMobHeads.config().hauntPhase) {
            HAUNTED.remove(player.getUUID());
            return;
        }
        long now = level.getGameTime();
        if (now >= state.end) {
            HAUNTED.remove(player.getUUID());
            return;
        }
        RandomSource random = player.getRandom();
        if (state.figure == null && now >= state.nextFigure) {
            state.figure = placeFigure(level, player, random);
            state.figureUntil = now + 100 + random.nextInt(100);
            state.nextFigure = now + 200;
        }
        if (state.figure != null) {
            if (now >= state.figureUntil || vanishes(player.getEyePosition(), player.getLookAngle(), state.figure)) {
                send(player, ParticleTypes.SMOKE, state.figure.add(0.0, 1.0, 0.0), 6, 0.2, 0.6);
                state.figure = null;
                state.nextFigure = now + 600 + random.nextInt(900);
            } else if (now % 5 == 0) {
                showFigure(player, state.figure);
            }
        }
        if (now >= state.nextSound) {
            state.nextSound = now + 400 + random.nextInt(800);
            Vec3 behind = player.position().subtract(player.getLookAngle().multiply(5.0, 0.0, 5.0));
            if (random.nextInt(3) == 0) {
                sound(player, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, player.position().add(random.nextInt(9) - 4, 1, random.nextInt(9) - 4), 0.15F, 1.6F);
            } else {
                sound(player, random.nextBoolean() ? SoundEvents.STONE_STEP : SoundEvents.GRAVEL_STEP, SoundSource.HOSTILE, behind, 0.35F, 0.9F);
            }
        }
    }

    /** The figure vanishes when the player is close or looks straight at it. */
    public static boolean vanishes(Vec3 eye, Vec3 look, Vec3 figure) {
        Vec3 eyes = figure.add(0.0, 1.6, 0.0);
        Vec3 to = eyes.subtract(eye);
        double distance = to.horizontalDistance();
        return distance < VANISH_DISTANCE || to.normalize().dot(look.normalize()) >= VANISH_LOOK;
    }

    /** A spot 40-60 blocks away on the ground in a loaded chunk, or null (no loaded ground there). */
    public static @Nullable Vec3 placeFigure(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = FIGURE_MIN + random.nextDouble() * (FIGURE_MAX - FIGURE_MIN);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, (int) player.getY(), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Vec3 spot = new Vec3(x + 0.5, y, z + 0.5);
            if (!vanishes(player.getEyePosition(), player.getLookAngle(), spot)) {
                return spot;
            }
        }
        return null;
    }

    private static void showFigure(ServerPlayer player, Vec3 figure) {
        Vec3 toPlayer = player.position().subtract(figure).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 side = new Vec3(-toPlayer.z, 0.0, toPlayer.x).scale(0.13);
        Vec3 eyes = figure.add(0.0, 1.62, 0.0).add(toPlayer.scale(0.3));
        send(player, ParticleTypes.END_ROD, eyes.add(side), 1, 0.0, 0.0);
        send(player, ParticleTypes.END_ROD, eyes.subtract(side), 1, 0.0, 0.0);
        for (double y = 0.2; y < 1.9; y += 0.4) {
            send(player, BODY, figure.add(0.0, y, 0.0), 1, 0.12, 0.0);
        }
    }

    private static void send(ServerPlayer player, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(player, particle, true, true, at.x, at.y, at.z, count, spread, spread, spread, speed);
        }
    }

    /** A sound only this player hears (straight to the connection, not through the level). */
    public static void sound(ServerPlayer player, SoundEvent sound, SoundSource source, Vec3 at, float volume, float pitch) {
        sound(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, at, volume, pitch);
    }

    public static void sound(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, Vec3 at, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(sound, source, at.x, at.y, at.z, volume, pitch, player.getRandom().nextLong()));
    }

    /** Server stop: no haunting outlives its world. */
    public static void stopAll() {
        HAUNTED.clear();
    }

    /** Tests only. */
    public static void resetForTests() {
        stopAll();
    }
}
