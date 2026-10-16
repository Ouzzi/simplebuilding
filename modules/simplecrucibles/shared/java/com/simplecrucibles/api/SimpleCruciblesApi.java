package com.simplecrucibles.api;

import com.simplecrucibles.crucible.HeatLevel;
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
 * The only classes a bundling mod may import from Simple Crucibles (principle 6a). It never names
 * its users; a partner registers what it adds here. Everything is plain Vanilla types plus the
 * nested interfaces below, so a partner never touches the implementation classes.
 */
public final class SimpleCruciblesApi {
    /** Heat of a block that tags cannot describe (e.g. a cauldron filled with soul lava). */
    @FunctionalInterface
    public interface HeatSource {
        HeatLevel heat(Level level, BlockPos pos, BlockState state);
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
        return com.simplecrucibles.crucible.Heat.at(level, crucible).level().name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Upgrades a crucible or barrel in place to {@code to}, keeping contents, progress and experience (for a partner's own way). */
    public static void upgradeCrucible(ServerLevel level, BlockPos pos, Block to) {
        com.simplecrucibles.crucible.CrucibleUpgrades.upgradeInPlace(level, pos, to);
    }

    /**
     * Crucible upgrades go strike by strike (owner N51): {@link #crucibleUpgradeStrikes()} strikes, one unit of material
     * every {@link #crucibleUpgradeStrikesPerUnit()} strikes. True for every crucible block (barrels keep their own way).
     */
    public static boolean stagedUpgrade(Block block) {
        return isCrucible(block);
    }

    public static int crucibleUpgradeStrikes() {
        return com.simplecrucibles.crucible.CrucibleUpgrades.CRUCIBLE_STRIKES;
    }

    public static int crucibleUpgradeStrikesPerUnit() {
        return com.simplecrucibles.crucible.CrucibleUpgrades.STRIKES_PER_UNIT;
    }

    /** Units already struck into the crucible at this state (0..3). */
    public static int crucibleUpgradeUnits(BlockState state) {
        return com.simplecrucibles.crucible.CrucibleUpgrades.unitsIn(state);
    }

    /**
     * Strike {@code done} of a partner's crucible upgrade to {@code to}: feedback, preview, every third strike one unit
     * from {@code material} (empty = creative, nothing taken), the last one upgrades in place. Returns the units struck in.
     */
    public static int crucibleUpgradeStrike(ServerLevel level, BlockPos pos, Block to, int done, ItemStack material) {
        return com.simplecrucibles.crucible.CrucibleUpgrades.strikeInUnit(level, pos, level.getBlockState(pos), to, done, material);
    }

    // ------------------------------------------------------------ partner tiers

    /**
     * Creates a crucible block of tier {@code tier} ("iron", "reinforced", "netherite", "enderite") for a
     * partner to register under its own id. It uses the module's block entity, menu and renderer.
     */
    public static Block newCrucible(BlockBehaviour.Properties properties, String tier) {
        com.simplecrucibles.crucible.CrucibleBlock block = new com.simplecrucibles.crucible.CrucibleBlock(properties,
                com.simplecrucibles.crucible.CrucibleTier.valueOf(tier.toUpperCase(java.util.Locale.ROOT)));
        com.simplecrucibles.registry.CrucibleBlocks.CRUCIBLES.add(block);
        return block;
    }

    /** As {@link #newCrucible}, for a barrel tier ("copper", "reinforced", "enderite"). */
    public static Block newBarrel(BlockBehaviour.Properties properties, String tier) {
        com.simplecrucibles.crucible.CrucibleBarrelBlock block = new com.simplecrucibles.crucible.CrucibleBarrelBlock(properties,
                com.simplecrucibles.crucible.BarrelTier.valueOf(tier.toUpperCase(java.util.Locale.ROOT)));
        com.simplecrucibles.registry.CrucibleBlocks.BARRELS.add(block);
        return block;
    }

    /** Block properties the module uses for crucibles (pickaxe, light while cooking, no occlusion). */
    public static BlockBehaviour.Properties crucibleProperties(net.minecraft.world.level.material.MapColor color, float hardness,
            float resistance, net.minecraft.world.level.block.SoundType sound) {
        return com.simplecrucibles.registry.CrucibleBlocks.props(color, hardness, resistance, sound);
    }

    public static boolean isCrucible(Block block) {
        return block instanceof com.simplecrucibles.crucible.CrucibleBlock;
    }

    public static boolean isBarrel(Block block) {
        return block instanceof com.simplecrucibles.crucible.CrucibleBarrelBlock;
    }

    /** Whether a block is upgraded through {@link #upgradeCrucible} (crucibles, barrels: block entity contents move). */
    public static boolean upgradesInPlace(Block block) {
        return isCrucible(block) || isBarrel(block);
    }

    /** Slot count of a crucible block (tests, info), or 0. */
    public static int crucibleSlots(Block block) {
        return block instanceof com.simplecrucibles.crucible.CrucibleBlock crucible ? crucible.tier().slots() : 0;
    }

    /** Heat id (0..3), occupied slots, capacity and next completion ticks (-1 if idle). */
    public static int[] crucibleStatus(net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (!(entity instanceof com.simplecrucibles.crucible.CrucibleBlockEntity crucible)) return new int[0];
        int occupied = 0;
        for (int slot = 0; slot < crucible.getContainerSize(); slot++) {
            if (!crucible.getItem(slot).isEmpty()) occupied++;
        }
        return new int[] {crucible.heat().ordinal(), occupied, crucible.tier().slots(), crucible.nextCompletionTicks()};
    }

    public static int requiredHeat(ItemStack input, net.minecraft.world.item.crafting.RecipeType<?> type) {
        return com.simplecrucibles.crucible.CrucibleJob.requiredHeat(input, type).ordinal();
    }

    /** Iron-tier duration at the given heat, without a two-block source distance penalty. */
    public static int crucibleCookingTicks(int baseTicks, int heat) {
        double factor = HeatLevel.byId(heat).factor();
        if (factor <= 0) return 0;
        long step = Math.max(1L, Math.round(1000 * factor));
        return (int) Math.min(Integer.MAX_VALUE, (Math.max(1L, baseTicks) * 1000 + step - 1) / step);
    }

    /** Stack multiplier of a crucible or barrel block (tests, info), or 0. */
    public static int stackMultiplier(Block block) {
        if (block instanceof com.simplecrucibles.crucible.CrucibleBlock crucible) return crucible.tier().stackMultiplier();
        if (block instanceof com.simplecrucibles.crucible.CrucibleBarrelBlock barrel) return barrel.tier().stackMultiplier();
        return 0;
    }

    // ------------------------------------------------------------ partner tool ways

    /** Whether the off-hand item is the right part for the next build strike on this block (iron block or blank). */
    public static boolean canBuildStrike(BlockState state, ItemStack offhand) {
        return com.simplecrucibles.crucible.CrucibleBlankBlock.fitsNext(com.simplecrucibles.crucible.CrucibleBlankBlock.strikesAt(state), offhand);
    }

    /** One build strike with a partner tool ({@code toolDamage} durability per strike); true when it hit. */
    public static boolean buildStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        return com.simplecrucibles.crucible.CrucibleBlankBlock.strike(level, pos, player, tool, toolDamage);
    }

    /** Whether an unattached barrel stands next to a crucible without a barrel. */
    public static boolean canAttach(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof com.simplecrucibles.crucible.CrucibleBarrelBlock
                && !state.getValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACHED)
                && com.simplecrucibles.crucible.CrucibleBarrelBlock.crucibleSide(level, pos, state) != null;
    }

    /** One attach strike with a partner tool; true when it counted. */
    public static boolean attachStrike(Level level, BlockPos pos, Player player, ItemStack tool, int toolDamage) {
        return com.simplecrucibles.crucible.CrucibleBarrelBlock.attachStrike(level, pos, player, tool, toolDamage);
    }

    /** Whether a barrel is attached to a crucible (state property). */
    public static boolean isAttached(BlockState state) {
        return state.getBlock() instanceof com.simplecrucibles.crucible.CrucibleBarrelBlock
                && state.getValue(com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACHED);
    }

    /** Strikes needed to build the iron crucible / attach a barrel (wiki, JEI). */
    public static int buildStrikes() {
        return com.simplecrucibles.crucible.CrucibleBlankBlock.STRIKES;
    }

    public static int attachStrikes() {
        return com.simplecrucibles.crucible.CrucibleBarrelBlock.ATTACH_STRIKES;
    }

    /** Of the build strikes, how many place a plate or rim piece (iron ingot); the rest place a handle (JEI, wiki). */
    public static int buildWallStrikes() {
        return com.simplecrucibles.crucible.CrucibleBlankBlock.WALLS;
    }

    /** Slots of an attached barrel that the crucible uses and shows, per crucible tier: "6/12/18/30" (JEI, wiki). */
    public static String attachedBarrelSlots() {
        StringBuilder out = new StringBuilder();
        for (com.simplecrucibles.crucible.CrucibleTier tier : com.simplecrucibles.crucible.CrucibleTier.values()) {
            if (!out.isEmpty()) out.append('/');
            out.append(tier.slots());
        }
        return out.toString();
    }

    // ------------------------------------------------------------ reinforced cauldron

    /** The reinforced cauldron (Simple Crucibles), or null before registration. */
    public static @Nullable Block reinforcedCauldron() {
        return com.simplecrucibles.registry.CrucibleBlocks.REINFORCED_CAULDRON;
    }

    /** The reinforced cauldron filled with {@code content} ("empty", "water", "lava", "powder_snow", "extreme"). */
    public static BlockState reinforcedCauldron(String content) {
        return com.simplecrucibles.registry.CrucibleBlocks.REINFORCED_CAULDRON.defaultBlockState().setValue(
                com.simplecrucibles.cauldron.ReinforcedCauldronBlock.CONTENT,
                com.simplecrucibles.cauldron.ReinforcedCauldronBlock.Content.valueOf(content.toUpperCase(java.util.Locale.ROOT)));
    }

    /** Content name of a reinforced cauldron state, or null for any other block. */
    public static @Nullable String cauldronContent(BlockState state) {
        return state.getBlock() instanceof com.simplecrucibles.cauldron.ReinforcedCauldronBlock
                ? state.getValue(com.simplecrucibles.cauldron.ReinforcedCauldronBlock.CONTENT).getSerializedName() : null;
    }

    /** Adds a partner container kind (its buckets, its extreme fluid) to the reinforced cauldron; asked before Vanilla's bucket. */
    public static void registerCauldronBucket(CauldronBucket bucket) {
        com.simplecrucibles.cauldron.ReinforcedCauldrons.BUCKETS.add(0, new com.simplecrucibles.cauldron.ReinforcedCauldrons.Bucket() {
            @Override
            public com.simplecrucibles.cauldron.ReinforcedCauldronBlock.Content pours(ItemStack held) {
                String content = bucket.pours(held);
                return content == null ? null
                        : com.simplecrucibles.cauldron.ReinforcedCauldronBlock.Content.valueOf(content.toUpperCase(java.util.Locale.ROOT));
            }

            @Override
            public ItemStack afterPour(ItemStack held) {
                return bucket.afterPour(held);
            }

            @Override
            public ItemStack take(ItemStack held, com.simplecrucibles.cauldron.ReinforcedCauldronBlock.Content content) {
                return bucket.take(held, content.getSerializedName());
            }
        });
    }

    /** What the extreme content does to an entity inside the reinforced cauldron (default: like lava). */
    public static void setExtremeCauldronEffect(java.util.function.BiConsumer<Level, Entity> effect) {
        com.simplecrucibles.cauldron.ReinforcedCauldrons.extremeInside = effect;
    }

    // ------------------------------------------------------------ soul lava and buckets (split stage 5b)

    private static final List<java.util.function.Consumer<Player>> SOUL_LAVA_SCOOPED = new CopyOnWriteArrayList<>();

    /** Called whenever a player scoops a soul lava source with any bucket (SimpleBuilding: its "Soul Scooper" advancement). */
    public static void onSoulLavaScooped(java.util.function.Consumer<Player> listener) {
        SOUL_LAVA_SCOOPED.add(listener);
    }

    /** Fires {@link #onSoulLavaScooped} (the soul lava block calls it). */
    public static void soulLavaScooped(Player player) {
        SOUL_LAVA_SCOOPED.forEach(listener -> listener.accept(player));
    }

    /**
     * The module's buckets (copper, Enderite, ceramic) used on a cauldron whose state is given as the matching Vanilla
     * cauldron (empty, water level 3, lava): a partner's own cauldron (SimpleBuilding's wooden cauldron) maps its content
     * to Vanilla, calls this and maps the result back. Null when {@code stack} is no such bucket case.
     */
    public static @Nullable net.minecraft.world.InteractionResult cauldronBucket(ItemStack stack, BlockState vanillaState, Level level, BlockPos pos,
                                                                                Player player, InteractionHand hand) {
        return com.simplecrucibles.fluid.CauldronBuckets.interact(stack, vanillaState, level, pos, player, hand);
    }

    /** Touching soul lava: burning, lava damage and soul burn (configured in simplecrucibles-server.json). */
    public static void touchSoulLava(Level level, Entity entity) {
        com.simplecrucibles.fluid.SoulLava.touch(level, entity);
    }

    private SimpleCruciblesApi() {}
}
