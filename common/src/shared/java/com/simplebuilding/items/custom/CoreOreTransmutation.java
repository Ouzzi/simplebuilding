package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Easter egg of the building cores (owner, 2026-09-29): every right click with a core on a block in
 * which ores generate has a tiny chance to turn that block into an ore. The core is not used up; the
 * chance applies per use, and the core's normal cooldown limits the uses.
 *
 * <p><strong>Chance ladder</strong> - "1 in N" per click, the better the core the likelier. The six
 * {@code *_CORE_ORE_CHANCE} constants are the one table the Balancing-Zentrale reads (code constants
 * with "CHANCE" in their name) and writes back. The copper core is a pure curiosity (1 in 10000 is
 * about three hours of clicking at one click per second); the enderite core about half an hour.
 *
 * <p><strong>Hosts</strong> - which ores are possible is decided by the block, exactly as world
 * generation does: {@code #minecraft:stone_ore_replaceables} (stone, granite, diorite, andesite) gives
 * the stone ores, {@code #minecraft:deepslate_ore_replaceables} and tuff give the deepslate ores,
 * netherrack gives the nether ores and end stone the mod's two end ores. The weights follow how common
 * each ore is in its host in a vanilla world (coal and iron most, emerald least; in deepslate redstone,
 * iron and diamond gain; ancient debris is a jackpot); every host's weights add up to 100, so they read
 * as percent.
 */
public final class CoreOreTransmutation {

    /** Copper core: 1 in 10000 per click - an easter egg, not a way to mine. */
    public static final int COPPER_CORE_ORE_CHANCE = 10000;
    /** Iron core: 1 in 7000 per click. */
    public static final int IRON_CORE_ORE_CHANCE = 7000;
    /** Gold core: 1 in 5000 per click. */
    public static final int GOLD_CORE_ORE_CHANCE = 5000;
    /** Diamond core: 1 in 3500 per click. */
    public static final int DIAMOND_CORE_ORE_CHANCE = 3500;
    /** Netherite core: 1 in 2500 per click. */
    public static final int NETHERITE_CORE_ORE_CHANCE = 2500;
    /** Enderite core: 1 in 2000 per click. */
    public static final int ENDERITE_CORE_ORE_CHANCE = 2000;

    /** The kinds of block a core can turn into ore; each has its own ore table. */
    public enum Host {
        STONE, DEEPSLATE, NETHERRACK, END_STONE
    }

    /** One ore of a host table with its weight (the weights of a host add up to 100). */
    public record WeightedOre(Block ore, int weight) {
    }

    /** Glocken-Dreiklang beim Umwandeln: Grundton, grosse Terz, Quinte (Tonhoehen der Notenblock-Glocke). */
    private static final float[] TRANSMUTE_CHORD = {1.0F, 1.26F, 1.498F};

    private CoreOreTransmutation() {
    }

    /** The host kind of {@code state}, or empty if no ore generates in this block. */
    public static Optional<Host> hostOf(BlockState state) {
        if (state.is(BlockTags.STONE_ORE_REPLACEABLES)) {
            return Optional.of(Host.STONE);
        }
        // Tuff sits in the deepslate layer and gets the deepslate ores (vanilla up to 26.2 kept it in
        // #deepslate_ore_replaceables; 26.3 moved it to its own tag, the ores stayed the deepslate ones).
        if (state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || state.is(Blocks.TUFF)) {
            return Optional.of(Host.DEEPSLATE);
        }
        if (state.is(Blocks.NETHERRACK)) {
            return Optional.of(Host.NETHERRACK);
        }
        if (state.is(Blocks.END_STONE)) {
            return Optional.of(Host.END_STONE);
        }
        return Optional.empty();
    }

    /** The ore table of {@code host}, most common first. */
    public static List<WeightedOre> ores(Host host) {
        return switch (host) {
            case STONE -> Tables.STONE;
            case DEEPSLATE -> Tables.DEEPSLATE;
            case NETHERRACK -> Tables.NETHERRACK;
            case END_STONE -> Tables.END_STONE;
        };
    }

    /** Sum of the weights of {@code host}'s table. */
    public static int totalWeight(Host host) {
        int total = 0;
        for (WeightedOre entry : ores(host)) {
            total += entry.weight();
        }
        return total;
    }

    /** The ore for a roll {@code 0 <= roll < totalWeight(host)}: the table is walked in order. */
    public static Block oreForRoll(Host host, int roll) {
        int bound = 0;
        List<WeightedOre> table = ores(host);
        for (WeightedOre entry : table) {
            bound += entry.weight();
            if (roll < bound) {
                return entry.ore();
            }
        }
        return table.get(table.size() - 1).ore();
    }

    /** Picks an ore of {@code host} by weight. */
    public static Block pickOre(Host host, RandomSource random) {
        return oreForRoll(host, random.nextInt(totalWeight(host)));
    }

    /** Whether a "1 in {@code oneIn}" chance comes up; 0 or less never does. */
    public static boolean chanceHits(int oneIn, RandomSource random) {
        return oneIn > 0 && random.nextInt(oneIn) == 0;
    }

    /**
     * One click of a core at {@code pos}: if the block is a host and the "1 in {@code oneIn}" chance
     * comes up, the block becomes an ore of that host, with a burst of its dust, sparks and a bell
     * chord - no text: gadgets show nothing in chat or above the hotbar (owner rule). The random
     * source is only drawn from for hosts: first the chance, then - on a hit - the ore.
     *
     * @return the ore placed, or empty if nothing changed
     */
    public static Optional<Block> tryTransmute(ServerLevel level, BlockPos pos, int oneIn, RandomSource random) {
        BlockState state = level.getBlockState(pos);
        Optional<Host> host = hostOf(state);
        if (host.isEmpty() || !chanceHits(oneIn, random)) {
            return Optional.empty();
        }
        Block ore = pickOre(host.get(), random);
        level.setBlock(pos, ore.defaultBlockState(), Block.UPDATE_ALL);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 30, 0.35, 0.35, 0.35, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 24, 0.4, 0.4, 0.4, 0.25);
        level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 12, 0.55, 0.55, 0.55, 0.0);
        // Unverwechselbar: tiefe Amethyst-Resonanz unter einem Glocken-Dreiklang (Grundton, Terz, Quinte),
        // darueber ein heller Aufstiegs-Klang - kein anderer Klick der Mod klingt so.
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        for (float pitch : TRANSMUTE_CHORD) {
            level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BELL, SoundSource.BLOCKS, 0.9F, pitch);
        }
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4F, 1.8F);
        return Optional.of(ore);
    }

    /** Built on first use, so the mod's blocks are registered by then. */
    private static final class Tables {
        static final List<WeightedOre> STONE = List.of(
                new WeightedOre(Blocks.COAL_ORE, 40),
                new WeightedOre(Blocks.IRON_ORE, 24),
                new WeightedOre(Blocks.COPPER_ORE, 20),
                new WeightedOre(Blocks.REDSTONE_ORE, 5),
                new WeightedOre(Blocks.GOLD_ORE, 4),
                new WeightedOre(Blocks.LAPIS_ORE, 4),
                new WeightedOre(Blocks.DIAMOND_ORE, 2),
                new WeightedOre(Blocks.EMERALD_ORE, 1));
        static final List<WeightedOre> DEEPSLATE = List.of(
                new WeightedOre(Blocks.DEEPSLATE_REDSTONE_ORE, 24),
                new WeightedOre(Blocks.DEEPSLATE_IRON_ORE, 22),
                new WeightedOre(Blocks.DEEPSLATE_COPPER_ORE, 12),
                new WeightedOre(Blocks.DEEPSLATE_GOLD_ORE, 12),
                new WeightedOre(Blocks.DEEPSLATE_LAPIS_ORE, 10),
                new WeightedOre(Blocks.DEEPSLATE_DIAMOND_ORE, 9),
                new WeightedOre(Blocks.DEEPSLATE_COAL_ORE, 8),
                new WeightedOre(Blocks.DEEPSLATE_EMERALD_ORE, 3));
        static final List<WeightedOre> NETHERRACK = List.of(
                new WeightedOre(Blocks.NETHER_QUARTZ_ORE, 70),
                new WeightedOre(Blocks.NETHER_GOLD_ORE, 27),
                new WeightedOre(Blocks.ANCIENT_DEBRIS, 3));
        static final List<WeightedOre> END_STONE = List.of(
                new WeightedOre(ModBlocks.NIHILITH_ORE, 92),
                new WeightedOre(ModBlocks.ASTRALIT_ORE, 8));
    }
}
