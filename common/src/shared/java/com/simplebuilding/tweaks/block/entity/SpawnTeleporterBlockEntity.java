package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.util.PlayerScan;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksClientHooks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.LegacySpawnTeleporterBlock;
import com.simplebuilding.tweaks.block.SpawnTeleporterBlock;
import com.simplebuilding.tweaks.easter.EasterEggs;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Logik des Spawn-Teleporters (aus Simple Tweaks). Drei Stufen seit 2026-09-28, die sich nur in der
 * Wartezeit unterscheiden (50/20/5 s); Stufe III sucht zuerst den eigenen Wiedereinstiegspunkt. Keine
 * Bildschirmtexte (Besitzer 2026-09-28): die Wartezeit hoert man am steigenden Klang, einen Abbruch am
 * Verpuffen, die Ankunft an Klang und Partikeln.
 */
public class SpawnTeleporterBlockEntity extends OwnedBlockEntity implements PadSignalSource {
    /** Ticks stillstehen bis zum Sprung je Stufe I-III: 50 s, 20 s, 5 s. */
    public static final int TIER_1_TICKS = 1000;
    public static final int TIER_2_TICKS = 400;
    public static final int ENDERITE_TICKS = 100;
    /** Ab so vielen Ticks Stehen klingt ein Abbruch hoerbar aus (vorher ist es nur ein Drueberlaufen). */
    public static final int CANCEL_SOUND_AFTER = 20;

    private final Map<UUID, Integer> timeStanding = new HashMap<>();
    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
    /** Zuletzt gemeldetes Komparator-Signal (Fortschritt der Wartezeit). */
    private int signal;

    public SpawnTeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.SPAWN_TELEPORTER, pos, state);
    }

    /** Ob der Teleporter noch Wartezeiten oder Positionen von Spielern fuehrt (sonst laeuft sein Tick leer). */
    public boolean isTracking() {
        return !timeStanding.isEmpty() || !lastPositions.isEmpty();
    }

    public static int tierOf(BlockState state) {
        return state.getBlock() instanceof SpawnTeleporterBlock block ? block.getTier() : 1;
    }

    public static int requiredTicks(int tier) {
        // Config tweaks.padTuning.teleporterTier1/2/3WarmupTicks (Standard TIER_1_TICKS / TIER_2_TICKS / ENDERITE_TICKS).
        return SimpleTweaks.config().padTuning.teleporterWarmup(tier);
    }

    /** Wartezeit dieses gesetzten Teleporters: die letzte Easter-Stufe ({@link EasterEggs}) wartet nur halb so lange. */
    public static int requiredTicks(Level level, BlockPos pos, int tier) {
        return EasterEggs.isBoosted(level, pos) ? requiredTicks(tier) / 2 : requiredTicks(tier);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpawnTeleporterBlockEntity be) {
        tickPlayers(level, pos, state, be);
        if (!be.isRemoved()) {
            int before = be.signal;
            be.signal = be.progressSignal(level, pos, state);
            if (be.signal != before) {
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
        }
    }

    /** Fortschritt des Spielers, der am laengsten still steht: 0 niemand, sonst 1..15 bis zum Sprung. */
    private int progressSignal(Level level, BlockPos pos, BlockState state) {
        int longest = 0;
        for (int ticks : timeStanding.values()) {
            longest = Math.max(longest, ticks);
        }
        return PadSignalSource.fillSignal(longest, Math.max(1, requiredTicks(level, pos, tierOf(state))));
    }

    /** Komparator: Fortschritt der Wartezeit (Besitzer 2026-09-28). */
    @Override
    public int comparatorSignal() {
        return signal;
    }

    private static void tickPlayers(Level level, BlockPos pos, BlockState state, SpawnTeleporterBlockEntity be) {
        if (state.getBlock() instanceof LegacySpawnTeleporterBlock legacy) {
            legacy.migrate(level, pos, state, be);
            return;
        }
        if (!SimpleTweaks.config().pads.enableSpawnTeleporters || com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, pos)) {
            // Abgeschaltet (Config oder Redstone-Signal, Besitzer 2026-09-28): keine Wartezeit, kein Sprung.
            be.timeStanding.clear();
            be.lastPositions.clear();
            return;
        }
        AABB box = new AABB(pos).move(0, 0.5, 0).inflate(0.1, 1.5, 0.1);
        List<ServerPlayer> players = PlayerScan.playersIn(level, box, ServerPlayer.class);
        // Leerlauf (docs/PERFORMANCE.md): niemand darauf und nichts mehr zu vergessen - die beiden
        // removeIf-Durchlaeufe und die Stufen-/Easter-Abfrage darunter aendern dann nichts.
        if (players.isEmpty() && !be.isTracking()) {
            return;
        }

        be.timeStanding.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));
        be.lastPositions.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));

        int tier = tierOf(state);
        int required = requiredTicks(level, pos, tier);
        for (ServerPlayer player : players) {
            UUID id = player.getUUID();
            Vec3 current = player.position();
            Vec3 last = be.lastPositions.get(id);
            boolean moved = last != null && last.distanceToSqr(current) > 0.0001;

            if (moved) {
                int before = be.timeStanding.getOrDefault(id, 0);
                be.timeStanding.put(id, 0);
                if (before >= CANCEL_SOUND_AFTER) {
                    // Abbruch: die Ladung verpufft hoerbar (statt einer Meldung ueber der Schnellleiste).
                    level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5f, 1.4f);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            } else {
                int ticks = be.timeStanding.getOrDefault(id, 0) + 1;
                be.timeStanding.put(id, ticks);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.5, 0.3, 0.1);
                }

                // Riser: Tonhoehe und Lautstaerke steigen mit der Wartezeit.
                if (ticks % 5 == 0 && ticks < required) {
                    float progress = ticks / (float) required;
                    float pitch = 0.1f + progress * 1.2f;
                    float volume = 0.05f + progress * 0.95f;
                    if (ticks % 10 == 0) {
                        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.3f * volume, pitch);
                    }
                    level.playSound(null, pos, SoundEvents.ZOMBIE_STEP, SoundSource.BLOCKS, 0.20f * volume, pitch);
                    level.playSound(null, pos, SoundEvents.ENDERMITE_STEP, SoundSource.BLOCKS, 0.10f * volume, pitch);
                    level.playSound(null, pos, SoundEvents.SILVERFISH_STEP, SoundSource.BLOCKS, 0.02f * volume, pitch);
                }

                if (ticks >= required) {
                    be.timeStanding.put(id, 0);
                    teleport(level, player, tier);
                    // Neue Position merken, sonst zaehlt der Sprung selbst als Bewegung.
                    be.lastPositions.remove(id);
                    continue;
                }
            }
            be.lastPositions.put(id, current);
        }
    }

    /** Besitzer sieht magische Partikel ueber seinem Teleporter (Simple Tweaks: SpawnTeleporterClientMixin). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SpawnTeleporterBlockEntity be) {
        UUID local = TweaksClientHooks.localPlayer();
        if (local == null || !local.equals(be.getOwner())) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextInt(5) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.1 + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, (random.nextDouble() - 0.5) * 0.05, 0.2, (random.nextDouble() - 0.5) * 0.05);
        }
    }

    /** Springt; Stufe III zum eigenen Wiedereinstiegspunkt, sonst zum Spawn-Ziel (2 Bloecke hoeher, Sanfter Fall). */
    public static void teleport(Level level, ServerPlayer player, int tier) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);

        ServerLevel targetLevel;
        Vec3 target;
        if (tier >= SpawnTeleporterBlock.ENDERITE_TIER && player.getRespawnConfig() != null) {
            TeleportTransition transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            targetLevel = transition.newLevel();
            target = transition.position();
            player.teleport(transition);
        } else {
            BlockPos custom = customTarget();
            if (custom != null) {
                targetLevel = serverLevel.getServer().overworld();
                target = Vec3.atBottomCenterOf(custom);
            } else {
                LevelData.RespawnData respawn = serverLevel.getServer().getRespawnData();
                ServerLevel respawnLevel = serverLevel.getServer().getLevel(respawn.dimension());
                targetLevel = respawnLevel != null ? respawnLevel : serverLevel.getServer().overworld();
                target = Vec3.atBottomCenterOf(respawn.pos());
            }
            target = target.add(0, 2.0, 0);
            player.teleportTo(targetLevel, target.x, target.y, target.z, Set.of(), 0f, 0f, false);
        }
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.SPAWN_TELEPORT);
        com.simplebuilding.stats.ModStats.award(player, com.simplebuilding.stats.ModStats.TELEPORTS);

        double tx = target.x;
        double ty = target.y;
        double tz = target.z;
        targetLevel.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 50, 0.2, 0.1, 0.2, 0.05);
        targetLevel.sendParticles(ParticleTypes.SOUL, tx, ty - 1, tz, 40, 0.5, 1, 0.5, 0.1);
        targetLevel.sendParticles(ParticleTypes.SCULK_SOUL, tx, ty, tz, 30, 0.2, 0.1, 0.2, 0.05);
        targetLevel.sendParticles(ParticleTypes.PORTAL, tx, ty, tz, 20, 0.2, 0.1, 0.2, 0.02);
        targetLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP, tx, ty, tz, 15, 0.2, 0.1, 0.2, 0.05);
        targetLevel.sendParticles(ParticleTypes.EXPLOSION, tx, ty - 2, tz + 1.5, 10, 0.1, 0.1, 0.2, 0.2);

        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));

        BlockPos targetPos = BlockPos.containing(target);
        targetLevel.playSound(null, targetPos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
        targetLevel.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.5f);
    }

    /** Das per Befehl ({@code worldspawn setspawn1}) gesetzte Spawn-Ziel aller Stufen, sonst null (dann Weltspawn). */
    public static BlockPos customTarget() {
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        return config.spawn1Y > -999 ? new BlockPos(config.spawn1X, config.spawn1Y, config.spawn1Z) : null;
    }
}
