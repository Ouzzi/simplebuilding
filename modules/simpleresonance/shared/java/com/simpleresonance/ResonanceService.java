package com.simpleresonance;

import com.simplebuilding.framework.api.ResonanceContracts;
import com.simpleresonance.config.ResonanceConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;

/**
 * The Simple Resonance service (framework contract {@link ResonanceContracts.Service}): player value, zone level, spawn owner and
 * the level of a spawn. Server thread only. The loader entries feed it: {@link #serverStarted}, {@link #tick}, {@link #onKill},
 * {@link #onDeath}, {@link #onRespawn}, {@link #serverStopped}.
 */
public final class ResonanceService implements ResonanceContracts.Service {
    public static final ResonanceService INSTANCE = new ResonanceService();
    /** Boss points of the Vanilla bosses (KONZEPT §1.2); other bosses use {@link #award}. */
    public static final Map<String, Double> BOSS_POINTS = Map.of("minecraft:wither", 5.0, "minecraft:ender_dragon", 10.0, "minecraft:warden", 8.0);

    /** Test hook: the players the service sees instead of the server's list (null = the server's). Server thread only. */
    public static List<ServerPlayer> testPlayers;
    private MinecraftServer server;
    private ResonanceData data;
    private final Map<UUID, Live> live = new HashMap<>();

    /** Per player, not saved: movement for the active time and the AFK rule, and the value of the last second. */
    private static final class Live {
        int lastDistance = -1;
        long lastMoved;
        double w;
        long wAt = Long.MIN_VALUE / 2;
    }

    /** One candidate owner of a spawn attempt and the chance that he is it. */
    public record Candidate(UUID player, String name, double distance, double probability) {}

    private ResonanceService() {}

    /** Registers the service in the framework contract (once, at mod start). */
    public static void install() {
        ResonanceContracts.set(INSTANCE);
    }

    public void serverStarted(MinecraftServer started) {
        this.server = started;
        this.data = ResonanceData.get(started);
    }

    public void serverStopped() {
        this.server = null;
        this.data = null;
        this.live.clear();
    }

    /** Test hook: use {@code testServer}'s own data (the game test server restarts per run). */
    public ResonanceData data() {
        return this.data;
    }

    private Live live(UUID id, long now) {
        Live l = this.live.get(id);
        if (l == null) {
            l = new Live();
            l.lastMoved = now;
            this.live.put(id, l);
        }
        return l;
    }

    public void forget(UUID id) {
        this.live.remove(id);
    }

    // ------------------------------------------------------------------ measuring

    public static int totalDistance(ServerPlayer p) {
        var stats = p.getStats();
        return stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) / 100 + stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)) / 100
                + stats.getValue(Stats.CUSTOM.get(Stats.CROUCH_ONE_CM)) / 100 + stats.getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM)) / 100
                + stats.getValue(Stats.CUSTOM.get(Stats.CLIMB_ONE_CM)) / 100;
    }

    public static int damageTaken(ServerPlayer p) {
        return p.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
    }

    /** Played ticks (Vanilla {@code play_time}). */
    public static long playTime(ServerPlayer p) {
        return p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
    }

    public static double playHours(ServerPlayer p) {
        return playTime(p) / 72000.0;
    }

    private List<ServerPlayer> players() {
        return testPlayers != null ? testPlayers : this.server.getPlayerList().getPlayers();
    }

    private @Nullable ServerPlayer online(UUID id) {
        if (this.server == null) return null;
        if (testPlayers != null) {
            for (ServerPlayer p : testPlayers) if (p.getUUID().equals(id)) return p;
        }
        return this.server.getPlayerList().getPlayer(id);
    }

    /** The three factors of the old resonance for an online player: {xp, survival, combat}. */
    public double[] factors(ServerPlayer p) {
        PlayerRecord r = this.data.record(p.getUUID());
        return new double[] {
                ResonanceMath.xpFactor(p.totalExperience - r.baseXp),
                ResonanceMath.survivalFactor(totalDistance(p) - r.baseDistance, r.active - r.baseActive),
                ResonanceMath.combatFactor(r.hostile - r.baseHostile, r.passive - r.basePassive, damageTaken(p) - r.baseDamage)};
    }

    /** F of an online player. */
    public double fractionOf(ServerPlayer p) {
        double[] f = factors(p);
        return ResonanceMath.fraction(ResonanceMath.meanFactor(f[0], f[1], f[2]));
    }

    /** W of an online player, cached for one second. */
    public double valueOf(ServerPlayer p) {
        long now = this.server.getTickCount();
        Live l = live(p.getUUID(), now);
        if (now - l.wAt >= 20) {
            l.w = ResonanceMath.value(this.data.record(p.getUUID()).points, fractionOf(p));
            l.wAt = now;
        }
        return l.w;
    }

    // ------------------------------------------------------------------ contract

    @Override
    public boolean enabled() {
        return this.server != null && SimpleResonance.on();
    }

    @Override
    public double playerValue(UUID id) {
        if (this.data == null) return 0.0;
        ServerPlayer p = online(id);
        if (p != null) return valueOf(p);
        PlayerRecord r = this.data.find(id);
        return r == null ? 0.0 : ResonanceMath.value(r.points, 0.0);
    }

    @Override
    public double playerLevel(UUID id) {
        return ResonanceMath.level(playerValue(id));
    }

    @Override
    public double fraction(UUID id) {
        ServerPlayer p = online(id);
        return p == null || this.data == null ? 0.0 : fractionOf(p);
    }

    @Override
    public double trimMultiplier(UUID id) {
        ServerPlayer p = online(id);
        if (p == null || this.data == null) return ResonanceConfig.trimBenefitBaseMultiplier * 0.1;
        double[] f = factors(p);
        return ResonanceConfig.trimBenefitBaseMultiplier * ResonanceMath.meanFactor(f[0], f[1], f[2]);
    }

    /** The zone centre {x, z}: the configured one or the world spawn. */
    public double[] centre() {
        double[] fixed = ResonanceConfig.parseCenter(ResonanceConfig.center);
        if (fixed != null) return fixed;
        if (this.server == null) return new double[] {0.0, 0.0};
        var pos = this.server.getRespawnData().pos();
        return new double[] {pos.getX() + 0.5, pos.getZ() + 0.5};
    }

    @Override
    public double zoneLevel(String dimension, double x, double z) {
        double[] c = centre();
        return ResonanceMath.zone(dimension, x, z, c[0], c[1]);
    }

    static String dimensionOf(ServerPlayer p) {
        return p.level().dimension().identifier().toString();
    }

    /** Players that may own a spawn attempt: same dimension, in {@code ownerRadius}, no spectators, no long AFK; with their chances. */
    public List<Candidate> candidates(String dimension, double x, double y, double z) {
        List<Candidate> out = new ArrayList<>();
        if (this.server == null) return out;
        long now = this.server.getTickCount();
        double radius2 = ResonanceConfig.ownerRadius * ResonanceConfig.ownerRadius;
        List<ServerPlayer> near = new ArrayList<>();
        List<Double> dist = new ArrayList<>();
        for (ServerPlayer p : players()) {
            if (p.isSpectator() || !dimensionOf(p).equals(dimension)) continue;
            double d2 = p.distanceToSqr(x, y, z);
            if (d2 > radius2) continue;
            if (ResonanceConfig.ignoreAfkMinutes > 0 && now - live(p.getUUID(), now).lastMoved > ResonanceConfig.ignoreAfkMinutes * 1200L) continue;
            near.add(p);
            dist.add(Math.sqrt(d2));
        }
        double[] d = dist.stream().mapToDouble(Double::doubleValue).toArray();
        double[] prob = ResonanceMath.ownerProbabilities(d, ResonanceConfig.snapRatio, ResonanceConfig.softSnap);
        for (int i = 0; i < near.size(); i++) {
            out.add(new Candidate(near.get(i).getUUID(), near.get(i).getGameProfile().name(), d[i], prob[i]));
        }
        return out;
    }

    @Override
    public Optional<UUID> pickOwner(String dimension, double x, double y, double z, double roll) {
        List<Candidate> c = candidates(dimension, x, y, z);
        if (c.isEmpty()) return Optional.empty();
        int i = ResonanceMath.pick(c.stream().mapToDouble(Candidate::probability).toArray(), roll);
        return i < 0 ? Optional.empty() : Optional.of(c.get(i).player());
    }

    @Override
    public ResonanceContracts.Spawn sample(String dimension, double x, double y, double z, double ownerRoll, double noiseRoll) {
        if (!enabled()) return ResonanceContracts.Spawn.NONE;
        double zone = zoneLevel(dimension, x, z);
        double noiseUnit = noiseRoll * 2.0 - 1.0;
        List<Candidate> c = candidates(dimension, x, y, z);
        if (c.isEmpty()) return new ResonanceContracts.Spawn(Optional.empty(), zone, ResonanceMath.ownerlessLevel(zone, noiseUnit));
        int i = ResonanceMath.pick(c.stream().mapToDouble(Candidate::probability).toArray(), ownerRoll);
        UUID owner = c.get(i).player();
        ServerPlayer op = online(owner);
        double w = op == null ? 0.0 : valueOf(op);
        double ownerCap = op == null ? 0.0 : ResonanceMath.playerCap(ResonanceMath.level(w), playHours(op));
        return new ResonanceContracts.Spawn(Optional.of(owner), zone, ResonanceMath.spawnLevel(zone, w, ownerCap, nearbyCaps(dimension, x, y, z, owner), noiseUnit));
    }

    /** Caps of the other players standing within {@code nearbyRadius} of the spawn (AFK ones count: they stand there). */
    public double[] nearbyCaps(String dimension, double x, double y, double z, UUID owner) {
        if (!ResonanceConfig.nearbyCap || this.server == null) return new double[0];
        double radius2 = ResonanceConfig.nearbyRadius * ResonanceConfig.nearbyRadius;
        List<Double> caps = new ArrayList<>();
        for (ServerPlayer p : players()) {
            if (p.getUUID().equals(owner) || p.isSpectator() || !dimensionOf(p).equals(dimension) || p.distanceToSqr(x, y, z) > radius2) continue;
            caps.add(ResonanceMath.playerCap(ResonanceMath.level(valueOf(p)), playHours(p)));
        }
        return caps.stream().mapToDouble(Double::doubleValue).toArray();
    }

    @Override
    public void award(UUID id, String source, double points) {
        if (this.data == null || points <= 0.0) return;
        ServerPlayer p = online(id);
        if (p == null) return;
        PlayerRecord r = this.data.record(id);
        long now = playTime(p);
        Long last = r.bosses.get(source);
        if (last != null && last <= now && now - last < ResonanceConfig.bossCooldownHours * 72000.0) return;
        r.bosses.put(source, now);
        r.points += points;
        this.data.setDirty();
    }

    @Override
    public boolean isNewcomer(UUID id) {
        ServerPlayer p = online(id);
        return p != null && playHours(p) < ResonanceConfig.graceHours;
    }

    // ------------------------------------------------------------------ the loader feeds

    /** Once a second per player: active time, value points, the one-time import of the old SimpleBuilding block. */
    public void tick(MinecraftServer s) {
        if (this.data == null || s.getTickCount() % 20 != 0) return;
        long now = s.getTickCount();
        boolean changed = false;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) changed |= update(p, now);
        if (changed) this.data.setDirty();
    }

    /** One second of one player ({@code now} = server tick); true when his record changed. */
    public boolean update(ServerPlayer p, long now) {
        if (this.data == null || p.isSpectator()) return false;
        boolean changed = false;
        PlayerRecord r = this.data.record(p.getUUID());
        if (!r.legacyImported) {
            var block = LegacyImport.take(p.getUUID());
            if (block != null) LegacyImport.apply(r, block);
            r.legacyImported = true;
            changed = true;
        }
        Live l = live(p.getUUID(), now);
        int distance = totalDistance(p);
        if (l.lastDistance >= 0 && distance != l.lastDistance) l.lastMoved = now;
        l.lastDistance = distance;
        if (now - l.lastMoved <= ResonanceMath.ACTIVE_GRACE_TICKS) {
            r.active += 20;
            r.points += 20.0 / ResonanceMath.TICKS_PER_POINT;
            changed = true;
        }
        return changed;
    }

    /** A player killed {@code victim}: kill counters, kill points (farm guard per played hour) and boss points. */
    public void onKill(ServerPlayer killer, LivingEntity victim) {
        if (this.data == null) return;
        PlayerRecord r = this.data.record(killer.getUUID());
        MobCategory category = victim.getType().getCategory();
        if (category == MobCategory.MONSTER) {
            r.hostile++;
            long now = playTime(killer);
            if (now - r.hourStart >= 72000L || r.hourStart > now) {
                r.hourStart = now;
                r.hourPoints = 0.0;
            }
            double give = Math.min(ResonanceConfig.killPoints, ResonanceConfig.killPointsPerHour - r.hourPoints);
            if (give > 0.0) {
                r.points += give;
                r.hourPoints += give;
            }
        } else if (category != MobCategory.MISC) {
            r.passive++;
        }
        EntityType<?> type = victim.getType();
        String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        Double boss = BOSS_POINTS.get(id);
        if (boss != null) award(killer.getUUID(), id, boss);
        this.data.setDirty();
    }

    /** A player died: the long-term value keeps {@code deathKeep} of itself. */
    public void onDeath(ServerPlayer p) {
        if (this.data == null) return;
        PlayerRecord r = this.data.record(p.getUUID());
        r.points *= ResonanceConfig.deathKeep;
        Live l = this.live.get(p.getUUID());
        if (l != null) l.wAt = Long.MIN_VALUE / 2;
        this.data.setDirty();
    }

    /** After a respawn from death the share F starts again (baselines now), like the core's tracker. */
    public void onRespawn(ServerPlayer p, boolean alive) {
        if (this.data == null || alive) return;
        PlayerRecord r = this.data.record(p.getUUID());
        r.baseDistance = totalDistance(p);
        r.baseActive = r.active;
        r.baseXp = p.totalExperience;
        r.baseHostile = r.hostile;
        r.basePassive = r.passive;
        r.baseDamage = damageTaken(p);
        Live l = this.live.get(p.getUUID());
        if (l != null) l.wAt = Long.MIN_VALUE / 2;
        this.data.setDirty();
    }
}
