package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Backrooms ambience (BACK18, owner: "Ambiente, NICHT nervig"): no constant loop any more. Every 20-60 s (the exit level: 40-100 s)
 * one quiet sound from the level's set plays for the player alone, 6-14 blocks away in a random direction - a buzzing tube, a
 * far door, a drip, steam, lapping water. Vanilla sounds first, the module's own neon buzz and knocking in the Lobby. Volume:
 * config {@code backrooms.ambientVolume} (percent, 0 = silent) on top of the player's Ambient slider (source AMBIENT).
 */
public final class BackroomsAmbience {
    /** One sound of a set: event and its base volume (before the config). */
    public record Cue(Holder<SoundEvent> sound, float volume) {
    }

    private static final Map<UUID, Integer> NEXT = new HashMap<>();
    private static Map<BackroomsLevel, List<Cue>> sets;

    private BackroomsAmbience() {}

    private static Cue v(SoundEvent event, float volume) {
        return new Cue(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event), volume);
    }

    /** The sound set of every level (built on first use, after the registries). */
    public static Map<BackroomsLevel, List<Cue>> sets() {
        if (sets == null) {
            Map<BackroomsLevel, List<Cue>> m = new EnumMap<>(BackroomsLevel.class);
            m.put(BackroomsLevel.LOBBY, List.of(v(LayersRegistry.NEON_BUZZ, 0.35F), v(LayersRegistry.BACKROOMS_ADDITIONS, 0.3F),
                    v(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.2F), v(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 0.4F), v(SoundEvents.COPPER_BULB_TURN_ON, 0.2F)));
            m.put(BackroomsLevel.WAREHOUSE, List.of(v(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.3F), v(SoundEvents.VAULT_AMBIENT, 0.3F),
                    v(SoundEvents.COPPER_GRATE_STEP, 0.3F), v(SoundEvents.LEVER_CLICK, 0.2F), v(LayersRegistry.NEON_BUZZ, 0.3F)));
            m.put(BackroomsLevel.PIPES, List.of(v(SoundEvents.FIRE_EXTINGUISH, 0.15F), v(SoundEvents.LAVA_POP, 0.25F),
                    v(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 0.45F), v(SoundEvents.CONDUIT_AMBIENT_SHORT, 0.25F), v(SoundEvents.COPPER_GRATE_STEP, 0.3F)));
            m.put(BackroomsLevel.POOLS, List.of(v(SoundEvents.WATER_AMBIENT, 0.3F), v(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 0.4F),
                    v(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 0.25F), v(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.2F)));
            m.put(BackroomsLevel.END, List.of(v(SoundEvents.BEACON_AMBIENT, 0.15F), v(SoundEvents.COPPER_BULB_TURN_ON, 0.15F)));
            sets = m;
        }
        return sets;
    }

    /** Ticks until the next sound in {@code level}: 20-60 s, the quiet exit level 40-100 s. */
    public static int pause(BackroomsLevel level, RandomSource random) {
        return level == BackroomsLevel.END ? 800 + random.nextInt(1200) : 400 + random.nextInt(800);
    }

    /** Effective volume of a cue: base x config percent; 0 with the config at 0. */
    public static float volume(Cue cue) {
        return cue.volume() * LayersConfig.ambientVolume / 100.0F;
    }

    public static void tick(MinecraftServer server) {
        if (!SimpleLayers.backroomsOn() || LayersConfig.ambientVolume <= 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BackroomsLevel level = SimpleLayers.levelOf(player.level());
            if (level == null) {
                NEXT.remove(player.getUUID());
                continue;
            }
            int left = NEXT.getOrDefault(player.getUUID(), pause(level, player.getRandom())) - 1;
            if (left > 0) {
                NEXT.put(player.getUUID(), left);
                continue;
            }
            NEXT.put(player.getUUID(), pause(level, player.getRandom()));
            play(player, level);
        }
    }

    static void play(ServerPlayer player, BackroomsLevel level) {
        List<Cue> set = sets().get(level);
        RandomSource random = player.getRandom();
        Cue cue = set.get(random.nextInt(set.size()));
        double angle = random.nextDouble() * Math.PI * 2.0, distance = 6.0 + random.nextDouble() * 8.0;
        double x = player.getX() + Math.cos(angle) * distance, z = player.getZ() + Math.sin(angle) * distance, y = player.getEyeY();
        player.connection.send(new ClientboundSoundPacket(cue.sound(), SoundSource.AMBIENT, x, y, z, volume(cue),
                0.85F + random.nextFloat() * 0.3F, random.nextLong()));
    }

    public static void forget(UUID player) {
        NEXT.remove(player);
    }
}
