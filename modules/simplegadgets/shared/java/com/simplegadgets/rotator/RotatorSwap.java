package com.simplegadgets.rotator;

import com.simplegadgets.config.GadgetsConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Rotator's second function (queue entry 63): a placeable block in the off hand + right click with
 * the Rotator swaps the clicked block for it. The old block comes back as its drops into the
 * inventory, the off-hand block is used up like a placement, the swap costs {@link #COST_FACTOR} times
 * a turn's charge, and a wind-up (hold the click, {@link #windupTicks}) comes first.
 *
 * <p><b>Which blocks may be swapped</b> ({@link #canSwapTarget}), in this order:
 * <ol>
 *   <li>never: air, fluids and waterlogged blocks, extended pistons and their heads, one half of a
 *       double block (door, tall plant, bed, double chest), unbreakable blocks (hardness &lt; 0),
 *       blocks with a block entity unless it is an <em>empty</em> container without an unopened loot
 *       table (beacon, spawner, sign, banner ... and every chest with content stay), and a short
 *       list of valuables ({@link #isValuable}). No list can lift these.</li>
 *   <li>the blacklist ({@code rotatorSwapBlacklist}) wins over everything below;</li>
 *   <li>the whitelist ({@code rotatorSwapWhitelist}) allows a block regardless of the rule;</li>
 *   <li>otherwise the mining rule: hardness up to {@code rotatorSwapMaxHardness} and a block a
 *       <em>stone</em> tool still harvests (a block that needs a correct tool must be mineable by a
 *       stone pickaxe, axe, shovel or hoe - so no obsidian, no diamond ore).</li>
 * </ol>
 *
 * <p>Lists are comma separated: {@code #namespace:tag} (a block tag) or {@code namespace:block}.
 * Everything here is read-only except {@link #perform}; the client hint asks the same predicates.
 */
public final class RotatorSwap {
    private RotatorSwap() {}

    /** A swap costs this many times the charge of a turn ({@link RotatorItem#useCost}). */
    public static final int COST_FACTOR = 2;
    /** Standard wind-up in ticks: one second. */
    public static final int DEFAULT_WINDUP_TICKS = 20;
    /** Extra wind-up ticks per point of block hardness (stone 1.5 = +6 ticks, an iron block 5 = +20). */
    public static final int HARDNESS_TICKS_PER_POINT = 4;
    /** Standard hardness limit of the mining rule. */
    public static final double DEFAULT_MAX_HARDNESS = 6.0;
    /** Efficiency level that makes the swap instant. */
    public static final int INSTANT_EFFICIENCY = 5;
    /** Cooldown after a finished swap, so a held click does not turn the fresh block. */
    public static final int COOLDOWN_TICKS = 10;

    private static final ItemStack[] STONE_TOOLS = {
            new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.STONE_AXE),
            new ItemStack(Items.STONE_SHOVEL), new ItemStack(Items.STONE_HOE)};

    // ---------------------------------------------------------------------------------- config

    public static boolean enabled() {
        return GadgetsConfig.rotatorSwapEnabled;
    }

    public static int baseWindupTicks() {
        return GadgetsConfig.rotatorSwapWindupTicks;
    }

    public static double maxHardness() {
        return GadgetsConfig.rotatorSwapMaxHardness;
    }

    /** Parsed block list: tags and ids; an unreadable entry is skipped. */
    public record BlockList(List<TagKey<Block>> tags, Set<Identifier> ids) {
        static final BlockList EMPTY = new BlockList(List.of(), Set.of());

        public static BlockList parse(@Nullable String text) {
            if (text == null || text.isBlank()) {
                return EMPTY;
            }
            List<TagKey<Block>> tags = new ArrayList<>();
            Set<Identifier> ids = new HashSet<>();
            for (String entry : text.split("[,;\\s]+")) {
                boolean tag = entry.startsWith("#");
                Identifier id = Identifier.tryParse(tag ? entry.substring(1) : entry);
                if (id == null) {
                    continue;
                }
                if (tag) {
                    tags.add(TagKey.create(Registries.BLOCK, id));
                } else {
                    ids.add(id);
                }
            }
            return new BlockList(List.copyOf(tags), Set.copyOf(ids));
        }

        public boolean matches(BlockState state) {
            if (ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
                return true;
            }
            for (TagKey<Block> tag : tags) {
                if (state.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String cachedWhiteRaw = "";
    private static BlockList cachedWhite = BlockList.EMPTY;
    private static String cachedBlackRaw = "";
    private static BlockList cachedBlack = BlockList.EMPTY;

    public static synchronized BlockList whitelist() {
        String raw = GadgetsConfig.rotatorSwapWhitelist;
        if (!raw.equals(cachedWhiteRaw)) {
            cachedWhite = BlockList.parse(raw);
            cachedWhiteRaw = raw;
        }
        return cachedWhite;
    }

    public static synchronized BlockList blacklist() {
        String raw = GadgetsConfig.rotatorSwapBlacklist;
        if (!raw.equals(cachedBlackRaw)) {
            cachedBlack = BlockList.parse(raw);
            cachedBlackRaw = raw;
        }
        return cachedBlack;
    }

    // ---------------------------------------------------------------------------------- rules

    /** The block the off-hand stack places, or null if it is not a placeable block. */
    public static @Nullable BlockItem placeableOf(ItemStack offhand) {
        return !offhand.isEmpty() && offhand.getItem() instanceof BlockItem item ? item : null;
    }

    /** Whether the swap is on, the off hand holds a block and the clicked block may be replaced by it. */
    public static boolean canSwap(Level level, BlockPos pos, ItemStack offhand) {
        BlockItem item = placeableOf(offhand);
        if (!enabled() || item == null || !item.getBlock().isEnabled(level.enabledFeatures())) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.is(item.getBlock()) && canSwapTarget(level, pos, state);
    }

    /** The rules for the clicked block (see the class comment). */
    public static boolean canSwapTarget(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || RotatorItem.isLockedPistonPart(state)
                || isHalfOfDoubleBlock(state) || isValuable(state)) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            return false;
        }
        if (!blockEntityAllows(level, pos)) {
            return false;
        }
        if (blacklist().matches(state)) {
            return false;
        }
        if (whitelist().matches(state)) {
            return true;
        }
        return hardness <= maxHardness() && stoneToolHarvests(state);
    }

    private static boolean isHalfOfDoubleBlock(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)
                || (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE);
    }

    /** Blocks that stay even when a list or the mining rule would allow them. */
    public static boolean isValuable(BlockState state) {
        return state.is(Blocks.DRAGON_EGG) || state.is(Blocks.BUDDING_AMETHYST) || state.is(Blocks.REINFORCED_DEEPSLATE)
                || state.getBlock() instanceof InfestedBlock;
    }

    /** No block entity, or an empty container whose loot table is not waiting to be rolled. */
    private static boolean blockEntityAllows(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return true;
        }
        if (!(entity instanceof Container container)) {
            return false;
        }
        if (entity instanceof RandomizableContainer random && random.getLootTable() != null) {
            return false;
        }
        return container.isEmpty();
    }

    /** True if the block needs no correct tool, or a stone pickaxe, axe, shovel or hoe is the correct one. */
    private static boolean stoneToolHarvests(BlockState state) {
        return harvestingStoneTool(state) != null;
    }

    private static @Nullable ItemStack harvestingStoneTool(BlockState state) {
        if (!state.requiresCorrectToolForDrops()) {
            return STONE_TOOLS[0]; // any tool harvests it (the tool rules only list blocks that need one)
        }
        for (ItemStack tool : STONE_TOOLS) {
            if (tool.isCorrectToolForDrops(state)) {
                return tool;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------- timing

    /** Efficiency level on the Rotator. */
    public static int efficiency(Level level, ItemStack rotator) {
        var enchantments = level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (enchantments.isEmpty()) {
            return 0;
        }
        var holder = enchantments.get().get(Enchantments.EFFICIENCY);
        return holder.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder.get(), rotator);
    }

    /**
     * Wind-up before a swap, in ticks: {@code (base + 4 per hardness point) * (5 - Efficiency) / 5},
     * so Efficiency V swaps at once and every level below takes a fifth less. Creative swaps at once.
     * The hardness term keeps the fitting tool plus a placement ahead of the Rotator on hard blocks.
     */
    public static int windupTicks(Level level, Player player, ItemStack rotator, BlockPos pos) {
        int base = baseWindupTicks();
        int efficiency = efficiency(level, rotator);
        if (base <= 0 || efficiency >= INSTANT_EFFICIENCY || player.hasInfiniteMaterials()) {
            return 0;
        }
        float hardness = Math.max(0.0F, level.getBlockState(pos).getDestroySpeed(level, pos));
        double total = base + HARDNESS_TICKS_PER_POINT * (double) hardness;
        return (int) Math.round(total * (INSTANT_EFFICIENCY - efficiency) / INSTANT_EFFICIENCY);
    }

    // ---------------------------------------------------------------------------------- action

    /** A place context that always replaces the block at the clicked position. */
    private static final class SwapContext extends BlockPlaceContext {
        SwapContext(Player player, ItemStack stack, BlockHitResult hit) {
            super(player, InteractionHand.OFF_HAND, stack, hit);
            this.replaceClicked = true;
        }
    }

    /**
     * Swaps the block at {@code pos} for the off-hand block: the old block's drops go to the inventory
     * (a stack that does not fit drops at the block), the off-hand block is placed as the player's
     * placement would be ({@code BlockItem#place}: orientation, sound, advancement, backpack refill),
     * the charge is drained. Atomic: when the placement is refused nothing changes and nothing is paid.
     * The caller checked the permissions ({@link com.simplegadgets.GadgetProtection#mayTransform}) and the charge.
     */
    public static boolean perform(ServerLevel level, Player player, BlockPos pos, Direction face, Vec3 hitLocation,
                                  ItemStack rotator) {
        ItemStack offhand = player.getOffhandItem();
        if (!canSwap(level, pos, offhand)) {
            return false;
        }
        BlockState old = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        List<ItemStack> drops = player.hasInfiniteMaterials() ? List.of() : drops(level, player, pos, old, entity);

        BlockPlaceContext context = new SwapContext(player, offhand, new BlockHitResult(hitLocation, face, pos, false));
        InteractionResult result = ((BlockItem) offhand.getItem()).place(context);
        if (!result.consumesAction()) {
            return false;
        }
        level.levelEvent(2001, pos, Block.getId(old)); // break sound and particles of the old block
        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop) && !drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        RotatorItem.drain(player, rotator, COST_FACTOR * RotatorItem.useCost());
        RotatorItem.swapEffects(level, player, pos);
        return true;
    }

    /**
     * What the old block gives: its loot as if a stone tool had mined it, and the block itself if
     * that is nothing (glass, ice, ...), so no building block is lost.
     */
    private static List<ItemStack> drops(ServerLevel level, Player player, BlockPos pos, BlockState state,
                                         @Nullable BlockEntity entity) {
        ItemStack tool = harvestingStoneTool(state);
        List<ItemStack> loot = Block.getDrops(state, level, pos, entity, player, tool == null ? STONE_TOOLS[0] : tool);
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : loot) {
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            }
        }
        if (result.isEmpty() && state.getBlock().asItem() != Items.AIR) {
            result.add(new ItemStack(state.getBlock().asItem()));
        }
        return result;
    }
}
