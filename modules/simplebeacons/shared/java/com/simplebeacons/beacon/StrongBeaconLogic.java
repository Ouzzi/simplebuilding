package com.simplebeacons.beacon;

import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.config.BeaconsConfig;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The rules of the Strong, Mighty, Evil and Mighty Evil beacons (concept §3, BS28b-4/5): Vanilla's pyramid scan with up to
 * six levels, the powers per level from {@link BeaconKind}, range {@code (rangeBase + rangePerLevel x levels) x material
 * factor} capped at {@link #RANGE_CAP} (REV2-A-1), duration {@code 9 + 2 x levels} seconds, players only, ambient like
 * Vanilla. Static and side free, so tests call it directly.
 */
public final class StrongBeaconLogic {
    /** Highest pyramid any kind can reach (the config may lower it to 4 or 5). */
    public static final int MAX_LEVELS = 6;
    /** Hard cap of the effect range in blocks (REV2-A-1): 50 + 20 x 6 = 170 at most, times the Netherite factor 2. */
    public static final int RANGE_CAP = 340;
    /** Highest amplifier a beacon gives (III). */
    public static final int MAX_AMPLIFIER = 2;
    /** Vanilla's own beacon powers: these (and Health Boost on Mighty beacons) can be raised by the secondary power. */
    public static final List<Holder<MobEffect>> VANILLA = List.of(MobEffects.SPEED, MobEffects.HASTE, MobEffects.RESISTANCE,
            MobEffects.JUMP_BOOST, MobEffects.STRENGTH, MobEffects.REGENERATION);
    /** Effects renewed in place instead of re-added, so their one-time buffer (hearts) is not reset every 4 seconds. */
    static final List<Holder<MobEffect>> RENEW_ONLY = List.of(MobEffects.HEALTH_BOOST);
    /**
     * Saturation feeds every tick while it lasts, so the beacon gives it as a one-tick pulse every third cycle (12 s):
     * one hunger point and two saturation, about what walking and mining use - no hunger in range, no extra healing
     * beyond eating (owner 22.10.: no Luck; plan docs/ai/PLAN-A-BEACONS22-2026-10-22.md).
     */
    public static final int SATURATION_EVERY_CYCLES = 3;
    /** Extra base blocks for modpacks (empty here); Vanilla's {@code #minecraft:beacon_base_blocks} always count. */
    public static final TagKey<Block> STRONG_BASE_BLOCKS = TagKey.create(Registries.BLOCK, SimpleBeacons.id("strong_base_blocks"));

    /** Pyramid blocks of a full pyramid with {@code levels} levels: 3x3 + 5x5 + ... (6 = 454). */
    public static int pyramidBlocks(int levels) {
        int sum = 0;
        for (int i = 1; i <= levels; i++) sum += (2 * i + 1) * (2 * i + 1);
        return sum;
    }

    public static int maxLevels() {
        return BeaconsConfig.strongBeaconMaxLevels;
    }

    /** Highest pyramid of {@code kind}: its own top level (primary levels + 1), lowered by the server config. */
    public static int maxLevels(BeaconKind kind) {
        return Math.min(kind.secondaryLevel(), maxLevels());
    }

    public static boolean isBase(BlockState state) {
        return state.is(BlockTags.BEACON_BASE_BLOCKS) || state.is(STRONG_BASE_BLOCKS);
    }

    /** Vanilla's pyramid scan with up to {@code maxLevels} levels; stops at the first incomplete level. */
    public static int scanPyramid(Level level, BlockPos beacon, int maxLevels) {
        int x = beacon.getX(), y = beacon.getY(), z = beacon.getZ();
        int levels = 0;
        for (int step = 1; step <= maxLevels; levels = step++) {
            int ly = y - step;
            if (ly < level.getMinY()) break;
            boolean ok = true;
            for (int lx = x - step; lx <= x + step && ok; lx++) {
                for (int lz = z - step; lz <= z + step; lz++) {
                    if (!isBase(level.getBlockState(new BlockPos(lx, ly, lz)))) {
                        ok = false;
                        break;
                    }
                }
            }
            if (!ok) break;
        }
        return levels;
    }

    /** Effect range in blocks before the material factor (horizontal box, full height like Vanilla). */
    public static int range(int levels) {
        return BeaconsConfig.strongBeaconRangeBase + BeaconsConfig.strongBeaconRangePerLevel * levels;
    }

    /** Effect range with the material factor in percent ({@link RangeFactor}). */
    public static double range(int levels, int percent) {
        return Math.min(RANGE_CAP, RangeFactor.apply(range(levels), percent));
    }

    /** Whether the beacon cycle at {@code gameTime} (every 80 ticks) gives the Saturation pulse. */
    public static boolean saturationDue(long gameTime) {
        return gameTime / 80 % SATURATION_EVERY_CYCLES == 0;
    }

    /** Effect duration in ticks (Vanilla formula). */
    public static int durationTicks(int levels) {
        return (9 + levels * 2) * 20;
    }

    /** Whether the server lets players choose {@code effect} (the extra effects can be switched off; Vanilla's always). */
    public static boolean effectOn(Holder<MobEffect> effect) {
        return BeaconsConfig.effectOn(path(effect));
    }

    /** The primary level the power ({@code effect}, {@code amplifier}) needs on {@code kind}, {@link Integer#MAX_VALUE} when it is unavailable. */
    public static int primaryLevel(BeaconKind kind, @Nullable Holder<MobEffect> effect, int amplifier) {
        int level = kind.primaryLevelOf(effect, amplifier);
        if (level == 0 || !effectOn(effect) || level > maxLevels(kind)) return Integer.MAX_VALUE;
        return level;
    }

    /** Whether the secondary power may raise {@code effect} by one step (the "II" button). */
    public static boolean canRaise(BeaconKind kind, @Nullable Holder<MobEffect> effect) {
        if (effect == null || kind.evil()) return false;
        return VANILLA.contains(effect) || kind.mighty() && MobEffects.HEALTH_BOOST.equals(effect);
    }

    /** The amplifier a primary power gets: its base, plus one when the secondary is the same effect and raising is allowed. */
    public static int amplifier(BeaconKind kind, @Nullable Holder<MobEffect> primary, int baseAmplifier, @Nullable Holder<MobEffect> secondary, int levels) {
        boolean raised = primary != null && primary.equals(secondary) && levels >= kind.secondaryLevel() && canRaise(kind, primary);
        return Math.min(MAX_AMPLIFIER, baseAmplifier + (raised ? 1 : 0));
    }

    /** {@link #validate(BeaconKind, Holder, int, Holder, int)} with the lowest amplifier of the primary power. */
    public static boolean validate(BeaconKind kind, @Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary, int levels) {
        return validate(kind, primary, Math.max(0, kind.baseAmplifier(primary)), secondary, levels);
    }

    /**
     * Server check of a choice (never trust the client): the primary power ({@code primaryAmp} picks between a power
     * and its "II" placeholder) is on {@code kind}, switched on and within reach; the secondary needs the top level
     * and is one of the kind's secondary powers or the same primary again (only where {@link #canRaise}).
     */
    public static boolean validate(BeaconKind kind, @Nullable Holder<MobEffect> primary, int primaryAmp, @Nullable Holder<MobEffect> secondary, int levels) {
        if (primary == null) return secondary == null;
        if (primaryLevel(kind, primary, primaryAmp) > levels) return false;
        if (secondary == null) return true;
        if (levels < kind.secondaryLevel() || kind.secondaryLevel() > maxLevels(kind)) return false;
        if (secondary.equals(primary)) return canRaise(kind, primary);
        return kind.isSecondary(secondary) && effectOn(secondary);
    }

    /** Vanilla's {@code applyEffects} with the kind's powers and range times {@code percent}; returns the players that got the primary effect. */
    public static List<Player> applyEffects(Level level, BlockPos pos, BeaconKind kind, int levels, @Nullable Holder<MobEffect> primary,
                                            int primaryAmp, @Nullable Holder<MobEffect> secondary, int percent) {
        if (level.isClientSide() || primary == null || primaryLevel(kind, primary, primaryAmp) > levels) return List.of();
        double range = range(levels, percent);
        int amplifier = amplifier(kind, primary, primaryAmp, secondary, levels);
        int duration = durationTicks(levels);
        // The box spans the whole height like Vanilla. Players are read from the level's short player list instead of an
        // entity search: with a range of up to 340 blocks the search would scan hundreds of chunks every 80 ticks (REV2-A-1).
        AABB box = new AABB(pos).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> players = new java.util.ArrayList<>();
        for (Player player : level.players()) {
            if (box.intersects(player.getBoundingBox()) && (!kind.evil() || EvilGate.affects(player))) players.add(player);
        }
        boolean pulse = MobEffects.SATURATION.equals(primary);
        if (!pulse || saturationDue(level.getGameTime())) {
            for (Player player : players) {
                give(player, primary, pulse ? 1 : duration, amplifier);
            }
        }
        if (levels >= kind.secondaryLevel() && secondary != null && !Objects.equals(primary, secondary)
                && kind.isSecondary(secondary) && effectOn(secondary) && kind.secondaryLevel() <= maxLevels(kind)) {
            for (Player player : players) {
                give(player, secondary, duration, 0);
            }
        }
        return players;
    }

    /**
     * Adds the ambient effect. Health Boost's extra hearts are a one-time buffer per stay (concept §3.2): while the player still has it,
     * only its duration is renewed - a plain {@code addEffect} would call {@code onEffectStarted} again and refill the
     * hearts every 4 seconds. Health Boost: renewing in place keeps its max-health modifier untouched instead of removing and re-adding it.
     */
    static void give(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance next = new MobEffectInstance(effect, duration, amplifier, true, true);
        MobEffectInstance existing = RENEW_ONLY.contains(effect) ? player.getEffect(effect) : null;
        if (existing != null && existing.getAmplifier() == amplifier) {
            if (existing.update(next) && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(serverPlayer.getId(), existing, false));
            }
            return;
        }
        player.addEffect(next);
    }

    /** The effect's id path ({@code night_vision}). */
    public static String path(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
    }

    private StrongBeaconLogic() {}
}
