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
import net.minecraft.sounds.SoundEvent;
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
import org.jetbrains.annotations.Nullable;

/**
 * Logik des Spawn-Teleporters (aus Simple Tweaks). Drei Stufen seit 2026-09-28, die sich nur in der
 * Wartezeit unterscheiden (50/20/5 s). Ziel (Besitzer 2026-09-29): der eigene Spawn (Bett/Anker), sonst
 * das Spawn-Ziel; mit Redstone-Signal immer das Spawn-Ziel/der Weltspawn ({@link Destination}). Keine
 * Bildschirmtexte (Besitzer 2026-09-28): die Wartezeit hoert man am Aufbau ({@link #playWarmup}), das Ziel
 * am Klangcharakter, einen Abbruch am Verpuffen, die Ankunft an Klang und Partikeln.
 */
public class SpawnTeleporterBlockEntity extends OwnedBlockEntity implements PadSignalSource {
    /** Ticks stillstehen bis zum Sprung je Stufe I-III: 50 s, 20 s, 5 s. */
    public static final int TIER_1_TICKS = 1000;
    public static final int TIER_2_TICKS = 400;
    public static final int ENDERITE_TICKS = 100;
    /** Ab so vielen Ticks Stehen klingt ein Abbruch hoerbar aus (vorher ist es nur ein Drueberlaufen). */
    public static final int CANCEL_SOUND_AFTER = 20;

    /**
     * Wohin der Sprung geht: {@link #OWN_SPAWN} = eigenes Bett/eigener Seelenanker (ohne gueltigen eigenen
     * Spawn das Spawn-Ziel), {@link #WORLD_SPAWN} = Spawn-Ziel/Weltspawn, solange ein Redstone-Signal anliegt.
     */
    public enum Destination {
        OWN_SPAWN,
        WORLD_SPAWN
    }

    private final Map<UUID, Integer> timeStanding = new HashMap<>();
    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
    /** Zuletzt gemeldetes Komparator-Signal (Fortschritt der Wartezeit). */
    private int signal;
    private int inputSignal = -1;

    /** Discard a charge whenever the input changes, including pulses between server ticks. */
    public void resetOnSignalChange(Level level, BlockPos pos) {
        int next = level.getBestNeighborSignal(pos);
        if (inputSignal == next) {
            return;
        }
        boolean charged = timeStanding.values().stream().anyMatch(ticks -> ticks >= CANCEL_SOUND_AFTER);
        inputSignal = next;
        timeStanding.clear();
        lastPositions.clear();
        int previous = signal;
        signal = 0;
        com.simplebuilding.tweaks.block.PadBlock.setActive(level, pos, SpawnTeleporterBlock.ACTIVE, false);
        if (previous != 0) {
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
        if (charged) {
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5f, 1.4f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

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
        be.resetOnSignalChange(level, pos);
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

    /** Ziel dieses Teleporters jetzt: mit Redstone-Signal der Weltspawn, sonst der eigene Spawn. */
    public static Destination destinationAt(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos) ? Destination.WORLD_SPAWN : Destination.OWN_SPAWN;
    }

    private static void tickPlayers(Level level, BlockPos pos, BlockState state, SpawnTeleporterBlockEntity be) {
        if (state.getBlock() instanceof LegacySpawnTeleporterBlock legacy) {
            legacy.migrate(level, pos, state, be);
            return;
        }
        if (!SimpleTweaks.config().pads.enableSpawnTeleporters) {
            // Abgeschaltet (Config): keine Wartezeit, kein Sprung. Redstone schaltet nicht ab, es waehlt das Ziel.
            be.timeStanding.clear();
            be.lastPositions.clear();
            com.simplebuilding.tweaks.block.PadBlock.setActive(level, pos, SpawnTeleporterBlock.ACTIVE, false);
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
        Destination destination = destinationAt(level, pos);
        boolean world = destination == Destination.WORLD_SPAWN;
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
                    serverLevel.sendParticles(world ? ParticleTypes.END_ROD : ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1.0, player.getZ(), world ? 1 : 2, 0.3, 0.5, 0.3, world ? 0.01 : 0.1);
                }
                if (ticks < required) {
                    playWarmup(level, pos, ticks, required, destination);
                }

                if (ticks >= required) {
                    be.timeStanding.put(id, 0);
                    teleport(level, player, tier, destination);
                    // Neue Position merken, sonst zaehlt der Sprung selbst als Bewegung.
                    be.lastPositions.remove(id);
                    continue;
                }
            }
            be.lastPositions.put(id, current);
        }
        boolean charging = false;
        for (int ticks : be.timeStanding.values()) {
            charging |= ticks > 0;
        }
        if (!be.isRemoved()) {
            com.simplebuilding.tweaks.block.PadBlock.setActive(level, pos, SpawnTeleporterBlock.ACTIVE, charging);
        }
    }

    /**
     * Der hoerbare Aufbau der Wartezeit (Besitzer 2026-09-29: passend zu 50 s und 20 s, nicht nur 5 s),
     * fuer beide Ziele verschieden, damit man ohne Text weiss, wohin es geht:
     * <ul>
     *   <li><b>Einsatz</b> (Tick 10): eigener Spawn ein tiefes Seelenanker-Aufladen, Weltspawn ein
     *       Glockennachhall.</li>
     *   <li><b>Puls</b>: anfangs weit auseinander (bei 50 s alle 2 s), zum Ende immer dichter (zuletzt alle
     *       4 Ticks), Tonhoehe und Lautstaerke steigen - eigener Spawn das Knirschen der Enderfuesse,
     *       Weltspawn ein Amethyst-Glockenspiel.</li>
     *   <li><b>Grundton</b> alle 3 s bei langen Wartezeiten (ab 10 s): Seelenanker- bzw. Leuchtfeuer-Summen.</li>
     *   <li><b>Viertel</b>: bei 25/50/75 % ein Aufladen (eigener Spawn) bzw. eine Leuchtfeuer-Wahl
     *       (Weltspawn), jedes Viertel hoeher - so zaehlt man die 50 s mit.</li>
     *   <li><b>Finale</b>: die letzten gut 4 s (bei 5 s: 3 s) das Rauschen eines Netherportals.</li>
     * </ul>
     */
    public static void playWarmup(Level level, BlockPos pos, int ticks, int required, Destination destination) {
        boolean world = destination == Destination.WORLD_SPAWN;
        float progress = ticks / (float) required;
        if (ticks == 10) {
            if (world) {
                level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 0.6f, 1.4f);
            } else {
                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.45f, 0.6f);
            }
        }
        if (ticks % pulseInterval(progress, required) == 0) {
            float pitch = 0.5f + progress * 1.3f;
            float volume = 0.15f + progress * 0.85f;
            if (world) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f * volume, pitch);
            } else {
                level.playSound(null, pos, SoundEvents.ENDERMITE_STEP, SoundSource.BLOCKS, 0.25f * volume, pitch);
                level.playSound(null, pos, SoundEvents.ZOMBIE_STEP, SoundSource.BLOCKS, 0.12f * volume, pitch);
            }
        }
        if (required >= 200 && ticks % 60 == 0 && progress < 0.9f) {
            level.playSound(null, pos, world ? SoundEvents.BEACON_AMBIENT : SoundEvents.RESPAWN_ANCHOR_AMBIENT,
                    SoundSource.BLOCKS, 0.3f + 0.4f * progress, world ? 1.0f + 0.5f * progress : 0.8f + 0.5f * progress);
        }
        for (int quarter = 1; quarter <= 3; quarter++) {
            if (ticks == required * quarter / 4) {
                level.playSound(null, pos, world ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.RESPAWN_ANCHOR_CHARGE,
                        SoundSource.BLOCKS, 0.5f + 0.1f * quarter, 0.6f + 0.2f * quarter);
            }
        }
        if (ticks == required - finaleTicks(required)) {
            level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.35f, world ? 1.4f : 1.0f);
        }
    }

    /** Abstand der Pulse in Ticks: anfangs ein Fuenfundzwanzigstel der Wartezeit (4..40), zum Ende 4. */
    public static int pulseInterval(float progress, int required) {
        int widest = Math.max(4, Math.min(40, required / 25));
        float left = 1.0f - Math.max(0.0f, Math.min(1.0f, progress));
        return Math.max(4, Math.round(4 + (widest - 4) * left * left));
    }

    /** Laenge des Finales: gut 4 s (das Portalrauschen), bei kurzen Wartezeiten hoechstens drei Fuenftel davon. */
    public static int finaleTicks(int required) {
        return Math.max(1, Math.min(85, required * 3 / 5));
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

    /** Springt zum eigenen Spawn (ohne gueltigen eigenen Spawn zum Spawn-Ziel), wie ohne Redstone-Signal. */
    public static void teleport(Level level, ServerPlayer player, int tier) {
        teleport(level, player, tier, Destination.OWN_SPAWN);
    }

    /**
     * Springt: zum eigenen Spawn genau an die Aufstehstelle, sonst 2 Bloecke ueber das Spawn-Ziel (mit
     * Sanftem Fall). Jede Stufe gleich; die Stufe zaehlt nur fuer die Wartezeit.
     */
    public static void teleport(Level level, ServerPlayer player, int tier, Destination destination) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean world = destination == Destination.WORLD_SPAWN;

        ServerLevel targetLevel;
        Vec3 target;
        TeleportTransition own = world ? null : ownSpawn(player);
        if (own != null) {
            targetLevel = own.newLevel();
            target = own.position();
        } else {
            SpawnTarget spawnTarget = spawnTarget(serverLevel);
            targetLevel = spawnTarget.level();
            target = spawnTarget.position();
        }
        if (!com.simplebuilding.api.WorldPermissions.mayTeleport(player, targetLevel, target)) return;
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, world ? 1.3f : 1f);
        if (own != null) player.teleport(own);
        else player.teleportTo(targetLevel, target.x, target.y, target.z, Set.of(), 0f, 0f, false);
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.SPAWN_TELEPORT);
        com.simplebuilding.stats.ModStats.award(player, com.simplebuilding.stats.ModStats.TELEPORTS);

        double tx = target.x;
        double ty = target.y;
        double tz = target.z;
        if (world) {
            // Weltspawn: weiss-goldener Funkenregen.
            targetLevel.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 60, 0.3, 0.2, 0.3, 0.06);
            targetLevel.sendParticles(ParticleTypes.WAX_OFF, tx, ty, tz, 30, 0.5, 0.6, 0.5, 0.5);
            targetLevel.sendParticles(ParticleTypes.FIREWORK, tx, ty, tz, 20, 0.2, 0.1, 0.2, 0.08);
        } else {
            // Eigener Spawn: violette Seelen- und Portalwolke.
            targetLevel.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 50, 0.2, 0.1, 0.2, 0.05);
            targetLevel.sendParticles(ParticleTypes.SOUL, tx, ty - 1, tz, 40, 0.5, 1, 0.5, 0.1);
            targetLevel.sendParticles(ParticleTypes.SCULK_SOUL, tx, ty, tz, 30, 0.2, 0.1, 0.2, 0.05);
            targetLevel.sendParticles(ParticleTypes.PORTAL, tx, ty, tz, 20, 0.2, 0.1, 0.2, 0.02);
            targetLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP, tx, ty, tz, 15, 0.2, 0.1, 0.2, 0.05);
            targetLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, tx, ty, tz, 30, 0.4, 0.6, 0.4, 0.05);
        }

        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));

        BlockPos targetPos = BlockPos.containing(target);
        targetLevel.playSound(null, targetPos, arrivalSound(destination), SoundSource.PLAYERS, 1.0f, world ? 1.0f : 1.2f);
        targetLevel.playSound(null, targetPos, world ? SoundEvents.BEACON_ACTIVATE : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, world ? 1.0f : 1.5f, world ? 1.5f : 0.5f);
    }

    /** Ankunftsklang je Ziel: eigener Spawn das Setzen eines Seelenankers, Weltspawn die Dorfglocke. */
    public static SoundEvent arrivalSound(Destination destination) {
        return destination == Destination.WORLD_SPAWN ? SoundEvents.BELL_BLOCK : SoundEvents.RESPAWN_ANCHOR_SET_SPAWN;
    }

    /**
     * Der eigene Spawn des Spielers (Bett oder Seelenanker, die Ladung bleibt), oder null, wenn er keinen
     * hat oder der Block fehlt bzw. versperrt ist - dann gilt das Spawn-Ziel.
     */
    public static @Nullable TeleportTransition ownSpawn(ServerPlayer player) {
        if (player.getRespawnConfig() == null) {
            return null;
        }
        TeleportTransition transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        return transition.missingRespawnBlock() ? null : transition;
    }

    /** Spawn-Ziel mit Dimension; die Position schon 2 Bloecke ueber dem Boden. */
    public record SpawnTarget(ServerLevel level, Vec3 position) {
    }

    /** Das Spawn-Ziel ({@code worldspawn setspawn1}, sonst der Weltspawn), 2 Bloecke hoeher. */
    public static SpawnTarget spawnTarget(ServerLevel any) {
        BlockPos custom = customTarget();
        if (custom != null) {
            return new SpawnTarget(any.getServer().overworld(), Vec3.atBottomCenterOf(custom).add(0, 2.0, 0));
        }
        LevelData.RespawnData respawn = any.getServer().getRespawnData();
        ServerLevel respawnLevel = any.getServer().getLevel(respawn.dimension());
        return new SpawnTarget(respawnLevel != null ? respawnLevel : any.getServer().overworld(),
                Vec3.atBottomCenterOf(respawn.pos()).add(0, 2.0, 0));
    }

    /** Das per Befehl ({@code worldspawn setspawn1}) gesetzte Spawn-Ziel aller Stufen, sonst null (dann Weltspawn). */
    public static BlockPos customTarget() {
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        return config.spawn1Y > -999 ? new BlockPos(config.spawn1X, config.spawn1Y, config.spawn1Z) : null;
    }
}
