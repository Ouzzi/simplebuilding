package com.simplebuilding.tweaks.block.entity;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksClientHooks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.SpawnTeleporterBlock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

/** Logik des Spawn-Teleporters, 1:1 aus Simple Tweaks, dazu Stufe V (eigener Wiedereinstiegspunkt). */
public class SpawnTeleporterBlockEntity extends OwnedBlockEntity {
    /** Ticks stillstehen bis zum Sprung: Stufen I-IV 5 s, Enderit 3 s. */
    public static final int STANDARD_TICKS = 100;
    public static final int ENDERITE_TICKS = 60;

    private final Map<UUID, Integer> timeStanding = new HashMap<>();
    private final Map<UUID, Vec3> lastPositions = new HashMap<>();

    public SpawnTeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(TweaksBlockEntities.SPAWN_TELEPORTER, pos, state);
    }

    public static int tierOf(BlockState state) {
        return state.getBlock() instanceof SpawnTeleporterBlock block ? block.getTier() : 1;
    }

    public static int requiredTicks(int tier) {
        return tier >= SpawnTeleporterBlock.ENDERITE_TIER ? ENDERITE_TICKS : STANDARD_TICKS;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpawnTeleporterBlockEntity be) {
        if (!SimpleTweaks.config().pads.enableSpawnTeleporters) {
            be.timeStanding.clear();
            be.lastPositions.clear();
            return;
        }
        AABB box = new AABB(pos).move(0, 0.5, 0).inflate(0.1, 1.5, 0.1);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box, p -> true);

        be.timeStanding.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));
        be.lastPositions.keySet().removeIf(id -> players.stream().noneMatch(p -> p.getUUID().equals(id)));

        int tier = tierOf(state);
        int required = requiredTicks(tier);
        for (ServerPlayer player : players) {
            UUID id = player.getUUID();
            Vec3 current = player.position();
            Vec3 last = be.lastPositions.get(id);
            boolean moved = last != null && last.distanceToSqr(current) > 0.0001;

            if (moved) {
                be.timeStanding.put(id, 0);
                player.sendOverlayMessage(Component.translatable("message.simplebuilding.spawn_teleporter.cancelled").withStyle(ChatFormatting.RED));
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

                if (ticks % 20 == 0 && ticks < required) {
                    int secondsLeft = (required - ticks) / 20;
                    player.sendOverlayMessage(Component.translatable("message.simplebuilding.spawn_teleporter.countdown", secondsLeft).withStyle(ChatFormatting.BLUE));
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

    /** Springt; Stufe V zum eigenen Wiedereinstiegspunkt, sonst zum Ziel der Stufe (2 Bloecke hoeher, Sanfter Fall). */
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
            int destination = Math.min(tier, 4);
            if (tier >= SpawnTeleporterBlock.ENDERITE_TIER) {
                destination = 1;
            }
            BlockPos custom = customTarget(destination);
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
        Component message = tier >= SpawnTeleporterBlock.ENDERITE_TIER
                ? Component.translatable("message.simplebuilding.spawn_teleporter.welcome_home")
                : Component.translatable("message.simplebuilding.spawn_teleporter.welcome", tier);
        player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** Das per Befehl gesetzte Ziel von Spawn 1-4, sonst null (dann Weltspawn). */
    public static BlockPos customTarget(int destination) {
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        return switch (destination) {
            case 2 -> config.spawn2Y > -999 ? new BlockPos(config.spawn2X, config.spawn2Y, config.spawn2Z) : null;
            case 3 -> config.spawn3Y > -999 ? new BlockPos(config.spawn3X, config.spawn3Y, config.spawn3Z) : null;
            case 4 -> config.spawn4Y > -999 ? new BlockPos(config.spawn4X, config.spawn4Y, config.spawn4Z) : null;
            default -> config.spawn1Y > -999 ? new BlockPos(config.spawn1X, config.spawn1Y, config.spawn1Z) : null;
        };
    }
}
