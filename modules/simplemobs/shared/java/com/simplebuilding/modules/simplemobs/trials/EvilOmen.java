package com.simplebuilding.modules.simplemobs.trials;

import com.simplebuilding.modules.simplemobs.CombatEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * The Evil / Void Omen at work (BS27-3/5/6): a player with the omen walking into a Weathered Trial Spawner makes it an Evil / Void
 * Trial (the omen is used up, {@code kind} and {@code level} of the block state tell which), its mobs get life and effects
 * ({@link #buff}), its reward is the key of that stage plus the good extras ({@link #rewardTable}); a player with the omen in a
 * village starts a Raid of that stage whose raiders get the same ({@link #serverTick}).
 *
 * <p>Concept 3.6, as built: life +10 % per stage (Void +60...100 %), the rest are effects; Void adds one effect step. The MOB27-1 effects
 * (Volatile, Deflection, Mirror) come with stage 4 / 5. Not built: the Wither hit chance of stage 2, the arrow slowing of stage 3, the
 * Void darkness aura and teleporting archers, the extra wave and the raider chest of the Evil Raid (queue note).
 */
public final class EvilOmen {
    /** How long raiders near the player count as the Evil Raid (30 minutes). */
    public static final long RAID_TICKS = 36000L;
    private static final Map<UUID, long[]> RAIDS = new HashMap<>();   // player -> {kind ordinal, stage, ends at game time}

    private EvilOmen() {}

    // --- the Evil Trial
    /** Called every scan of a Weathered Spawner (mixin on the Vanilla player detection): a player with the omen turns it. */
    public static void tryTrigger(ServerLevel level, BlockPos pos, TrialSpawner spawner) {
        if (level.getGameTime() % 20 != 0 || !Trials.evilOn() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        turn(level, pos, spawner);
    }

    /** Turns the spawner at {@code pos} evil when a player in range has the omen; {@code false} when nothing happened. */
    public static boolean turn(ServerLevel level, BlockPos pos, TrialSpawner spawner) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WeatheredTrialSpawnerBlock) || spawner.isOminous()) return false;
        double range = spawner.getRequiredPlayerRange();
        ServerPlayer best = null;
        TrialKind bestKind = null;
        int bestStage = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > range * range) continue;
            for (TrialKind kind : new TrialKind[] {TrialKind.VOID, TrialKind.EVIL}) {
                int stage = Trials.omenStage(p, kind);
                if (stage > 0 && (kind.ordinal() > (bestKind == null ? -1 : bestKind.ordinal()) || (kind == bestKind && stage > bestStage))) {
                    best = p; bestKind = kind; bestStage = stage;
                }
            }
        }
        if (best == null) return false;
        best.removeEffect(Trials.omen(bestKind));
        spawner.applyOminous(level, pos);
        BlockState now = level.getBlockState(pos);
        if (now.getBlock() instanceof WeatheredTrialSpawnerBlock) {
            level.setBlock(pos, now.setValue(WeatheredTrialSpawnerBlock.KIND, bestKind).setValue(WeatheredTrialSpawnerBlock.LEVEL, bestStage), 3);
        }
        best.sendOverlayMessage(Component.translatable("simplemobs.trials.turned." + bestKind.id(), bestStage));
        return true;
    }

    /** The spawner stops being ominous: back to a plain Weathered Trial. */
    public static void reset(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WeatheredTrialSpawnerBlock && WeatheredTrialSpawnerBlock.kind(state) != TrialKind.WEATHERED) {
            level.setBlock(pos, state.setValue(WeatheredTrialSpawnerBlock.KIND, TrialKind.WEATHERED).setValue(WeatheredTrialSpawnerBlock.LEVEL, 0), 3);
        }
    }

    /** The loot table an Evil / Void spawner ejects instead of the config's (key of the stage + extras), {@code null} for a plain one. */
    public static @Nullable ResourceKey<LootTable> rewardTable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        TrialKind kind = WeatheredTrialSpawnerBlock.kind(state);
        int stage = WeatheredTrialSpawnerBlock.level(state);
        return kind == TrialKind.WEATHERED || stage <= 0 ? null : Trials.spawnerLoot(kind, stage);
    }

    /** Red (Evil) / violet (Void) particles over a spawner that is an Evil / Void Trial. */
    public static void particles(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 5 != 0) return;
        BlockState state = level.getBlockState(pos);
        TrialKind kind = WeatheredTrialSpawnerBlock.kind(state);
        if (kind == TrialKind.WEATHERED) return;
        level.sendParticles(kind == TrialKind.VOID ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.CRIMSON_SPORE,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.4, 0.3, 0.4, 0.02);
    }

    /** A mob a spawner just spawned: buffed when the spawner is an Evil / Void Trial. */
    public static void onSpawned(ServerLevel level, BlockPos pos, @Nullable Entity entity) {
        BlockState state = level.getBlockState(pos);
        TrialKind kind = WeatheredTrialSpawnerBlock.kind(state);
        if (kind != TrialKind.WEATHERED && entity instanceof LivingEntity mob) buff(mob, kind, WeatheredTrialSpawnerBlock.level(state), level.getRandom().nextFloat());
    }

    // --- the buff
    /** Max life bonus as a fraction of the base: Evil 10 % per stage, Void 50 % + 10 % per stage. */
    public static double lifeBonus(TrialKind kind, int stage) {
        return (kind == TrialKind.VOID ? 0.5 : 0.0) + 0.1 * Math.max(0, Math.min(Trials.STAGES, stage));
    }

    /** One effect of a stage: {@code holder} at {@code amplifier} (0 = level I). */
    public record Buff(Holder<MobEffect> effect, int amplifier) {}

    /** The permanent effects of a stage (concept 3.6); Void adds one level to each. {@code roll} (0..1) picks the stage 4 / 5 special. */
    public static List<Buff> buffs(TrialKind kind, int stage, float roll) {
        int up = kind == TrialKind.VOID ? 1 : 0;
        List<Buff> out = new ArrayList<>();
        int strength = stage >= 5 ? 2 : stage >= 3 ? 1 : 0;
        out.add(new Buff(MobEffects.STRENGTH, strength + up));
        if (stage >= 2) out.add(new Buff(MobEffects.SPEED, (stage >= 4 ? 1 : 0) + up));
        if (stage >= 3) out.add(new Buff(MobEffects.RESISTANCE, (stage >= 5 ? 1 : 0) + up));
        if (stage >= 4) out.add(new Buff(MobEffects.REGENERATION, (stage >= 5 ? 1 : 0) + up));
        if (stage == 4 && roll < 0.25f) out.add(new Buff(CombatEffects.volatileEffect(), 0));
        if (stage >= 5) out.add(new Buff(roll < 0.5f ? CombatEffects.deflection() : CombatEffects.mirror(), 0));
        return out;
    }

    /** Gives the mob its life bonus (full health) and the stage's effects, for good. */
    public static void buff(LivingEntity mob, TrialKind kind, int stage, float roll) {
        if (stage <= 0 || kind == TrialKind.WEATHERED) return;
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(Trials.id("evil_life"), lifeBonus(kind, stage), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            mob.setHealth(mob.getMaxHealth());
        }
        for (Buff b : buffs(kind, stage, roll)) {
            mob.addEffect(new MobEffectInstance(b.effect(), MobEffectInstance.INFINITE_DURATION, b.amplifier(), true, false));
        }
    }

    // --- the Evil Raid
    /** Every 2 s: a player with the omen inside a village gets the Vanilla Raid Omen of that stage and the raiders become Evil Raiders. */
    public static void serverTick(MinecraftServer server) {
        if (!Trials.evilOn() || server.getTickCount() % 40 != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) continue;
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || !level.isVillage(p.blockPosition())) continue;
                for (TrialKind kind : new TrialKind[] {TrialKind.VOID, TrialKind.EVIL}) {
                    int stage = Trials.omenStage(p, kind);
                    if (stage > 0) {
                        startRaid(level, p, kind, stage);
                        break;
                    }
                }
            }
        }
        long now = server.overworld().getGameTime();
        RAIDS.values().removeIf(r -> r[2] < now);
    }

    public static void startRaid(ServerLevel level, ServerPlayer player, TrialKind kind, int stage) {
        player.removeEffect(Trials.omen(kind));
        player.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 200, Math.min(4, stage - 1), false, true, true));
        RAIDS.put(player.getUUID(), new long[] {kind.ordinal(), stage, level.getGameTime() + RAID_TICKS});
        player.sendOverlayMessage(Component.translatable("simplemobs.trials.raid." + kind.id(), stage));
    }

    /** A raider that joined the world near a player whose Evil Raid is running. */
    public static void onRaiderLoaded(Entity entity, ServerLevel level) {
        if (!(entity instanceof Raider raider) || RAIDS.isEmpty() || !Trials.evilOn()) return;
        for (Map.Entry<UUID, long[]> e : RAIDS.entrySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || p.level() != level || e.getValue()[2] < level.getGameTime() || p.distanceToSqr(raider) > 160.0 * 160.0) continue;
            buff(raider, TrialKind.values()[(int) e.getValue()[0]], (int) e.getValue()[1], level.getRandom().nextFloat());
            return;
        }
    }

    /** Test hook: forget running Evil Raids. */
    public static void clearRaids() {
        RAIDS.clear();
    }

    /** Test hook: mark a player as being in an Evil Raid of {@code stage}. */
    public static void markRaid(UUID player, TrialKind kind, int stage, long endsAt) {
        RAIDS.put(player, new long[] {kind.ordinal(), stage, endsAt});
    }
}
