package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.util.ConstructorsTouchInteraction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
     * </ul>
     *
     * <p>Each step also cross checks {@code ChiselItem#canChisel} against what the click really
     * did. Those two methods repeat the same map selection logic in two places; when they drift,
     * the client side highlight promises a transformation the click then refuses.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code hasConstructorsTouch}
     * ternaries in {@code tryChiselBlock} or {@code canChisel}, replacing {@code merge(plain,
     * extras)} with the bare extras map, wiring every tier to the same pair of maps, losing the
     * {@code isSneaking} branch, or charging the reverse step the same as the forward one.
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
        helper.assertValueEqual(untouched, Blocks.COBBLESTONE,
                "an unenchanted stone chisel reached into the Constructor's Touch table and produced " + untouched);

        // --- and unlocked with it ---
        helper.setBlock(mossy, Blocks.COBBLESTONE);
        Block mossed = chiselAt(helper, player, touchedStoneChisel, mossy, false, "touched stone chisel on cobblestone");
        helper.assertValueEqual(mossed, Blocks.MOSSY_COBBLESTONE,
                "Constructor's Touch did not unlock cobblestone -> mossy cobblestone, got " + mossed);

        // --- logs are the other stone tier extra: only the enchantment strips them ---
        helper.setBlock(log, Blocks.OAK_LOG);
        Block bark = chiselAt(helper, player, stoneChisel, log, false, "plain stone chisel on an oak log");
        helper.assertValueEqual(bark, Blocks.OAK_LOG,
                "an unenchanted stone chisel stripped a log and produced " + bark);

        helper.setBlock(log, Blocks.OAK_LOG);
        Block stripped = chiselAt(helper, player, touchedStoneChisel, log, false, "touched stone chisel on an oak log");
        helper.assertValueEqual(stripped, Blocks.STRIPPED_OAK_LOG,
                "Constructor's Touch did not strip the log, got " + stripped);

        // --- additive: the enchanted chisel must KEEP its ordinary transformations ---
        helper.setBlock(plain, Blocks.STONE);
        Block chiselled = chiselAt(helper, player, touchedStoneChisel, plain, false, "touched stone chisel on stone");
        helper.assertValueEqual(chiselled, Blocks.CHISELED_STONE_BRICKS,
                "the Constructor's Touch table replaced the ordinary one instead of extending it; "
                        + "stone became " + chiselled + " rather than chiseled stone bricks");

        // --- reverse direction, ordinary entry: sneaking walks the same step back ---
        helper.setBlock(plain, Blocks.CHISELED_STONE_BRICKS);
        Block backToStone = chiselAt(helper, player, touchedStoneChisel, plain, true,
                "touched stone chisel sneaking on chiseled stone bricks");
        helper.assertValueEqual(backToStone, Blocks.STONE,
                "the reverse Constructor's Touch table lost the ordinary entries, got " + backToStone);

        // --- reverse direction, extra entry: only the enchantment can undo the moss ---
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block stubborn = chiselAt(helper, player, stoneChisel, mossy, true,
                "plain stone chisel sneaking on mossy cobblestone");
        helper.assertValueEqual(stubborn, Blocks.MOSSY_COBBLESTONE,
                "an unenchanted stone chisel undid a Constructor's Touch transformation, got " + stubborn);

        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block demossed = chiselAt(helper, player, touchedStoneChisel, mossy, true,
                "touched stone chisel sneaking on mossy cobblestone");
        helper.assertValueEqual(demossed, Blocks.COBBLESTONE,
                "Constructor's Touch has no reverse for its own extra entry, got " + demossed);

        // --- the tier gate survives the enchantment ---
        helper.setBlock(tier, Blocks.END_STONE);
        Block tooWeak = chiselAt(helper, player, touchedStoneChisel, tier, false,
                "touched stone chisel on end stone");
        helper.assertValueEqual(tooWeak, Blocks.END_STONE,
                "a touched stone chisel reached into the diamond tier's extras and produced " + tooWeak);

        helper.setBlock(tier, Blocks.END_STONE);
        Block stillWeak = chiselAt(helper, player, diamondChisel, tier, false,
                "plain diamond chisel on end stone");
        helper.assertValueEqual(stillWeak, Blocks.END_STONE,
                "an unenchanted diamond chisel used the Constructor's Touch table and produced " + stillWeak);

        helper.setBlock(tier, Blocks.END_STONE);
        Block bricks = chiselAt(helper, player, touchedDiamondChisel, tier, false,
                "touched diamond chisel on end stone");
        helper.assertValueEqual(bricks, Blocks.END_STONE_BRICKS,
                "the diamond tier's Constructor's Touch extras are gone, end stone became " + bricks);

        // --- the sneaking chisel step costs double, which is the whole balancing of "unchiselling" ---
        ItemStack forwardTool = enchanted(helper, ModItems.STONE_CHISEL, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(mossy, Blocks.COBBLESTONE);
        chiselAt(helper, player, forwardTool, mossy, false, "durability probe, chisel forwards");
        helper.assertValueEqual(forwardTool.getDamageValue(), 1,
                "a forward chisel step no longer costs exactly one point of durability");

        ItemStack reverseTool = enchanted(helper, ModItems.STONE_CHISEL, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        chiselAt(helper, player, reverseTool, mossy, true, "durability probe, chisel backwards");
        helper.assertValueEqual(reverseTool.getDamageValue(), 2,
                "the sneaking chisel step is no longer twice as expensive as the forward one");

        // --- the spatula reads the very same touch table from the other end, for a single point ---
        ItemStack plainSpatula = new ItemStack(ModItems.STONE_SPATULA);
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block spatulaRefused = chiselAt(helper, player, plainSpatula, mossy, false,
                "plain stone spatula on mossy cobblestone");
        helper.assertValueEqual(spatulaRefused, Blocks.MOSSY_COBBLESTONE,
                "an unenchanted stone spatula undid a Constructor's Touch transformation, got " + spatulaRefused);

        ItemStack touchedSpatula = enchanted(helper, ModItems.STONE_SPATULA, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(mossy, Blocks.MOSSY_COBBLESTONE);
        Block spatulaUndid = chiselAt(helper, player, touchedSpatula, mossy, false,
                "touched stone spatula on mossy cobblestone");
        helper.assertValueEqual(spatulaUndid, Blocks.COBBLESTONE,
                "the spatula's default direction no longer reads the Constructor's Touch table, got " + spatulaUndid);
        helper.assertValueEqual(touchedSpatula.getDamageValue(), 1,
                "the spatula started paying the sneaking chisel's double cost; isReverseAction leaked "
                        + "out of the chisel branch");

        player.setShiftKeyDown(false);
        helper.succeed();
    }

    // =====================================================================================
    // CONSTRUCTOR'S TOUCH - THE STICK
    // =====================================================================================

    /**
     * Constructor's Touch has a second, completely separate effect: on a plain stick it turns
     * every right click into a blockstate edit, cycling the first property of the clicked block.
     * That is the mod's only in-world debug tool, and nothing else in the suite touches it.
     *
     * <p>Pinned here: the enchantment and the stick are <em>both</em> required (either gate alone
     * would let any enchanted tool rewrite blockstates), the cycle wraps around at the end of the
     * value list, sneaking runs it backwards, and a block without any property is consumed but
     * left alone instead of crashing on an empty iterator.
     *
     * <p>An oak log is used for the concrete transitions because it has exactly one property, so
     * the expected values do not depend on which property the code picks. The multi property case
     * only asserts that exactly one property moved.
     *
     * <p><strong>What breaks this test:</strong> dropping either half of
     * {@code hasEnchantment(...) && stack.is(Items.STICK)}, replacing the modulo wrap with a
     * clamp, ignoring {@code isShiftKeyDown}, cycling every property instead of the first, or
     * removing the {@code properties.isEmpty()} guard (which would throw on stone).
     *
     * <p><strong>Not covered:</strong> that Fabric behaves the same way. This drives
     * {@link ConstructorsTouchInteraction}, which its own javadoc claims both loaders delegate
     * to - but on MC 26.2 only {@code NeoForgeGameplayEvents} does. Fabric's
     * {@code ModRegistries#registerEvents} still carries a line by line copy of this logic inside
     * its {@code UseBlockCallback} registration, so a fix made here reaches one loader only.
     */
    public static void constructorsTouchStickCyclesTheFirstBlockStateProperty(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        BlockPos log = new BlockPos(3, 1, 3);
        BlockPos bare = new BlockPos(5, 1, 3);
        BlockPos stairs = new BlockPos(1, 1, 3);

        ItemStack plainStick = new ItemStack(Items.STICK);
        ItemStack touchedStick = new ItemStack(Items.STICK);
        touchedStick.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        ItemStack touchedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        touchedPickaxe.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);

        // --- an unenchanted stick must not edit anything ---
        setLogAxis(helper, log, Direction.Axis.Y);
        InteractionResult plainResult = touchBlock(helper, player, plainStick, log, false);
        helper.assertTrue(plainResult == InteractionResult.PASS,
                "an unenchanted stick consumed the interaction, got " + plainResult);
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Y,
                "an unenchanted stick rewrote a blockstate, the log now points along " + logAxis(helper, log));

        // --- neither may an enchanted tool that is not a stick ---
        setLogAxis(helper, log, Direction.Axis.Y);
        InteractionResult wrongItem = touchBlock(helper, player, touchedPickaxe, log, false);
        helper.assertTrue(wrongItem == InteractionResult.PASS,
                "an enchanted pickaxe consumed the interaction, got " + wrongItem);
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Y,
                "Constructor's Touch edited a blockstate from something other than a stick, the log "
                        + "now points along " + logAxis(helper, log));

        // --- the enchanted stick advances the single property by one step ---
        setLogAxis(helper, log, Direction.Axis.Y);
        InteractionResult forward = touchBlock(helper, player, touchedStick, log, false);
        helper.assertTrue(forward != InteractionResult.PASS,
                "the enchanted stick did not consume the interaction, got " + forward);
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "the axis did not advance Y -> Z, it is " + logAxis(helper, log));

        // --- and wraps around at the end of the list instead of stopping there ---
        setLogAxis(helper, log, Direction.Axis.Z);
        touchBlock(helper, player, touchedStick, log, false);
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.X,
                "the axis did not wrap Z -> X, it is " + logAxis(helper, log));

        // --- sneaking runs the cycle backwards ---
        setLogAxis(helper, log, Direction.Axis.Y);
        touchBlock(helper, player, touchedStick, log, true);
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.X,
                "sneaking did not step the axis back Y -> X, it is " + logAxis(helper, log));

        // --- a block with several properties: exactly one of them may move ---
        player.setShiftKeyDown(false);
        helper.setBlock(stairs, Blocks.OAK_STAIRS);
        BlockState before = helper.getBlockState(stairs);
        touchBlock(helper, player, touchedStick, stairs, false);
        BlockState after = helper.getBlockState(stairs);
        helper.assertTrue(after.is(Blocks.OAK_STAIRS),
                "the stick replaced the block instead of editing it, it is now " + after.getBlock());
        helper.assertValueEqual(changedProperties(before, after), 1,
                "the stick did not cycle exactly one property; before " + before + ", after " + after);

        // --- a block without properties is consumed but left alone ---
        helper.setBlock(bare, Blocks.STONE);
        InteractionResult onBare = touchBlock(helper, player, touchedStick, bare, false);
        helper.assertTrue(onBare != InteractionResult.PASS,
                "the enchanted stick let a property-less block through, got " + onBare);
        helper.assertBlockPresent(Blocks.STONE, bare);

        player.setShiftKeyDown(false);
        helper.succeed();
    }

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
     * <p>The mining side is asserted as a delta on top of the unenchanted speed, so it pins the
     * two bonus constants without pinning vanilla's stone tool speed. The bonus must also stay
     * behind the "is this tool effective here" check - otherwise an enchanted chisel would tear
     * through blocks it has no business mining.
     *
     * <p><strong>What breaks this test:</strong> never calling {@code addCooldown}, dropping the
     * {@code fastChiselingLevel > 0} branch, reading the level from a different enchantment
     * (every level would then measure 30 ticks), retuning the {@code 0.3f} step, changing the
     * {@code 5.0f}/{@code 17.0f} mining bonuses or the {@code * 0.5f} halving, or moving the
     * bonus in front of the {@code isCorrectToolForDrops} early return.
     */
    public static void fastChiselingShortensTheCooldownAndSpeedsUpMining(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos target = new BlockPos(3, 1, 3);

        ChiselItem chisel = ModItems.STONE_CHISEL;
        int base = chisel.getCooldownTicks();
        helper.assertValueEqual(base, 30,
                "the stone chisel's base cooldown was retuned; update the expected Fast Chiseling "
                        + "cooldowns in this test to match");

        int plainCooldown = chiselAndDrainCooldown(helper, player, new ItemStack(chisel), target);
        int oneCooldown = chiselAndDrainCooldown(helper, player,
                enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 1), target);
        int twoCooldown = chiselAndDrainCooldown(helper, player,
                enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2), target);

        helper.assertValueEqual(plainCooldown, 30,
                "an unenchanted stone chisel did not wait out its full cooldown");
        helper.assertValueEqual(oneCooldown, 21,
                "Fast Chiseling I did not take 30% off the cooldown");
        helper.assertValueEqual(twoCooldown, 11,
                "Fast Chiseling II did not take 60% off the cooldown");

        // --- mining speed: +5 and +17 raw, halved like the rest of the chisel's speed ---
        BlockState stone = Blocks.STONE.defaultBlockState();
        float plainSpeed = chisel.getDestroySpeed(new ItemStack(chisel), stone);
        float oneSpeed = chisel.getDestroySpeed(enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 1), stone);
        float twoSpeed = chisel.getDestroySpeed(enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2), stone);

        helper.assertTrue(plainSpeed > 1.0F,
                "the chisel is not treated as an effective tool on stone any more, speed is " + plainSpeed);
        assertSpeed(helper, oneSpeed, plainSpeed + 2.5F, "Fast Chiseling I");
        assertSpeed(helper, twoSpeed, plainSpeed + 8.5F, "Fast Chiseling II");

        // --- and none of that leaks onto blocks the chisel is not effective on ---
        BlockState glass = Blocks.GLASS.defaultBlockState();
        helper.assertTrue(chisel.getDestroySpeed(new ItemStack(chisel), glass) == 1.0F,
                "glass gained a mineable tag, so it no longer works as the ineffective control "
                        + "block here - pick another untagged block");
        helper.assertTrue(chisel.getDestroySpeed(
                        enchanted(helper, chisel, ModEnchantments.FAST_CHISELING, 2), glass) == 1.0F,
                "Fast Chiseling speeds up a block the chisel cannot mine");

        helper.succeed();
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
     * <p><strong>What breaks this test:</strong> dropping the {@code hasColorPalette} branch in
     * {@code getPreviewStates}, letting the plain branch mix blocks, seeding the index from
     * {@code Random} instead of the position, or removing the {@code palette.isEmpty()} guard
     * (which would divide by zero).
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
        helper.assertValueEqual(plain.size(), 9,
                "the previewed plane is not the 3x3 the wand's own radius setting asks for");
        Set<Block> plainBlocks = distinctBlocks(plain);
        Set<Block> onlyPlanks = Set.of(Blocks.OAK_PLANKS);
        helper.assertValueEqual(plainBlocks, onlyPlanks,
                "a wand without Color Palette mixed blocks into its preview: " + plainBlocks);

        // --- the enchanted wand reaches for the whole hotbar ---
        Map<BlockPos, BlockState> palette = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.NORTH, diameter);
        helper.assertValueEqual(palette.size(), 9,
                "Color Palette changed how many blocks the wand previews");
        Set<Block> paletteBlocks = distinctBlocks(palette);
        Set<Block> bothBlocks = Set.of(Blocks.OAK_PLANKS, Blocks.GLASS);
        helper.assertValueEqual(paletteBlocks, bothBlocks,
                "Color Palette did not spread both carried blocks over the plane, it used " + paletteBlocks);

        // --- and does so deterministically, or the preview would flicker every frame ---
        Map<BlockPos, BlockState> again = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.NORTH, diameter);
        helper.assertValueEqual(again, palette,
                "the Color Palette preview is not stable between two calls, so it would flicker");

        // --- the striping quirk described in the javadoc ---
        // Which of the two blocks wins depends on the parity of the structure's absolute Y, so
        // only the count is asserted here, not the colour.
        Map<BlockPos, BlockState> floor = BuildingWandItem.getPreviewStates(
                helper.getLevel(), player, paletteWand, origin, Direction.UP, diameter);
        helper.assertValueEqual(distinctBlocks(floor).size(), 1,
                "Color Palette now varies within one horizontal layer. That is very likely an "
                        + "improvement, but the palette index is documented here as a function of Y "
                        + "only - re-read the javadoc and update it.");

        helper.succeed();
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
        helper.assertValueEqual(placedOffsets(helper, plainAnchor).size(), 3,
                "a wand without Color Palette did not stop after its three oak planks were used up");
        Set<Block> plainBlocks = distinctPlaced(helper, plainAnchor);
        helper.assertValueEqual(plainBlocks, Set.of(Blocks.OAK_PLANKS),
                "a wand without Color Palette placed something other than the block it was armed "
                        + "with: " + plainBlocks);

        // --- the enchanted one carries on with whatever is left ---
        helper.assertValueEqual(placedOffsets(helper, paletteAnchor).size(), 9,
                "Color Palette did not let the wand finish the plane out of a second stack");
        Set<Block> paletteBlocks = distinctPlaced(helper, paletteAnchor);
        helper.assertValueEqual(paletteBlocks, Set.of(Blocks.OAK_PLANKS, Blocks.GLASS),
                "the Color Palette run did not draw on both stacks, it placed " + paletteBlocks);
        helper.assertValueEqual(countPlaced(helper, paletteAnchor, Blocks.OAK_PLANKS), 3,
                "the Color Palette run did not spend exactly the three planks it was given");
        helper.assertValueEqual(countPlaced(helper, paletteAnchor, Blocks.GLASS), 6,
                "the Color Palette run did not fall through to the glass for the remaining six");

        helper.succeed();
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
     * <p><strong>What breaks this test:</strong> dropping the {@code isLinePlace} ternary (both
     * runs would take {@code DELAY_TICKS + 2}), swapping the two constants, reading Linear from
     * the wrong stack, or Linear starting to change {@code calculatePositions}.
     */
    public static void linearOnlyShortensTheWandStepDelay(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Two separate layers, so the 5x5 windows the two runs are read back through cannot
        // overlap and count each other's blocks.
        BlockPos plainAnchor = new BlockPos(3, 1, 3);
        BlockPos linearAnchor = new BlockPos(3, 4, 3);

        ItemStack plainWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        ItemStack linearWand = wandWithRadiusOne(new ItemStack(ModItems.DIAMOND_BUILDING_WAND));
        linearWand.enchant(enchantment(helper, ModEnchantments.LINEAR), 1);

        int plainTicks = runWandUntilIdle(helper, player, plainWand, plainAnchor, new ItemStack(Items.STONE, 64));
        int linearTicks = runWandUntilIdle(helper, player, linearWand, linearAnchor, new ItemStack(Items.STONE, 64));

        // --- the shape is untouched ---
        Set<BlockPos> plainShape = placedOffsets(helper, plainAnchor);
        Set<BlockPos> linearShape = placedOffsets(helper, linearAnchor);
        helper.assertValueEqual(plainShape.size(), 9,
                "the unenchanted wand did not fill the expected 3x3, it placed " + plainShape.size() + " blocks");
        helper.assertValueEqual(linearShape, plainShape,
                "Linear changed the shape the wand builds. That is a real feature now, so this test "
                        + "has to be replaced by one that states what the new shape is.");

        // --- only the pacing is ---
        helper.assertTrue(linearTicks < plainTicks,
                "Linear did not speed the wand up at all: " + linearTicks + " ticks against " + plainTicks);
        helper.assertValueEqual(plainTicks, BuildingWandItem.DELAY_TICKS + 2,
                "the unenchanted wand no longer paces itself with DELAY_TICKS");
        helper.assertValueEqual(linearTicks, BuildingWandItem.DELAY_TICKS_LINE + 2,
                "the Linear wand no longer paces itself with DELAY_TICKS_LINE");

        helper.succeed();
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
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Right clicks the top face of a block with a chisel or spatula and returns the block that is
     * there afterwards. Any cooldown left over from an earlier step is cleared first, so every
     * case starts from the same state instead of being silently swallowed by the previous one.
     *
     * <p>On the way through it compares {@code ChiselItem#canChisel} - the predicate the client
     * highlight uses - with what the click actually did. The two repeat the same map selection
     * in two places and have to agree.
     */
    private static Block chiselAt(GameTestHelper helper, ServerPlayer player, ItemStack chisel,
                                  BlockPos relativePos, boolean sneaking, String what) {
        player.setShiftKeyDown(sneaking);
        clearCooldown(player, chisel);
        BlockPos pos = helper.absolutePos(relativePos);

        boolean predicted = ((ChiselItem) chisel.getItem()).canChisel(helper.getLevel(), pos, chisel, player);
        InteractionResult result = useOnTopFace(helper, player, chisel, relativePos);
        boolean acted = result != InteractionResult.PASS;

        helper.assertValueEqual(predicted, acted,
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
        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_TICK_CAP) {
            item.inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < WAND_TICK_CAP,
                "the wand never finished within " + WAND_TICK_CAP + " ticks");
        return ticks;
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

    /** Runs the shared Constructor's Touch stick interaction on the top face of a block. */
    private static InteractionResult touchBlock(GameTestHelper helper, ServerPlayer player,
                                                ItemStack stack, BlockPos relativePos, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false);
        return ConstructorsTouchInteraction.handleUseBlock(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
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
        CompoundTag settings = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        settings.putInt("SettingsRadius", 1);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    private static void assertSpeed(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4F,
                what + " changed the chisel's mining speed to " + actual + " instead of " + expected);
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
