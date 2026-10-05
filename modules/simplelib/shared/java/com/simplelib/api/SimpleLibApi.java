package com.simplelib.api;

import com.simplelib.crucible.HeatLevel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The only classes a bundling mod may import from SimpleLib (principle 6a). SimpleLib never names
 * its users; a partner registers what it adds here. Everything is plain Vanilla types plus the
 * nested interfaces below, so a partner never touches SimpleLib's implementation classes.
 */
public final class SimpleLibApi {
    /** Heat of a block that tags cannot describe (e.g. a cauldron filled with soul lava). */
    @FunctionalInterface
    public interface HeatSource {
        HeatLevel heat(Level level, BlockPos pos, BlockState state);
    }

    /**
     * A partner's tool way on a SimpleLib block (e.g. SimpleBuilding's sledgehammer upgrade): when it
     * returns true, a right click no longer opens the crucible/barrel menu but reaches the item's
     * {@code useOn}.
     */
    @FunctionalInterface
    public interface ToolUse {
        boolean wants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand);
    }

    /** A container kind for the reinforced cauldron. Contents: "water", "lava", "powder_snow", "extreme". */
    public interface CauldronBucket {
        /** Content {@code held} pours into the empty cauldron, or null when it is not a full container of this kind. */
        @Nullable String pours(ItemStack held);

        /** What stays in the hand after pouring; {@link ItemStack#EMPTY} when the container breaks. */
        ItemStack afterPour(ItemStack held);

        /** The filled container when {@code held} takes {@code content}; null when it cannot. */
        @Nullable ItemStack take(ItemStack held, String content);
    }

    private static final List<HeatSource> HEAT_SOURCES = new CopyOnWriteArrayList<>();
    private static final List<ToolUse> TOOL_USES = new CopyOnWriteArrayList<>();
    private static volatile boolean axeWaysDisabled;

    public static void registerHeatSource(HeatSource source) {
        HEAT_SOURCES.add(source);
    }

    public static HeatLevel heatOf(Level level, BlockPos pos, BlockState state) {
        HeatLevel best = HeatLevel.NONE;
        for (HeatSource source : HEAT_SOURCES) {
            HeatLevel heat = source.heat(level, pos, state);
            if (heat != null && heat.ordinal() > best.ordinal()) best = heat;
        }
        return best;
    }

    /** Name of the crucible heat at a position ("none", "medium", "high", "extreme"), for partner tests and info. */
    public static String heatAt(Level level, BlockPos crucible) {
        return com.simplelib.crucible.Heat.at(level, crucible).level().name().toLowerCase(java.util.Locale.ROOT);
    }

    public static void registerToolUse(ToolUse use) {
        TOOL_USES.add(use);
    }

    /** Whether a registered tool way (or the axe way) takes this click instead of the block's menu. */
    public static boolean toolWants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (com.simplelib.crucible.AxeWays.wants(state, level, pos, player, hand)) return true;
        for (ToolUse use : TOOL_USES) {
            if (use.wants(state, level, pos, player, hand)) return true;
        }
        return false;
    }

    /**
     * Principle 5a: a partner with a working mod way (SimpleBuilding's sledgehammer) switches the
     * Vanilla axe ways off.
     */
    public static void disableAxeWays() {
        axeWaysDisabled = true;
    }

    public static boolean axeWaysEnabled() {
        return !axeWaysDisabled;
    }

    /** Upgrades a crucible or barrel in place to {@code to}, keeping contents, progress and experience (for a partner's own way). */
    public static void upgradeCrucible(ServerLevel level, BlockPos pos, Block to) {
        com.simplelib.crucible.CrucibleUpgrades.upgradeInPlace(level, pos, to);
    }

    // ------------------------------------------------------------ partner tiers

    /**
     * Creates a crucible block of tier {@code tier} ("iron", "reinforced", "netherite", "enderite") for a
     * partner to register under its own id. It uses SimpleLib's block entity, menu and renderer.
     */
    public static Block newCrucible(BlockBehaviour.Properties properties, String tier) {
        com.simplelib.crucible.CrucibleBlock block = new com.simplelib.crucible.CrucibleBlock(properties,
                com.simplelib.crucible.CrucibleTier.valueOf(tier.toUpperCase(java.util.Locale.ROOT)));
        com.simplelib.registry.LibBlocks.CRUCIBLES.add(block);
        return block;
    }

    /** As {@link #newCrucible}, for a barrel tier ("copper", "reinforced", "enderite"). */
    public static Block newBarrel(BlockBehaviour.Properties properties, String tier) {
        com.simplelib.crucible.CrucibleBarrelBlock block = new com.simplelib.crucible.CrucibleBarrelBlock(properties,
                com.simplelib.crucible.BarrelTier.valueOf(tier.toUpperCase(java.util.Locale.ROOT)));
        com.simplelib.registry.LibBlocks.BARRELS.add(block);
        return block;
    }

    /** Block properties SimpleLib uses for crucibles (pickaxe, light while cooking, no occlusion). */
    public static BlockBehaviour.Properties crucibleProperties(net.minecraft.world.level.material.MapColor color, float hardness,
            float resistance, net.minecraft.world.level.block.SoundType sound) {
        return com.simplelib.registry.LibBlocks.props(color, hardness, resistance, sound);
    }

    public static boolean isCrucible(Block block) {
        return block instanceof com.simplelib.crucible.CrucibleBlock;
    }

    public static boolean isBarrel(Block block) {
        return block instanceof com.simplelib.crucible.CrucibleBarrelBlock;
    }

    /** Whether a block is upgraded through {@link #upgradeCrucible} (crucibles, barrels: block entity contents move). */
    public static boolean upgradesInPlace(Block block) {
        return isCrucible(block) || isBarrel(block);
    }

    /** Slot count of a crucible block (tests, info), or 0. */
    public static int crucibleSlots(Block block) {
        return block instanceof com.simplelib.crucible.CrucibleBlock crucible ? crucible.tier().slots() : 0;
    }

    /** Stack multiplier of a crucible or barrel block (tests, info), or 0. */
    public static int stackMultiplier(Block block) {
        if (block instanceof com.simplelib.crucible.CrucibleBlock crucible) return crucible.tier().stackMultiplier();
        if (block instanceof com.simplelib.crucible.CrucibleBarrelBlock barrel) return barrel.tier().stackMultiplier();
        return 0;
    }

    // ------------------------------------------------------------ partner tool ways

    /** Whether the off-hand item is the right part for the next build strike on this block (iron block or blank). */
    public static boolean canBuildStrike(BlockState state, ItemStack offhand) {
        return com.simplelib.crucible.CrucibleBlankBlock.fitsNext(com.simplelib.crucible.CrucibleBlankBlock.strikesAt(state), offhand);
    }

    /** One build strike with a partner tool ({@code toolDamage} durability per strike); true when it hit. */
    public static boolean buildStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        return com.simplelib.crucible.CrucibleBlankBlock.strike(level, pos, player, tool, toolDamage);
    }

    /** Whether an unattached barrel stands next to a crucible without a barrel. */
    public static boolean canAttach(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof com.simplelib.crucible.CrucibleBarrelBlock
                && !state.getValue(com.simplelib.crucible.CrucibleBarrelBlock.ATTACHED)
                && com.simplelib.crucible.CrucibleBarrelBlock.crucibleSide(level, pos, state) != null;
    }

    /** One attach strike with a partner tool; true when it counted. */
    public static boolean attachStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        return com.simplelib.crucible.CrucibleBarrelBlock.attachStrike(level, pos, player, tool, toolDamage);
    }

    /** Whether a barrel is attached to a crucible (state property). */
    public static boolean isAttached(BlockState state) {
        return state.getBlock() instanceof com.simplelib.crucible.CrucibleBarrelBlock
                && state.getValue(com.simplelib.crucible.CrucibleBarrelBlock.ATTACHED);
    }

    /** Strikes needed to build the iron crucible / attach a barrel (wiki, JEI). */
    public static int buildStrikes() {
        return com.simplelib.crucible.CrucibleBlankBlock.STRIKES;
    }

    public static int attachStrikes() {
        return com.simplelib.crucible.CrucibleBarrelBlock.ATTACH_STRIKES;
    }

    // ------------------------------------------------------------ reinforced cauldron

    /** The reinforced cauldron (SimpleLib), or null before registration. */
    public static @Nullable Block reinforcedCauldron() {
        return com.simplelib.registry.LibBlocks.REINFORCED_CAULDRON;
    }

    /** The reinforced cauldron filled with {@code content} ("empty", "water", "lava", "powder_snow", "extreme"). */
    public static BlockState reinforcedCauldron(String content) {
        return com.simplelib.registry.LibBlocks.REINFORCED_CAULDRON.defaultBlockState().setValue(
                com.simplelib.cauldron.ReinforcedCauldronBlock.CONTENT,
                com.simplelib.cauldron.ReinforcedCauldronBlock.Content.valueOf(content.toUpperCase(java.util.Locale.ROOT)));
    }

    /** Content name of a reinforced cauldron state, or null for any other block. */
    public static @Nullable String cauldronContent(BlockState state) {
        return state.getBlock() instanceof com.simplelib.cauldron.ReinforcedCauldronBlock
                ? state.getValue(com.simplelib.cauldron.ReinforcedCauldronBlock.CONTENT).getSerializedName() : null;
    }

    /** Adds a partner container kind (its buckets, its extreme fluid) to the reinforced cauldron; asked before Vanilla's bucket. */
    public static void registerCauldronBucket(CauldronBucket bucket) {
        com.simplelib.cauldron.ReinforcedCauldrons.BUCKETS.add(0, new com.simplelib.cauldron.ReinforcedCauldrons.Bucket() {
            @Override
            public com.simplelib.cauldron.ReinforcedCauldronBlock.Content pours(ItemStack held) {
                String content = bucket.pours(held);
                return content == null ? null
                        : com.simplelib.cauldron.ReinforcedCauldronBlock.Content.valueOf(content.toUpperCase(java.util.Locale.ROOT));
            }

            @Override
            public ItemStack afterPour(ItemStack held) {
                return bucket.afterPour(held);
            }

            @Override
            public ItemStack take(ItemStack held, com.simplelib.cauldron.ReinforcedCauldronBlock.Content content) {
                return bucket.take(held, content.getSerializedName());
            }
        });
    }

    /** What the extreme content does to an entity inside the reinforced cauldron (default: like lava). */
    public static void setExtremeCauldronEffect(java.util.function.BiConsumer<Level, Entity> effect) {
        com.simplelib.cauldron.ReinforcedCauldrons.extremeInside = effect;
    }

    private SimpleLibApi() {}
}
