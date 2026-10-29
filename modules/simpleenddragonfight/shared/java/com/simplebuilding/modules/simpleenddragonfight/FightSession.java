package com.simplebuilding.modules.simpleenddragonfight;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * State of one dragon fight (concept sections 2 and 4, owner decisions 2026-10-11): who takes part (island radius plus a
 * grace period after leaving), the health bars per player, the latched half-health state with its Dragonmite waves, and
 * the entry animation queue. The server keeps one session for the End; game tests drive their own with a test dragon.
 */
public final class FightSession {
    public static final Identifier SCALE_ID = Identifier.fromNamespaceAndPath(DragonFightRegistry.ID, "player_scale");
    public static final int SCAN_TICKS = 20;
    public static final int PAUSE_TICKS = 10;
    public static final int RING_MIN = 3, RING_MAX = 8, SPAWN_TRIES = 24;

    /** A queued entry animation: who entered and their name at that moment. */
    public record Entry(Player player, String name) {}

    private final Map<UUID, Long> lastSeen = new LinkedHashMap<>();
    private final ArrayDeque<Entry> queue = new ArrayDeque<>();
    private @Nullable UUID dragonId;
    private boolean halfReached;
    private long nextWave = Long.MAX_VALUE;
    private @Nullable Entry current;
    private int step, pause, played;
    private double target;
    private int lastWaveSize;
    private int summons;

    // ---- read access for tests and the wiki-facing messages ----
    public int counted() { return lastSeen.size(); }
    public boolean halfReached() { return halfReached; }
    public int queued() { return queue.size() + (current != null ? 1 : 0); }
    public @Nullable Entry current() { return current; }
    public double targetHealth() { return target; }
    public long nextWave() { return nextWave; }
    public int lastWaveSize() { return lastWaveSize; }
    public @Nullable UUID dragonId() { return dragonId; }
    public int summons() { return summons; }

    /** Further summonings so far (world-wide {@link DragonSummons}); applied at the next scaling. */
    public void setSummons(int summons) { this.summons = Math.max(0, summons); }

    /** The current summoning multiplier on health, damage and speed. */
    public double summonFactor() {
        return DragonScaling.summonFactor(DragonFightConfig.summonScaling, DragonFightConfig.summonMaxMultiplier, summons);
    }

    /** One server tick: scan every second, keep the scaling in step, run waves, the queue and crystal healing. */
    public void tick(ServerLevel level, @Nullable EnderDragon dragon, BlockPos center, List<? extends Player> players, long now) {
        if (dragon != null && !dragon.isAlive()) dragon = null;
        if (dragon == null) {
            if (dragonId != null) endFight();
            if (now % SCAN_TICKS == 0) scan(level, null, center, players, now);
            return;
        }
        if (!DragonFightRegistry.on()) {
            unscale(dragon);
            endFight();
            return;
        }
        if (!dragon.getUUID().equals(dragonId)) startFight(dragon);
        if (now % SCAN_TICKS == 0) scan(level, dragon, center, players, now);
        if (dying(dragon)) return;
        healFromCrystals(dragon);
        waves(level, dragon, center, now);
        animate(level, dragon);
    }

    /** Participants: present players count at once; absent ones keep counting until the grace period ran out. */
    public void scan(ServerLevel level, @Nullable EnderDragon dragon, BlockPos center, List<? extends Player> players, long now) {
        double r2 = (double) DragonFightConfig.islandRadius * DragonFightConfig.islandRadius;
        boolean fighting = dragon != null && !dying(dragon);
        List<Entry> joined = new java.util.ArrayList<>();
        for (Player p : players) {
            if (!counts(p)) continue;
            double dx = p.getX() - (center.getX() + 0.5), dz = p.getZ() - (center.getZ() + 0.5);
            if (dx * dx + dz * dz > r2 || p.getY() < level.getMinY()) continue;
            if (lastSeen.put(p.getUUID(), now) == null && fighting) joined.add(new Entry(p, p.getName().getString()));
        }
        long grace = DragonFightConfig.leaveGraceSeconds * 20L;
        lastSeen.values().removeIf(seen -> now - seen > grace);
        if (dragon == null) return;
        if (!dying(dragon)) scale(dragon, lastSeen.size());
        for (Entry e : joined) {
            Component msg = Component.translatable("message.simpleenddragonfight.joins", e.name(), (int) Math.round(target));
            for (Player p : players) if (lastSeen.containsKey(p.getUUID())) p.sendOverlayMessage(msg);
            if (DragonFightConfig.animationEnabled) enqueue(e);
        }
    }

    /** Whether a player counts at all: alive, not a spectator, creative only with the config switch. */
    public static boolean counts(Player p) {
        return p.isAlive() && !p.isSpectator() && (!p.isCreative() || DragonFightConfig.countCreative);
    }

    /** Sets the health bars for {@code n} counted players and keeps the current health share. */
    public void scale(EnderDragon dragon, int n) {
        AttributeInstance attr = dragon.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        double base = attr.getBaseValue();
        int players = DragonScaling.players(n, DragonFightConfig.maxPlayers);
        double summon = summonFactor();
        target = DragonScaling.targetHealth(DragonFightConfig.raiseBaseHealth, base, players) * summon;
        double applied = DragonScaling.appliedMax(target);
        double oldMax = dragon.getMaxHealth();
        if (Math.abs(applied - oldMax) > 0.01) {
            float health = dragon.getHealth();
            attr.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, applied - base, AttributeModifier.Operation.ADD_VALUE));
            if (health > 0) dragon.setHealth(DragonScaling.rescale(health, oldMax, dragon.getMaxHealth()));
        }
        double speed = DragonScaling.speed(DragonFightConfig.speedMultiplier, DragonFightConfig.speedPerPlayer, DragonFightConfig.speedCap, players);
        DragonFightServer.setFactors(dragon.getUUID(), DragonScaling.damageFactor(target),
                (float) Math.min(Math.max(speed, DragonFightConfig.summonSpeedCap), speed * summon), (float) summon);
    }

    /** Back to Vanilla (module or switch off): removes the modifier, keeps the health share. */
    public static void unscale(EnderDragon dragon) {
        DragonFightServer.clearFactors(dragon.getUUID());
        AttributeInstance attr = dragon.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null || !attr.hasModifier(SCALE_ID)) return;
        float health = dragon.getHealth();
        double oldMax = dragon.getMaxHealth();
        attr.removeModifier(SCALE_ID);
        if (health > 0) dragon.setHealth(DragonScaling.rescale(health, oldMax, dragon.getMaxHealth()));
    }

    private void startFight(EnderDragon dragon) {
        endFight();
        dragonId = dragon.getUUID();
    }

    /** A new fight starts clean: waves, half state and queue reset; participants stay (they are still on the island). */
    private void endFight() {
        if (dragonId != null) DragonFightServer.clearFactors(dragonId);
        dragonId = null;
        halfReached = false;
        nextWave = Long.MAX_VALUE;
        queue.clear();
        current = null;
        step = pause = played = 0;
    }

    /** Server stop: forget everything (participants hold Player objects of the stopped world). */
    void reset() {
        endFight();
        lastSeen.clear();
        summons = 0;
    }

    private static boolean dying(EnderDragon dragon) {
        return dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
    }

    /** Crystal healing grows with the bars (Vanilla: +1 every 10 ticks at 200), so crystals stay as important. */
    private void healFromCrystals(EnderDragon dragon) {
        if (!DragonFightConfig.crystalHealScaling || dragon.nearestCrystal == null || dragon.nearestCrystal.isRemoved()) return;
        if (dragon.tickCount % 10 != 0 || dragon.getHealth() >= dragon.getMaxHealth()) return;
        AttributeInstance attr = dragon.getAttribute(Attributes.MAX_HEALTH);
        double base = attr == null ? 200.0 : attr.getBaseValue();
        double extra = dragon.getMaxHealth() / base - 1.0;
        if (extra > 0) dragon.setHealth((float) Math.min(dragon.getMaxHealth(), dragon.getHealth() + extra));
    }

    // ---------------------------------------------------------------- Dragonmites

    private void waves(ServerLevel level, EnderDragon dragon, BlockPos center, long now) {
        if (!DragonFightConfig.mitesEnabled) return;
        if (!halfReached && DragonScaling.half(dragon.getHealth(), dragon.getMaxHealth())) {
            halfReached = true;
            nextWave = now;
        }
        if (halfReached && now >= nextWave) {
            lastWaveSize = spawnWave(level, dragon, center);
            nextWave = now + DragonFightConfig.waveIntervalSeconds * 20L;
        }
    }

    /** Spawns one wave on the ring around the exit portal; returns how many mites appeared. */
    public int spawnWave(ServerLevel level, EnderDragon dragon, BlockPos center) {
        int n = DragonScaling.players(lastSeen.size(), DragonFightConfig.maxPlayers);
        int alive = aliveMites(level, dragon.getUUID(), center);
        int room = DragonScaling.maxAlive(DragonFightConfig.maxAliveMites, n) - alive;
        int want = Math.min(room, DragonScaling.mitesPerWave(DragonFightConfig.miteBase, DragonFightConfig.miteExtraPerTwoPlayers, n));
        Player preferred = dragon.getLastHurtByPlayer();
        int spawned = 0;
        for (int i = 0; i < want; i++) {
            BlockPos at = spawnSpot(level, center);
            if (at == null) continue;
            Dragonmite mite = DragonFightRegistry.DRAGONMITE.create(level, EntitySpawnReason.EVENT);
            if (mite == null) continue;
            mite.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            mite.bindTo(dragon.getUUID());
            if (preferred != null && counts(preferred) && lastSeen.containsKey(preferred.getUUID())) mite.setTarget(preferred);
            level.addFreshEntity(mite);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
            spawned++;
        }
        if (spawned > 0) level.playSound(null, center, SoundEvents.ENDERMITE_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.6F);
        return spawned;
    }

    /** Living mites bound to this dragon near the portal. */
    public static int aliveMites(ServerLevel level, UUID dragon, BlockPos center) {
        AABB box = new AABB(center).inflate(DragonFightConfig.islandRadius, 128, DragonFightConfig.islandRadius);
        return level.getEntities(DragonFightRegistry.DRAGONMITE, box, m -> m.isAlive() && dragon.equals(m.boundDragon())).size();
    }

    /** A free, safe spot on the ring 3-8 blocks around the portal column, on top of the ground; null after 24 misses. */
    public static @Nullable BlockPos spawnSpot(ServerLevel level, BlockPos center) {
        var random = level.getRandom();
        for (int t = 0; t < SPAWN_TRIES; t++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = RING_MIN + random.nextDouble() * (RING_MAX - RING_MIN);
            int x = center.getX() + Mth.floor(Math.cos(angle) * radius + 0.5);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * radius + 0.5);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (y <= level.getMinY() || level.getBlockState(pos.below()).isAir() || !level.getFluidState(pos).isEmpty()
                    || !level.getBlockState(pos.below()).getFluidState().isEmpty()) continue;
            if (!level.noCollision(DragonFightRegistry.DRAGONMITE.getSpawnAABB(x + 0.5, y, z + 0.5))) continue;
            return pos;
        }
        return null;
    }

    // ---------------------------------------------------------------- entry animation (stage 1, server side only)

    /** Queues an entry animation; beyond the limit the oldest waiting entry is dropped. */
    public void enqueue(Entry entry) {
        while (queue.size() >= DragonFightConfig.maxQueued) queue.pollFirst();
        queue.addLast(entry);
    }

    private void animate(ServerLevel level, EnderDragon dragon) {
        if (current == null) {
            if (pause > 0) { pause--; return; }
            if (queue.isEmpty()) return;
            current = queue.pollFirst();
            step = 0;
        }
        Player p = current.player();
        int total = DragonFightConfig.animationTicks;
        if (p.isRemoved() || p.level() != level || !DragonFightConfig.animationEnabled) step = total;
        else playStep(level, dragon, p, step, total, played);
        if (++step >= total) {
            current = null;
            played++;
            pause = PAUSE_TICKS;
        }
    }

    /**
     * One tick of the 60-tick sequence (stretched to {@code total}): 0-10 portal ring at the newcomer, 10-35 the dragon
     * rises and a breath trail arcs to the newcomer, 35-45 growl with a growing shock ring, 45-60 sparks rain down and a
     * violet wave runs over the ground. Purely cosmetic: no stun, no phase change.
     */
    public static void playStep(ServerLevel level, EnderDragon dragon, Player p, int step, int total, int queuePosition) {
        int a = total / 6, b = total * 35 / 60, c = total * 45 / 60;
        ParticleOptions breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
        Vec3 head = new Vec3(dragon.head.getX(), dragon.head.getY() + 1.0, dragon.head.getZ());
        Vec3 feet = p.position();
        boolean sitting = dragon.getPhaseManager().getCurrentPhase().isSitting();
        if (step < a) {
            if (step == 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6F, 0.8F);
            if (step % 5 == 0) ring(level, ParticleTypes.REVERSE_PORTAL, feet.add(0, 0.2 + step * 0.08, 0), 1.5, 24);
            if (step % 5 == 0) ring(level, ParticleTypes.PORTAL, feet.add(0, 1.0, 0), 1.2, 12);
        } else if (step < b) {
            if (!sitting && step < a + 12) dragon.setDeltaMovement(dragon.getDeltaMovement().add(0.0, 0.07, 0.0));
            if ((step - a) % 5 == 0) arc(level, breath, head, feet.add(0, 1.2, 0));
        } else if (step < c) {
            if (step == b) level.playSound(null, head.x, head.y, head.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8.0F, DragonScaling.growlPitch(queuePosition));
            double radius = 8.0 * (step - b + 1) / Math.max(1, c - b);
            ring(level, breath, head, radius, 6);
            ring(level, ParticleTypes.END_ROD, head, radius, 6);
        } else {
            if (step % 3 == 0) {
                Vec3 d = feet.add(0, 1, 0).subtract(head);
                double t = (double) (step - c) / Math.max(1, total - c);
                Vec3 at = head.add(d.scale(t));
                level.sendParticles(ParticleTypes.END_ROD, true, false, at.x, at.y, at.z, 6, 0.6, 0.6, 0.6, 0.02);
            }
            if (step % 4 == 0) ring(level, ParticleTypes.WITCH, feet.add(0, 0.1, 0), 0.5 + 2.5 * (step - c) / Math.max(1, total - c), 16);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 at, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            level.sendParticles(particle, true, false, at.x + Math.cos(angle) * radius, at.y, at.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** A breath trail from the dragon's head to the newcomer, one particle per block on a raised arc (at most 64). */
    private static void arc(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to) {
        int n = Math.min(64, Math.max(2, (int) from.distanceTo(to)));
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            Vec3 at = from.lerp(to, t).add(0, Math.sin(t * Math.PI) * 4.0, 0);
            level.sendParticles(particle, true, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
