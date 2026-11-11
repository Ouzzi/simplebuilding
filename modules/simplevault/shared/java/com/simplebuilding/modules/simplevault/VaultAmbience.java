package com.simplebuilding.modules.simplevault;

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
 * Bedrock Vault ambience (UNDER21, quiet and calm): every 30-90 s one soft Vanilla sound for the player alone, 8-16 blocks away -
 * a drip, a far crystal chime, the cave mood, a settling stone. Config {@code vault.ambientVolume} (percent, 0 = silent) on top of
 * the Ambient slider.
 */
public final class VaultAmbience {
    public record Cue(Holder<SoundEvent> sound, float volume) {
    }

    private static final Map<UUID, Integer> NEXT = new HashMap<>();
    private static List<Cue> set;

    private VaultAmbience() {}

    public static List<Cue> set() {
        if (set == null) {
            set = List.of(new Cue(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER), 0.4F),
                    new Cue(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME), 0.35F),
                    new Cue(SoundEvents.AMBIENT_CAVE, 0.25F),
                    new Cue(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.VAULT_AMBIENT), 0.25F),
                    new Cue(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.DEEPSLATE_STEP), 0.2F));
        }
        return set;
    }

    public static int pause(RandomSource random) {
        return 600 + random.nextInt(1200);
    }

    public static float volume(Cue cue) {
        return cue.volume() * VaultConfig.ambientVolume / 100.0F;
    }

    public static void tick(MinecraftServer server) {
        if (!VaultRegistry.on() || VaultConfig.ambientVolume <= 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!VaultRegistry.inVault(player.level())) {
                NEXT.remove(player.getUUID());
                continue;
            }
            int left = NEXT.getOrDefault(player.getUUID(), pause(player.getRandom())) - 1;
            if (left > 0) {
                NEXT.put(player.getUUID(), left);
                continue;
            }
            NEXT.put(player.getUUID(), pause(player.getRandom()));
            RandomSource random = player.getRandom();
            Cue cue = set().get(random.nextInt(set().size()));
            double angle = random.nextDouble() * Math.PI * 2.0, distance = 8.0 + random.nextDouble() * 8.0;
            player.connection.send(new ClientboundSoundPacket(cue.sound(), SoundSource.AMBIENT, player.getX() + Math.cos(angle) * distance, player.getEyeY(),
                    player.getZ() + Math.sin(angle) * distance, volume(cue), 0.8F + random.nextFloat() * 0.3F, random.nextLong()));
        }
    }

    public static void forget(UUID player) {
        NEXT.remove(player);
    }
}
