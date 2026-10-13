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
 * The rules of the Strong Beacon (concept §3): Vanilla's levels 1-4 unchanged, levels 5 and 6 with three more
 * primary effects each, range {@code rangeBase + rangePerLevel x levels} (10/10: level 5 = 60, level 6 = 70), duration
 * {@code 9 + 2 x levels} seconds, players only, ambient like Vanilla. Static and side free, so tests call it directly.
 */
public final class StrongBeaconLogic {
    /** Highest pyramid the block can ever reach (the config may lower it to 4 or 5). */
    public static final int MAX_LEVELS = 6;
    public static final int LEVELS_NEEDED_FOR_SECONDARY = 4;
    /** Effects per level (index = level - 1). Level 4 is Vanilla's secondary (Regeneration). */
    public static final List<List<Holder<MobEffect>>> EFFECTS = List.of(
            List.of(MobEffects.SPEED, MobEffects.HASTE),
            List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST),
            List.of(MobEffects.STRENGTH),
            List.of(MobEffects.REGENERATION),
            List.of(MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING, MobEffects.SLOW_FALLING),
            List.of(MobEffects.FIRE_RESISTANCE, MobEffects.DOLPHINS_GRACE, MobEffects.ABSORPTION));
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

    /** Effect range in blocks (horizontal box, full height like Vanilla). */
    public static int range(int levels) {
        return BeaconsConfig.strongBeaconRangeBase + BeaconsConfig.strongBeaconRangePerLevel * levels;
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
                if (i >= 4 && !BeaconsConfig.effectOn(path(effect))) return Integer.MAX_VALUE;
                return i + 1 > maxLevels() ? Integer.MAX_VALUE : i + 1;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Whether {@code effect} is one of the six new ones (level 5 or 6). */
    public static boolean isStrongEffect(@Nullable Holder<MobEffect> effect) {
        return effect != null && (EFFECTS.get(4).contains(effect) || EFFECTS.get(5).contains(effect));
    }

    public static boolean isKnown(@Nullable Holder<MobEffect> effect) {
        return effect != null && EFFECTS.stream().anyMatch(list -> list.contains(effect));
    }

    /**
     * Server check of a choice (never trust the client): the primary is from levels 1-3 or 5-6 and within reach, the
     * secondary needs level 4 and is Regeneration or the same primary ("II" - only for Vanilla's level 1-3 effects,
     * so no Absorption II).
     */
    public static boolean validate(@Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary, int levels) {
        if (primary == null) return secondary == null;
        int primaryLevel = requiredLevel(primary);
        if (primaryLevel > levels || primaryLevel == LEVELS_NEEDED_FOR_SECONDARY) return false;
        if (secondary == null) return true;
        if (levels < LEVELS_NEEDED_FOR_SECONDARY) return false;
        if (secondary.equals(primary)) return primaryLevel < LEVELS_NEEDED_FOR_SECONDARY;
        return requiredLevel(secondary) == LEVELS_NEEDED_FOR_SECONDARY;
    }

    /** Vanilla's {@code applyEffects} with the Strong range; returns the players that got the primary effect. */
    public static List<Player> applyEffects(Level level, BlockPos pos, int levels, @Nullable Holder<MobEffect> primary,
                                            @Nullable Holder<MobEffect> secondary) {
        if (level.isClientSide() || primary == null || requiredLevel(primary) > levels) return List.of();
        double range = range(levels);
        int amplifier = levels >= LEVELS_NEEDED_FOR_SECONDARY && Objects.equals(primary, secondary) && !isStrongEffect(primary) ? 1 : 0;
        int duration = durationTicks(levels);
        AABB box = new AABB(pos).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        for (Player player : players) {
            give(player, primary, duration, amplifier);
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
     * hearts every 4 seconds.
     */
    static void give(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance next = new MobEffectInstance(effect, duration, amplifier, true, true);
        MobEffectInstance existing = effect.equals(MobEffects.ABSORPTION) ? player.getEffect(effect) : null;
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
