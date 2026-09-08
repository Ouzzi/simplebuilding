package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.trade.ModTradeDefinitions;
import com.simplebuilding.trade.ModTradeDefinitions.VillagerTradeGroup;
import com.simplebuilding.trade.TradeDefinition;
import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.StripMinerUsageEvent;
import com.simplebuilding.util.VeinMinerUsageEvent;
import com.simplebuilding.util.VersatilityUsageEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * The six mining enchantments - Vein Miner, Strip Miner, Versatility, and the three the
 * sledgehammer carries (Radius, Break Through, Override) - from the sides the existing suite
 * leaves open: the numbers behind the levels, the gates that decide whether an enchantment
 * fires at all, the tool ranking Versatility applies, and the data that has to ship for any of
 * it to be reachable in a real world.
 *
 * <p><b>What is deliberately not repeated here.</b> {@link VeinAndStripMinerTests} already
 * drives both break hooks end to end (sneak gate, ore/log gate, harvest tier gate, flood fill,
 * the mining direction looking down and south, drops, and the net durability of a tunnel), and
 * {@link ToolBehaviourTests} pins the two selection helpers in {@code MiningUtils}.
 * {@link EnchantmentEffectTests} pins Radius, Break Through and the two Versatility search
 * ranges, and {@link DataIntegrityTests} pins that every mod enchantment loads and that the
 * mining exclusive set makes Vein Miner and Strip Miner incompatible. None of that is
 * re-asserted; every test below picks up where those stop.
 *
 * <p><b>Known defects</b> - pinned around, never written in as expected behaviour:
 * <ul>
 *   <li><b>Two ore lists that disagree.</b> {@code MiningUtils#isOre} - the copy the highlight
 *       preview asks - counts nether quartz ore <em>and</em> ancient debris as ore, while the
 *       private {@code isOre} inside {@code VeinMinerUsageEvent} lists neither. A player sees a
 *       whole ancient debris cluster outlined and then breaks a single block.
 *       {@link VeinAndStripMinerTests} pins the quartz half of that divergence;
 *       {@link #veinMinerAndStripMinerIgnoreToolsAndBlocksOutsideTheirGates} pins the ancient
 *       debris half. Both fail the moment the two lists agree, which is exactly when they
 *       should be rewritten into plain "the vein mines" assertions.</li>
 *   <li><b>The mining pickaxe trade can hand out two mutually exclusive enchantments.</b>
 *       {@code ModTradeDefinitions#miningPool} declares a second chance of {@code 0.1F} over a
 *       pool that holds both Strip Miner and Vein Miner - {@code WeightedPicker#pickOneOrTwo}
 *       only rejects a second pick of the same enchantment - and {@code EnchantmentPool#apply}
 *       writes both picks with {@code ItemEnchantments.Mutable#set}, which performs no
 *       exclusivity check. Roughly one traded pickaxe in ten therefore carries a combination that
 *       {@code simplebuilding:mining_exclusive_set} declares impossible.
 *       {@link #miningPickaxeTradeAlwaysCarriesAnEnchantmentFromItsPool} asserts what the
 *       function is meant to guarantee (every enchantment on the result comes from the pool at
 *       the level the pool declares) and stays silent about the count, so it neither trips over
 *       the defect nor blesses it.</li>
 * </ul>
 *
 * <p><b>Carried by vanilla, not by the mod</b> - documented instead of asserted, because an
 * assertion could not fail. {@code StripMinerUsageEvent} clamps its durability refund with
 * {@code Math.max(0, currentDamage - damageRefund)}, but {@code ItemStack#setDamageValue}
 * already stores {@code Mth.clamp(value, 0, getMaxDamage())} (verified against the 26.2 jar).
 * A test for "a fresh pickaxe never ends up with negative damage" would pass with the mod's
 * clamp deleted, so it is not written.
 *
 * <p><b>Not covered</b> - out of reach for a server side gametest:
 * <ul>
 *   <li>The highlight overlay itself. {@code MiningUtils} only supplies the positions; the
 *       renderer that draws the break outline over them is client only.</li>
 *   <li>{@code ClientboundSetHeldSlotPacket} and the container sync Versatility sends after a
 *       swap. The swap is asserted on the server inventory; whether the client is told is a
 *       packet the harness has no receiver for.</li>
 *   <li>The chisel's own priority step in {@code VersatilityUsageEvent#getToolScore}. Its
 *       {@code +0} differs from the {@code +500} fallback only for an item that is
 *       correct-for-drops on the block yet in none of the pickaxe/axe/shovel tags - and since
 *       26.2 {@code Item#isCorrectToolForDrops} is a pure read of the {@code TOOL} component's
 *       rules, no reachable chisel/block pair produces a speed anywhere near the 500 point gap.
 *       {@link #versatilityPrefersTheHammerAndRanksTheChiselLast} therefore pins what is
 *       reachable: the ladder outranks raw speed in both directions.</li>
 * </ul>
 */
public final class MiningEnchantmentTests {

    // --- Vein Miner budgets (test 1) -------------------------------------------------------
    /** Corner of the 5x4 coal slab; every budget run starts here. */
    private static final BlockPos SLAB_ORIGIN = new BlockPos(1, 1, 1);
    /** The other nineteen blocks of the slab, all connected to {@link #SLAB_ORIGIN}. */
    private static final List<BlockPos> SLAB_TAIL = slabTail();
    /**
     * What Vein Miner V may take beside the origin: the budget of 18 blocks the hook maps level
     * five to, minus the origin vanilla breaks itself. The slab is deliberately larger than
     * this, because a vein that runs out first turns the case into a lower bound only - the
     * budget could then be lowered to the size of the vein without anything going red.
     */
    private static final int LEVEL_V_BUDGET = 17;
    /** A two block vein far away from the slab; breaking it costs exactly one block of wear. */
    private static final BlockPos PAIR_ORIGIN = new BlockPos(6, 5, 6);
    private static final BlockPos PAIR_NEIGHBOUR = new BlockPos(6, 5, 5);
    /**
     * A second two block vein, in a corner nothing else in this class touches, for the case that
     * lets {@code destroyBlock} throw. {@link #GUARD_NEIGHBOUR} is the block the hook is in the
     * middle of taking when the throw happens, so it is the position that would stay behind in
     * {@code MINED_BLOCKS}; the recovery run afterwards starts from exactly there.
     */
    private static final BlockPos GUARD_ORIGIN = new BlockPos(6, 3, 1);
    private static final BlockPos GUARD_NEIGHBOUR = new BlockPos(6, 3, 2);

    // --- tool and block gates (test 2) -----------------------------------------------------
    private static final BlockPos DIRT_ORIGIN = new BlockPos(1, 1, 1);
    private static final List<BlockPos> DIRT_TAIL = List.of(new BlockPos(2, 1, 1), new BlockPos(1, 1, 2));
    private static final BlockPos LOG_ORIGIN = new BlockPos(6, 1, 1);
    private static final List<BlockPos> LOG_TAIL = List.of(new BlockPos(6, 2, 1), new BlockPos(6, 3, 1));
    private static final BlockPos DEBRIS_ORIGIN = new BlockPos(1, 1, 6);
    private static final List<BlockPos> DEBRIS_TAIL = List.of(new BlockPos(2, 1, 6), new BlockPos(1, 1, 5));
    private static final BlockPos DIRT_SHAFT_ORIGIN = new BlockPos(6, 6, 6);
    private static final List<BlockPos> DIRT_SHAFT = List.of(new BlockPos(6, 5, 6), new BlockPos(6, 4, 6));
    private static final BlockPos STONE_SHAFT_ORIGIN = new BlockPos(4, 6, 4);
    private static final List<BlockPos> STONE_SHAFT = List.of(new BlockPos(4, 5, 4), new BlockPos(4, 4, 4));

    // --- the upward branch (test 3) --------------------------------------------------------
    private static final BlockPos COLUMN_ORIGIN = new BlockPos(2, 2, 2);
    /** Straight up from {@link #COLUMN_ORIGIN}; Strip Miner III reaches all four. */
    private static final List<BlockPos> COLUMN = List.of(
            new BlockPos(2, 3, 2),
            new BlockPos(2, 4, 2),
            new BlockPos(2, 5, 2),
            new BlockPos(2, 6, 2));
    /** One block past the level III depth of four; it has to survive. */
    private static final BlockPos COLUMN_CAP = new BlockPos(2, 7, 2);
    private static final BlockPos LEVEL_ORIGIN = new BlockPos(5, 2, 1);
    private static final List<BlockPos> LEVEL_TUNNEL = List.of(new BlockPos(5, 2, 2), new BlockPos(5, 2, 3));
    /** Above {@link #LEVEL_ORIGIN}: reached only if the pitch threshold slipped. */
    private static final BlockPos LEVEL_ROOF = new BlockPos(5, 3, 1);
    /**
     * A three block column in the far corner for the guard case. Mined straight down at level I,
     * so each run takes exactly one block: {@link #GUARD_MIDDLE} is where {@code destroyBlock} is
     * made to throw, and {@link #GUARD_BOTTOM} is what the recovery run from
     * {@link #GUARD_MIDDLE} has to reach.
     */
    private static final BlockPos GUARD_TOP = new BlockPos(7, 3, 7);
    private static final BlockPos GUARD_MIDDLE = new BlockPos(7, 2, 7);
    private static final BlockPos GUARD_BOTTOM = new BlockPos(7, 1, 7);

    /** The six enchantments this class calls "the mining enchantments". */
    private static final List<ResourceKey<Enchantment>> MINING_ENCHANTMENTS = List.of(
            ModEnchantments.VEIN_MINER,
            ModEnchantments.STRIP_MINER,
            ModEnchantments.VERSATILITY,
            ModEnchantments.RADIUS,
            ModEnchantments.BREAK_THROUGH,
            ModEnchantments.OVERRIDE);

    /** Component key the loot pools carry an enchanted book's enchantments under. */
    private static final String STORED_ENCHANTMENTS_KEY = "minecraft:stored_enchantments";

    /**
     * How much mining efficiency
     * {@link #stripMinerDividesThePlayerDestroySpeedPerLevel} hangs on its player. Chosen well
     * above an iron pickaxe's 6.0 on stone, so a divisor that is folded in before the addition
     * instead of after it misses the expected speed by a wide margin and not by rounding.
     */
    private static final double MINING_EFFICIENCY_BONUS = 12.0;

    /**
     * Fixed seeds the mining pickaxe trade is rolled with. Large enough that every declared pool
     * entry is reached with room to spare - see
     * {@link #miningPickaxeTradeAlwaysCarriesAnEnchantmentFromItsPool} for the measured counts.
     */
    private static final int TRADE_ROLLS = 160;

    private MiningEnchantmentTests() {
    }

    // =====================================================================================
    // VEIN MINER
    // =====================================================================================

    /**
     * The block budget behind every Vein Miner level, measured on one cluster that is bigger
     * than the largest budget, plus the stop the loop makes when the pickaxe gives out.
     *
     * <p>{@code VeinMinerUsageEvent} maps level 1..5 to a budget of 3/6/9/12/18 blocks
     * <em>including</em> the origin, and the origin is broken by vanilla, so the hook may take
     * budget minus one. Only levels I and V were ever driven; 6, 9 and 12 hung on nothing at
     * all. The slab holds twenty connected blocks - more than the largest budget - so every
     * level is capped by the budget and not by the layout, level V included.
     *
     * <p>That last block is why the slab is a 5x4 and not the 4x4 it used to be. With sixteen
     * blocks the level V run could only ever count the fifteen that were there, so the case read
     * "the budget is at least 16" and every value from 16 upwards passed it: the top of the
     * table was free to drift downwards while the four levels below it stayed pinned. Level V is
     * reachable in a real world ({@code max_level} is 5), so that was the one entry of the table
     * a player could actually be shortchanged on. With four blocks to spare the count is
     * {@link #LEVEL_V_BUDGET} exactly, which fails in both directions.
     *
     * <p>This is also where the {@code MINED_BLOCKS} re-entrancy guard is covered. In a real
     * run - and in this test, because gametests run inside the loaded mod - every
     * {@code serverPlayer.gameMode.destroyBlock} the hook makes fires the loader's block break
     * event again and lands back in {@code handleBeforeBlockBreak} for the block being
     * destroyed. The guard is the only thing that makes it return immediately; without it each
     * neighbour would start its own flood fill and the counts below would run away.
     *
     * <p>The last case switches the player out of {@code instabuild} so vanilla charges the
     * pickaxe for the blocks, and the per block cost is <em>measured</em> on a two block vein
     * rather than assumed, so the assertion is about the mod's stop condition and not about
     * vanilla's tool damage.
     *
     * <p>The last case is about the other half of that guard: taking positions back <em>out</em>
     * of the set. {@code MINED_BLOCKS} is static and is never cleared, so a position that stays
     * behind stays behind for the rest of the server session, and the guard then swallows every
     * later vein mine that starts on that block - silently, because the hook returns {@code true}
     * and vanilla still breaks the single origin. The only way in is a {@code destroyBlock} that
     * throws, which {@link #trapPlayer} arranges for one position.
     *
     * <p>What breaks it: any change to the level-to-budget table, dropping the {@code -1} that
     * accounts for the origin, letting the flood fill count the origin twice, losing the
     * {@code MINED_BLOCKS} guard, removing the {@code stack.isEmpty()} break - that case would
     * then keep mining with a pickaxe that no longer exists - and taking the {@code try/finally}
     * off the {@code destroyBlock} call, which is what the last case is there for.
     */
    public static void veinMinerSpendsItsPerLevelBudgetAndStopsWhenTheToolBreaks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 90.0F);
        player.setShiftKeyDown(true);

        // Budget minus one, because vanilla breaks the origin and the hook skips it.
        assertBudget(helper, player, 1, 2);
        assertBudget(helper, player, 2, 5);
        assertBudget(helper, player, 3, 8);
        assertBudget(helper, player, 4, 11);
        // The slab has to outlast the largest budget, or the case below only says "at least".
        helper.assertTrue(SLAB_TAIL.size() > LEVEL_V_BUDGET,
                "the coal slab offers " + SLAB_TAIL.size() + " blocks beside the origin, which is "
                        + "not more than the " + LEVEL_V_BUDGET + " Vein Miner V is allowed to "
                        + "take - the case below would be capped by the layout again and a "
                        + "shrunken level V budget would go unnoticed");
        assertBudget(helper, player, 5, LEVEL_V_BUDGET);

        // --- what one block costs the pickaxe, measured on a two block vein ---
        player.getAbilities().instabuild = false;
        helper.setBlock(PAIR_ORIGIN, Blocks.COAL_ORE);
        helper.setBlock(PAIR_NEIGHBOUR, Blocks.COAL_ORE);
        ItemStack probe = veinMinerTool(helper, Items.IRON_PICKAXE, 1);
        veinMine(helper, player, probe, PAIR_ORIGIN);
        helper.assertBlockPresent(Blocks.AIR, PAIR_NEIGHBOUR);
        int costPerBlock = probe.getDamageValue();
        helper.assertTrue(costPerBlock > 0,
                "breaking a block through the Vein Miner hook cost the pickaxe no durability, so "
                        + "the tool cannot be made to break inside the vein and the case below "
                        + "would assert nothing");

        // --- the pickaxe gives out after three blocks; the rest of the slab has to survive ---
        buildSlab(helper);
        ItemStack doomed = veinMinerTool(helper, Items.IRON_PICKAXE, 5);
        doomed.setDamageValue(doomed.getMaxDamage() - 3 * costPerBlock);
        veinMine(helper, player, doomed, SLAB_ORIGIN);

        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "the pickaxe was supposed to break inside the vein but is still in the hand as "
                        + player.getMainHandItem() + " with damage " + doomed.getDamageValue());
        Assertions.valueEqual(helper, brokenCount(helper, SLAB_TAIL), 3,
                "blocks the Vein Miner hook took before the pickaxe broke");

        // --- a destroyBlock that throws must not cost the position for good ---
        helper.setBlock(GUARD_ORIGIN, Blocks.COAL_ORE);
        helper.setBlock(GUARD_NEIGHBOUR, Blocks.COAL_ORE);

        ServerPlayer trap = trapPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 90.0F,
                helper.absolutePos(GUARD_NEIGHBOUR));
        boolean threw = false;
        try {
            veinMine(helper, trap, veinMinerTool(helper, Items.IRON_PICKAXE, 5), GUARD_ORIGIN);
        } catch (DestroyBlockFailure expected) {
            threw = true;
        }
        helper.assertTrue(threw,
                "the booby trapped destroyBlock never fired, so the hook never reached "
                        + GUARD_NEIGHBOUR + " and the case below proves nothing");
        helper.assertBlockPresent(Blocks.COAL_ORE, GUARD_NEIGHBOUR);

        // The same position, now as the origin of an ordinary vein mine. Without the try/finally
        // it is still sitting in MINED_BLOCKS and the hook returns before it looks at anything.
        veinMine(helper, player, veinMinerTool(helper, Items.IRON_PICKAXE, 5), GUARD_NEIGHBOUR);
        helper.assertBlockPresent(Blocks.AIR, GUARD_ORIGIN);

        TestCleanup.succeed(helper);
    }

    /**
     * The two gates that decide whether a hook runs at all, each entered with a tool that
     * passes everything in front of it.
     *
     * <p><b>The tool family gate.</b> {@code VeinMinerUsageEvent} only continues for
     * {@code minecraft:pickaxes} or {@code minecraft:axes}, {@code StripMinerUsageEvent} only
     * for {@code minecraft:pickaxes}. Every existing case holds one of those, so both gates
     * were free. A shovel on dirt is the case that isolates them: the shovel harvests dirt
     * correctly, so the harvest gate in front passes, and in Vein Miner the ore/log branches
     * behind the gate are both keyed on {@code isPickaxe}/{@code isAxe} and would simply not
     * apply - drop the tag gate and an enchanted shovel eats a dirt floor. Both halves come
     * with a positive control on the same player and the same sneak flag, so a silent negative
     * cannot pass just because nothing was wired up.
     *
     * <p><b>The ore list.</b> Ancient debris is the second half of the divergence described in
     * the class javadoc: {@code MiningUtils#isOre} says yes, the copy inside the hook says no.
     * The test asserts both sides, so it fails when they are reconciled - at which point it
     * has to be rewritten, not deleted.
     *
     * <p>What breaks it: widening either tag gate, moving the tag gate behind the ore/log
     * branches, or a harvest check that no longer runs before it (the shovel would then reach
     * the flood fill). The controls break if the sneak gate, the enchantment lookup or the
     * hook wiring go missing.
     */
    public static void veinMinerAndStripMinerIgnoreToolsAndBlocksOutsideTheirGates(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 90.0F);
        player.setShiftKeyDown(true);

        buildCluster(helper, Blocks.DIRT, DIRT_ORIGIN, DIRT_TAIL);
        buildCluster(helper, Blocks.OAK_LOG, LOG_ORIGIN, LOG_TAIL);
        buildCluster(helper, Blocks.ANCIENT_DEBRIS, DEBRIS_ORIGIN, DEBRIS_TAIL);

        // --- Vein Miner, shovel: everything in front of the tag gate has to pass ---
        ItemStack shovel = veinMinerTool(helper, Items.DIAMOND_SHOVEL, 5);
        BlockState dirt = helper.getBlockState(DIRT_ORIGIN);
        helper.assertTrue(shovel.getItem().isCorrectToolForDrops(shovel, dirt),
                "a diamond shovel no longer harvests dirt, so this case stops in front of the "
                        + "tool tag gate and proves nothing");
        helper.assertTrue(!shovel.is(ItemTags.PICKAXES) && !shovel.is(ItemTags.AXES),
                "a diamond shovel is now inside the pickaxe or axe tag, so it is no longer a "
                        + "third tool family and this case has to be rewritten");

        veinMine(helper, player, shovel, DIRT_ORIGIN);
        for (BlockPos pos : DIRT_TAIL) {
            helper.assertBlockPresent(Blocks.DIRT, pos);
        }

        // Positive control on the very same player: an axe on a log trunk does mine.
        veinMine(helper, player, veinMinerTool(helper, Items.IRON_AXE, 5), LOG_ORIGIN);
        for (BlockPos pos : LOG_TAIL) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }

        // --- the ore list the hook uses is not the one the highlight uses ---
        BlockState debris = helper.getBlockState(DEBRIS_ORIGIN);
        helper.assertTrue(MiningUtils.isOre(debris),
                "MiningUtils stopped counting ancient debris as ore - if that was deliberate, "
                        + "this case has to be rewritten, not deleted");
        veinMine(helper, player, veinMinerTool(helper, Items.DIAMOND_PICKAXE, 5), DEBRIS_ORIGIN);
        for (BlockPos pos : DEBRIS_TAIL) {
            helper.assertBlockPresent(Blocks.ANCIENT_DEBRIS, pos);
        }

        // --- Strip Miner, shovel: same gate, one branch earlier in the method ---
        buildShaft(helper, Blocks.DIRT, DIRT_SHAFT_ORIGIN, DIRT_SHAFT);
        buildShaft(helper, Blocks.STONE, STONE_SHAFT_ORIGIN, STONE_SHAFT);

        stripMine(helper, player, stripMinerTool(helper, Items.DIAMOND_SHOVEL, 3), DIRT_SHAFT_ORIGIN);
        for (BlockPos pos : DIRT_SHAFT) {
            helper.assertBlockPresent(Blocks.DIRT, pos);
        }

        // Positive control: the same enchantment on a pickaxe, in the same tick, does dig.
        stripMine(helper, player, stripMinerTool(helper, Items.IRON_PICKAXE, 3), STONE_SHAFT_ORIGIN);
        for (BlockPos pos : STONE_SHAFT) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // STRIP MINER
    // =====================================================================================

    /**
     * The upward branch of the mining direction, and the exact pitch at which it starts.
     *
     * <p>{@code StripMinerUsageEvent#getMiningDirection} answers {@code UP} for
     * {@code pitch < -60}, {@code DOWN} for {@code pitch > 60} and the player facing otherwise.
     * The existing suite only ever looks straight down or straight ahead, so the {@code UP}
     * branch was dead in both copies of the method - and a swapped {@code UP}/{@code DOWN} pair
     * would have gone unnoticed, because looking down is the case everyone tests.
     *
     * <p>The second half is the boundary itself. At exactly -60 the comparison is false, so the
     * tunnel must follow the yaw instead of the pitch. Testing only -90 would leave
     * {@code <=} and {@code <} indistinguishable; the roof block above the second origin is
     * what turns that difference red.
     *
     * <p>The last case is the {@code MINING_BLOCKS} guard's cleanup path, the mirror of the
     * {@code MINED_BLOCKS} case in
     * {@link #veinMinerSpendsItsPerLevelBudgetAndStopsWhenTheToolBreaks}: the set is static and
     * never cleared, so a position left behind by a {@code destroyBlock} that threw kills every
     * later tunnel that starts on that block for the rest of the session.
     *
     * <p>What breaks it: an inverted or missing {@code UP} branch, a threshold that drifts
     * (the -60 case would dig upwards), a depth that no longer maps level III to four -
     * {@link #COLUMN_CAP} sits one block past the reach on purpose - and taking the
     * {@code try/finally} off the {@code destroyBlock} call.
     */
    public static void stripMinerDigsUpwardsOnlyPastTheSteepPitchThreshold(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 1.0, 3.5), 0.0F, -90.0F);
        player.setShiftKeyDown(true);

        // --- looking straight up: the column above the origin goes, the cap survives ---
        buildShaft(helper, Blocks.STONE, COLUMN_ORIGIN, COLUMN);
        helper.setBlock(COLUMN_CAP, Blocks.STONE);
        stripMine(helper, player, stripMinerTool(helper, Items.IRON_PICKAXE, 3), COLUMN_ORIGIN);

        for (BlockPos pos : COLUMN) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        helper.assertBlockPresent(Blocks.STONE, COLUMN_ORIGIN);
        helper.assertBlockPresent(Blocks.STONE, COLUMN_CAP);

        // --- exactly -60 is not steep enough: the tunnel follows the yaw ---
        aim(player, 0.0F, -60.0F);
        helper.assertTrue(player.getXRot() == -60.0F,
                "the mock player kept a pitch of " + player.getXRot() + " instead of -60, so the "
                        + "threshold itself is not being tested any more");
        helper.assertTrue(player.getDirection() == Direction.SOUTH,
                "the mock player is not facing south, so this case would not separate the yaw "
                        + "from the pitch");

        buildShaft(helper, Blocks.STONE, LEVEL_ORIGIN, LEVEL_TUNNEL);
        helper.setBlock(LEVEL_ROOF, Blocks.STONE);
        stripMine(helper, player, stripMinerTool(helper, Items.IRON_PICKAXE, 3), LEVEL_ORIGIN);

        for (BlockPos pos : LEVEL_TUNNEL) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        helper.assertBlockPresent(Blocks.STONE, LEVEL_ROOF);

        // --- a destroyBlock that throws must not cost the position for good ---
        helper.setBlock(GUARD_TOP, Blocks.STONE);
        helper.setBlock(GUARD_MIDDLE, Blocks.STONE);
        helper.setBlock(GUARD_BOTTOM, Blocks.STONE);

        ServerPlayer trap = trapPlayer(helper, new Vec3(3.5, 1.0, 3.5), 0.0F, 90.0F,
                helper.absolutePos(GUARD_MIDDLE));
        boolean threw = false;
        try {
            stripMine(helper, trap, stripMinerTool(helper, Items.IRON_PICKAXE, 1), GUARD_TOP);
        } catch (DestroyBlockFailure expected) {
            threw = true;
        }
        helper.assertTrue(threw,
                "the booby trapped destroyBlock never fired, so the hook never reached "
                        + GUARD_MIDDLE + " and the case below proves nothing");
        helper.assertBlockPresent(Blocks.STONE, GUARD_MIDDLE);

        // The same position, now as the origin of an ordinary tunnel. Without the try/finally it
        // is still sitting in MINING_BLOCKS and the hook returns before it digs anything.
        aim(player, 0.0F, 90.0F);
        stripMine(helper, player, stripMinerTool(helper, Items.IRON_PICKAXE, 1), GUARD_MIDDLE);
        helper.assertBlockPresent(Blocks.AIR, GUARD_BOTTOM);

        TestCleanup.succeed(helper);
    }

    /**
     * Strip Miner is not free: {@code PlayerEntityMixin} divides the player's whole destroy
     * speed by 2, 3 or 4 while an enchanted pickaxe is in the hand. Nothing called
     * {@code Player#getDestroySpeed} anywhere in the suite, so the entire mixin could have been
     * deleted without a single test turning red - and a mining enchantment that silently stops
     * costing anything is a balance change nobody would notice.
     *
     * <p>The divisor is measured as a ratio against the same pickaxe without the enchantment, on
     * the same player in the same place, so the test states the mod's factor rather than a hard
     * coded speed lifted from a wiki.
     *
     * <p>The player carries a mining efficiency bonus while that ratio is taken, and that is the
     * only reason the ratio says anything about <em>where</em> the mixin injects. Mining
     * efficiency is the single step in {@code Player#getDestroySpeed} that is added rather than
     * multiplied; every other one - potion effects, the block break speed attribute, the
     * not-on-ground penalty - divides straight back out of a ratio of two runs. On a
     * modifier-free player the two runs therefore look identical whether the divisor is applied
     * to the finished return value or in front of all of vanilla's own arithmetic, and the
     * {@code RETURN} injection point was free to move. With a bonus on the player only the real
     * placement gives {@code (tool + bonus) / divisor}; anything earlier gives
     * {@code tool / divisor + bonus}. See {@link #giveMiningEfficiency}.
     *
     * <p>Both guards in front of the divisor get their own case: dirt (a pickaxe is not the
     * correct tool) and a Strip Miner shovel on that same dirt (a correct tool outside
     * {@code minecraft:pickaxes}). Either one alone would leave half the condition free.
     *
     * <p>What breaks it: a lost or inverted divisor (a multiplier would show up immediately),
     * a level-to-divisor table that shifts, dropping either guard - the dirt cases would then
     * slow down too - and moving the injection point so the penalty applies before vanilla's
     * own factors instead of after.
     */
    public static void stripMinerDividesThePlayerDestroySpeedPerLevel(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 0.0F);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();

        float bareOnStone = destroySpeed(player, new ItemStack(Items.IRON_PICKAXE), stone);
        giveMiningEfficiency(player);
        float plainOnStone = destroySpeed(player, new ItemStack(Items.IRON_PICKAXE), stone);
        helper.assertTrue(plainOnStone > 0.0F,
                "an unenchanted iron pickaxe mines stone at speed " + plainOnStone
                        + ", so the ratios below would divide by zero");
        helper.assertTrue(plainOnStone > bareOnStone,
                "the mining efficiency bonus never reached the destroy speed (" + bareOnStone
                        + " without it, " + plainOnStone + " with it), so the number the ratios "
                        + "below are taken of holds no additive vanilla step and they would not "
                        + "pin the injection point any more");

        assertSpeedDivisor(helper, player, stone, plainOnStone, 1, 2.0F);
        assertSpeedDivisor(helper, player, stone, plainOnStone, 2, 3.0F);
        assertSpeedDivisor(helper, player, stone, plainOnStone, 3, 4.0F);

        // --- guard 1: a pickaxe that cannot harvest the block keeps its full speed ---
        float plainOnDirt = destroySpeed(player, new ItemStack(Items.IRON_PICKAXE), dirt);
        helper.assertTrue(plainOnDirt > 0.0F,
                "an unenchanted iron pickaxe mines dirt at speed " + plainOnDirt
                        + ", so the comparison below would be zero against zero");
        float enchantedOnDirt = destroySpeed(player, stripMinerTool(helper, Items.IRON_PICKAXE, 3), dirt);
        Assertions.valueEqual(helper, enchantedOnDirt, plainOnDirt,
                "Strip Miner slowed a pickaxe down on a block it cannot harvest");

        // --- guard 2: a correct tool outside the pickaxe tag keeps its full speed ---
        ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
        helper.assertTrue(shovel.getItem().isCorrectToolForDrops(shovel, dirt),
                "a diamond shovel no longer harvests dirt, so this case never reaches the tool "
                        + "tag half of the guard");
        float plainShovel = destroySpeed(player, shovel, dirt);
        float enchantedShovel = destroySpeed(player, stripMinerTool(helper, Items.DIAMOND_SHOVEL, 3), dirt);
        Assertions.valueEqual(helper, enchantedShovel, plainShovel,
                "Strip Miner slowed a shovel down, although the mixin only applies to pickaxes");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // VERSATILITY
    // =====================================================================================

    /**
     * A tool that cannot harvest the block scores {@code -1} and is out of the running, however
     * fast it looks. In the existing case the pickaxe already wins on raw speed, so deleting
     * that branch changed nothing - and the branch is the difference between a helpful swap and
     * one that silently costs the player their drops.
     *
     * <p>A golden pickaxe is the clean probe: it is the fastest pickaxe in the game and at the
     * same time too low a tier for diamond ore. Both facts are asserted before the case runs,
     * so the test says out loud what it depends on. The control repeats the identical inventory
     * on plain stone, where the golden pickaxe <em>is</em> allowed - it swaps there, which is
     * what proves the negative half is the harvest check and not a dead hook.
     *
     * <p>What breaks it: dropping the {@code isCorrectToolForDrops} check in
     * {@code getToolScore}, or returning 0 instead of -1 for an unusable tool (a candidate that
     * merely scores 0 still loses on stone, but on diamond ore it would beat nothing and the
     * control would keep passing while the real case failed).
     */
    public static void versatilityRefusesCandidatesThatCannotHarvestTheBlock(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 0.0F);
        player.setShiftKeyDown(true);

        BlockPos ore = new BlockPos(3, 1, 3);
        BlockPos stone = new BlockPos(5, 1, 3);
        helper.setBlock(ore, Blocks.DIAMOND_ORE);
        helper.setBlock(stone, Blocks.STONE);

        ItemStack gold = new ItemStack(Items.GOLDEN_PICKAXE);
        ItemStack diamond = new ItemStack(Items.DIAMOND_PICKAXE);
        BlockState oreState = helper.getBlockState(ore);

        helper.assertTrue(gold.getDestroySpeed(oreState) > diamond.getDestroySpeed(oreState),
                "a golden pickaxe is no longer faster than a diamond one ("
                        + gold.getDestroySpeed(oreState) + " vs " + diamond.getDestroySpeed(oreState)
                        + "), so it could lose on speed alone and this case would prove nothing");
        helper.assertTrue(!gold.isCorrectToolForDrops(oreState),
                "a golden pickaxe now harvests diamond ore, so it is no longer an unusable "
                        + "candidate and this case has to be rewritten");
        helper.assertTrue(diamond.isCorrectToolForDrops(oreState),
                "a diamond pickaxe no longer harvests diamond ore");

        // --- the fast but unusable pickaxe must not be reached for ---
        arm(player, versatile(helper, new ItemStack(Items.DIAMOND_PICKAXE), 1), 3, new ItemStack(Items.GOLDEN_PICKAXE));
        attack(helper, player, ore);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Versatility swapped in a golden pickaxe that cannot harvest diamond ore, hand "
                        + "holds " + player.getMainHandItem());

        // --- control: on stone the very same golden pickaxe is both faster and allowed ---
        arm(player, versatile(helper, new ItemStack(Items.DIAMOND_PICKAXE), 1), 3, new ItemStack(Items.GOLDEN_PICKAXE));
        attack(helper, player, stone);
        helper.assertTrue(player.getMainHandItem().is(Items.GOLDEN_PICKAXE),
                "Versatility did not reach for the faster golden pickaxe on plain stone, hand "
                        + "holds " + player.getMainHandItem() + " - the negative case above is "
                        + "therefore not about the harvest check");

        TestCleanup.succeed(helper);
    }

    /**
     * The priority ladder in {@code getToolScore}: a sledgehammer outranks every ordinary tool
     * by 2000 points, an ordinary pickaxe/axe/shovel by 1000, and a chisel by nothing at all.
     * The existing case puts a shovel and a pickaxe against each other - both in the same 1000
     * point class - so the whole ladder could collapse into plain destroy speed without a test
     * noticing.
     *
     * <p>Every case here is set up so that raw speed points the other way, and the test asserts
     * that before it runs: the stone sledgehammer is <em>slower</em> on stone than the diamond
     * pickaxe it has to displace, and the diamond chisel with Fast Chiseling is <em>faster</em>
     * than the stone pickaxe that has to displace it. Whichever way the ladder is flattened,
     * one of the three cases turns red.
     *
     * <p>What breaks it: equal bonuses for hammer and pickaxe, a score that is speed only, or a
     * chisel that is allowed into the 1000 point class. See the class javadoc for why the
     * chisel's own {@code +0} step cannot be separated from the {@code +500} fallback.
     */
    public static void versatilityPrefersTheHammerAndRanksTheChiselLast(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5), 0.0F, 0.0F);
        player.setShiftKeyDown(true);

        BlockPos stone = new BlockPos(3, 1, 3);
        helper.setBlock(stone, Blocks.STONE);
        BlockState stoneState = helper.getBlockState(stone);

        ItemStack hammer = new ItemStack(ModItems.STONE_SLEDGEHAMMER);
        ItemStack diamondPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack stonePickaxe = new ItemStack(Items.STONE_PICKAXE);
        ItemStack chisel = fastChisel(helper);

        helper.assertTrue(hammer.isCorrectToolForDrops(stoneState) && chisel.isCorrectToolForDrops(stoneState),
                "the sledgehammer or the chisel no longer harvests stone, so it would score -1 "
                        + "and these cases would test the harvest check instead of the ladder");
        helper.assertTrue(hammer.getDestroySpeed(stoneState) < diamondPickaxe.getDestroySpeed(stoneState),
                "a stone sledgehammer is no longer slower than a diamond pickaxe ("
                        + hammer.getDestroySpeed(stoneState) + " vs "
                        + diamondPickaxe.getDestroySpeed(stoneState)
                        + "), so it could win on speed and the 2000 point step would be unproven");
        helper.assertTrue(chisel.getDestroySpeed(stoneState) > stonePickaxe.getDestroySpeed(stoneState),
                "the enchanted diamond chisel is no longer faster than a stone pickaxe ("
                        + chisel.getDestroySpeed(stoneState) + " vs "
                        + stonePickaxe.getDestroySpeed(stoneState)
                        + "), so the two chisel cases would prove nothing");

        // --- the slower hammer still outranks the faster pickaxe ---
        arm(player, versatile(helper, new ItemStack(Items.DIAMOND_PICKAXE), 1), 3, new ItemStack(ModItems.STONE_SLEDGEHAMMER));
        attack(helper, player, stone);
        helper.assertTrue(player.getMainHandItem().is(ModItems.STONE_SLEDGEHAMMER),
                "Versatility did not put the sledgehammer first, hand holds "
                        + player.getMainHandItem());

        // --- the faster chisel is given up for the slower pickaxe ---
        arm(player, versatile(helper, fastChisel(helper), 1), 3, new ItemStack(Items.STONE_PICKAXE));
        attack(helper, player, stone);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE_PICKAXE),
                "Versatility kept the faster chisel instead of reaching for the pickaxe, hand "
                        + "holds " + player.getMainHandItem());

        // --- and the same chisel never takes a pickaxe out of the hand ---
        arm(player, versatile(helper, new ItemStack(Items.STONE_PICKAXE), 1), 3, fastChisel(helper));
        attack(helper, player, stone);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE_PICKAXE),
                "Versatility swapped a pickaxe out for a chisel, hand holds "
                        + player.getMainHandItem());

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // THE DATA THE MINING ENCHANTMENTS NEED
    // =====================================================================================

    /**
     * The two item tags that decide where the mining enchantments may go, and the one vanilla
     * tag they must stay out of.
     *
     * <p>{@code simplebuilding:veinmine_enchantable} is read straight into Vein Miner's
     * definition, so it is asserted through {@code Enchantment#getSupportedItems} rather than
     * on the raw tag: that covers the tag content <em>and</em> the wiring in one go. The
     * comparison is item for item against the union of {@code #minecraft:pickaxes} and
     * {@code #minecraft:axes}, both read off the same registry, because that union is exactly
     * what {@code VeinMinerUsageEvent} lets through - it continues only for those two families.
     * Any third family in the tag (hoes, swords, a mod tool) is a dead enchantment: the anvil
     * accepts it and the hook never fires. Three sample items could not see that; only the
     * upper bound can, so both bounds are asserted at once.
     *
     * <p>{@code minecraft:in_enchanting_table} is merged into, not replaced. The existing data
     * test asserts that Fast Chiseling is in it and that the vanilla entries survived; it says
     * nothing about what else the mod may have added. All six mining enchantments are meant to
     * be treasure only, so the mod namespace inside that tag has to be exactly Fast Chiseling -
     * an accidentally enchantable Vein Miner would be a balance change no other test sees.
     *
     * <p>What breaks it: a datagen run that widens or narrows {@code veinmine_enchantable} by a
     * single item or by a whole nested tag, a definition that stops reading it, and any mod
     * enchantment added to the enchanting table tag.
     */
    public static void miningEnchantmentTagsHoldExactlyWhatTheyDeclare(GameTestHelper helper) {
        Registry<Enchantment> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> problems = new ArrayList<>();

        // --- veinmine_enchantable: the pickaxes and the axes, item for item and nothing else ---
        HolderSet<Item> supported = registry.getValueOrThrow(ModEnchantments.VEIN_MINER).getSupportedItems();
        Set<Identifier> enchantable = itemIds(supported);
        Set<Identifier> pickaxesAndAxes = itemTagContents(ItemTags.PICKAXES);
        pickaxesAndAxes.addAll(itemTagContents(ItemTags.AXES));

        // Shape guard: an empty or half read pair of vanilla tags would make the comparison
        // below report a mod regression that is really a broken lookup in this test.
        Identifier diamondPickaxe = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_PICKAXE);
        Identifier diamondAxe = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_AXE);
        helper.assertTrue(pickaxesAndAxes.contains(diamondPickaxe) && pickaxesAndAxes.contains(diamondAxe),
                "#minecraft:pickaxes plus #minecraft:axes came back as " + pickaxesAndAxes
                        + ", which does not even hold the diamond pair; this test reads the vanilla "
                        + "tags wrong and has to be rewritten - it is not a mod regression");

        if (!enchantable.equals(pickaxesAndAxes)) {
            Set<Identifier> extra = idSet();
            extra.addAll(enchantable);
            extra.removeAll(pickaxesAndAxes);
            Set<Identifier> missing = idSet();
            missing.addAll(pickaxesAndAxes);
            missing.removeAll(enchantable);
            problems.add("vein_miner's supported items are not exactly #minecraft:pickaxes plus "
                    + "#minecraft:axes - enchantable but outside both families, where the hook "
                    + "never fires: " + extra + "; inside the families but not enchantable: " + missing);
        }

        // The tag itself, so a definition that quietly points somewhere else is separable from
        // a tag that lost its content.
        if (!new ItemStack(Items.DIAMOND_PICKAXE).is(ModTags.Items.VEINMINE_ENCHANTABLE)) {
            problems.add("simplebuilding:veinmine_enchantable does not contain minecraft:diamond_pickaxe");
        }
        if (new ItemStack(Items.DIAMOND_SHOVEL).is(ModTags.Items.VEINMINE_ENCHANTABLE)) {
            problems.add("simplebuilding:veinmine_enchantable contains minecraft:diamond_shovel");
        }

        // --- in_enchanting_table: exactly one mod entry, and it is not a mining enchantment ---
        Set<Identifier> table = tagContents(registry, EnchantmentTags.IN_ENCHANTING_TABLE);
        helper.assertTrue(!table.isEmpty(),
                "minecraft:in_enchanting_table came back empty, so the filter below would accept "
                        + "anything; this test reads the wrong tag and has to be rewritten");

        Set<Identifier> modEntries = idSet();
        for (Identifier id : table) {
            if (Simplebuilding.MOD_ID.equals(id.getNamespace())) {
                modEntries.add(id);
            }
        }
        if (!modEntries.equals(Set.of(ModEnchantments.FAST_CHISELING.identifier()))) {
            problems.add("minecraft:in_enchanting_table holds the mod entries " + modEntries
                    + " instead of only " + ModEnchantments.FAST_CHISELING.identifier());
        }
        for (ResourceKey<Enchantment> key : MINING_ENCHANTMENTS) {
            if (table.contains(key.identifier())) {
                problems.add(key.identifier() + " became obtainable from an enchanting table");
            }
        }

        helper.assertTrue(problems.isEmpty(), "mining enchantment tag problems: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * Which structure chest hands out which mining enchantment, and the diamond sledgehammer in
     * the end city treasure.
     *
     * <p>{@link ConfigOptionTests} proves the mod adds pools at all and that
     * {@code worldGen.enableLootTableChanges} switches them off; it counts pools and never looks
     * inside one. So the whole progression - Strip Miner from the nether and the mineshaft,
     * Vein Miner from the mineshaft, the dungeon and the mansion, Versatility from the
     * stronghold and the end city, Radius from the ancient city, Break Through from the bastion
     * - could be shuffled or dropped entirely with every test still green.
     *
     * <p>The pools are serialised through their own codec rather than rolled: rolling needs
     * dice and statistics, the codec output is exact. The first assertion is a shape guard, so
     * a serialisation change is reported as "this test reads the wrong shape" instead of
     * silently passing on an empty search.
     *
     * <p>What breaks it: a book moved to another chest, a level that changes, a table that
     * loses its pool, and the weight of the end city sledgehammer.
     */
    public static void miningEnchantmentBooksAndTheDiamondHammerSitInTheirLootPools(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        List<String> problems = new ArrayList<>();
        try {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = true;

            Map<Identifier, Set<Integer>> mineshaft = storedEnchantments(helper, registries, BuiltInLootTables.ABANDONED_MINESHAFT);
            helper.assertTrue(!mineshaft.isEmpty(),
                    "no stored enchantments could be read out of the abandoned mineshaft pools; "
                            + "the loot pool serialisation changed shape and this test has to be "
                            + "rewritten - it is not a mod regression");

            expectBook(mineshaft, ModEnchantments.STRIP_MINER, Set.of(1, 3), "abandoned_mineshaft", problems);
            expectBook(mineshaft, ModEnchantments.VEIN_MINER, Set.of(3, 4), "abandoned_mineshaft", problems);

            expectBook(storedEnchantments(helper, registries, BuiltInLootTables.NETHER_BRIDGE),
                    ModEnchantments.STRIP_MINER, Set.of(1, 2), "nether_bridge", problems);
            expectBook(storedEnchantments(helper, registries, BuiltInLootTables.SIMPLE_DUNGEON),
                    ModEnchantments.VEIN_MINER, Set.of(2, 3, 4), "simple_dungeon", problems);
            expectBook(storedEnchantments(helper, registries, BuiltInLootTables.WOODLAND_MANSION),
                    ModEnchantments.VEIN_MINER, Set.of(4, 5), "woodland_mansion", problems);
            expectBook(storedEnchantments(helper, registries, BuiltInLootTables.STRONGHOLD_LIBRARY),
                    ModEnchantments.VERSATILITY, Set.of(1, 2), "stronghold_library", problems);
            expectBook(storedEnchantments(helper, registries, BuiltInLootTables.ANCIENT_CITY),
                    ModEnchantments.RADIUS, Set.of(1), "ancient_city", problems);
            expectBook(storedEnchantments(helper, registries, BuiltInLootTables.BASTION_TREASURE),
                    ModEnchantments.BREAK_THROUGH, Set.of(1), "bastion_treasure", problems);

            Map<Identifier, Set<Integer>> endCity = storedEnchantments(helper, registries, BuiltInLootTables.END_CITY_TREASURE);
            expectBook(endCity, ModEnchantments.VERSATILITY, Set.of(1, 2), "end_city_treasure", problems);
            expectBook(endCity, ModEnchantments.OVERRIDE, Set.of(2), "end_city_treasure", problems);

            // --- the hammer itself, not a book ---
            Integer weight = entryWeight(helper, registries, BuiltInLootTables.END_CITY_TREASURE,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "diamond_sledgehammer"));
            if (weight == null) {
                problems.add("the end city treasure has no diamond sledgehammer entry at all");
            } else if (weight != 20) {
                problems.add("the end city diamond sledgehammer has weight " + weight + " instead of 20");
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.assertTrue(problems.isEmpty(), "mining enchantment loot problems: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * The toolsmith's mining pickaxe is the one trade whose whole pool is Vein Miner and Strip
     * Miner, so every purchase carries one of the two - the librarian's master book mixes them
     * in with four other enchantments. Everything that makes the pickaxe worth buying lives in
     * one enchantment pool. {@link TradeAndMigrationTests} checks the price, the result item and
     * the trade counters of other offers; nothing anywhere checks that the pickaxe comes out
     * enchanted, let alone that the enchantment is one of the six the table declares.
     *
     * <p>This Minecraft line has no data driven villager trades, so the offer is rolled from the
     * definition the mod registers in code ({@link ModTradeDefinitions#villagerTrades()},
     * toolsmith level 5) instead of from a trade read out of {@code Registries.VILLAGER_TRADE}
     * as on 26.2. The pool the result is held against is spelled out in this test either way, so
     * a level edited in {@code ModTradeDefinitions#miningPool()} still shows up as a failure.
     *
     * <p>{@code EnchantmentPool#apply} draws from a weighted pool, so a single roll proves
     * little. The trade is rolled with a spread of fixed seeds, and the result is held to the
     * pool from <em>both</em> sides:
     * <ul>
     *   <li>Per roll: at least one enchantment, and every enchantment on the result is a pool
     *       entry <em>at the level that entry declares</em>. The level is the part a typo in the
     *       table would break silently - a Strip Miner IV that no code path can produce would
     *       still look like a working trade.</li>
     *   <li>Over all rolls: the {@code (enchantment, level)} pairs that actually came out are
     *       exactly the six the table declares. A subset check alone would stay green if the
     *       three Vein Miner entries were deleted, if {@code strip_miner} level 3 were edited
     *       down to 1, or if an entry's weight were zeroed - the pickaxe would simply stop
     *       selling half of what the mod advertises, with no test noticing.</li>
     * </ul>
     *
     * <p>The equality half only carries if every declared pair is genuinely reachable within
     * the seeds rolled, so it was measured rather than assumed: the whole draw is deterministic
     * ({@code RandomSource#create(long)} hands out a {@code LegacyRandomSource}, whose
     * {@code next}/{@code nextInt(int)}/{@code nextFloat} are bit for bit
     * {@code java.util.Random}, and the listing behind {@code TradeDefinition#toListing} draws
     * nothing before the pool). Replaying the pool over these {@value #TRADE_ROLLS} seeds hands
     * out the pairs 48/29/12 (Strip Miner I/II/III) and 46/30/15 (Vein Miner I/II/III), 180
     * enchantments in all because 20 rolls keep a second pick - the rarest pair still comes up
     * twelve times, so the equality does not hang on a single lucky seed, and the pool's weights
     * (40/30/10 per branch of 160 total) are visible in the spread. Both lines draw through
     * {@code WeightedPicker} from the same weights, and these seeds reproduce the 26.2 spread
     * number for number.
     *
     * <p>Deliberately not asserted: how many enchantments come out per roll. See the class
     * javadoc - the pool's second chance can put two mutually exclusive mining enchantments on
     * one pickaxe, and pinning the count would either bless that defect or fail on a tenth of
     * the seeds. The weights themselves are not asserted either; only that no entry is weighted
     * out of existence.
     *
     * <p>What breaks it: a pool entry that changes level, enchantment or weight-to-zero, an
     * entry deleted from the table, a pool that stops applying anything, and a toolsmith level 5
     * group that loses its pickaxe.
     */
    public static void miningPickaxeTradeAlwaysCarriesAnEnchantmentFromItsPool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 2, 1));

        VillagerTrades.ItemListing listing = miningPickaxeTrade(helper).toListing();

        // Exactly the pool the trade table declares: enchantment id -> the levels it may hand out.
        Map<Identifier, Set<Integer>> pool = Map.of(
                ModEnchantments.STRIP_MINER.identifier(), Set.of(1, 2, 3),
                ModEnchantments.VEIN_MINER.identifier(), Set.of(1, 2, 3));
        Set<String> declared = new TreeSet<>();
        pool.forEach((id, levels) -> levels.forEach(poolLevel -> declared.add(pair(id, poolLevel))));

        // Every (enchantment, level) the rolls actually produced, pool entry or not.
        Set<String> handedOut = new TreeSet<>();

        List<String> problems = new ArrayList<>();
        for (int seed = 1; seed <= TRADE_ROLLS && problems.isEmpty(); seed++) {
            // The seed goes straight into the listing here; 26.2 installs it on the loot context.
            MerchantOffer offer = listing.getOffer(level, villager, RandomSource.create(seed * 7919L));
            helper.assertTrue(offer != null, "the mining pickaxe trade produced no offer at all");

            ItemStack result = offer.getResult();
            if (!result.is(Items.DIAMOND_PICKAXE)) {
                problems.add("seed " + seed + ": the trade gave " + result + " instead of a diamond pickaxe");
                continue;
            }

            ItemEnchantments enchantments = result.getEnchantments();
            if (enchantments.isEmpty()) {
                problems.add("seed " + seed + ": the traded pickaxe came out unenchanted");
                continue;
            }
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                Identifier id = registry.getKey(holder.value());
                int rolledLevel = enchantments.getLevel(holder);
                handedOut.add(pair(id, rolledLevel));

                Set<Integer> levels = id == null ? null : pool.get(id);
                if (levels == null) {
                    problems.add("seed " + seed + ": " + id + " is not in the trade's enchantment pool");
                } else if (!levels.contains(rolledLevel)) {
                    problems.add("seed " + seed + ": " + id + " came out at level " + rolledLevel
                            + ", which the pool does not declare");
                }
            }
        }

        // The other direction: nothing the table declares may have gone missing. Only worth
        // asking once every roll was clean - after an early exit the set is half collected.
        if (problems.isEmpty() && !handedOut.equals(declared)) {
            problems.add("over " + TRADE_ROLLS + " rolls the trade handed out " + handedOut
                    + " instead of the pairs ModTradeDefinitions declares, " + declared);
        }

        helper.assertTrue(problems.isEmpty(), "mining pickaxe trade problems: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * The creative tab is the only place a player can pick a mining enchantment up without
     * hunting for a chest, and {@code ModItemGroupsContent#populate} was called by nothing at
     * all - not by a test, and on every loader only by the tab builder itself.
     *
     * <p>Each of the six has to be there as an enchanted book on its maximum level, and the
     * maximum is read out of the loaded enchantment instead of copied from the source: a level
     * cap that is raised without touching the tab shows up here as a book that is one level
     * short, which is exactly the mistake the hard coded alternative would hide.
     *
     * <p>{@code addEnchantAtMax} swallows a missing enchantment through
     * {@code registry.get(key).ifPresent(...)}, so a broken key produces no book and no error.
     * That is the failure this test is really for.
     *
     * <p>What breaks it: a book dropped from the tab, a book added at a fixed level while the
     * cap moves, and a {@code populate} that throws or returns early - the six searches would
     * all come back empty at once.
     */
    public static void creativeTabOffersEveryMiningEnchantmentBookAtMaxLevel(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        HolderLookup<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);

        List<ItemStack> collected = new ArrayList<>();
        ModItemGroupsContent.populate(
                (CreativeModeTab.Output) (stack, visibility) -> collected.add(stack), registries);

        helper.assertTrue(!collected.isEmpty(),
                "ModItemGroupsContent.populate offered no entries at all, so the searches below "
                        + "could not fail");

        List<String> problems = new ArrayList<>();
        for (ResourceKey<Enchantment> key : MINING_ENCHANTMENTS) {
            Holder<Enchantment> holder = registry.getOrThrow(key);
            int maxLevel = holder.value().getMaxLevel();

            int matches = 0;
            Set<Integer> seenLevels = new LinkedHashSet<>();
            for (ItemStack stack : collected) {
                if (!stack.is(Items.ENCHANTED_BOOK)) {
                    continue;
                }
                ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (stored == null) {
                    continue;
                }
                int level = stored.getLevel(holder);
                if (level <= 0) {
                    continue;
                }
                seenLevels.add(level);
                if (level == maxLevel) {
                    matches++;
                }
            }

            if (matches != 1) {
                problems.add(key.identifier() + ": expected exactly one book at level " + maxLevel
                        + ", found " + matches + " (levels offered: " + seenLevels + ")");
            }
        }

        helper.assertTrue(problems.isEmpty(), "creative tab problems: " + problems);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * Creates a mock server player, points it where the test needs it and hands it back to the
     * server when the test ends - a leaked mock player keeps the player list non-empty and the
     * gametest server then stalls on shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relativePos, float yRot, float xRot) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, yRot, xRot);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Points the mock player without moving it; pitch and yaw are what select the branch. */
    @SuppressWarnings("removal")
    private static void aim(ServerPlayer player, float yRot, float xRot) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yRot, xRot);
    }

    /** Thrown out of {@link #trapPlayer}'s {@code destroyBlock}, and out of nothing else. */
    private static final class DestroyBlockFailure extends RuntimeException {
        DestroyBlockFailure(BlockPos pos) {
            super("booby trapped destroyBlock at " + pos);
        }
    }

    /**
     * A mock player whose {@code destroyBlock} throws for one single world position, so that the
     * cleanup path of the hooks' recursion guards can be driven at all.
     *
     * <p>The lever is {@code Player#blockActionRestricted}:
     * {@code ServerPlayerGameMode#destroyBlock} asks it about every block it is about to take,
     * with the absolute position, and it does so before it touches the world - checked against
     * both the 26.2 and the 1.21.11 jar. Overriding it is therefore the one place where a test
     * can fail a single {@code destroyBlock} without changing a line of the mod, and the world is
     * left exactly as it was, which keeps the assertions afterwards readable.
     *
     * <p>Built like vanilla's own {@code GameTestHelper#makeMockServerPlayer(GameType)}: never
     * placed in the level and never in the player list, so it needs no cleanup and cannot stall
     * the server on shutdown. That player has no {@code connection} and would throw on anything
     * that sends it a packet - which is fine here, because it never gets past the trap.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer trapPlayer(GameTestHelper helper, Vec3 relativePos, float yRot, float xRot,
                                           BlockPos absoluteTrap) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-trap-player"),
                ClientInformation.createDefault()) {
            @Override
            public boolean blockActionRestricted(Level level, BlockPos pos, GameType mode) {
                if (pos.equals(absoluteTrap)) {
                    throw new DestroyBlockFailure(pos);
                }
                return super.blockActionRestricted(level, pos, mode);
            }
        };
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, yRot, xRot);
        // Both hooks are behind a sneak gate; without this the trap would never be reached.
        player.setShiftKeyDown(true);
        return player;
    }

    /**
     * Rebuilds the coal slab, runs the hook at one level and checks how much of it went. The
     * count is what is asserted, not which blocks: the flood fill visits the 26 neighbours in
     * its own order, so the identity of the last block taken is an implementation detail while
     * the number is the promise.
     */
    private static void assertBudget(GameTestHelper helper, ServerPlayer player, int level, int expected) {
        buildSlab(helper);
        veinMine(helper, player, veinMinerTool(helper, Items.IRON_PICKAXE, level), SLAB_ORIGIN);
        Assertions.valueEqual(helper, brokenCount(helper, SLAB_TAIL), expected,
                "blocks taken by Vein Miner " + level + " beside the origin");
        helper.assertBlockPresent(Blocks.COAL_ORE, SLAB_ORIGIN);
    }

    /**
     * Holds {@code tool} and reads the player's destroy speed for {@code state}, which is where
     * {@code PlayerEntityMixin} folds the Strip Miner penalty in.
     */
    private static float destroySpeed(ServerPlayer player, ItemStack tool, BlockState state) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player.getDestroySpeed(state);
    }

    /**
     * Hangs {@link #MINING_EFFICIENCY_BONUS} of mining efficiency on the player, which is what
     * turns the destroy speed ratios into a statement about the mixin's injection point.
     *
     * <p>1.21.11 {@code Player#getDestroySpeed} adds the attribute to the tool speed before it
     * multiplies anything else in - the same shape as on 26.2. The bonus is therefore the one
     * term of the finished speed that does not survive a division unchanged, and the only handle
     * a ratio has on whether the mod divides the whole result or something earlier in that
     * method. The bonus goes on the player rather than on the tool so that the two runs differ
     * in nothing but the enchantment, and so that no equipment tick is needed between them.
     */
    private static void giveMiningEfficiency(ServerPlayer player) {
        player.getAttribute(Attributes.MINING_EFFICIENCY).addPermanentModifier(new AttributeModifier(
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "gametest_mining_efficiency"),
                MINING_EFFICIENCY_BONUS, AttributeModifier.Operation.ADD_VALUE));
    }

    /** One level of the Strip Miner slowdown, stated as a ratio against the unenchanted tool. */
    private static void assertSpeedDivisor(GameTestHelper helper, ServerPlayer player, BlockState state,
                                           float plainSpeed, int level, float divisor) {
        float slowed = destroySpeed(player, stripMinerTool(helper, Items.IRON_PICKAXE, level), state);
        float expected = plainSpeed / divisor;
        helper.assertTrue(Math.abs(slowed - expected) <= expected * 1.0E-4F,
                "Strip Miner " + level + " left the destroy speed at " + slowed + " instead of "
                        + expected + " (unenchanted " + plainSpeed + " divided by " + divisor + ")");
    }

    /** Runs the Vein Miner hook with exactly the arguments the loader hooks pass. */
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

    /** Sneak hits a block with the main hand, which is what Versatility listens to. */
    private static void attack(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        VersatilityUsageEvent.handleAttackBlock(
                player, helper.getLevel(), InteractionHand.MAIN_HAND,
                helper.absolutePos(relativePos), Direction.UP);
    }

    /** Empties the inventory, puts {@code hand} in the selected slot and {@code other} beside it. */
    private static void arm(ServerPlayer player, ItemStack hand, int otherSlot, ItemStack other) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, hand);
        player.getInventory().setItem(otherSlot, other);
    }

    private static void buildSlab(GameTestHelper helper) {
        helper.setBlock(SLAB_ORIGIN, Blocks.COAL_ORE);
        for (BlockPos pos : SLAB_TAIL) {
            helper.setBlock(pos, Blocks.COAL_ORE);
        }
    }

    /** The 5x4 coal slab minus its corner - twenty blocks in total, all connected. */
    private static List<BlockPos> slabTail() {
        List<BlockPos> tail = new ArrayList<>();
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 4; z++) {
                BlockPos pos = new BlockPos(x, 1, z);
                if (!pos.equals(new BlockPos(1, 1, 1))) {
                    tail.add(pos);
                }
            }
        }
        return List.copyOf(tail);
    }

    private static void buildCluster(GameTestHelper helper, net.minecraft.world.level.block.Block block,
                                     BlockPos origin, List<BlockPos> tail) {
        helper.setBlock(origin, block);
        for (BlockPos pos : tail) {
            helper.setBlock(pos, block);
        }
    }

    private static void buildShaft(GameTestHelper helper, net.minecraft.world.level.block.Block block,
                                   BlockPos origin, List<BlockPos> shaft) {
        helper.setBlock(origin, block);
        for (BlockPos pos : shaft) {
            helper.setBlock(pos, block);
        }
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

    private static ItemStack veinMinerTool(GameTestHelper helper, Item item, int level) {
        return enchanted(helper, new ItemStack(item), ModEnchantments.VEIN_MINER, level);
    }

    private static ItemStack stripMinerTool(GameTestHelper helper, Item item, int level) {
        return enchanted(helper, new ItemStack(item), ModEnchantments.STRIP_MINER, level);
    }

    private static ItemStack versatile(GameTestHelper helper, ItemStack stack, int level) {
        return enchanted(helper, stack, ModEnchantments.VERSATILITY, level);
    }

    /** A diamond chisel fast enough to beat a stone pickaxe on raw destroy speed. */
    private static ItemStack fastChisel(GameTestHelper helper) {
        return enchanted(helper, new ItemStack(ModItems.DIAMOND_CHISEL), ModEnchantments.FAST_CHISELING, 2);
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /**
     * One pool entry as {@code enchantment@level}. Comparing these flat strings is what makes
     * "the pool declares six pairs" and "six pairs came out" the same question; a null id (an
     * enchantment that is not in the registry at all) prints as {@code null@n} rather than
     * throwing, so the failure message still names the seed's problem.
     */
    private static String pair(Identifier id, int level) {
        return id + "@" + level;
    }

    /** An id set that prints in a stable order, so a failure message is diffable. */
    private static Set<Identifier> idSet() {
        return new TreeSet<>(java.util.Comparator.comparing(Identifier::toString));
    }

    /** The ids behind a {@link HolderSet}, e.g. the one an enchantment's supported items form. */
    private static Set<Identifier> itemIds(HolderSet<Item> items) {
        Set<Identifier> ids = idSet();
        for (Holder<Item> holder : items) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.identifier()));
        }
        return ids;
    }

    /** What one item tag resolves to on the loaded server, nested tags already flattened. */
    private static Set<Identifier> itemTagContents(TagKey<Item> tag) {
        Set<Identifier> ids = idSet();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.identifier()));
        }
        return ids;
    }

    private static Set<Identifier> tagContents(Registry<Enchantment> registry, TagKey<Enchantment> tag) {
        Set<Identifier> contents = new LinkedHashSet<>();
        for (Holder<Enchantment> holder : registry.getTagOrEmpty(tag)) {
            holder.unwrapKey().ifPresent(key -> contents.add(key.identifier()));
        }
        return contents;
    }

    private static void expectBook(Map<Identifier, Set<Integer>> found, ResourceKey<Enchantment> key,
                                   Set<Integer> levels, String table, List<String> problems) {
        Set<Integer> actual = found.get(key.identifier());
        if (actual == null) {
            problems.add(table + " holds no " + key.identifier() + " book at all");
        } else if (!actual.equals(levels)) {
            problems.add(table + " holds " + key.identifier() + " at levels " + actual
                    + " instead of " + levels);
        }
    }

    /**
     * Every {@code stored_enchantments} the mod's pools for one table carry, as
     * {@code enchantment id -> levels}. The pools are serialised through {@code LootPool.CODEC}
     * because a built pool keeps its entry list private; unlike rolling the pool this needs no
     * dice and no statistics.
     */
    private static Map<Identifier, Set<Integer>> storedEnchantments(GameTestHelper helper,
                                                                    HolderLookup.Provider registries,
                                                                    ResourceKey<LootTable> table) {
        Map<Identifier, Set<Integer>> found = new LinkedHashMap<>();
        for (JsonElement pool : encodePools(helper, registries, table)) {
            collectStoredEnchantments(pool, found);
        }
        return found;
    }

    /** Weight of the first loot entry for {@code item} in the mod's pools, or {@code null}. */
    private static Integer entryWeight(GameTestHelper helper, HolderLookup.Provider registries,
                                       ResourceKey<LootTable> table, Identifier item) {
        for (JsonElement pool : encodePools(helper, registries, table)) {
            Integer weight = findEntryWeight(pool, item.toString());
            if (weight != null) {
                return weight;
            }
        }
        return null;
    }

    private static List<JsonElement> encodePools(GameTestHelper helper, HolderLookup.Provider registries,
                                                 ResourceKey<LootTable> table) {
        PoolCollector collector = new PoolCollector();
        ModLootTableModifications.apply(table, collector, registries);

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<JsonElement> encoded = new ArrayList<>();
        for (LootPool pool : collector.pools) {
            encoded.add(LootPool.CODEC.encodeStart(ops, pool)
                    .getOrThrow(message -> helper.assertionException(
                            "a loot pool the mod adds to " + table.identifier()
                                    + " cannot be serialised: " + message)));
        }
        return encoded;
    }

    /**
     * Walks the serialised pool for objects stored under {@link #STORED_ENCHANTMENTS_KEY} and
     * collects the {@code id -> level} pairs below them. The search below that key is recursive
     * on purpose: whether the component encodes as a bare map or wraps its levels in another
     * object is vanilla's business, and either shape has to keep working.
     */
    private static void collectStoredEnchantments(JsonElement element, Map<Identifier, Set<Integer>> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectStoredEnchantments(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (STORED_ENCHANTMENTS_KEY.equals(entry.getKey())) {
                collectLevels(entry.getValue(), out);
            } else {
                collectStoredEnchantments(entry.getValue(), out);
            }
        }
    }

    private static void collectLevels(JsonElement element, Map<Identifier, Set<Integer>> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectLevels(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            Optional<Identifier> id = Identifier.read(entry.getKey()).result();
            if (id.isPresent() && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                out.computeIfAbsent(id.get(), unused -> new TreeSet<>()).add(value.getAsInt());
            } else {
                collectLevels(value, out);
            }
        }
    }

    /** The {@code weight} of the first entry whose {@code name} is {@code itemId}. */
    private static Integer findEntryWeight(JsonElement element, String itemId) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                Integer weight = findEntryWeight(child, itemId);
                if (weight != null) {
                    return weight;
                }
            }
            return null;
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        JsonElement name = object.get("name");
        if (name != null && name.isJsonPrimitive() && itemId.equals(name.getAsString())) {
            JsonElement weight = object.get("weight");
            return weight != null && weight.isJsonPrimitive() ? weight.getAsInt() : 1;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Integer weight = findEntryWeight(entry.getValue(), itemId);
            if (weight != null) {
                return weight;
            }
        }
        return null;
    }

    /** Collects the pools the mod offers for one table, from both editor paths. */
    private static final class PoolCollector implements ModLootTableModifications.Editor {
        private final List<LootPool> pools = new ArrayList<>();

        @Override
        public void addPool(LootPool.Builder pool) {
            this.pools.add(pool.build());
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.pools.add(pool);
        }
    }

    /**
     * The toolsmith level 5 definition that gives a diamond pickaxe. This line builds its offers
     * in code, so the one under test is looked up in {@link ModTradeDefinitions}; the 26.2 twin
     * pulls the same trade out of the {@code villager_trade} registry by id and needs no search.
     */
    private static TradeDefinition miningPickaxeTrade(GameTestHelper helper) {
        for (VillagerTradeGroup group : ModTradeDefinitions.villagerTrades()) {
            if (!VillagerProfession.TOOLSMITH.equals(group.profession()) || group.level() != 5) {
                continue;
            }
            for (TradeDefinition trade : group.trades()) {
                if (trade.result().is(Items.DIAMOND_PICKAXE)) {
                    return trade;
                }
            }
        }
        helper.fail("no toolsmith level 5 trade gives a diamond pickaxe");
        throw new IllegalStateException("unreachable");
    }
}
