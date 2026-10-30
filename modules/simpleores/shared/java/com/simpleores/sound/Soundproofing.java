package com.simpleores.sound;

import com.simpleores.SimpleOres;
import com.simpleores.config.OresConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lead walls muffle world sounds (concept ORES31 section 3). The mixin {@code SoundproofMixin} hands every sound packet that goes to a
 * player to {@link #filter}: the blocks of the tag {@code simpleores:soundproofing} (Standard: the lead block) on the straight way from the
 * sound to the ear scale the volume by {@code factor ^ count}; a result below the threshold is not sent at all. Only sounds from 0.3 up are
 * looked at, the way is at most 24 cells long and unloaded chunks count as air. The cells of the sound and of the ear do not count: whoever
 * mines or walks on lead is loud.
 */
public final class Soundproofing {
    public static final TagKey<Block> BLOCKS = TagKey.create(Registries.BLOCK, SimpleOres.id("soundproofing"));
    /** Quieter sounds are never looked at. */
    public static final float MIN_VOLUME = 0.3F;
    /** Longest way (cells) that is searched for muffling blocks. */
    public static final int MAX_STEPS = 24;
    /** The client plays an explosion with this volume and pitch (ClientPacketListener#handleExplosion). */
    private static final float EXPLOSION_VOLUME = 4.0F;
    private static final float EXPLOSION_PITCH = 0.7F;
    /** Set while the replacement packets go out, so they are not muffled twice. */
    private static final ThreadLocal<Boolean> SENDING = ThreadLocal.withInitial(() -> false);

    private Soundproofing() {}

    // ---------------------------------------------------------------- pure maths

    /** Volume factor of {@code blocks} muffling blocks: {@code factor ^ blocks}. */
    public static float attenuation(int blocks, double factor) {
        return (float) Math.pow(factor, Math.max(0, blocks));
    }

    /** Whether a sound of the volume still reaches the ear after the attenuation. */
    public static boolean audible(float volume, float attenuation, double threshold) {
        return volume * attenuation >= threshold;
    }

    // ---------------------------------------------------------------- world

    /** Muffling blocks strictly between the two points (3D DDA, at most {@link #MAX_STEPS} cells). */
    public static int blocksBetween(Level level, Vec3 from, Vec3 to) {
        int x = floor(from.x), y = floor(from.y), z = floor(from.z);
        int ex = floor(to.x), ey = floor(to.y), ez = floor(to.z);
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        int sx = (int) Math.signum(dx), sy = (int) Math.signum(dy), sz = (int) Math.signum(dz);
        double tdx = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double tdy = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double tdz = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double tx = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? x + 1 - from.x : from.x - x) * tdx;
        double ty = dy == 0 ? Double.POSITIVE_INFINITY : (dy > 0 ? y + 1 - from.y : from.y - y) * tdy;
        double tz = dz == 0 ? Double.POSITIVE_INFINITY : (dz > 0 ? z + 1 - from.z : from.z - z) * tdz;
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int step = 0; step < MAX_STEPS && !(x == ex && y == ey && z == ez); step++) {
            if (tx <= ty && tx <= tz) {
                x += sx;
                tx += tdx;
            } else if (ty <= tz) {
                y += sy;
                ty += tdy;
            } else {
                z += sz;
                tz += tdz;
            }
            if (x == ex && y == ey && z == ez) break;
            pos.set(x, y, z);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BLOCKS)) count++;
        }
        return count;
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }

    /** Muffling factor of the way from a sound to the player's ear (1 = nothing in the way). */
    public static float wayFactor(ServerPlayer player, Vec3 source) {
        int blocks = blocksBetween(player.level(), source, player.getEyePosition());
        return blocks == 0 ? 1.0F : attenuation(blocks, OresConfig.soundproofFactor);
    }

    // ---------------------------------------------------------------- packets

    /** Whether the replacement packets of this thread are being sent (the mixin lets them pass). */
    public static boolean sending() {
        return SENDING.get();
    }

    public static void sending(boolean value) {
        SENDING.set(value);
    }

    /**
     * The packets to send instead of {@code packet} to the player: {@code null} = send it unchanged, an empty array = send nothing.
     */
    public static Packet<?> @Nullable [] filter(ServerPlayer player, Packet<?> packet) {
        if (!OresConfig.soundproofing) return null;
        if (packet instanceof ClientboundSoundPacket sound) {
            if (!relevant(sound.getVolume(), sound.getSource())) return null;
            float f = wayFactor(player, new Vec3(sound.getX(), sound.getY(), sound.getZ()));
            if (f >= 1.0F) return null;
            if (!audible(sound.getVolume(), f, OresConfig.soundproofThreshold)) return new Packet<?>[0];
            return new Packet<?>[] {new ClientboundSoundPacket(sound.getSound(), sound.getSource(), sound.getX(), sound.getY(), sound.getZ(),
                    sound.getVolume() * f, sound.getPitch(), sound.getSeed())};
        }
        if (packet instanceof ClientboundSoundEntityPacket sound) {
            if (!relevant(sound.getVolume(), sound.getSource())) return null;
            Entity entity = player.level().getEntity(sound.getId());
            if (entity == null) return null;
            float f = wayFactor(player, entity.position());
            if (f >= 1.0F) return null;
            if (!audible(sound.getVolume(), f, OresConfig.soundproofThreshold)) return new Packet<?>[0];
            return new Packet<?>[] {new ClientboundSoundEntityPacket(sound.getSound(), sound.getSource(), entity, sound.getVolume() * f, sound.getPitch(), sound.getSeed())};
        }
        if (packet instanceof ClientboundExplodePacket boom && boom.playSound()) {
            float f = wayFactor(player, boom.center());
            if (f >= 1.0F) return null;
            ClientboundExplodePacket silent = new ClientboundExplodePacket(boom.center(), boom.radius(), boom.blockCount(), boom.playerKnockback(),
                    boom.explosionParticle(), boom.explosionSound(), boom.blockParticles(), false);
            if (!audible(EXPLOSION_VOLUME, f, OresConfig.soundproofThreshold)) return new Packet<?>[] {silent};
            Vec3 c = boom.center();
            return new Packet<?>[] {silent, new ClientboundSoundPacket(boom.explosionSound(), SoundSource.BLOCKS, c.x, c.y, c.z, EXPLOSION_VOLUME * f, EXPLOSION_PITCH,
                    player.level().getRandom().nextLong())};
        }
        return null;
    }

    private static boolean relevant(float volume, SoundSource source) {
        return volume >= MIN_VOLUME && (OresConfig.soundproofRecords || source != SoundSource.RECORDS);
    }
}
