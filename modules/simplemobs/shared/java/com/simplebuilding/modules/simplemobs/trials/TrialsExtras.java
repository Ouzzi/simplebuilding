package com.simplebuilding.modules.simplemobs.trials;

import com.simplebuilding.modules.simplemobs.MobsConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The rest of the trial chain (TR31-R): the cap of active spawners per structure, the loot draw without replacement, the hit effects of Evil / Void
 * mobs, the Void darkness and teleporting archers, and the switches of the Weathered structure. All of it is opt-out by config
 * ({@link MobsConfig#trialSpawnerCap}, {@code trialLootNoRepeat}, {@code evilHitEffects}, {@code voidMobTricks}, {@code weatheredStructures},
 * {@code weatheredDecayPercent}) and touches only Weathered Trials, the Evil / Void Vaults and mobs marked by {@link EvilOmen#buff} (concept A12).
 */
public final class TrialsExtras {
    /** Entity tag of an Evil / Void mob: {@code simplemobs_evil:<kind>:<stage>}. */
    public static final String TAG = "simplemobs_evil:";
    /** How far one structure reaches for the spawner cap: the biggest room is 21 blocks wide. */
    private static final int STRUCTURE_REACH = 22, STRUCTURE_REACH_Y = 14;
    /** How long an "active" mark counts without a new tick (the spawner stopped ticking, e.g. its chunk unloaded). */
    private static final long ACTIVE_MARK_TICKS = 40L;
    /** Void aura: radius in blocks and how often it is renewed. */
    public static final double AURA_RADIUS = 6.0;
    private static final Map<ResourceKey<net.minecraft.world.level.Level>, Map<Long, Long>> ACTIVE = new HashMap<>();
    private static final Identifier ROTTABLE = Identifier.fromNamespaceAndPath(Trials.ID, "weathered_rottable");
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(Trials.ID, "weathered_trial");

    private TrialsExtras() {}

    // --- mob tags
    public static String tag(TrialKind kind, int stage) {
        return TAG + kind.id() + ":" + stage;
    }

    /** {@code {kind ordinal, stage}} of an Evil / Void mob, {@code null} for every other entity. */
    public static int @Nullable [] evilOf(Entity entity) {
        if (entity.entityTags().isEmpty()) return null;
        for (String t : entity.entityTags()) {
            if (!t.startsWith(TAG)) continue;
            String[] parts = t.substring(TAG.length()).split(":");
            if (parts.length != 2) continue;
            for (TrialKind kind : TrialKind.values()) {
                if (kind.id().equals(parts[0])) {
                    try {
                        return new int[] {kind.ordinal(), Integer.parseInt(parts[1])};
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // --- cap of active spawners per structure
    /** Called after every tick of a spawner: remembers which Weathered Spawners are active right now. */
    public static void markTick(ServerLevel level, BlockPos pos, TrialSpawner spawner) {
        if (MobsConfig.trialSpawnerCap <= 0 || !(level.getBlockState(pos).getBlock() instanceof WeatheredTrialSpawnerBlock)) return;
        Map<Long, Long> map = ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        if (spawner.getState() == TrialSpawnerState.ACTIVE) map.put(pos.asLong(), level.getGameTime());
        else map.remove(pos.asLong());
    }

    /** Whether the Weathered Spawner at {@code pos} may not wake up now: it is not active and the cap of its structure is reached. */
    public static boolean capReached(ServerLevel level, BlockPos pos, TrialSpawner spawner) {
        int cap = MobsConfig.trialSpawnerCap;
        if (cap <= 0 || spawner.getState() == TrialSpawnerState.ACTIVE || !(level.getBlockState(pos).getBlock() instanceof WeatheredTrialSpawnerBlock)) return false;
        Map<Long, Long> map = ACTIVE.get(level.dimension());
        if (map == null || map.size() < cap) return false;
        return activeNear(map, pos, level.getGameTime()) >= cap;
    }

    static int activeNear(Map<Long, Long> map, BlockPos pos, long now) {
        int n = 0;
        for (Map.Entry<Long, Long> e : map.entrySet()) {
            if (now - e.getValue() > ACTIVE_MARK_TICKS || e.getKey() == pos.asLong()) continue;
            BlockPos o = BlockPos.of(e.getKey());
            if (Math.abs(o.getX() - pos.getX()) <= STRUCTURE_REACH && Math.abs(o.getZ() - pos.getZ()) <= STRUCTURE_REACH && Math.abs(o.getY() - pos.getY()) <= STRUCTURE_REACH_Y) n++;
        }
        return n;
    }

    /** Test hook: forget the active marks. */
    public static void clearActive() {
        ACTIVE.clear();
    }

    /** Test hook: mark a spawner as active. */
    public static void markActive(ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos, long gameTime) {
        ACTIVE.computeIfAbsent(dimension, k -> new HashMap<>()).put(pos.asLong(), gameTime);
    }

    // --- loot without replacement
    /** One vault opening: rolls the table again (up to 12 times) until no item appears twice, else keeps the draw with the most different items, each item once. */
    public static List<ItemStack> drawDistinct(Supplier<List<ItemStack>> roll) {
        List<ItemStack> first = roll.get();
        List<ItemStack> best = distinct(first);
        for (int i = 0; i < 12 && best.size() < first.size(); i++) {
            List<ItemStack> next = distinct(roll.get());
            if (next.size() > best.size()) best = next;
        }
        return best;
    }

    /** Each item type only once (the first stack of it stays). */
    public static List<ItemStack> distinct(List<ItemStack> stacks) {
        Map<Item, ItemStack> seen = new LinkedHashMap<>();
        for (ItemStack s : stacks) seen.putIfAbsent(s.getItem(), s);
        return new ArrayList<>(seen.values());
    }

    // --- hit effects of Evil / Void mobs
    /** One effect a hit puts on a player. */
    public record Hit(Holder<MobEffect> effect, int ticks, int amplifier) {}

    /**
     * The effects of one hit (concept 3.6): from stage 2 a 10 % chance of Wither I for 5 s, from stage 3 an arrow slows (Slowness II, 5 s), at stage 5 every
     * hit gives Darkness for 5 s. A Void mob counts one stage higher. {@code roll} is the chance roll in [0,1).
     */
    public static List<Hit> hitEffects(TrialKind kind, int stage, boolean arrow, float roll) {
        int s = Math.min(Trials.STAGES, stage + (kind == TrialKind.VOID ? 1 : 0));
        List<Hit> out = new ArrayList<>();
        if (s >= 2 && roll < 0.10f) out.add(new Hit(MobEffects.WITHER, 100, 0));
        if (s >= 3 && arrow) out.add(new Hit(MobEffects.SLOWNESS, 100, 1));
        if (s >= 5) out.add(new Hit(MobEffects.DARKNESS, 100, 0));
        return out;
    }

    /** After a landed hit on {@code victim}: the hit effects of an Evil attacker, the teleport of a hurt Void archer. */
    public static void onHit(LivingEntity victim, ServerLevel level, DamageSource source) {
        if (!Trials.evilOn()) return;
        Entity attacker = source.getEntity();
        if (MobsConfig.evilHitEffects && victim instanceof ServerPlayer && attacker instanceof Mob) {
            int[] evil = evilOf(attacker);
            if (evil != null) {
                for (Hit h : hitEffects(TrialKind.values()[evil[0]], evil[1], source.getDirectEntity() instanceof AbstractArrow, level.getRandom().nextFloat())) {
                    victim.addEffect(new MobEffectInstance(h.effect(), h.ticks(), h.amplifier(), false, true, true));
                }
            }
        }
        if (MobsConfig.voidMobTricks && Trials.voidOn() && victim instanceof Mob mob && isArcher(mob) && level.getRandom().nextFloat() < 0.25f) {
            int[] evil = evilOf(mob);
            if (evil != null && evil[0] == TrialKind.VOID.ordinal()) teleportAway(mob, level);
        }
    }

    public static boolean isArcher(Mob mob) {
        return mob instanceof RangedAttackMob || mob instanceof CrossbowAttackMob;
    }

    /** The archer blinks to a free spot within 8 blocks (up to 8 tries). */
    public static boolean teleportAway(Mob mob, ServerLevel level) {
        for (int i = 0; i < 8; i++) {
            double x = mob.getX() + (level.getRandom().nextDouble() - 0.5) * 16.0, z = mob.getZ() + (level.getRandom().nextDouble() - 0.5) * 16.0;
            double y = mob.getY() + level.getRandom().nextInt(5) - 2;
            if (mob.randomTeleport(x, y, z, true, state -> false)) {
                level.playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8f, 1.2f);
                return true;
            }
        }
        return false;
    }

    // --- Void aura
    /** Every second: a player within 6 blocks of a Void mob is darkened for 3 s. */
    public static void aura(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0 || !MobsConfig.voidMobTricks || !Trials.voidOn()) return;
        for (ServerLevel level : server.getAllLevels()) auraLevel(level);
    }

    /** The aura of one level, without the tick gate (also the test entry). */
    public static void auraLevel(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            boolean near = !level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(AURA_RADIUS), m -> {
                int[] evil = evilOf(m);
                return evil != null && evil[0] == TrialKind.VOID.ordinal() && m.isAlive() && m.distanceToSqr(p) <= AURA_RADIUS * AURA_RADIUS;
            }).isEmpty();
            if (near) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, false));
        }
    }

    // --- structure switches
    public static boolean isWeatheredRottable(TagKey<Block> key) {
        return key.location().equals(ROTTABLE);
    }

    /** The block_rot integrity of the Weathered Trials: 1 - decay percent. */
    public static float integrity() {
        return 1.0f - MobsConfig.weatheredDecayPercent / 100.0f;
    }

    public static boolean weatheredBlocked(Holder<Structure> structure) {
        return !MobsConfig.weatheredStructures && structure.unwrapKey().map(k -> k.identifier().equals(STRUCTURE)).orElse(false);
    }

    /** The loot item of an entry for the recipe viewer. */
    public static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }
}
