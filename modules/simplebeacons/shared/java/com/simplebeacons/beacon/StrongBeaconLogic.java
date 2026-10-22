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
 * The rules of the Strong Beacon (concept §3): Vanilla's levels 1-4 plus one or two more primary effects on levels 1-3
 * (SP22-10: three per level), levels 5 and 6 with three more primary effects each, range
 * {@code (rangeBase + rangePerLevel x levels) x material factor} (10/10: level 5 = 60, level 6 = 70 with Iron), duration
 * {@code 9 + 2 x levels} seconds, players only, ambient like Vanilla. Static and side free, so tests call it directly.
 */
public final class StrongBeaconLogic {
    /** Highest pyramid the block can ever reach (the config may lower it to 4 or 5). */
    public static final int MAX_LEVELS = 6;
    public static final int LEVELS_NEEDED_FOR_SECONDARY = 4;
    /**
     * Effects per level (index = level - 1). Level 4 is Vanilla's secondary (Regeneration). The third column of levels
     * 1-3 is new (SP22-10, plan docs/ai/PLAN-A-BEACONS22-2026-10-22.md): Saturation, Health Boost, Invisibility and Hero of the Village.
     */
    public static final List<List<Holder<MobEffect>>> EFFECTS = List.of(
            List.of(MobEffects.SPEED, MobEffects.HASTE, MobEffects.SATURATION),
            List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST, MobEffects.HEALTH_BOOST),
            List.of(MobEffects.STRENGTH, MobEffects.INVISIBILITY, MobEffects.HERO_OF_THE_VILLAGE),
            List.of(MobEffects.REGENERATION),
            List.of(MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING, MobEffects.SLOW_FALLING),
            List.of(MobEffects.FIRE_RESISTANCE, MobEffects.DOLPHINS_GRACE, MobEffects.ABSORPTION));
    /** Vanilla's own beacon powers: only these have a level II and no server switch. */
    public static final List<Holder<MobEffect>> VANILLA = List.of(MobEffects.SPEED, MobEffects.HASTE, MobEffects.RESISTANCE,
            MobEffects.JUMP_BOOST, MobEffects.STRENGTH, MobEffects.REGENERATION);
    /** Effects renewed in place instead of re-added, so their one-time buffer (hearts) is not reset every 4 seconds. */
    static final List<Holder<MobEffect>> RENEW_ONLY = List.of(MobEffects.ABSORPTION, MobEffects.HEALTH_BOOST);
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
        return RangeFactor.apply(range(levels), percent);
    }

    /** Whether the beacon cycle at {@code gameTime} (every 80 ticks) gives the Saturation pulse. */
    public static boolean saturationDue(long gameTime) {
        return gameTime / 80 % SATURATION_EVERY_CYCLES == 0;
    }

    /** Effect duration in ticks (Vanilla formula). */
    public static int durationTicks(int levels) {
        return (9 + levels * 2) * 20;
    }

    /** The level an effect needs, {@link Integer#MAX_VALUE} for foreign or switched off effects, 0 for none. */
    public static int requiredLevel(@Nullable Holder<MobEffect> effect) {
        if (effect == null) return 0;
        for (int i = 0; i < EFFECTS.size(); i++) {
            if (EFFECTS.get(i).contains(effect)) {
                if (isStrongEffect(effect) && !BeaconsConfig.effectOn(path(effect))) return Integer.MAX_VALUE;
                return i + 1 > maxLevels() ? Integer.MAX_VALUE : i + 1;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Whether {@code effect} is one of the ten new ones (not Vanilla's): no level II, switchable by the server. */
    public static boolean isStrongEffect(@Nullable Holder<MobEffect> effect) {
        return effect != null && isKnown(effect) && !VANILLA.contains(effect);
    }

    public static boolean isKnown(@Nullable Holder<MobEffect> effect) {
        return effect != null && EFFECTS.stream().anyMatch(list -> list.contains(effect));
    }

    /**
     * Server check of a choice (never trust the client): the primary is from levels 1-3 or 5-6 and within reach, the
     * secondary needs level 4 and is Regeneration or the same primary ("II" - only for Vanilla's own level 1-3 effects,
     * so no Absorption II or Saturation II).
     */
    public static boolean validate(@Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary, int levels) {
        if (primary == null) return secondary == null;
        int primaryLevel = requiredLevel(primary);
        if (primaryLevel > levels || primaryLevel == LEVELS_NEEDED_FOR_SECONDARY) return false;
        if (secondary == null) return true;
        if (levels < LEVELS_NEEDED_FOR_SECONDARY) return false;
        if (secondary.equals(primary)) return primaryLevel < LEVELS_NEEDED_FOR_SECONDARY && !isStrongEffect(primary);
        return requiredLevel(secondary) == LEVELS_NEEDED_FOR_SECONDARY;
    }

    /** {@link #applyEffects(Level, BlockPos, int, Holder, Holder, int)} without a material factor. */
    public static List<Player> applyEffects(Level level, BlockPos pos, int levels, @Nullable Holder<MobEffect> primary,
                                            @Nullable Holder<MobEffect> secondary) {
        return applyEffects(level, pos, levels, primary, secondary, RangeFactor.NONE);
    }

    /** Vanilla's {@code applyEffects} with the Strong range times {@code percent}; returns the players that got the primary effect. */
    public static List<Player> applyEffects(Level level, BlockPos pos, int levels, @Nullable Holder<MobEffect> primary,
                                            @Nullable Holder<MobEffect> secondary, int percent) {
        if (level.isClientSide() || primary == null || requiredLevel(primary) > levels) return List.of();
        double range = range(levels, percent);
        int amplifier = levels >= LEVELS_NEEDED_FOR_SECONDARY && Objects.equals(primary, secondary) && !isStrongEffect(primary) ? 1 : 0;
        int duration = durationTicks(levels);
        AABB box = new AABB(pos).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        boolean pulse = MobEffects.SATURATION.equals(primary);
        if (!pulse || saturationDue(level.getGameTime())) {
            for (Player player : players) {
                give(player, primary, pulse ? 1 : duration, amplifier);
            }
        }
        if (levels >= LEVELS_NEEDED_FOR_SECONDARY && secondary != null && !Objects.equals(primary, secondary)
                && requiredLevel(secondary) <= levels) {
            for (Player player : players) {
                give(player, secondary, duration, 0);
            }
        }
        return players;
    }

    /**
     * Adds the ambient effect. Absorption is a one-time buffer per stay (concept §3.2): while the player still has it,
     * only its duration is renewed - a plain {@code addEffect} would call {@code onEffectStarted} again and refill the
     * hearts every 4 seconds. Health Boost likewise: renewing in place keeps its max-health modifier untouched instead of removing and re-adding it.
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
