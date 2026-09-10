package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The four enchantments that sit on the <em>building</em> tools - Constructor's Touch, Fast
 * Chiseling, Color Palette and Linear - none of which had a behaviour test before.
 *
 * <p>All four are the same kind of risk: they are registered, they appear in loot and in trades,
 * they are checked by the data integrity tests, and every one of them could stop doing anything
 * at all without a single test turning red. Constructor's Touch is the worst of them - it is the
 * key to a whole second set of chisel transformation tables per tier, plus the reverse direction
 * of those tables, and it also drives an entirely separate feature on a plain stick.
 *
 * <p>Most of what these enchantments change is a <em>payment</em>: a cooldown, a point of
 * durability, a block taken out of the inventory. Every one of those sits behind
 * {@code !player.getAbilities().instabuild}, so {@link #mockPlayer(GameTestHelper)} clears that
 * flag by hand. It does <em>not</em> try to change the game mode: {@code GameTestHelper}'s in
 * level mock overrides {@code gameMode()} to {@code CREATIVE} unconditionally, so
 * {@code setGameMode} cannot move {@code isCreative()} - see the class javadoc of
 * {@code ConsumptionAndDurabilityTests} for the full write up. {@code instabuild} is a plain
 * public field and is the flag every branch in this file actually reads.
 *
 * <p><strong>Fabric registration:</strong> the catalogue entry in {@link SimpleBuildingGameTests}
 * is what NeoForge reads, but Fabric registers only the classes listed under the
 * {@code fabric-gametest} entrypoint in {@code fabric.mod.json}. These six tests therefore also
 * need a {@code BuildingEnchantmentGameTest} adapter and that entrypoint line, or they will run
 * on NeoForge only and the Fabric test count will simply not move.
 */
public final class BuildingEnchantmentTests {

    private BuildingEnchantmentTests() {
    }

    /** Upper bound for the cooldown drain loop, so a cooldown that never ends fails instead of hanging. */
    private static final int COOLDOWN_TICK_CAP = 400;

    /** Upper bound for the wand tick loop, for the same reason. */
    private static final int WAND_TICK_CAP = 60;

    // =====================================================================================
    // CONSTRUCTOR'S TOUCH - THE EXTRA CHISEL TABLES
    // =====================================================================================

    /**
     * Constructor's Touch is what unlocks the second transformation table of a chisel. Every
     * tier carries two maps: the plain one the tool always uses, and a "touch" one that is the
     * plain map <em>merged with</em> the tier's extra entries. Four separate things have to
     * hold:
     *
     * <ul>
     *   <li><strong>The extras are unlocked.</strong> Cobblestone is not in the stone chisel's
     *       plain table at all; only the enchantment turns it into mossy cobblestone.</li>
     *   <li><strong>The extras are additive.</strong> The touch table is built with
     *       {@code merge(plain, extras)}. If that merge is ever dropped, an enchanted chisel
     *       would <em>lose</em> its ordinary transformations - stone would stop turning into
     *       chiseled stone bricks the moment the tool is enchanted, which is the kind of
     *       regression a "does the enchantment work" test alone never catches.</li>
     *   <li><strong>The reverse direction exists.</strong> Sneaking runs the touch table
     *       backwards and costs two durability instead of one, while a dedicated spatula runs
     *       the same table backwards without sneaking and pays only one - the
     *       {@code isReverseAction} flag is set in the chisel branch only.</li>
     *   <li><strong>The tier gate survives.</strong> End stone belongs to the diamond tier's
     *       extras, so a stone chisel must refuse it even while enchanted. If the tier gate
     *       were lost, the cheapest chisel in the game would do everything the most expensive
     *       one does.</li>
     *   <li><strong>The tiers stack upwards.</strong> Every tier's table is built as
     *       {@code merge(the tier below, its own entries)}, in both the plain and the touch
     *       direction. A diamond chisel therefore still has to do the stone tier's work - if
     *       those merges were replaced by the bare per tier maps, the expensive tools would
     *       quietly stop being able to chisel stone at all, and a test that only ever uses each
     *       tier on its own entries would not notice.</li>
     * </ul>
     *
     * <p>Each step also cross checks {@code ChiselItem#canChisel} against what the click really
     * did. Those two methods repeat the same map selection logic in two places; when they drift,
     * the client side highlight promises a transformation the click then refuses. The cooldown
     * is the third such branch and gets a case of its own at the end: every other step clears the
     * cooldown first, so without it {@code canChisel}'s {@code isOnCooldown} early return would
     * never once be executed while it is actually true.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code hasConstructorsTouch}
     * ternaries in {@code tryChiselBlock} or {@code canChisel}, replacing {@code merge(plain,
     * extras)} with the bare extras map, dropping the {@code merge(lower tier, own entries)} that
     * chains the tiers, wiring every tier to the same pair of maps, losing the
     * {@code isSneaking} branch, charging the reverse step the same as the forward one, or
     * deleting the cooldown check from {@code canChisel} alone.
     */
    public static void constructorsTouchUnlocksTheExtraChiselTablesInBothDirections(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        BlockPos mossy = new BlockPos(2, 1, 2);
        BlockPos plain = new BlockPos(4, 1, 2);
        BlockPos tier = new BlockPos(2, 1, 4);
        BlockPos log = new BlockPos(4, 1, 4);

        ItemStack stoneChisel = new ItemStack(ModItems.STONE_CHISEL);
        ItemStack touchedStoneChisel = enchanted(helper, ModItems.STONE_CHISEL, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        ItemStack diamondChisel = new ItemStack(ModItems.DIAMOND_CHISEL);
        ItemStack touchedDiamondChisel = enchanted(helper, ModItems.DIAMOND_CHISEL, ModEnchantments.CONSTRUCTORS_TOUCH, 1);

        // --- the extras are locked without the enchantment ---
        helper.setBlock(mossy, Blocks.COBBLESTONE);
        Block untouched = chiselAt(helper, player, stoneChisel, mossy, false, "plain stone chisel on cobblestone");
        Assertions.valueEqual(helper, untouched, Blocks.COBBLESTONE,
                "an unenchanted stone chisel reached into the Constructor's Touch table and produced " + untouched);

        // --- and unlocked with it ---
        helper.setBlock(mossy, Blocks.COBBLESTONE);
        Block mossed = chiselAt(helper, player, touchedStoneChisel, mossy, false, "touched stone chisel on cobblestone");
        Assertions.valueEqual(helper, mossed, Blocks.MOSSY_COBBLESTONE,
                "Constructor's Touch did not unlock cobblestone -> mossy cobblestone, got " + mossed);

        // --- logs are the other stone tier extra: only the enchantment strips them ---
        helper.setBlock(log, Blocks.OAK_LOG);
        Block bark = chiselAt(helper, player, stoneChisel, log, false, "plain stone chisel on an oak log");
        Assertions.valueEqual(helper, bark, Blocks.OAK_LOG,
                "an unenchanted stone chisel stripped a log and produced " + bark);

        helper.setBlock(log, Blocks.OAK_LOG);
        Block stripped = chiselAt(helper, player, touchedStoneChisel, log, false, "touched stone chisel on an oak log");
        Assertions.valueEqual(helper, stripped, Blocks.STRIPPED_OAK_LOG,
                "Constructor's Touch did not strip the log, got " + stripped);

        // --- additive: the enchanted chisel must KEEP its ordinary transformations ---
        helper.setBlock(plain, Blocks.STONE);
        Block chiselled = chiselAt(helper, player, touchedStoneChisel, plain, false, "touched stone chisel on stone");
        Assertions.valueEqual(helper, chiselled, Blocks.CHISELED_STONE_BRICKS,
                "the Constructor's Touch table replaced the ordinary one instead of extending it; "
                        + "stone became " + chiselled + " rather than chiseled stone bricks");

        // --- reverse direction, ordinary entry: sneaking walks the same step back ---
        helper.setBlock(plain, Blocks.CHISELED_STONE_BRICKS);
        Block backToStone = chiselAt(helper, player, touchedStoneChisel, plain, true,
                "touched stone chisel sneaking on chiseled stone bricks");
        Assertions.valueEqual(helper, backToStone, Blocks.STONE,
                "the reverse Constructor's Touch table lost the ordinary entries, got " + backToStone);

        // --- reverse direction, extra entry: only the enchantment can undo the moss ---
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block stubborn = chiselAt(helper, player, stoneChisel, mossy, true,
                "plain stone chisel sneaking on mossy cobblestone");
        Assertions.valueEqual(helper, stubborn, Blocks.MOSSY_COBBLESTONE,
                "an unenchanted stone chisel undid a Constructor's Touch transformation, got " + stubborn);

        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block demossed = chiselAt(helper, player, touchedStoneChisel, mossy, true,
                "touched stone chisel sneaking on mossy cobblestone");
        Assertions.valueEqual(helper, demossed, Blocks.COBBLESTONE,
                "Constructor's Touch has no reverse for its own extra entry, got " + demossed);

        // --- the tier gate survives the enchantment ---
        helper.setBlock(tier, Blocks.END_STONE);
        Block tooWeak = chiselAt(helper, player, touchedStoneChisel, tier, false,
                "touched stone chisel on end stone");
        Assertions.valueEqual(helper, tooWeak, Blocks.END_STONE,
                "a touched stone chisel reached into the diamond tier's extras and produced " + tooWeak);

        helper.setBlock(tier, Blocks.END_STONE);
        Block stillWeak = chiselAt(helper, player, diamondChisel, tier, false,
                "plain diamond chisel on end stone");
        Assertions.valueEqual(helper, stillWeak, Blocks.END_STONE,
                "an unenchanted diamond chisel used the Constructor's Touch table and produced " + stillWeak);

        helper.setBlock(tier, Blocks.END_STONE);
        Block bricks = chiselAt(helper, player, touchedDiamondChisel, tier, false,
                "touched diamond chisel on end stone");
        Assertions.valueEqual(helper, bricks, Blocks.END_STONE_BRICKS,
                "the diamond tier's Constructor's Touch extras are gone, end stone became " + bricks);

        // --- the gate only points one way: the higher tier keeps the lower tier's work ---
        // Stone -> chiseled stone bricks is a stone tier entry and cobblestone -> mossy cobblestone
        // a stone tier Constructor's Touch extra. Both have to survive all the way up through the
        // merge chain, or every chisel above the cheapest one loses the bulk of its table.
        helper.setBlock(plain, Blocks.STONE);
        Block diamondOnStone = chiselAt(helper, player, diamondChisel, plain, false,
                "plain diamond chisel on stone");
        Assertions.valueEqual(helper, diamondOnStone, Blocks.CHISELED_STONE_BRICKS,
                "the diamond tier's ordinary table no longer merges the tiers below it; stone became "
                        + diamondOnStone + " instead of chiseled stone bricks");

        helper.setBlock(mossy, Blocks.COBBLESTONE);
        Block diamondMoss = chiselAt(helper, player, touchedDiamondChisel, mossy, false,
                "touched diamond chisel on cobblestone");
        Assertions.valueEqual(helper, diamondMoss, Blocks.MOSSY_COBBLESTONE,
                "the diamond tier's Constructor's Touch table no longer merges the tiers below it; "
                        + "cobblestone became " + diamondMoss + " instead of mossy cobblestone");

        // --- the sneaking chisel step costs double, which is the whole balancing of "unchiselling" ---
        ItemStack forwardTool = enchanted(helper, ModItems.STONE_CHISEL, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(mossy, Blocks.COBBLESTONE);
        chiselAt(helper, player, forwardTool, mossy, false, "durability probe, chisel forwards");
        Assertions.valueEqual(helper, forwardTool.getDamageValue(), 1,
                "a forward chisel step no longer costs exactly one point of durability");

        ItemStack reverseTool = enchanted(helper, ModItems.STONE_CHISEL, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        chiselAt(helper, player, reverseTool, mossy, true, "durability probe, chisel backwards");
        Assertions.valueEqual(helper, reverseTool.getDamageValue(), 2,
                "the sneaking chisel step is no longer twice as expensive as the forward one");

        // --- the spatula reads the very same touch table from the other end, for a single point ---
        ItemStack plainSpatula = new ItemStack(ModItems.STONE_SPATULA);
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block spatulaRefused = chiselAt(helper, player, plainSpatula, mossy, false,
                "plain stone spatula on mossy cobblestone");
        Assertions.valueEqual(helper, spatulaRefused, Blocks.MOSSY_COBBLESTONE,
                "an unenchanted stone spatula undid a Constructor's Touch transformation, got " + spatulaRefused);

        ItemStack touchedSpatula = enchanted(helper, ModItems.STONE_SPATULA, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block spatulaUndid = chiselAt(helper, player, touchedSpatula, mossy, false,
                "touched stone spatula on mossy cobblestone");
        Assertions.valueEqual(helper, spatulaUndid, Blocks.COBBLESTONE,
                "the spatula's default direction no longer reads the Constructor's Touch table, got " + spatulaUndid);
        Assertions.valueEqual(helper, touchedSpatula.getDamageValue(), 1,
                "the spatula started paying the sneaking chisel's double cost; isReverseAction leaked "
                        + "out of the chisel branch");

        // --- the cooldown belongs to canChisel as much as to the click ---
        // This is the one case that does not clear the cooldown first. Without it the
        // isOnCooldown early return in canChisel is never once taken while it is true, and
        // deleting it would leave the block highlight promising a transformation for the whole
        // length of the cooldown that useOn then answers with PASS.
        ItemStack busyChisel = new ItemStack(ModItems.STONE_CHISEL);
        helper.setBlock(plain, Blocks.STONE);
        Block firstClick = chiselAt(helper, player, busyChisel, plain, false, "cooldown probe, first click");
        Assertions.valueEqual(helper, firstClick, Blocks.CHISELED_STONE_BRICKS,
                "the cooldown probe chiselled nothing, so there is no cooldown to measure against");
        helper.assertTrue(player.getCooldowns().isOnCooldown(busyChisel),
                "the chisel put itself on no cooldown at all, so the branch below is unreachable");

        helper.setBlock(plain, Blocks.STONE);
        boolean promisedWhileBusy = ((ChiselItem) busyChisel.getItem())
                .canChisel(helper.getLevel(), helper.absolutePos(plain), busyChisel, player);
        helper.assertFalse(promisedWhileBusy,
                "canChisel promises a transformation while the chisel is still on cooldown; the "
                        + "highlight lights up for a click the item then refuses");
        Block swallowed = chiselWhileOnCooldown(helper, player, busyChisel, plain,
                "cooldown probe, second click");
        Assertions.valueEqual(helper, swallowed, Blocks.STONE,
                "a chisel on cooldown transformed the block anyway, it became " + swallowed);
        clearCooldown(player, busyChisel);

        player.setShiftKeyDown(false);
        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // CONSTRUCTOR'S TOUCH - THE STICK
    // =====================================================================================

    /**
     * <strong>Not present on this Minecraft line.</strong> The 26.2 copy of this class carries
     * {@code constructorsTouchStickCyclesTheFirstBlockStateProperty} and the check that Fabric
     * delegates to the shared class; both drive
     * {@code com.simplebuilding.util.ConstructorsTouchInteraction}.
     *
     * <p>MC 1.21.11 has no such class: the very same stick logic lives twice over, once inside
     * {@code mc1_21_11/fabric/.../ModRegistries#registerEvents} and once inside
     * {@code mc1_21_11/neoforge/.../ModRegistriesNeoForge}, each behind its own loader's use-block
     * event. A loader-neutral test body cannot reach either without importing a loader, so the
     * tests are left out here rather than being duplicated per loader - and the catalogue of this
     * line leaves their ids out with that reason (see LINE_DIFFERENCES in the test runner).
     *
     * <p>To close this gap, extract the logic on this line into a shared class the way 26.2 did;
     * the tests then port across unchanged.
     */
    // =====================================================================================
    // FAST CHISELING
    // =====================================================================================

    /**
     * Fast Chiseling is the only thing that makes a chisel usable in bulk: it shortens the
     * cooldown between two transformations by 30% per level, and it doubles as an Efficiency
     * style mining bonus on the same tool.
     *
     * <p>The cooldown side is measured the way the server measures it - by draining
     * {@code ItemCooldowns} one tick at a time after a real transformation. The numbers are
     * spelled out rather than recomputed from the formula: they are truncated {@code int}s of a
     * {@code float} product, not exact percentages, and a test that mirrors the formula cannot
     * notice the formula changing.
     *
     * <p>Every tier is drained, not just the stone one. The cooldown is the whole difference
     * between the tiers as a working tool, it is a different constant per tier, and the only
     * other reader of {@code getCooldownTicks} is the wiki export - so a tier whose constant was
     * retuned to anything at all had nothing to fail against.
     *
     * <p>The mining side pins the halving against a <em>measured</em> reference rather than
     * against the mod's own material: whatever a vanilla stone tool is worth on stone, the stone
     * chisel has to be worth exactly half of it. Stating only the enchantment bonus as a delta
     * would leave {@code material.speed() * 0.5f} free to become anything, because both the plain
     * and the enchanted measurement would move together. The same halved speed is then asserted
     * on an axe block and on a shovel block, which are the other two thirds of the chisel's
     * "correct tool" claim, and on glass, which is in none of the three tags and must stay at the
     * bare hand's 1.0.
     *
     * <p><strong>What breaks this test:</strong> never calling {@code addCooldown}, dropping the
     * {@code fastChiselingLevel > 0} branch, reading the level from a different enchantment
     * (every level would then measure 30 ticks), retuning the {@code 0.3f} step or any tier's
     * cooldown constant, changing the {@code 5.0f}/{@code 17.0f} mining bonuses or the
     * {@code * 0.5f} halving, dropping the axe or shovel tag from
     * {@code ChiselItem#isCorrectToolForDrops}, or moving the bonus in front of the
     * {@code isCorrectToolForDrops} early return.
     */
    public static void fastChiselingShortensTheCooldownAndSpeedsUpMining(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos target = new BlockPos(3, 1, 3);

        ChiselItem chisel = ModItems.STONE_CHISEL;
        int base = chisel.getCooldownTicks();
        Assertions.valueEqual(helper, base, 30,
                "the stone chisel's base cooldown was retuned; update the expected Fast Chiseling "
                        + "cooldowns in this test to match");

        int plainCooldown = chiselAndDrainCooldown(helper, player, new ItemStack(chisel), target);
        int oneCooldown = chiselAndDrainCooldown(helper, player,
                enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 1), target);
        int twoCooldown = chiselAndDrainCooldown(helper, player,
                enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2), target);

        Assertions.valueEqual(helper, plainCooldown, 30,
                "an unenchanted stone chisel did not wait out its full cooldown");
        Assertions.valueEqual(helper, oneCooldown, 21,
                "Fast Chiseling I did not take 30% off the cooldown");
        Assertions.valueEqual(helper, twoCooldown, 11,
                "Fast Chiseling II did not take 60% off the cooldown");

        // --- the whole staircase of tier cooldowns, each one drained the same way ---
        assertTierCooldown(helper, player, ModItems.STONE_CHISEL, 30, target);
        assertTierCooldown(helper, player, ModItems.COPPER_CHISEL, 25, target);
        assertTierCooldown(helper, player, ModItems.IRON_CHISEL, 25, target);
        assertTierCooldown(helper, player, ModItems.GOLD_CHISEL, 20, target);
        assertTierCooldown(helper, player, ModItems.DIAMOND_CHISEL, 10, target);
        assertTierCooldown(helper, player, ModItems.NETHERITE_CHISEL, 5, target);
        assertTierCooldown(helper, player, ModItems.ENDERITE_CHISEL, 5, target);

        // --- mining speed: half the material speed, plus +5 and +17 raw for the enchantment ---
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();

        // Measured off vanilla instead of read out of the mod: a stone pickaxe on stone is worth
        // exactly ToolMaterial.STONE's speed, and the chisel has to come out at half of that.
        float vanillaStoneToolSpeed = new ItemStack(Items.STONE_PICKAXE).getDestroySpeed(stone);
        helper.assertTrue(vanillaStoneToolSpeed > 1.0F,
                "a vanilla stone pickaxe no longer mines stone faster than a bare hand (" + vanillaStoneToolSpeed
                        + "), so it cannot serve as the reference for the chisel's material speed");
        float halvedMaterial = vanillaStoneToolSpeed * 0.5F;

        float plainSpeed = chisel.getDestroySpeed(new ItemStack(chisel), stone);
        float oneSpeed = chisel.getDestroySpeed(enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 1), stone);
        float twoSpeed = chisel.getDestroySpeed(enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2), stone);

        assertSpeed(helper, plainSpeed, halvedMaterial,
                "an unenchanted stone chisel on stone (half of a vanilla stone tool's " + vanillaStoneToolSpeed + ")");
        assertSpeed(helper, oneSpeed, plainSpeed + 2.5F, "Fast Chiseling I");
        assertSpeed(helper, twoSpeed, plainSpeed + 8.5F, "Fast Chiseling II");

        // --- the chisel is the correct tool for pickaxe, axe AND shovel blocks, at the same speed ---
        helper.assertTrue(chisel.isCorrectToolForDrops(new ItemStack(chisel), stone),
                "the chisel stopped being the correct tool for a pickaxe block");
        helper.assertTrue(chisel.isCorrectToolForDrops(new ItemStack(chisel), planks),
                "the chisel stopped being the correct tool for an axe block, so it drops nothing there");
        helper.assertTrue(chisel.isCorrectToolForDrops(new ItemStack(chisel), dirt),
                "the chisel stopped being the correct tool for a shovel block, so it drops nothing there");
        assertSpeed(helper, chisel.getDestroySpeed(new ItemStack(chisel), planks), halvedMaterial,
                "an unenchanted stone chisel on oak planks (mineable with an axe)");
        assertSpeed(helper, chisel.getDestroySpeed(new ItemStack(chisel), dirt), halvedMaterial,
                "an unenchanted stone chisel on dirt (mineable with a shovel)");

        // --- and none of that leaks onto blocks the chisel is not effective on ---
        helper.assertFalse(chisel.isCorrectToolForDrops(new ItemStack(chisel), glass),
                "glass gained a mineable tag, so it no longer works as the ineffective control "
                        + "block here - pick another untagged block");
        helper.assertTrue(chisel.getDestroySpeed(new ItemStack(chisel), glass) == 1.0F,
                "the chisel mines glass at " + chisel.getDestroySpeed(new ItemStack(chisel), glass)
                        + " although it is the wrong tool for it");
        helper.assertTrue(chisel.getDestroySpeed(
                        enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2), glass) == 1.0F,
                "Fast Chiseling speeds up a block the chisel cannot mine");

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    /**
     * Chisels once with one tier and drains the cooldown that click imposed, then cross checks it
     * against {@code getCooldownTicks}. Measured rather than read: a tier that stopped calling
     * {@code addCooldown}, or that puts a different number in than it reports, fails here even
     * though its getter still answers correctly.
     */
    private static void assertTierCooldown(GameTestHelper helper, ServerPlayer player,
                                           ChiselItem chisel, int expected, BlockPos target) {
        String name = String.valueOf(BuiltInRegistries.ITEM.getKey(chisel));
        int measured = chiselAndDrainCooldown(helper, player, new ItemStack(chisel), target);
        Assertions.valueEqual(helper, measured, expected,
                name + " no longer holds the player for " + expected + " ticks between two transformations");
        Assertions.valueEqual(helper, chisel.getCooldownTicks(), measured,
                name + " imposes a different cooldown than getCooldownTicks() reports, so the wiki "
                        + "export and the game disagree");
    }

    // =====================================================================================
    // COLOR PALETTE - THE PREVIEW
    // =====================================================================================

    /**
     * Color Palette changes the wand's <em>preview</em>: instead of showing one block repeated
     * across the whole plane, {@code BuildingWandItem#getPreviewStates} spreads every building
     * block the player is carrying over it. That is the branch the client side highlight draws
     * from; what the server actually places is a different branch and is pinned separately in
     * {@link #colorPaletteKeepsTheWandBuildingWhenOneBlockRunsOut}. The two do not agree, and
     * saying so is the honest description of this feature.
     *
     * <p>Pinned here: the plain wand still paints one single block, the enchanted one really
     * reaches for more than one, the result is stable across calls (the wand seeds itself from
     * the position so the preview does not flicker), and an empty inventory produces an empty
     * preview instead of a modulo by a palette size of zero.
     *
     * <p><strong>Also pinned, deliberately, as a quirk rather than as an endorsement:</strong>
     * the seed is {@code BlockPos#asLong}, whose lowest bit is the Y coordinate (Y sits at shift
     * 0 in the packed long). With two blocks to choose from the palette therefore stripes by
     * height - and a flat floor, where every position shares one Y, comes out in a single
     * colour. That is why the interesting case below clicks a vertical face. If this ever becomes
     * real randomness the horizontal assertion fails, which is the point at which this paragraph
     * should be deleted.
     *
     * <p><strong>Also pinned: where "carried" starts.</strong> Both preview branches read the off
     * hand before the hotbar ({@code findFirstBlockStateClient} and {@code findAllBuildingBlocks}
     * each open with it). Every other wand test in the suite clears the off hand first, so that
     * step could be deleted without a single assertion moving. The last block below therefore puts
     * the only, or the first, block into the off hand and states all three halves of it: the plain
     * preview has to reach it at all, it has to prefer it over the hotbar, and in the palette
     * branch the off hand block has to be the <em>first entry of the list</em>. That last one is
     * stated position by position on purpose. The palette branch cannot express "first" as a
     * different set of blocks - collecting the off hand after the hotbar produces the same two
     * blocks either way - but the list index picks per position, so swapping the two entries
     * swaps the drawing on every position of the plane while leaving the set identical.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code hasColorPalette} branch in
     * {@code getPreviewStates}, letting the plain branch mix blocks, seeding the index from
     * {@code Random} instead of the position, removing the {@code palette.isEmpty()} guard
     * (which would divide by zero), dropping the off hand out of either material search, or
     * collecting it after the hotbar rather than before it.
     */
    public static void colorPaletteSpreadsTheCarriedBlocksOverTheWandPreview(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        int diameter = ModItems.DIAMOND_BUILDING_WAND.getWandSquareDiameter();
        ItemStack plainWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        ItemStack paletteWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        paletteWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);

        BlockPos origin = helper.absolutePos(new BlockPos(3, 3, 3));

        // --- nothing to build from: the palette branch must not divide by an empty palette ---
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, paletteWand);
        Map<BlockPos, BlockState> empty = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.NORTH, diameter);
        helper.assertTrue(empty.isEmpty(),
                "Color Palette previewed " + empty.size() + " blocks out of an empty inventory");

        // --- two different building blocks in the hotbar ---
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 16));
        player.getInventory().setItem(2, new ItemStack(Items.GLASS, 16));

        // --- the plain wand repeats the first block it finds ---
        Map<BlockPos, BlockState> plain = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, plainWand, origin, Direction.NORTH, diameter);
        Assertions.valueEqual(helper, plain.size(), 9,
                "the previewed plane is not the 3x3 the wand's own radius setting asks for");
        Set<Block> plainBlocks = distinctBlocks(plain);
        Set<Block> onlyPlanks = Set.of(Blocks.OAK_PLANKS);
        Assertions.valueEqual(helper, plainBlocks, onlyPlanks,
                "a wand without Color Palette mixed blocks into its preview: " + plainBlocks);

        // --- the enchanted wand reaches for the whole hotbar ---
        Map<BlockPos, BlockState> palette = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.NORTH, diameter);
        Assertions.valueEqual(helper, palette.size(), 9,
                "Color Palette changed how many blocks the wand previews");
        Set<Block> paletteBlocks = distinctBlocks(palette);
        Set<Block> bothBlocks = Set.of(Blocks.OAK_PLANKS, Blocks.GLASS);
        Assertions.valueEqual(helper, paletteBlocks, bothBlocks,
                "Color Palette did not spread both carried blocks over the plane, it used " + paletteBlocks);

        // --- and does so deterministically, or the preview would flicker every frame ---
        Map<BlockPos, BlockState> again = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.NORTH, diameter);
        Assertions.valueEqual(helper, again, palette,
                "the Color Palette preview is not stable between two calls, so it would flicker");

        // --- the striping quirk described in the javadoc ---
        // Which of the two blocks wins depends on the parity of the structure's absolute Y, so
        // only the count is asserted here, not the colour.
        Map<BlockPos, BlockState> floor = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.UP, diameter);
        Assertions.valueEqual(helper, distinctBlocks(floor).size(), 1,
                "Color Palette now varies within one horizontal layer. That is very likely an "
                        + "improvement, but the palette index is documented here as a function of Y "
                        + "only - re-read the javadoc and update it.");

        // --- the off hand is the first place both branches look ---
        // Only the off hand carries a building block here, so a preview that skipped it would come
        // back empty rather than merely wrong.
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, paletteWand);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BRICKS, 16));

        Map<BlockPos, BlockState> offhandOnly = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, plainWand, origin, Direction.NORTH, diameter);
        Set<Block> offhandOnlyBlocks = distinctBlocks(offhandOnly);
        Assertions.valueEqual(helper, offhandOnlyBlocks, Set.of(Blocks.BRICKS),
                "a wand whose only building block is in the off hand previewed " + offhandOnlyBlocks
                        + "; findFirstBlockStateClient stopped looking there");

        // ... and it wins over the hotbar, which is what makes it the *first* place.
        player.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS, 16));
        Map<BlockPos, BlockState> offhandFirst = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, plainWand, origin, Direction.NORTH, diameter);
        Set<Block> offhandFirstBlocks = distinctBlocks(offhandFirst);
        Assertions.valueEqual(helper, offhandFirstBlocks, Set.of(Blocks.BRICKS),
                "the plain wand preferred the hotbar over the off hand and previewed "
                        + offhandFirstBlocks + "; the off hand is supposed to be searched first");

        // The palette branch has its own copy of that search, so it needs its own case: both the
        // off hand block and the hotbar block have to end up in the spread.
        Map<BlockPos, BlockState> offhandPalette = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.NORTH, diameter);
        Set<Block> offhandPaletteBlocks = distinctBlocks(offhandPalette);
        Assertions.valueEqual(helper, offhandPaletteBlocks, Set.of(Blocks.BRICKS, Blocks.OAK_PLANKS),
                "Color Palette did not spread the off hand block over the plane together with the "
                        + "hotbar one, it used " + offhandPaletteBlocks
                        + "; findAllBuildingBlocks stopped collecting from the off hand");

        // A set cannot see the *order* of that list, and the order is what decides which block
        // lands where: the index is Math.abs(pos.asLong() % size), so with two entries the first
        // one is drawn on every evenly seeded position. Collecting the off hand after the hotbar
        // instead of before it leaves the set above completely untouched while repainting every
        // single position with the other block.
        for (Map.Entry<BlockPos, BlockState> entry : offhandPalette.entrySet()) {
            Block expectedHere = Math.abs((int) (entry.getKey().asLong() % 2)) == 0
                    ? Blocks.BRICKS
                    : Blocks.OAK_PLANKS;
            Assertions.valueEqual(helper, entry.getValue().getBlock(), expectedHere,
                    "the Color Palette spread drew " + entry.getValue().getBlock() + " at "
                            + entry.getKey() + " where the off hand block was expected to be the "
                            + "palette's first entry; findAllBuildingBlocks is collecting the hotbar "
                            + "before the off hand");
        }

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // COLOR PALETTE - THE PLACEMENT
    // =====================================================================================

    /**
     * What Color Palette does on the <em>server</em>, which is not what its preview shows.
     * {@code BuildingWandItem#inventoryTick} does not stripe anything: with the enchantment,
     * {@code findMaterialForPlacement} stops asking for the one block the wand was armed with
     * and takes whatever building block comes first instead. The visible consequence is that an
     * enchanted wand keeps going when its first stack runs out, while a plain wand gives up.
     *
     * <p>The setup makes that difference the whole result. Three oak planks and a stack of glass
     * against a 3x3 plane: the plain wand places three planks and then switches itself off,
     * because {@code findSpecificMaterial} only ever matches oak planks. The enchanted wand
     * places the same three planks and then six glass, filling all nine.
     *
     * <p><strong>What breaks this test:</strong> deleting the {@code colorPaletteActive} branch
     * in {@code findMaterialForPlacement} (the enchanted run would abort at three blocks like
     * the plain one), making the plain branch fall back to any block (the plain run would fill
     * all nine), or removing the {@code material == null && !instabuild} abort (the plain run
     * would place air or free blocks for the remaining six).
     *
     * <p>It also fails, deliberately, if Color Palette ever grows the per-position spread its
     * preview already draws: the placed plane would then mix planks and glass in a different
     * ratio than 3 to 6, and this test has to be rewritten to state the new rule.
     *
     * <p>The tail pins the <em>other</em> end of the material search, which both branches share:
     * the wand looks in the off hand before it looks anywhere else. Every other wand test in the
     * suite empties the off hand first, so the whole branch was free to be deleted - and a player
     * who carries their blocks in the off hand would then no longer be able to arm the wand at all
     * ({@code useOn} answers {@code FAIL} when {@code findFirstBuildingBlock} finds nothing). The
     * run below therefore keeps the hotbar empty and pays for the whole plane out of the off hand,
     * so both readers of that branch - the arming check and {@code findSpecificMaterial} inside
     * the placement loop - have to be there for it to pass.
     */
    public static void colorPaletteKeepsTheWandBuildingWhenOneBlockRunsOut(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Two layers, so the two 5x5 read back windows cannot count each other's blocks.
        BlockPos plainAnchor = new BlockPos(3, 1, 3);
        BlockPos paletteAnchor = new BlockPos(3, 4, 3);

        ItemStack plainWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        ItemStack paletteWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        paletteWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);

        runWandUntilIdle(helper, player, plainWand, plainAnchor,
                new ItemStack(Items.OAK_PLANKS, 3), new ItemStack(Items.GLASS, 16));
        runWandUntilIdle(helper, player, paletteWand, paletteAnchor,
                new ItemStack(Items.OAK_PLANKS, 3), new ItemStack(Items.GLASS, 16));

        // --- the plain wand stops when its one block is gone ---
        Assertions.valueEqual(helper, placedOffsets(helper, plainAnchor).size(), 3,
                "a wand without Color Palette did not stop after its three oak planks were used up");
        Set<Block> plainBlocks = distinctPlaced(helper, plainAnchor);
        Assertions.valueEqual(helper, plainBlocks, Set.of(Blocks.OAK_PLANKS),
                "a wand without Color Palette placed something other than the block it was armed "
                        + "with: " + plainBlocks);

        // --- the enchanted one carries on with whatever is left ---
        Assertions.valueEqual(helper, placedOffsets(helper, paletteAnchor).size(), 9,
                "Color Palette did not let the wand finish the plane out of a second stack");
        Set<Block> paletteBlocks = distinctPlaced(helper, paletteAnchor);
        Assertions.valueEqual(helper, paletteBlocks, Set.of(Blocks.OAK_PLANKS, Blocks.GLASS),
                "the Color Palette run did not draw on both stacks, it placed " + paletteBlocks);
        Assertions.valueEqual(helper, countPlaced(helper, paletteAnchor, Blocks.OAK_PLANKS), 3,
                "the Color Palette run did not spend exactly the three planks it was given");
        Assertions.valueEqual(helper, countPlaced(helper, paletteAnchor, Blocks.GLASS), 6,
                "the Color Palette run did not fall through to the glass for the remaining six");

        // --- material carried in the off hand, which is the first place the wand looks ---
        // A third layer of its own, so its 5x5 read back window cannot reach the two runs above.
        BlockPos offhandAnchor = new BlockPos(3, 6, 3);
        ItemStack offhandWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        ItemStack offhandStock = new ItemStack(Items.BRICKS, 16);

        helper.setBlock(offhandAnchor, Blocks.STONE);
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, offhandWand);
        // After clearContent: Inventory#clearContent empties the off hand compartment too.
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStock);

        InteractionResult armed = useOnTopFace(helper, player, offhandWand, offhandAnchor);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "a wand whose only building block sits in the off hand refused to arm itself, it "
                        + "returned " + armed + "; findFirstBuildingBlock no longer looks there, so "
                        + "carrying your blocks in the off hand switches the wand off entirely");

        BuildingWandItem offhandItem = (BuildingWandItem) offhandWand.getItem();
        int offhandTicks = 0;
        while (wandIsActive(offhandWand) && offhandTicks < WAND_TICK_CAP) {
            offhandItem.inventoryTick(offhandWand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            offhandTicks++;
        }
        helper.assertTrue(offhandTicks < WAND_TICK_CAP,
                "the off hand wand never finished within " + WAND_TICK_CAP + " ticks");

        Assertions.valueEqual(helper, placedOffsets(helper, offhandAnchor).size(), 9,
                "the wand did not finish its plane out of the off hand stack; findSpecificMaterial "
                        + "stopped looking in the off hand");
        Set<Block> offhandBlocks = distinctPlaced(helper, offhandAnchor);
        Assertions.valueEqual(helper, offhandBlocks, Set.of(Blocks.BRICKS),
                "the off hand run placed something other than the off hand block: " + offhandBlocks);
        Assertions.valueEqual(helper, offhandStock.getCount(), 16 - 9,
                "the nine blocks were not taken out of the off hand stack, so they came from "
                        + "somewhere the player is not carrying");

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // LINEAR
    // =====================================================================================

    /**
     * Linear does not do what its name suggests. The building wand reads it in exactly one
     * place: it picks {@code DELAY_TICKS_LINE} instead of {@code DELAY_TICKS} for the pause
     * between two rings. The shape it builds is position for position the same square plane.
     *
     * <p>This test pins both halves of that. The two runs are compared position by position, so
     * the day Linear grows an actual line shape this test fails and has to be rewritten - which
     * is the honest way to record that the enchantment is currently only a speed up. And the
     * tick counts are measured, so removing the branch (or swapping the two constants) is caught
     * as well.
     *
     * <p>The wand's {@code inventoryTick} is called directly instead of through the player tick:
     * a gametest server never pumps a mock player's connection, and driving the item hook is the
     * only way to count ticks exactly rather than "somewhere in the next twenty". The expected
     * counts are {@code DELAY + 2}: one tick places the centre and arms the timer, {@code DELAY}
     * ticks drain it, and one more places the outer ring and switches the wand off.
     *
     * <p><strong>A third run, two rings wide, closes that formula.</strong> The two runs the
     * enchantment is measured on have a radius of one, which is a single step outwards - so they
     * see one single pause, the one between the centre and the outer ring. A wand that armed the
     * timer on the way out of ring zero and left it at zero for every further ring would pace both
     * of them exactly as it does today, and a 9x9 plane would appear in one blink after its first
     * ring. The run at the end therefore builds a radius of two and states the general shape of the
     * formula: one tick for the centre plus {@code DELAY_TICKS + 1} per ring, with the inner 3x3
     * standing alone for the whole first delay and the outer ring for the whole second one.
     *
     * <p>Three things are measured that a plain "how many ticks did the run take" cannot see:
     *
     * <ul>
     *   <li><strong>The plane really is built ring by ring.</strong> The block count is read back
     *       after <em>every single</em> tick, and the first tick has to show exactly the one
     *       centre block. A wand that computed all rings in its first tick would still spend the
     *       same number of ticks draining the same timer afterwards, and would still finish with
     *       the same nine blocks - the tick count alone cannot tell the two apart.</li>
     *   <li><strong>The two delays are the numbers they are supposed to be.</strong> Comparing
     *       the measurement against {@code DELAY_TICKS} only proves the wand uses the constant,
     *       not what the constant says; setting {@code DELAY_TICKS} to 40 would keep both sides
     *       of that comparison in step and make the wand ten times slower in silence. The two
     *       balancing numbers are therefore spelled out here as well - they are pinned nowhere
     *       else in the repository.</li>
     *   <li><strong>Nothing is built anywhere else.</strong> The runs are compared against a
     *       scan of the <em>whole</em> 8x8x8 room instead of the 5x5 window above each anchor.
     *       A Linear branch that quietly added a second layer, or reached further out sideways,
     *       lands outside that window and would otherwise be invisible.</li>
     * </ul>
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code isLinePlace} ternary (both
     * runs would take {@code DELAY_TICKS + 2}), swapping or retuning the two constants, reading
     * Linear from the wrong stack, collapsing the per-ring loop into a single tick, arming the
     * timer for the first ring only, or Linear starting to change {@code calculatePositions} - in
     * the plane or out of it.
     */
    public static void linearOnlyShortensTheWandStepDelay(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Two separate layers, so the 5x5 windows the two runs are read back through cannot
        // overlap and count each other's blocks.
        BlockPos plainAnchor = new BlockPos(3, 1, 3);
        BlockPos linearAnchor = new BlockPos(3, 4, 3);
        // A third layer for the two ring wide run at the end, far enough from the other two that
        // its 5x5 window cannot reach them either.
        BlockPos wideAnchor = new BlockPos(3, 6, 3);

        // Whatever the empty test room already contains - the loaders do not agree on whether it
        // has a floor - so the "nothing else was touched" comparison below states a difference
        // instead of a room layout.
        Set<BlockPos> roomBefore = solidPositions(helper);

        ItemStack plainWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        ItemStack linearWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        linearWand.enchant(enchantment(helper, ModEnchantments.LINEAR), 1);

        List<Integer> plainProgress =
                runWandTickByTick(helper, player, plainWand, plainAnchor, new ItemStack(Items.STONE, 64));
        List<Integer> linearProgress =
                runWandTickByTick(helper, player, linearWand, linearAnchor, new ItemStack(Items.STONE, 64));
        int plainTicks = plainProgress.size();
        int linearTicks = linearProgress.size();

        // A third run, two rings wide. Both runs above take exactly one step outwards, so they
        // measure one single pause; a wand that arms the timer for the first ring and leaves it at
        // zero for every ring after that paces them identically and only shows up here.
        ItemStack wideWand = wandWithRadius(new ItemStack(ModItems.DIAMOND_BUILDING_WAND), 2);
        List<Integer> wideProgress =
                runWandTickByTick(helper, player, wideWand, wideAnchor, new ItemStack(Items.STONE, 64));
        int wideTicks = wideProgress.size();

        // --- the shape is untouched ---
        Set<BlockPos> plainShape = placedOffsets(helper, plainAnchor);
        Set<BlockPos> linearShape = placedOffsets(helper, linearAnchor);
        Assertions.valueEqual(helper, plainShape.size(), 9,
                "the unenchanted wand did not fill the expected 3x3, it placed " + plainShape.size() + " blocks");
        Assertions.valueEqual(helper, linearShape, plainShape,
                "Linear changed the shape the wand builds. That is a real feature now, so this test "
                        + "has to be replaced by one that states what the new shape is.");

        // --- and nothing at all stands outside those three planes ---
        // The window above only looks at one layer, five blocks wide. A run that also placed a
        // block one step higher, or six blocks out, would fill exactly the same window.
        Set<BlockPos> expected = new HashSet<>(roomBefore);
        expected.add(plainAnchor);
        expected.add(linearAnchor);
        expected.add(wideAnchor);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                expected.add(plainAnchor.offset(dx, 1, dz));
                expected.add(linearAnchor.offset(dx, 1, dz));
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                expected.add(wideAnchor.offset(dx, 1, dz));
            }
        }
        Set<BlockPos> stray = new HashSet<>(solidPositions(helper));
        stray.removeAll(expected);
        helper.assertTrue(stray.isEmpty(),
                "the wand put blocks outside the two 3x3 planes and the one 5x5 plane it was asked "
                        + "for, at " + stray + " (relative positions). Linear is only supposed to "
                        + "change the pacing.");

        // --- one ring per step: the centre first, the outer ring only on the very last tick ---
        Assertions.valueEqual(helper, plainProgress.get(0), 1,
                "the unenchanted wand did not start with the single centre block; it placed "
                        + plainProgress.get(0) + " blocks in its first tick, so the ring by ring build "
                        + "is gone and the delay measured below paces nothing");
        Assertions.valueEqual(helper, plainProgress.get(plainTicks - 2), 1,
                "the unenchanted wand had already placed " + plainProgress.get(plainTicks - 2)
                        + " blocks one tick before it finished; the outer ring is supposed to wait "
                        + "out the whole delay");
        Assertions.valueEqual(helper, plainProgress.get(plainTicks - 1), 9, "blocks after the last plain tick");
        Assertions.valueEqual(helper, linearProgress.get(0), 1,
                "the Linear wand did not start with the single centre block either, it placed "
                        + linearProgress.get(0));
        Assertions.valueEqual(helper, linearProgress.get(linearTicks - 2), 1,
                "the Linear wand had already placed " + linearProgress.get(linearTicks - 2)
                        + " blocks one tick before it finished");
        Assertions.valueEqual(helper, linearProgress.get(linearTicks - 1), 9, "blocks after the last Linear tick");

        // --- only the pacing is ---
        helper.assertTrue(linearTicks < plainTicks,
                "Linear did not speed the wand up at all: " + linearTicks + " ticks against " + plainTicks);
        Assertions.valueEqual(helper, plainTicks, BuildingWandItem.DELAY_TICKS + 2,
                "the unenchanted wand no longer paces itself with DELAY_TICKS");
        Assertions.valueEqual(helper, linearTicks, BuildingWandItem.DELAY_TICKS_LINE + 2,
                "the Linear wand no longer paces itself with DELAY_TICKS_LINE");

        // The two constants themselves, because the two assertions above compare the wand against
        // them and would follow them anywhere. These are the only two places in the repository
        // where the wand's step delay is stated as a number; if one of them is retuned on purpose,
        // this is the line that has to be updated with it.
        Assertions.valueEqual(helper, BuildingWandItem.DELAY_TICKS, 4,
                "BuildingWandItem.DELAY_TICKS was retuned. Nothing else pins it, so state the new "
                        + "pause between two rings here on purpose or the wand can be slowed down at will.");
        Assertions.valueEqual(helper, BuildingWandItem.DELAY_TICKS_LINE, 2,
                "BuildingWandItem.DELAY_TICKS_LINE was retuned; same story as DELAY_TICKS above.");

        // --- every ring waits, not only the first one ---
        // Everything above this line is measured on a radius of one, which is a single step
        // outwards: centre, one pause, outer ring. A timer that is armed on the way out of ring
        // zero and left at zero afterwards produces exactly those numbers as well. The two ring
        // run separates the two readings - a run of n rings costs one tick for the centre plus
        // DELAY_TICKS + 1 for every ring after it.
        Assertions.valueEqual(helper, wideTicks, 2 * BuildingWandItem.DELAY_TICKS + 3,
                "a two ring wand took " + wideTicks + " ticks; every ring after the first one is "
                        + "supposed to wait out the same delay, so the wand is no longer pacing the "
                        + "rings past the first");
        Assertions.valueEqual(helper, wideProgress.get(BuildingWandItem.DELAY_TICKS + 1), 9,
                "the two ring wand did not have exactly the inner 3x3 standing on the tick its "
                        + "first delay ran out, it had " + wideProgress.get(BuildingWandItem.DELAY_TICKS + 1));
        Assertions.valueEqual(helper, wideProgress.get(wideTicks - 2), 9,
                "the outer ring of the two ring wand was already standing one tick before the run "
                        + "ended, so the second delay paced nothing");
        Assertions.valueEqual(helper, wideProgress.get(wideTicks - 1), 25,
                "the two ring wand did not finish the 5x5 plane its radius setting asks for");

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * Creates a mock player, moves it into the test room and takes creative building away from
     * it.
     *
     * <p>{@code GameTestHelper}'s in level mock hard-overrides {@code gameMode()} to
     * {@code CREATIVE}, so {@code isCreative()} cannot be moved and {@code setGameMode} is not
     * even attempted here. What every payment in this file is actually gated on is
     * {@code Abilities.instabuild}, a plain public field - the chisel's cooldown and durability,
     * the wand's block consumption and the wand's abort-when-out-of-material branch all read it,
     * and so does vanilla's own {@code ItemStack#processDurabilityChange}. Clearing it is
     * therefore the whole premise of this file; the assertion below fails loudly if a later edit
     * to this helper ever drops that line, instead of letting half the file pass while measuring
     * nothing.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 6.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        helper.assertFalse(player.getAbilities().instabuild,
                "the mock player is still building for free, so no cooldown, no durability and no "
                        + "block consumption would ever be paid and this file would measure nothing");
        // Hand the player back no matter how the test ends; a leaked mock player keeps the
        // player list non-empty and the gametest server then stalls on shutdown.
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Right clicks the top face of a block with a chisel or spatula and returns the block that is
     * there afterwards. Any cooldown left over from an earlier step is cleared first, so every
     * case starts from the same state instead of being silently swallowed by the previous one -
     * which is exactly why the cooldown itself needs {@link #chiselWhileOnCooldown}.
     */
    private static Block chiselAt(GameTestHelper helper, ServerPlayer player, ItemStack chisel,
                                  BlockPos relativePos, boolean sneaking, String what) {
        clearCooldown(player, chisel);
        return chiselClick(helper, player, chisel, relativePos, sneaking, what);
    }

    /**
     * The same click as {@link #chiselAt}, but with whatever cooldown the item is under left in
     * place - the only way to run {@code canChisel}'s cooldown branch while it is actually true.
     */
    private static Block chiselWhileOnCooldown(GameTestHelper helper, ServerPlayer player,
                                               ItemStack chisel, BlockPos relativePos, String what) {
        return chiselClick(helper, player, chisel, relativePos, false, what);
    }

    /**
     * Right clicks the top face and compares {@code ChiselItem#canChisel} - the predicate the
     * client highlight uses - with what the click actually did. The two repeat the same map
     * selection plus the same cooldown check in two places and have to agree.
     */
    private static Block chiselClick(GameTestHelper helper, ServerPlayer player, ItemStack chisel,
                                     BlockPos relativePos, boolean sneaking, String what) {
        player.setShiftKeyDown(sneaking);
        BlockPos pos = helper.absolutePos(relativePos);

        boolean predicted = ((ChiselItem) chisel.getItem()).canChisel(helper.getLevel(), pos, chisel, player);
        InteractionResult result = useOnTopFace(helper, player, chisel, relativePos);
        boolean acted = result != InteractionResult.PASS;

        Assertions.valueEqual(helper, predicted, acted,
                "canChisel and the actual click disagree for " + what + " (canChisel said " + predicted
                        + ", the click returned " + result + "), so the block highlight lies to the player");
        return helper.getBlockState(relativePos).getBlock();
    }

    /**
     * Chisels stone into chiseled stone bricks and then drains the resulting cooldown one tick
     * at a time, returning how many ticks that took. Bounded, so a cooldown that never ends
     * fails the test instead of hanging the run.
     */
    private static int chiselAndDrainCooldown(GameTestHelper helper, ServerPlayer player,
                                              ItemStack chisel, BlockPos relativePos) {
        player.setShiftKeyDown(false);
        clearCooldown(player, chisel);
        helper.setBlock(relativePos, Blocks.STONE);

        InteractionResult result = useOnTopFace(helper, player, chisel, relativePos);
        helper.assertTrue(result != InteractionResult.PASS,
                "the chisel refused stone, so there is no cooldown to measure; it returned " + result);
        helper.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, relativePos);

        ItemCooldowns cooldowns = player.getCooldowns();
        helper.assertTrue(cooldowns.isOnCooldown(chisel),
                "the chisel put itself on no cooldown at all after a real transformation");

        int ticks = 0;
        while (cooldowns.isOnCooldown(chisel) && ticks < COOLDOWN_TICK_CAP) {
            cooldowns.tick();
            ticks++;
        }
        helper.assertTrue(ticks < COOLDOWN_TICK_CAP,
                "the chisel cooldown never ran out within " + COOLDOWN_TICK_CAP + " ticks");
        return ticks;
    }

    /**
     * Arms the wand on the top face of the anchor with the given supplies in the hotbar, then
     * drives its item tick until the wand switches itself off - either because the plane is
     * finished or because it ran out of material. Returns the number of ticks that took.
     *
     * <p>Stopping on the wand's own {@code Active} flag rather than on "the outer ring appeared"
     * is what lets the same driver measure a run that deliberately aborts half way. Every caller
     * therefore has to assert the resulting shape itself; a run that quietly built nothing would
     * otherwise return a plausible tick count.
     */
    private static int runWandUntilIdle(GameTestHelper helper, ServerPlayer player, ItemStack wand,
                                        BlockPos anchor, ItemStack... supplies) {
        return runWandTickByTick(helper, player, wand, anchor, supplies).size();
    }

    /**
     * The same run as {@link #runWandUntilIdle}, but handing back how many blocks stood in the
     * plane after <em>each</em> tick - one entry per tick, so the list length is the tick count.
     *
     * <p>That intermediate view is the only thing that can tell "one ring per step" apart from
     * "everything at once, then wait": both build the same nine blocks and both leave the timer
     * running for the same number of ticks, so both finish on the same tick count.
     */
    private static List<Integer> runWandTickByTick(GameTestHelper helper, ServerPlayer player, ItemStack wand,
                                                   BlockPos anchor, ItemStack... supplies) {
        helper.setBlock(anchor, Blocks.STONE);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        for (int i = 0; i < supplies.length; i++) {
            player.getInventory().setItem(i + 1, supplies[i]);
        }

        InteractionResult armed = useOnTopFace(helper, player, wand, anchor);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand did not arm itself on the clicked face, it returned " + armed);
        helper.assertTrue(wandIsActive(wand), "the wand did not switch itself on when it was armed");

        BuildingWandItem item = (BuildingWandItem) wand.getItem();
        List<Integer> perTick = new ArrayList<>();
        while (wandIsActive(wand) && perTick.size() < WAND_TICK_CAP) {
            item.inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            perTick.add(placedOffsets(helper, anchor).size());
        }
        helper.assertTrue(perTick.size() < WAND_TICK_CAP,
                "the wand never finished within " + WAND_TICK_CAP + " ticks");
        helper.assertTrue(perTick.size() >= 2,
                "the wand finished in " + perTick.size() + " tick(s); the per tick assertions below "
                        + "need at least an arming tick and a closing tick to compare");
        return perTick;
    }

    /**
     * Every position in the whole 8x8x8 room that is not air, in room relative coordinates.
     *
     * <p>Used as a before/after pair: the empty test room is not guaranteed to be empty (the
     * loaders differ on whether it carries a floor), so what the tests state is the
     * <em>difference</em> a run made, not the room's contents.
     */
    private static Set<BlockPos> solidPositions(GameTestHelper helper) {
        Set<BlockPos> solid = new HashSet<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 8; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!helper.getBlockState(pos).isAir()) {
                        solid.add(pos);
                    }
                }
            }
        }
        return solid;
    }

    /** Whether the wand's own NBT still says it has building left to do. */
    private static boolean wandIsActive(ItemStack wand) {
        return wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBooleanOr("Active", false);
    }

    /** Every position in the 5x5 window one block above the anchor that is no longer air. */
    private static Set<BlockPos> placedOffsets(GameTestHelper helper, BlockPos anchor) {
        Set<BlockPos> placed = new HashSet<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!helper.getBlockState(anchor.offset(dx, 1, dz)).isAir()) {
                    placed.add(new BlockPos(dx, 1, dz));
                }
            }
        }
        return placed;
    }

    /** The distinct blocks in that same window. */
    private static Set<Block> distinctPlaced(GameTestHelper helper, BlockPos anchor) {
        Set<Block> blocks = new HashSet<>();
        for (BlockPos offset : placedOffsets(helper, anchor)) {
            blocks.add(helper.getBlockState(anchor.offset(offset.getX(), 1, offset.getZ())).getBlock());
        }
        return blocks;
    }

    /** How often {@code block} occurs in that same window. */
    private static int countPlaced(GameTestHelper helper, BlockPos anchor, Block block) {
        int count = 0;
        for (BlockPos offset : placedOffsets(helper, anchor)) {
            if (helper.getBlockState(anchor.offset(offset.getX(), 1, offset.getZ())).is(block)) {
                count++;
            }
        }
        return count;
    }

    /** Right clicks the centre of a block's top face, server side. */
    private static InteractionResult useOnTopFace(GameTestHelper helper, ServerPlayer player,
                                                  ItemStack stack, BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /** Drops whatever cooldown this stack's item is under, so the next case starts clean. */
    private static void clearCooldown(ServerPlayer player, ItemStack stack) {
        ItemCooldowns cooldowns = player.getCooldowns();
        cooldowns.removeCooldown(cooldowns.getCooldownGroup(stack));
    }

    /** How many of {@code before}'s properties carry a different value in {@code after}. */
    private static int changedProperties(BlockState before, BlockState after) {
        int changed = 0;
        for (Property<?> property : before.getProperties()) {
            if (!after.hasProperty(property)) {
                changed++;
            } else if (!before.getValue(property).equals(after.getValue(property))) {
                changed++;
            }
        }
        return changed;
    }

    private static Set<Block> distinctBlocks(Map<BlockPos, BlockState> preview) {
        Set<Block> blocks = new HashSet<>();
        for (BlockState state : preview.values()) {
            blocks.add(state.getBlock());
        }
        return blocks;
    }

    /**
     * Pins the wand's own radius setting to 1, so the expected plane is a 3x3 on every tier and
     * the test states a shape instead of restating a balancing number.
     */
    private static ItemStack wandWithRadiusOne(ItemStack wand) {
        return wandWithRadius(wand, 1);
    }

    /**
     * The same, for the one case that needs more than a single step outwards: how the wand paces
     * the rings <em>after</em> the first one cannot be seen on a radius of one at all.
     */
    private static ItemStack wandWithRadius(ItemStack wand, int radius) {
        CompoundTag settings = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        settings.putInt("SettingsRadius", radius);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    private static void assertSpeed(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4F,
                what + ": the chisel's mining speed is " + actual + " instead of " + expected);
    }

    private static void setLogAxis(GameTestHelper helper, BlockPos pos, Direction.Axis axis) {
        helper.setBlock(pos, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, axis));
    }

    private static Direction.Axis logAxis(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BlockStateProperties.AXIS);
    }

    private static ItemStack enchanted(GameTestHelper helper, Item item,
                                       ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
