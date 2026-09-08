package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.StripMinerUsageEvent;
import com.simplebuilding.util.VeinMinerUsageEvent;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Vein Miner and Strip Miner as a player actually meets them: the enchantment sits on the tool,
 * the block break hook fires, blocks disappear from the world and their drops land on the floor.
 *
 * <p>{@link ToolBehaviourTests} already covers the two <em>selection</em> helpers in
 * {@code MiningUtils} - it calls them directly, with an unenchanted pickaxe and an explicit
 * level argument. That proves which blocks would be highlighted; it proves nothing about the
 * path that runs in play. Everything between "the enchantment is on the tool" and "the block is
 * gone" was untested: the sneak gate, reading the level off the stack, the ore/log gate, the
 * harvest-tier gate, the hooks' own block search and mining direction (both hooks carry a second,
 * independent copy of the code in {@code MiningUtils}), the actual {@code destroyBlock} calls,
 * the drops, and the net durability a tunnel costs.
 *
 * <p>Both hooks are plain {@code static} methods in the shared tree - the loader hooks
 * (Fabric's {@code PlayerBlockBreakEvents.BEFORE}, NeoForge's/Forge's {@code BlockEvent}) do
 * nothing but forward to them - so the whole path is reachable from here without a single
 * loader import, exactly like {@code SledgehammerUsageEvent} already is. The {@code destroyBlock}
 * calls the hooks make re-enter that same loader event in a real run; the hooks' own
 * {@code MINED_BLOCKS}/{@code MINING_BLOCKS} guards are what stops the recursion, and this test
 * exercises them because it drives the real vanilla break.
 *
 * <p><b>Why the drops and the durability are reachable at all.</b> The gametest mock player is
 * hard wired to creative through {@code gameMode()}, so anything behind
 * {@code !player.isCreative()} is dead in this harness. The vanilla break is <em>not</em> behind
 * that check - verified against the 26.2 jar:
 * {@code ServerPlayerGameMode#destroyBlock} returns early from the loot half on
 * {@code Player#preventsBlockDrops()}, which is a bare read of
 * {@code abilities.instabuild}, and {@code ItemStack#processDurabilityChange} bails on
 * {@code Player#hasInfiniteMaterials()}, which reads the very same field. Clearing
 * {@code instabuild} on the mock player therefore opens both halves, which is what makes these
 * tests possible.
 */
public final class VeinAndStripMinerTests {

    /** Tick budget for the tests that let the dropped items settle before counting them. */
    public static final int DROP_MAX_TICKS = 60;

    /** Ticks to wait before looking for drops - the same delay {@link DataIntegrityTests} uses. */
    private static final int DROP_SETTLE_TICKS = 3;

    // --- Vein Miner, ore cluster (test 1) -------------------------------------------------
    private static final BlockPos ORE_ORIGIN = new BlockPos(3, 1, 3);
    /** Connected to {@link #ORE_ORIGIN}, diagonals included; the hook must take all of them. */
    private static final List<BlockPos> ORE_TAIL = List.of(
            new BlockPos(4, 1, 3),
            new BlockPos(4, 1, 4),
            new BlockPos(3, 1, 5),
            new BlockPos(2, 1, 2));
    /**
     * Hangs off {@link #ORE_TAIL}'s (2,1,2) by a step that changes x, y and z at once - the only
     * ore in the cluster that needs a true 3D diagonal <em>upwards</em>, and the stepping stone
     * {@link #DESCENDING_ORE} hangs under. Everything else in {@link #ORE_TAIL} lies in the y=1
     * plane and the log trunk is a plain column, so without this block the flood fill could be
     * cut down to the 8 horizontal diagonals plus the 2 vertical faces and no test in the suite
     * would notice. Real ore blobs step diagonally in height all the time.
     */
    private static final BlockPos DIAGONAL_ORE = new BlockPos(1, 2, 1);
    /**
     * Hangs off {@link #DIAGONAL_ORE} by the mirror image of that step: one down, and x and z
     * with it. It is the only ore the flood fill cannot reach without a <em>negative</em> y
     * offset, and it is here because every other block these tests hand the search - the rest of
     * the cluster, the trunk, the gate clusters - sits at or above the block the player struck.
     * Narrowing the neighbour loop to {@code y = 0..1} takes nine of the 26 neighbours away,
     * every downward one among them, and left the whole suite green while in play a vein that
     * continues below the struck block - the normal shape of an ore blob - was cut off.
     */
    private static final BlockPos DESCENDING_ORE = new BlockPos(0, 1, 0);
    /** Every ore the flood fill may reach from {@link #ORE_ORIGIN}. */
    private static final List<BlockPos> ORE_CLUSTER_EXTRAS =
            Stream.concat(ORE_TAIL.stream(), Stream.of(DIAGONAL_ORE, DESCENDING_ORE)).toList();
    /**
     * Touching the origin, same ore, different block. The hooks match on
     * {@code neighborState.getBlock() == targetState.getBlock()}, so a deepslate variant is a
     * different vein - deliberately, see the comment in {@code MiningUtils}.
     */
    private static final BlockPos DEEPSLATE_NEIGHBOUR = new BlockPos(3, 1, 2);
    /** Same block, but not connected - reaching it would mean the flood fill leaks. */
    private static final BlockPos DETACHED_ORE = new BlockPos(6, 1, 6);

    // --- Vein Miner, log cluster (test 2) --------------------------------------------------
    private static final BlockPos LOG_ORIGIN = new BlockPos(2, 1, 2);
    private static final List<BlockPos> LOG_TAIL = List.of(
            new BlockPos(2, 2, 2),
            new BlockPos(2, 3, 2),
            new BlockPos(2, 4, 2));
    private static final BlockPos DETACHED_LOG = new BlockPos(5, 1, 5);
    /** Axe food, but not a log: the second half of the axe gate. */
    private static final BlockPos PLANK_ORIGIN = new BlockPos(5, 1, 1);
    private static final List<BlockPos> PLANK_NEIGHBOURS = List.of(
            new BlockPos(6, 1, 1),
            new BlockPos(5, 1, 2));

    // --- Vein Miner, gates (test 3) --------------------------------------------------------
    private static final BlockPos STONE_ORIGIN = new BlockPos(2, 1, 2);
    private static final List<BlockPos> STONE_NEIGHBOURS = List.of(
            new BlockPos(3, 1, 2),
            new BlockPos(2, 1, 3));
    private static final BlockPos QUARTZ_ORIGIN = new BlockPos(5, 1, 5);
    private static final List<BlockPos> QUARTZ_NEIGHBOURS = List.of(
            new BlockPos(6, 1, 5),
            new BlockPos(5, 1, 6));
    private static final BlockPos DIAMOND_ORIGIN = new BlockPos(1, 4, 1);
    private static final List<BlockPos> DIAMOND_NEIGHBOURS = List.of(
            new BlockPos(2, 4, 1),
            new BlockPos(1, 4, 2));

    // --- Strip Miner, vertical shaft (test 4) ----------------------------------------------
    private static final BlockPos SHAFT_ORIGIN = new BlockPos(3, 5, 3);
    /** Straight down from the origin, in mining order; Strip Miner III reaches all four. */
    private static final List<BlockPos> SHAFT = List.of(
            new BlockPos(3, 4, 3),
            new BlockPos(3, 3, 3),
            new BlockPos(3, 2, 3),
            new BlockPos(3, 1, 3));
    /** One block past the level III depth of 4; it has to survive. */
    private static final BlockPos SHAFT_FLOOR = new BlockPos(3, 0, 3);

    // --- Strip Miner, horizontal tunnel (test 4) -------------------------------------------
    private static final BlockPos TUNNEL_ORIGIN = new BlockPos(2, 6, 1);
    /** Two stone blocks south of the origin; level III would take four if nothing stopped it. */
    private static final List<BlockPos> TUNNEL = List.of(
            new BlockPos(2, 6, 2),
            new BlockPos(2, 6, 3));
    /** A pickaxe cannot harvest dirt, so the tunnel ends here. */
    private static final BlockPos TUNNEL_PLUG = new BlockPos(2, 6, 4);
    /** Behind the plug, inside the level III reach: only a broken stop condition gets here. */
    private static final BlockPos TUNNEL_BEYOND = new BlockPos(2, 6, 5);

    // --- Strip Miner, level II depth (test 4) ----------------------------------------------
    /**
     * The depth Strip Miner II digs. The table is {@code (level == 3) ? 4 : level}, and level I
     * and level III alone pin only its two ends: with nothing driving the middle, level II could
     * be bent to any depth at all and stay green, while it is a perfectly ordinary level a player
     * reaches (the enchantment's max level is 3).
     */
    private static final int LEVEL_TWO_DEPTH = 2;

    // --- Strip Miner, the lower half of the mining direction (test 4) ----------------------
    /**
     * Two probes for the {@code pitch > 60 -> DOWN} branch, one just past the threshold and one
     * exactly on it. Every downward case in the suite looks straight down at pitch 90, so the
     * threshold could be pushed all the way up to 89 - or dragged down to 30 - without a single
     * assertion moving, while a player looking down at 70 degrees suddenly tunnelled sideways.
     * Each probe is a block the hook is asked to break plus one block under it; the shallow probe
     * has a third block to its south so that "not down" is pinned as "along the facing" and not
     * merely as "nothing happened".
     */
    private static final BlockPos STEEP_PROBE_ORIGIN = new BlockPos(0, 4, 0);
    private static final BlockPos STEEP_PROBE_BELOW = new BlockPos(0, 3, 0);
    private static final BlockPos SHALLOW_PROBE_ORIGIN = new BlockPos(0, 4, 5);
    private static final BlockPos SHALLOW_PROBE_BELOW = new BlockPos(0, 3, 5);
    private static final BlockPos SHALLOW_PROBE_SOUTH = new BlockPos(0, 4, 6);
    /** Just past {@code pitch > 60}: the hook has to dig downwards here. */
    private static final float STEEP_PITCH = 61.0F;
    /** Exactly on the threshold, which is exclusive: the hook has to follow the facing instead. */
    private static final float SHALLOW_PITCH = 60.0F;
    /** Stone the two probe runs break between them - one under the steep, one south of the shallow. */
    private static final int PITCH_PROBE_BLOCKS = 2;

    private VeinAndStripMinerTests() {
    }

    // =====================================================================================
    // VEIN MINER
    // =====================================================================================

    /**
     * The whole path for a pickaxe: an enchanted pickaxe, a sneaking player, the break hook, a
     * coal vein that is gone afterwards and one piece of coal per broken block on the floor.
     *
     * <p>The cluster deliberately needs every kind of step the 26 neighbour search offers: face
     * neighbours, horizontal diagonals, one step that changes x, y and z at the same time
     * ({@link #DIAGONAL_ORE}), and - through {@link #DESCENDING_ORE} - one that does so while
     * going <em>down</em>, which is the half of the search everything else in the suite leaves
     * unused.
     *
     * <p>What breaks it: dropping the sneak gate (case 1 would then mine), reading the Vein
     * Miner level from somewhere other than the held stack (case 2 would then mine nothing),
     * letting the flood fill escape the cluster ({@link #DETACHED_ORE} would go), matching
     * neighbours by tag instead of by block ({@link #DEEPSLATE_NEIGHBOUR} would go, and the coal
     * count would rise with it), narrowing the neighbour search to the flat ring plus the two
     * vertical faces ({@link #DIAGONAL_ORE} would survive), cutting the downward half of that
     * search away ({@link #DESCENDING_ORE} would survive), losing the per level budget (case 4
     * would take the whole vein instead of two blocks), and - the part nothing else in the suite
     * watches - breaking the
     * blocks in a way that yields no items, for instance by swapping
     * {@code serverPlayer.gameMode.destroyBlock} for a bare {@code setBlock(AIR)}.
     */
    public static void veinMinerBreaksTheWholeVeinThroughTheBlockBreakEvent(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 0.0F);

        // --- 1. sneaking, but the pickaxe carries no enchantment: nothing may happen ---
        buildOreVein(helper);
        player.setShiftKeyDown(true);
        veinMine(helper, player, new ItemStack(Items.IRON_PICKAXE), ORE_ORIGIN);
        assertOreVeinIntact(helper, "an unenchanted pickaxe vein mined anyway");

        // --- 2. enchanted, but standing upright: the sneak gate has to hold ---
        player.setShiftKeyDown(false);
        veinMine(helper, player, veinMinerPickaxe(helper, Items.IRON_PICKAXE, 5), ORE_ORIGIN);
        assertOreVeinIntact(helper, "Vein Miner fired without the player sneaking");

        // --- 3. the real thing: level V has a budget of 18, the cluster is 7 blocks ---
        player.setShiftKeyDown(true);
        veinMine(helper, player, veinMinerPickaxe(helper, Items.IRON_PICKAXE, 5), ORE_ORIGIN);

        for (BlockPos pos : ORE_CLUSTER_EXTRAS) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        // The origin is vanilla's block to break; the hook only handles the rest. Leaving it
        // standing is how a hook that destroys the origin a second time becomes visible.
        helper.assertBlockPresent(Blocks.COAL_ORE, ORE_ORIGIN);
        helper.assertBlockPresent(Blocks.COAL_ORE, DETACHED_ORE);
        helper.assertBlockPresent(Blocks.DEEPSLATE_COAL_ORE, DEEPSLATE_NEIGHBOUR);

        // --- 4. level I has a budget of 3 blocks including the origin -> exactly 2 extra ---
        buildOreVein(helper);
        veinMine(helper, player, veinMinerPickaxe(helper, Items.IRON_PICKAXE, 1), ORE_ORIGIN);
        Assertions.valueEqual(helper, brokenCount(helper, ORE_CLUSTER_EXTRAS), 2,
                "Vein Miner block budget at level I, driven through the break event");
        helper.assertBlockPresent(Blocks.COAL_ORE, DETACHED_ORE);
        helper.assertBlockPresent(Blocks.DEEPSLATE_COAL_ORE, DEEPSLATE_NEIGHBOUR);

        // 6 blocks in run 3 plus 2 in run 4, and coal ore drops exactly one coal without
        // fortune, so anything less means the breaks did not produce real loot.
        helper.runAfterDelay(DROP_SETTLE_TICKS, () -> {
            Assertions.valueEqual(helper, droppedCount(helper, Items.COAL),
                    ORE_CLUSTER_EXTRAS.size() + 2,
                    "coal dropped by the vein mined ore");
            TestCleanup.succeed(helper);
        });
    }

    /**
     * The same hook with an axe: Vein Miner is enchantable onto axes too, and there the vein is
     * a tree trunk rather than an ore cluster. This is a separate branch of the hook
     * ({@code isAxe && !state.is(BlockTags.LOGS)}) that no other test in the suite enters -
     * {@link ToolBehaviourTests} only ever hands the selection helper a pickaxe.
     *
     * <p>The planks are the negative half. An axe harvests them correctly, so the tool check and
     * the harvest check both pass and only the {@code LOGS} tag stands between the player and a
     * hook that eats a wall.
     *
     * <p>What breaks it: narrowing the tool gate back to pickaxes, swapping the log tag for the
     * ore check (the trunk would then survive), dropping the log tag entirely (the planks would
     * go), and a flood fill that only walks horizontally - the trunk is stacked on the vertical
     * axis on purpose.
     */
    public static void veinMinerFollowsLogsWithAnAxeThroughTheBlockBreakEvent(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 3.0, 4.5), 0.0F, 0.0F);
        player.setShiftKeyDown(true);

        buildLogTrunk(helper);
        veinMine(helper, player, veinMinerAxe(helper, 5), LOG_ORIGIN);

        for (BlockPos pos : LOG_TAIL) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        helper.assertBlockPresent(Blocks.OAK_LOG, LOG_ORIGIN);
        helper.assertBlockPresent(Blocks.OAK_LOG, DETACHED_LOG);

        // --- planks: an axe block, but not a log ---
        veinMine(helper, player, veinMinerAxe(helper, 5), PLANK_ORIGIN);
        for (BlockPos pos : PLANK_NEIGHBOURS) {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, pos);
        }

        helper.runAfterDelay(DROP_SETTLE_TICKS, () -> {
            Assertions.valueEqual(helper, droppedCount(helper, Items.OAK_LOG), LOG_TAIL.size(),
                    "logs dropped by the vein mined trunk");
            TestCleanup.succeed(helper);
        });
    }

    /**
     * The three gates in front of the pickaxe branch, each from the side that actually proves
     * something.
     *
     * <p><b>Plain stone</b> is the side the ore check carries alone: a pickaxe may harvest it, so
     * the tool check and the harvest check both pass and only {@code isOre} stands between the
     * player and a hook that would eat the floor.
     *
     * <p><b>Diamond ore with a stone pickaxe</b> is the harvest gate
     * ({@code isCorrectToolForDrops}). Diamond ore sits in {@code minecraft:needs_iron_tool}, so a
     * stone pickaxe must not vein mine it - without that check Vein Miner would hand a stone
     * pickaxe a whole diamond vein for free. The same cluster is then taken with an iron pickaxe,
     * so the negative half cannot pass just because the layout was wrong.
     *
     * <p><b>Nether quartz ore</b> is where the preview and the mining used to disagree:
     * {@code MiningUtils#isOre} counted nether quartz ore and ancient debris by hand while the
     * hook carried its own copy that listed neither, so a player saw the whole quartz vein
     * outlined and then broke a single block. There is only one list now -
     * {@code VeinMinerUsageEvent} asks {@code MiningUtils#isOre} - and the manual decides which
     * way it points: the eight ore tags are the rule of Vein Miner, so the preview is the side
     * that gave way. This case therefore holds both ends together: the ore check says no, the
     * preview selects nothing, and the vein stays in the world. Putting quartz back into the list
     * is a balance change, not a repair, and it turns all three of those assertions red at once.
     * (Ancient debris, the other block that was in the preview list only, is covered the same way
     * by {@code MiningEnchantmentTests}.)
     */
    public static void veinMinerRefusesNonOresAndTooWeakPickaxesAndDivergesFromTheHighlightOnQuartz(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 3.0, 4.5), 0.0F, 0.0F);
        player.setShiftKeyDown(true);

        // --- plain stone: harvestable, but not an ore ---
        helper.setBlock(STONE_ORIGIN, Blocks.STONE);
        for (BlockPos pos : STONE_NEIGHBOURS) {
            helper.setBlock(pos, Blocks.STONE);
        }
        veinMine(helper, player, veinMinerPickaxe(helper, Items.IRON_PICKAXE, 5), STONE_ORIGIN);
        for (BlockPos pos : STONE_NEIGHBOURS) {
            helper.assertBlockPresent(Blocks.STONE, pos);
        }

        // --- diamond ore: an ore, but a stone pickaxe may not harvest it ---
        helper.setBlock(DIAMOND_ORIGIN, Blocks.DIAMOND_ORE);
        for (BlockPos pos : DIAMOND_NEIGHBOURS) {
            helper.setBlock(pos, Blocks.DIAMOND_ORE);
        }
        veinMine(helper, player, veinMinerPickaxe(helper, Items.STONE_PICKAXE, 5), DIAMOND_ORIGIN);
        for (BlockPos pos : DIAMOND_NEIGHBOURS) {
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, pos);
        }
        // Positive control on the very same cluster: with an iron pickaxe it does go, so the
        // assertion above is about the tool tier and not about a mislaid block.
        veinMine(helper, player, veinMinerPickaxe(helper, Items.IRON_PICKAXE, 5), DIAMOND_ORIGIN);
        for (BlockPos pos : DIAMOND_NEIGHBOURS) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }

        // --- nether quartz ore: outside the ore list, and the preview says so too ---
        helper.setBlock(QUARTZ_ORIGIN, Blocks.NETHER_QUARTZ_ORE);
        for (BlockPos pos : QUARTZ_NEIGHBOURS) {
            helper.setBlock(pos, Blocks.NETHER_QUARTZ_ORE);
        }

        helper.assertTrue(!MiningUtils.isOre(helper.getBlockState(QUARTZ_ORIGIN)),
                "nether quartz ore is back in the one ore list - the preview would outline a "
                        + "vein again, and the hook would now break it");

        List<BlockPos> highlighted = MiningUtils.getVeinMinerBlocks(
                helper.getLevel(),
                helper.absolutePos(QUARTZ_ORIGIN),
                helper.getBlockState(QUARTZ_ORIGIN),
                5,
                new ItemStack(Items.IRON_PICKAXE));
        Assertions.valueEqual(helper, highlighted.size(), 0,
                "the crack preview outlined a quartz vein the hook does not mine - the two ore "
                        + "lists have drifted apart again");

        veinMine(helper, player, veinMinerPickaxe(helper, Items.IRON_PICKAXE, 5), QUARTZ_ORIGIN);
        for (BlockPos pos : QUARTZ_NEIGHBOURS) {
            helper.assertBlockPresent(Blocks.NETHER_QUARTZ_ORE, pos);
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // STRIP MINER
    // =====================================================================================

    /**
     * Strip Miner through the break hook, in both directions it can dig, plus what the tunnel
     * really costs the tool.
     *
     * <p>Looking straight down, level I digs one block, level II two and level III four, and each
     * stops there. All three levels are driven on purpose: the depth table is
     * {@code (level == 3) ? 4 : level}, and level I and level III between them pin only its two
     * ends, leaving the middle free to be bent to any depth in silence.
     *
     * <p>Looking level and facing south, the tunnel follows the facing and stops at a block the
     * pickaxe cannot harvest -
     * {@code StripMinerUsageEvent} has its <em>own</em> private copy of
     * {@code getMiningDirection} and of the stop conditions, so
     * {@link ToolBehaviourTests#stripMinerFollowsPlayerFacingAndStopsAtGaps} (which asks
     * {@code MiningUtils}) would stay green if that copy were inverted and the mod dug upwards.
     *
     * <p>Where the downward branch <em>starts</em> is measured too, and not from pitch 90. Every
     * other downward case in the repo looks straight down, 30 degrees clear of the decision, so
     * {@code pitch > 60} could be pushed to 89 or dragged down to 30 with nothing turning red.
     * The two probes sit one degree apart around the threshold: at 61 the hook has to dig down,
     * at 60 - the threshold is exclusive - it has to dig south instead.
     *
     * <p>The per block durability cost is measured in the level I run rather than hard coded, so
     * the refund assertions test the mod's formula and not vanilla's tool damage: level I breaks
     * one block and earns a refund of {@code (1 + 1) / 3 == 0}, level II breaks two and earns
     * {@code (2 + 1) / 3 == 1}, level III breaks four and earns {@code (4 + 1) / 3 == 1}, the
     * plugged tunnel breaks two and earns {@code (2 + 1) / 3 == 1}.
     * Unlike {@code ConsumptionAndDurabilityTests}, which isolates the refund by putting the
     * player in {@code instabuild} (switching vanilla's own wear off), this player pays for the
     * blocks, so what is asserted here is the <em>net</em> damage a real pickaxe ends up with.
     *
     * <p>What breaks it: dropping the sneak gate or the enchantment lookup (cases 1 and 2 would
     * then dig), a depth that no longer maps level III to 4 ({@link #SHAFT_FLOOR} would go, or
     * the shaft would come up short), a depth that no longer maps level II to 2 (case 3b would
     * dig past {@link #LEVEL_TWO_DEPTH}), a broken mining direction (case 5 would leave the
     * tunnel standing), a downward threshold that has moved off 60 (one of the two probes in
     * case 4a would answer wrongly), a lost stop condition ({@link #TUNNEL_PLUG} and
     * {@link #TUNNEL_BEYOND} would go), removing the durability refund (the tool would take the
     * full four points), and again a break that produces no drops.
     */
    public static void stripMinerTunnelsAlongTheFacingAndRefundsDurabilityThroughTheBlockBreakEvent(GameTestHelper helper) {
        fillFloor(helper);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 6.0, 3.5), 0.0F, 90.0F);

        // --- 1. enchanted, but standing upright ---
        buildShaft(helper);
        player.setShiftKeyDown(false);
        stripMine(helper, player, stripMinerPickaxe(helper, 3), SHAFT_ORIGIN);
        assertShaftIntact(helper, "Strip Miner fired without the player sneaking");

        // --- 2. sneaking, but no enchantment on the pickaxe ---
        player.setShiftKeyDown(true);
        stripMine(helper, player, new ItemStack(Items.IRON_PICKAXE), SHAFT_ORIGIN);
        assertShaftIntact(helper, "an unenchanted pickaxe dug a tunnel anyway");

        // --- 3. level I: one block, and the yardstick for what one block costs the tool ---
        stripMine(helper, player, stripMinerPickaxe(helper, 1), SHAFT_ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, SHAFT.get(0));
        helper.assertBlockPresent(Blocks.STONE, SHAFT.get(1));
        int damagePerBlock = player.getMainHandItem().getDamageValue();
        helper.assertTrue(damagePerBlock > 0,
                "breaking a block through the Strip Miner hook cost the pickaxe no durability at "
                        + "all, so the refund below cannot be measured");

        // --- 3b. level II: the middle of the depth table, which neither other run touches ---
        buildShaft(helper);
        stripMine(helper, player, stripMinerPickaxe(helper, 2), SHAFT_ORIGIN);
        for (int i = 0; i < SHAFT.size(); i++) {
            if (i < LEVEL_TWO_DEPTH) {
                helper.assertBlockPresent(Blocks.AIR, SHAFT.get(i));
            } else {
                helper.assertBlockPresent(Blocks.STONE, SHAFT.get(i));
            }
        }
        Assertions.valueEqual(helper, player.getMainHandItem().getDamageValue(),
                LEVEL_TWO_DEPTH * damagePerBlock - 1,
                "Strip Miner durability refund for the level II tunnel");

        // --- 4. level III: depth 4, no further, and one point of damage refunded ---
        buildShaft(helper);
        stripMine(helper, player, stripMinerPickaxe(helper, 3), SHAFT_ORIGIN);
        for (BlockPos pos : SHAFT) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        helper.assertBlockPresent(Blocks.STONE, SHAFT_ORIGIN);
        helper.assertBlockPresent(Blocks.STONE, SHAFT_FLOOR);
        Assertions.valueEqual(helper, player.getMainHandItem().getDamageValue(), 4 * damagePerBlock - 1,
                "Strip Miner durability refund for a four block tunnel");

        // --- 4a. the downward threshold, measured from both sides instead of from pitch 90 ---
        // Every other downward case in the suite stands on pitch 90, which is 30 degrees clear of
        // the decision, so the threshold itself was free to move in either direction.
        buildPitchProbes(helper);
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, STEEP_PITCH);
        Assertions.valueEqual(helper, MiningUtils.getMiningDirection(player), Direction.DOWN,
                "MiningUtils stopped digging downwards just past pitch 60, so the preview and the "
                        + "hook would point in different directions");
        stripMine(helper, player, stripMinerPickaxe(helper, 1), STEEP_PROBE_ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, STEEP_PROBE_BELOW);

        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, SHALLOW_PITCH);
        Assertions.valueEqual(helper, MiningUtils.getMiningDirection(player), Direction.SOUTH,
                "MiningUtils started digging downwards at pitch 60 itself, so the preview and the "
                        + "hook would point in different directions");
        stripMine(helper, player, stripMinerPickaxe(helper, 1), SHALLOW_PROBE_ORIGIN);
        helper.assertBlockPresent(Blocks.STONE, SHALLOW_PROBE_BELOW);
        helper.assertBlockPresent(Blocks.AIR, SHALLOW_PROBE_SOUTH);

        // --- 5. looking level: the tunnel follows the facing and stops at the dirt plug ---
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        helper.assertTrue(player.getDirection() == Direction.SOUTH,
                "the mock player is not facing south, so this case would not test the mining "
                        + "direction any more");
        buildTunnel(helper);
        stripMine(helper, player, stripMinerPickaxe(helper, 3), TUNNEL_ORIGIN);
        for (BlockPos pos : TUNNEL) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        helper.assertBlockPresent(Blocks.STONE, TUNNEL_ORIGIN);
        helper.assertBlockPresent(Blocks.DIRT, TUNNEL_PLUG);
        helper.assertBlockPresent(Blocks.STONE, TUNNEL_BEYOND);
        Assertions.valueEqual(helper, player.getMainHandItem().getDamageValue(), 2 * damagePerBlock - 1,
                "Strip Miner durability refund for a two block tunnel");

        // 1 block in run 3, 2 in run 3b, 4 in run 4, one under each probe in run 4a, 2 in run 5;
        // stone drops exactly one cobblestone each, and the dirt plug would show up as one more
        // if the stop condition ever went away.
        helper.runAfterDelay(DROP_SETTLE_TICKS, () -> {
            Assertions.valueEqual(helper, droppedCount(helper, Items.COBBLESTONE),
                    1 + LEVEL_TWO_DEPTH + SHAFT.size() + PITCH_PROBE_BLOCKS + TUNNEL.size(),
                    "cobblestone dropped by the strip mined tunnels");
            TestCleanup.succeed(helper);
        });
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * Creates a mock server player, points it where the test needs it and hands it back to the
     * server when the test ends - a leaked mock player keeps the player list non-empty and the
     * gametest server then stalls on shutdown.
     *
     * <p>Clearing {@code instabuild} is load bearing, not tidiness: see the class javadoc. It is
     * the one switch that makes the vanilla break produce loot and consume durability, and both
     * are what these tests assert.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relativePos, float yRot, float xRot) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, yRot, xRot);
        player.getAbilities().instabuild = false;
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Runs the Vein Miner hook for {@code relativeOrigin}, with exactly the arguments the
     * loader hooks pass: the level, the breaking player, the position, its state and its block
     * entity.
     */
    private static void veinMine(GameTestHelper helper, ServerPlayer player, ItemStack tool, BlockPos relativeOrigin) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos origin = helper.absolutePos(relativeOrigin);
        VeinMinerUsageEvent.handleBeforeBlockBreak(
                helper.getLevel(), player, origin,
                helper.getLevel().getBlockState(origin),
                helper.getLevel().getBlockEntity(origin));
    }

    /** The same for the Strip Miner hook. */
    private static void stripMine(GameTestHelper helper, ServerPlayer player, ItemStack tool, BlockPos relativeOrigin) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos origin = helper.absolutePos(relativeOrigin);
        StripMinerUsageEvent.handleBeforeBlockBreak(
                helper.getLevel(), player, origin,
                helper.getLevel().getBlockState(origin),
                helper.getLevel().getBlockEntity(origin));
    }

    private static void buildOreVein(GameTestHelper helper) {
        helper.setBlock(ORE_ORIGIN, Blocks.COAL_ORE);
        for (BlockPos pos : ORE_CLUSTER_EXTRAS) {
            helper.setBlock(pos, Blocks.COAL_ORE);
        }
        helper.setBlock(DEEPSLATE_NEIGHBOUR, Blocks.DEEPSLATE_COAL_ORE);
        helper.setBlock(DETACHED_ORE, Blocks.COAL_ORE);
    }

    private static void assertOreVeinIntact(GameTestHelper helper, String message) {
        helper.assertTrue(helper.getBlockState(ORE_ORIGIN).is(Blocks.COAL_ORE), message);
        for (BlockPos pos : ORE_CLUSTER_EXTRAS) {
            helper.assertTrue(helper.getBlockState(pos).is(Blocks.COAL_ORE), message);
        }
    }

    private static void buildLogTrunk(GameTestHelper helper) {
        helper.setBlock(LOG_ORIGIN, Blocks.OAK_LOG);
        for (BlockPos pos : LOG_TAIL) {
            helper.setBlock(pos, Blocks.OAK_LOG);
        }
        helper.setBlock(DETACHED_LOG, Blocks.OAK_LOG);
        helper.setBlock(PLANK_ORIGIN, Blocks.OAK_PLANKS);
        for (BlockPos pos : PLANK_NEIGHBOURS) {
            helper.setBlock(pos, Blocks.OAK_PLANKS);
        }
    }

    private static void buildShaft(GameTestHelper helper) {
        helper.setBlock(SHAFT_ORIGIN, Blocks.STONE);
        for (BlockPos pos : SHAFT) {
            helper.setBlock(pos, Blocks.STONE);
        }
        helper.setBlock(SHAFT_FLOOR, Blocks.STONE);
    }

    private static void assertShaftIntact(GameTestHelper helper, String message) {
        for (BlockPos pos : SHAFT) {
            helper.assertTrue(helper.getBlockState(pos).is(Blocks.STONE), message);
        }
    }

    /**
     * The two mining direction probes. They stand apart from the shaft so that neither of them
     * has to be rebuilt under a dropped item: everything in this test happens inside one tick,
     * and a block put back over a fresh drop is how the drop count starts wandering.
     */
    private static void buildPitchProbes(GameTestHelper helper) {
        helper.setBlock(STEEP_PROBE_ORIGIN, Blocks.STONE);
        helper.setBlock(STEEP_PROBE_BELOW, Blocks.STONE);
        helper.setBlock(SHALLOW_PROBE_ORIGIN, Blocks.STONE);
        helper.setBlock(SHALLOW_PROBE_BELOW, Blocks.STONE);
        helper.setBlock(SHALLOW_PROBE_SOUTH, Blocks.STONE);
    }

    private static void buildTunnel(GameTestHelper helper) {
        helper.setBlock(TUNNEL_ORIGIN, Blocks.STONE);
        for (BlockPos pos : TUNNEL) {
            helper.setBlock(pos, Blocks.STONE);
        }
        helper.setBlock(TUNNEL_PLUG, Blocks.DIRT);
        helper.setBlock(TUNNEL_BEYOND, Blocks.STONE);
    }

    /** How many of {@code positions} the hook turned into air. */
    private static int brokenCount(GameTestHelper helper, List<BlockPos> positions) {
        int broken = 0;
        for (BlockPos pos : positions) {
            if (helper.getBlockState(pos).isAir()) {
                broken++;
            }
        }
        return broken;
    }

    /**
     * Sums up {@code item} over every item entity inside the test structure. Counting items
     * instead of entities is deliberate: dropped items merge while they settle, so the number
     * of entities is not stable but the number of items is.
     */
    private static int droppedCount(GameTestHelper helper, Item item) {
        int total = 0;
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }

    /**
     * Solid ground under the whole room, so the drops stay where they were mined instead of
     * falling through the empty gametest floor while the test waits for them.
     */
    private static void fillFloor(GameTestHelper helper) {
        fillLayer(helper, 0, 0, 7, 0, 7, Blocks.STONE);
    }

    private static void fillLayer(GameTestHelper helper, int y, int minX, int maxX, int minZ, int maxZ, Block block) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, y, z), block);
            }
        }
    }

    private static ItemStack veinMinerPickaxe(GameTestHelper helper, Item pickaxe, int level) {
        return enchanted(helper, new ItemStack(pickaxe), ModEnchantments.VEIN_MINER, level);
    }

    private static ItemStack veinMinerAxe(GameTestHelper helper, int level) {
        return enchanted(helper, new ItemStack(Items.IRON_AXE), ModEnchantments.VEIN_MINER, level);
    }

    private static ItemStack stripMinerPickaxe(GameTestHelper helper, int level) {
        return enchanted(helper, new ItemStack(Items.IRON_PICKAXE), ModEnchantments.STRIP_MINER, level);
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
