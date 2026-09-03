package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.LockedFrameExtensions;
import com.simplebuilding.util.ModWorldGen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The two End ores and the lockable item frame - two features that had no test at all.
 *
 * <p><b>Ore generation.</b> Gametests run inside an 8x8x8 room in an already generated world, so
 * the ores are never actually placed by world generation here and this suite cannot prove that a
 * player will ever find one. What it <em>can</em> do is resolve the configured and placed features
 * out of the running server's datapack registries and check the numbers the mod put there, plus
 * check that the placed features really were attached to the End biomes - and only to those - by
 * whichever loader is running. That covers the failure modes that have actually bitten this mod: a
 * datagen run that was never re-executed after {@code ModWorldGen} changed, a renamed ore block
 * leaving the feature pointing at nothing, and a loader port where the biome injection (Fabric
 * {@code BiomeModifications} vs. the NeoForge {@code biome_modifier} JSON) was forgotten or
 * widened on one side. It does <em>not</em> prove vein shape, real spawn rates, or that the End
 * islands offer enough end stone for the placement to succeed.
 *
 * <p><b>Item frame.</b> This half is a real behaviour test: frames are spawned, the interactions
 * are driven server side and the resulting state is read back out of the entity.
 *
 * <p>All frame tests are registered with an unrotated structure (see
 * {@link SimpleBuildingGameTests}) because a hanging entity needs its supporting block on a fixed
 * side, and {@link #FRAME_FACING} is an absolute direction that is deliberately <em>not</em> put
 * through {@code helper.getAbsoluteDirection(...)}.
 *
 * <h2>Which mock player, and why</h2>
 * <p>The mod charges for the lock and for the shears behind {@code !player.isCreative()}, and
 * {@code makeMockServerPlayerInLevel()} is creative for good. The way to those branches here is
 * {@code GameTestHelper#makeMockPlayer(GameType)}, which returns a plain {@link Player} whose
 * {@code gameMode()} is exactly the type it was handed. It has to be that factory and not
 * {@code makeMockServerPlayer(GameType)}: that one builds a detached {@link ServerPlayer} with a
 * {@code null} connection, and every branch of the frame's interact hook ends in
 * {@code player.sendOverlayMessage(...)} - a no-op on {@code Player}, but a
 * {@code connection.send(...)} on {@code ServerPlayer}, so the survival test would die of a
 * {@code NullPointerException} inside the mod's own success path. {@code makeMockPlayer} is also
 * the only one of the two that exists on the 1.21.11 line.
 */
public final class OreGenAndItemFrameTests {

    private OreGenAndItemFrameTests() {
    }

    /** NBT key {@code ItemFrameEntityMixin} saves the lock under. */
    private static final String LOCK_TAG = "SimpleBuildingLocked";

    /** Custom-data key {@code ItemFrameEntityMixin} writes the magnet filter into. */
    private static final String MAGNET_FILTER_TAG = "MagnetFilter";

    /** Where the mock players stand; in front of both frames and clear of them. */
    private static final Vec3 PLAYER_POS = new Vec3(3.5, 3.0, 5.5);

    /** Same spot as {@link #PLAYER_POS}, for the dropped item assertions. */
    private static final BlockPos PLAYER_BLOCK = new BlockPos(3, 3, 5);

    /** Generous enough to catch a drop anywhere around the player, tight enough to stay in the room. */
    private static final double DROP_RADIUS = 4.0;

    /** Frames face south, so their supporting block sits one step north of them. */
    private static final Direction FRAME_FACING = Direction.SOUTH;

    private static final BlockPos FRAME_POS = new BlockPos(2, 3, 3);
    private static final BlockPos SECOND_FRAME_POS = new BlockPos(5, 3, 3);
    private static final BlockPos THIRD_FRAME_POS = new BlockPos(2, 5, 3);

    // =====================================================================================
    // ORE GENERATION
    // =====================================================================================

    /**
     * The two configured features have to stay ore features that replace End stone with the mod's
     * own ore block, with the vein sizes {@code ModWorldGen} asks for.
     *
     * <p>Breaks if the ore block is renamed or re-registered without re-running datagen (the
     * feature then carries a stale or missing block state), if the replace rule drifts away from
     * End stone (the ore would generate in the Overworld or nowhere at all), or if somebody edits
     * the vein sizes in {@code ModWorldGen} and ships the old JSON.
     */
    public static void endOreFeaturesCarryTheRightOreBlockAndVeinSize(GameTestHelper helper) {
        assertOreFeature(helper, ModWorldGen.ASTRALIT_ORE_KEY, ModBlocks.ASTRALIT_ORE, 3);
        assertOreFeature(helper, ModWorldGen.NIHILITH_ORE_KEY, ModBlocks.NIHILITH_ORE, 5);
        TestCleanup.succeed(helper);
    }

    /**
     * The two ores are deliberately placed in opposite ways: astralit is a rare surface find
     * (rarity filter plus a heightmap anchor and a "must be replaceable above" predicate),
     * nihilith is brute forced under the islands (no rarity filter at all, many attempts, a fixed
     * y band and a "must be replaceable below" predicate).
     *
     * <p>The placement parameters themselves are private fields with no accessors, so each
     * modifier is compared by re-encoding it through its own codec - the exact same shape datagen
     * writes to JSON. That means this test pins the numbers, not just the modifier types: the
     * rarity of 2, the counts 1 and 13, the y band 0..60 and the direction of the block predicate.
     *
     * <p>Breaks if a modifier is added, dropped or reordered, if one of those numbers changes
     * without the JSON being regenerated, or if the two placed features are swapped so that
     * nihilith ends up on the surface. Note that the German comments in {@code ModWorldGen} do
     * <em>not</em> match the code (they say rarity 8 and count 160); the code is what ships, so
     * the code is what this test pins down. {@code PlacementUtils.HEIGHTMAP} is spelled out as
     * {@code MOTION_BLOCKING} on purpose - if vanilla ever repoints that constant, the JSON in the
     * jar keeps the old heightmap and this test says so.
     */
    public static void endOrePlacementDiffersBetweenAstralitAndNihilith(GameTestHelper helper) {
        assertPlacement(helper, ModWorldGen.ASTRALIT_ORE_PLACED_KEY, ModWorldGen.ASTRALIT_ORE_KEY, List.of(
                RarityFilter.onAverageOnceEvery(2),
                CountPlacement.of(1),
                InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING),
                BlockPredicateFilter.forPredicate(BlockPredicate.replaceable(Direction.UP.getUnitVec3i())),
                BiomeFilter.biome()));

        assertPlacement(helper, ModWorldGen.NIHILITH_ORE_PLACED_KEY, ModWorldGen.NIHILITH_ORE_KEY, List.of(
                CountPlacement.of(13),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)),
                BlockPredicateFilter.forPredicate(BlockPredicate.replaceable(Direction.DOWN.getUnitVec3i())),
                BiomeFilter.biome()));

        TestCleanup.succeed(helper);
    }

    /**
     * A placed feature that nothing references generates nowhere - and one that everything
     * references generates everywhere. Fabric attaches these two through
     * {@code BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(), ...)}, NeoForge through
     * a {@code biome_modifier} JSON keyed on {@code #minecraft:is_end} - completely different
     * mechanisms that have to end in the same place: the End biomes carry the placed feature in
     * their underground ores step, and the Overworld does not carry it at all.
     *
     * <p>This is the one assertion here that covers the loader wiring rather than the data, and it
     * is the check a port breaks first. Only {@code the_end} and {@code end_highlands} are
     * required: both are in every End biome source and in {@code #minecraft:is_end}, so both
     * loaders' selectors must reach them, which keeps the test about the mod's wiring instead of
     * about the exact biome list a loader happens to select. {@code plains} is the counter sample.
     *
     * <p>Breaks if {@code ModOreGeneration.generateOres()} is no longer called on Fabric, if the
     * NeoForge biome modifier files are dropped or renamed, if the generation step is moved, or if
     * a selector is widened to something like "all biomes" and End ore starts appearing in the
     * Overworld.
     */
    public static void bothEndOresReachTheEndBiomesAndStayOutOfTheOverworld(GameTestHelper helper) {
        for (ResourceKey<PlacedFeature> ore : List.of(
                ModWorldGen.ASTRALIT_ORE_PLACED_KEY, ModWorldGen.NIHILITH_ORE_PLACED_KEY)) {
            assertOreInBiome(helper, Biomes.THE_END, ore);
            assertOreInBiome(helper, Biomes.END_HIGHLANDS, ore);
            assertOreNotInBiome(helper, Biomes.PLAINS, ore);
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ITEM FRAME: LOCK
    // =====================================================================================

    /**
     * Sneak plus a glass pane locks a filled frame; a locked frame refuses every normal right
     * click, and sneaking again gives the pane back and releases it. The lock finally has to
     * survive a save/load round trip, because it lives in a mixin field and not in vanilla data.
     *
     * <p>Every step is checked against its own opposite, so the test cannot pass on a frame that
     * simply never reacts: the frame is proven to rotate its item before the lock, proven to stop
     * rotating while locked, and proven to rotate again afterwards.
     *
     * <p>The tail pins the other side of the two creative guards in the mixin: a creative player
     * does not pay a glass pane for the lock, and a creative punch goes straight through it. Both
     * are deliberate - the lock is protection from other survival players, not from the owner -
     * and both fail here the moment the {@code isCreative()} conditions are removed.
     *
     * <p>Breaks if the lock flag stops being written or read (a locked frame would silently open
     * itself on the next server restart), if the {@code InteractionResult.FAIL} guard at the end
     * of the interact hook is lost (locked frames would be lootable again), or if the
     * "frame must not be empty" condition disappears and empty frames start swallowing panes.
     */
    public static void glassPaneLocksTheFrameAndTheLockSurvivesTheSaveRoundTrip(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        ItemFrame frame = frameWithItem(helper, FRAME_POS);

        // --- an unlocked frame still turns its item on a normal right click ---
        int rotation = frame.getRotation();
        interact(frame, player, ItemStack.EMPTY, false);
        helper.assertTrue(frame.getRotation() != rotation,
                "an unlocked frame did not react to a normal right click at all, so nothing below proves anything");

        // --- sneak plus glass pane locks it, and costs a creative player nothing ---
        ItemStack panes = new ItemStack(Items.GLASS_PANE, 3);
        InteractionResult locking = interact(frame, player, panes, true);
        helper.assertTrue(locking == InteractionResult.SUCCESS,
                "sneaking with a glass pane did not lock the frame, result was " + locking);
        helper.assertTrue(isLocked(helper, frame),
                "the interaction reported success but no lock flag was written to the frame");
        helper.assertValueEqual(panes.getCount(), 3, "glass panes left in a creative player's hand");

        CompoundTag lockedSave = saveOf(helper, frame);

        // --- while locked, a normal right click is refused and the item does not turn ---
        rotation = frame.getRotation();
        InteractionResult blocked = interact(frame, player, ItemStack.EMPTY, false);
        helper.assertTrue(blocked == InteractionResult.FAIL,
                "a locked frame accepted a normal right click, result was " + blocked);
        helper.assertValueEqual(frame.getRotation(), rotation, "rotation of the locked frame");

        // --- an empty frame must not be lockable ---
        ItemFrame empty = emptyFrame(helper, SECOND_FRAME_POS);
        interact(empty, player, new ItemStack(Items.GLASS_PANE), true);
        helper.assertFalse(isLocked(helper, empty),
                "an empty frame was locked, so the 'frame must hold an item' guard is gone");

        // --- sneaking again unlocks it and hands the pane back ---
        InteractionResult unlocking = interact(frame, player, ItemStack.EMPTY, true);
        helper.assertTrue(unlocking == InteractionResult.SUCCESS,
                "sneaking on a locked frame did not unlock it, result was " + unlocking);
        helper.assertFalse(isLocked(helper, frame), "the frame is still locked after the unlock interaction");
        helper.assertItemEntityPresent(Items.GLASS_PANE, PLAYER_BLOCK, DROP_RADIUS);

        rotation = frame.getRotation();
        interact(frame, player, ItemStack.EMPTY, false);
        helper.assertTrue(frame.getRotation() != rotation, "the frame kept refusing right clicks after it was unlocked");

        // --- the lock comes back with the saved data ---
        // Deliberately a second, never added frame: loading into the frame that is already in the
        // level would hand it a second entity with the same UUID.
        ItemFrame reloaded = new ItemFrame(helper.getLevel(), helper.absolutePos(FRAME_POS), FRAME_FACING);
        reloaded.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), lockedSave));
        helper.assertTrue(isLocked(helper, reloaded), "the lock flag did not survive a save and load round trip");
        InteractionResult afterLoad = interact(reloaded, player, ItemStack.EMPTY, false);
        helper.assertTrue(afterLoad == InteractionResult.FAIL,
                "a frame that was loaded as locked behaves as unlocked, result was " + afterLoad);

        // --- a creative punch is deliberately not stopped by the lock ---
        interact(frame, player, new ItemStack(Items.GLASS_PANE), true);
        helper.assertTrue(isLocked(helper, frame), "the frame did not lock again, the punch below would prove nothing");
        DamageSource creativePunch = helper.getLevel().damageSources().playerAttack(player);
        helper.assertTrue(frame.hurtServer(helper.getLevel(), creativePunch, 1.0F),
                "a creative player could no longer break a locked frame; the hurtServer guard has stopped "
                        + "checking isCreative() and now locks the owner out as well");
        helper.assertTrue(frame.getItem().isEmpty(), "the locked frame kept its item after a creative punch");

        TestCleanup.succeed(helper);
    }

    /**
     * Sneak plus shears hides a filled frame, sneaking again brings it back, and an empty frame is
     * never hidden. The last block is about priority: the lock branch is checked before the shear
     * branch, so shears on a locked frame have to unlock it rather than hide it.
     *
     * <p>That ordering is the whole safety story of the feature - if the shear branch ever moves
     * in front of the unlock branch, a locked frame can be made invisible and then no longer be
     * unlocked in any obvious way.
     *
     * <p>Breaks if the shear branch loses its {@code !isInvisible()} condition (the frame could
     * never be revealed again), if the "already invisible" fallback disappears, if the branch
     * order in the interact hook changes, or if a creative player suddenly starts paying shear
     * durability.
     */
    public static void shearsHideTheFrameAndTheLockTakesPriorityOverThem(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        ItemFrame frame = frameWithItem(helper, FRAME_POS);
        helper.assertFalse(frame.isInvisible(), "a freshly spawned item frame is already invisible");

        // --- sneak plus shears hides it, free of charge in creative ---
        ItemStack shears = new ItemStack(Items.SHEARS);
        InteractionResult hide = interact(frame, player, shears, true);
        helper.assertTrue(hide == InteractionResult.SUCCESS,
                "sneaking with shears did not hide the frame, result was " + hide);
        helper.assertTrue(frame.isInvisible(), "the frame is still visible after the shear interaction");
        helper.assertValueEqual(shears.getDamageValue(), 0, "shear durability spent by a creative player");

        // --- sneaking again is the only way back ---
        InteractionResult reveal = interact(frame, player, new ItemStack(Items.SHEARS), true);
        helper.assertTrue(reveal == InteractionResult.SUCCESS,
                "sneaking on a hidden frame did not reveal it, result was " + reveal);
        helper.assertFalse(frame.isInvisible(), "the frame stayed invisible, so it can never be found again");

        // --- an empty frame is not hidden (vanilla puts the shears in it instead) ---
        ItemFrame empty = emptyFrame(helper, SECOND_FRAME_POS);
        interact(empty, player, new ItemStack(Items.SHEARS), true);
        helper.assertFalse(empty.isInvisible(),
                "an empty frame was hidden, so the 'frame must hold an item' guard is gone");

        // --- on a locked frame the shears unlock instead of hiding ---
        interact(frame, player, new ItemStack(Items.GLASS_PANE), true);
        helper.assertTrue(isLocked(helper, frame), "the frame did not lock, the rest of this test would prove nothing");
        InteractionResult shearsOnLocked = interact(frame, player, new ItemStack(Items.SHEARS), true);
        helper.assertTrue(shearsOnLocked == InteractionResult.SUCCESS,
                "shears on a locked frame did nothing at all, result was " + shearsOnLocked);
        helper.assertFalse(isLocked(helper, frame), "shears on a locked frame did not take the unlock branch");
        helper.assertFalse(frame.isInvisible(), "shears hid a locked frame instead of unlocking it");

        TestCleanup.succeed(helper);
    }

    /**
     * Everything the feature only does for players who are <em>not</em> in creative: the glass
     * pane is taken out of the hand, the pane really comes back on unlock, the shears lose
     * durability, and a locked frame shrugs off a punch that would otherwise knock the item out
     * of it.
     *
     * <p>These branches are usually out of reach, because {@code makeMockServerPlayerInLevel()}
     * returns a player whose {@code gameMode()} is hard wired to {@code CREATIVE}. The way in is
     * {@code makeMockPlayer(GameType)}: its anonymous subclass of {@link Player} returns exactly
     * the game type it was handed, so {@code isCreative()} is false for a survival one, and -
     * unlike the {@code ServerPlayer} the sibling factory builds - its {@code sendOverlayMessage}
     * is the empty {@code Player} implementation, so the overlay message at the end of every
     * branch of the mixin cannot walk into a null connection. The player is never put in the
     * player list either, so it cannot stall server shutdown. The whole survival half of the
     * feature is confined to this one test so that a surprise from that factory cannot take the
     * other frame tests down with it.
     *
     * <p>The last block is uncomfortable on purpose: the guard in {@code hurtServer} only looks at
     * {@code source.getDirectEntity() instanceof Player}, so a source with no player behind it -
     * TNT, a skeleton's arrow, a cactus - empties a locked frame exactly as before. That is pinned
     * rather than hidden; it is not a claim that it is right, and it fails the day the guard is
     * widened, which is the day a proper test for the wider guard is owed.
     *
     * <p>Breaks if the frame stops charging for the lock or for the shears (both become free), if
     * the pane is no longer handed back on unlock, or if the damage guard is lost - which is the
     * entire point of locking a frame, since a single punch would empty it again.
     */
    public static void survivalPlayersPayForTheLockAndCannotBreakTheLockedFrame(GameTestHelper helper) {
        Player player = survivalPlayer(helper);
        helper.assertFalse(player.isCreative(),
                "the survival mock player reports creative mode, so this test would silently prove nothing");
        helper.assertFalse(player.getAbilities().instabuild,
                "the survival mock player has instabuild set, so no hurtAndBreak anywhere would do anything");

        ItemFrame frame = frameWithItem(helper, FRAME_POS);

        // --- the pane is taken out of the hand ---
        ItemStack panes = new ItemStack(Items.GLASS_PANE, 3);
        interact(frame, player, panes, true);
        helper.assertTrue(isLocked(helper, frame), "the survival player could not lock the frame at all");
        helper.assertValueEqual(panes.getCount(), 2, "glass panes left in the survival player's hand");

        // --- a punch does not empty a locked frame ---
        DamageSource punch = helper.getLevel().damageSources().playerAttack(player);
        helper.assertFalse(frame.hurtServer(helper.getLevel(), punch, 1.0F),
                "the locked frame accepted damage from a survival player");
        helper.assertFalse(frame.getItem().isEmpty(),
                "the locked frame lost its item to a survival player's punch");

        // --- ... but a source without a player behind it still does. Pinned, not endorsed. ---
        ItemFrame collateral = frameWithItem(helper, SECOND_FRAME_POS);
        interact(collateral, player, new ItemStack(Items.GLASS_PANE), true);
        helper.assertTrue(isLocked(helper, collateral), "the second frame did not lock");
        helper.assertTrue(collateral.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F),
                "a locked frame now survives damage that has no player behind it. That is very likely an "
                        + "improvement - the guard in hurtServer only ever checked getDirectEntity() instanceof "
                        + "Player - but it needs its own test now; replace this pin with one.");

        // --- unlocked, the very same punch knocks the item out, and the pane comes back ---
        interact(frame, player, ItemStack.EMPTY, true);
        helper.assertFalse(isLocked(helper, frame), "the frame did not unlock");
        helper.assertItemEntityPresent(Items.GLASS_PANE, PLAYER_BLOCK, DROP_RADIUS);
        helper.assertTrue(frame.hurtServer(helper.getLevel(), punch, 1.0F),
                "an unlocked frame ignored the punch as well, so the protection above proves nothing");
        helper.assertTrue(frame.getItem().isEmpty(), "the unlocked frame kept its item after a punch");

        // --- hiding a frame costs shear durability ---
        ItemFrame third = frameWithItem(helper, THIRD_FRAME_POS);
        ItemStack shears = new ItemStack(Items.SHEARS);
        interact(third, player, shears, true);
        helper.assertTrue(third.isInvisible(), "the survival player could not hide the frame");
        helper.assertValueEqual(shears.getDamageValue(), 1, "shear durability spent on hiding a frame");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ITEM FRAME: MAGNET FILTER
    // =====================================================================================

    /**
     * The third branch of the frame's interact hook: a magnet carrying Constructor's Touch reads
     * the framed item's id into its own {@code MagnetFilter} custom data, so a frame can be used
     * as a filter template. It is the only branch of the mixin that fires without sneaking, and it
     * has to swallow the click - otherwise the frame would rotate its item every time somebody
     * sets a filter.
     *
     * <p>All three conditions are driven against their opposite: a magnet without the enchantment
     * writes nothing and falls through to the vanilla rotation, an enchanted magnet on an
     * <em>empty</em> frame writes nothing either (there is nothing to point at), and only the
     * enchanted magnet on a filled frame both writes the id and stops the rotation.
     *
     * <p>Breaks if the enchantment lookup in the mixin stops resolving (a renamed enchantment id
     * would silently turn every magnet into a plain one), if the {@code !getItem().isEmpty()}
     * condition is dropped so an empty frame clears the filter to nothing, or if the branch stops
     * returning {@code SUCCESS} and vanilla rotates the item behind it.
     */
    public static void constructorsTouchMagnetTakesItsFilterFromTheFramedItem(GameTestHelper helper) {
        ServerPlayer player = creativePlayer(helper);
        ItemFrame frame = frameWithItem(helper, FRAME_POS);

        // --- a plain magnet is just an item: no filter, and the frame turns as usual ---
        ItemStack plain = new ItemStack(ModItems.MAGNET);
        int rotation = frame.getRotation();
        interact(frame, player, plain, false);
        helper.assertValueEqual(filterOf(plain), "",
                "an unenchanted magnet wrote a filter, so the Constructor's Touch check is gone");
        helper.assertTrue(frame.getRotation() != rotation,
                "an unenchanted magnet swallowed the click; the magnet branch now fires without the enchantment");

        // --- with Constructor's Touch it takes the id and consumes the click ---
        ItemStack enchanted = magnetWithConstructorsTouch(helper);
        rotation = frame.getRotation();
        InteractionResult result = interact(frame, player, enchanted, false);
        helper.assertTrue(result == InteractionResult.SUCCESS,
                "an enchanted magnet did not take the filter branch, result was " + result);
        helper.assertValueEqual(filterOf(enchanted), "minecraft:diamond", "magnet filter taken from the frame");
        helper.assertValueEqual(frame.getRotation(), rotation,
                "rotation of the frame the filter was read from - the branch has to swallow the click");

        // --- an empty frame offers nothing to filter on ---
        ItemFrame empty = emptyFrame(helper, SECOND_FRAME_POS);
        ItemStack onEmpty = magnetWithConstructorsTouch(helper);
        interact(empty, player, onEmpty, false);
        helper.assertValueEqual(filterOf(onEmpty), "",
                "an empty frame set a magnet filter, so the 'frame must hold an item' guard is gone");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ITEM FRAME: THE UNFINISHED HALF
    // =====================================================================================

    /**
     * The brush-to-reveal loop in {@code BlockAttachedEntityTickMixin} is dead code: it only runs
     * for an entity that implements {@code LockedFrameExtensions}, and nothing does -
     * {@code ItemFrameEntityMixin} keeps its lock in a plain {@code @Unique} field and never adds
     * the interface. Nothing ever calls {@code simplebuilding$setBrushingPlayer} either, so the
     * guard could not fire even if the interface were there.
     *
     * <p>Like the Cover/Bridge marker in {@link EnchantmentEffectTests}, this pins the state
     * rather than hiding it. It is <em>not</em> a claim that the current state is correct: it is a
     * marker that the brush half of the feature is unfinished, and it fails the moment somebody
     * wires the interface up - which is exactly when a real behaviour test has to be written.
     */
    public static void brushRevealIsWiredToAnInterfaceNothingImplements(GameTestHelper helper) {
        ItemFrame frame = frameWithItem(helper, FRAME_POS);
        helper.assertFalse(frame instanceof LockedFrameExtensions,
                "ItemFrame now implements LockedFrameExtensions, so the brush-to-reveal loop in "
                        + "BlockAttachedEntityTickMixin can finally run. Good news - but it now needs a real "
                        + "behaviour test; replace this marker with one.");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS: ORE GENERATION
    // =====================================================================================

    private static void assertOreFeature(GameTestHelper helper, ResourceKey<ConfiguredFeature<?, ?>> key,
                                         Block ore, int veinSize) {
        Registry<ConfiguredFeature<?, ?>> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> configured = registry.getValue(key);
        helper.assertTrue(configured != null,
                key.identifier() + " is not in the configured feature registry; the generated worldgen JSON is "
                        + "missing from the jar or was never regenerated");

        // Ueber Feature<?> statt direkt, sonst vergleicht javac zwei Capture-Typen miteinander.
        Feature<?> feature = configured.feature();
        helper.assertTrue(feature == Feature.ORE, key.identifier() + " is no longer an ore feature");
        helper.assertTrue(configured.config() instanceof OreConfiguration,
                key.identifier() + " no longer carries an OreConfiguration");
        OreConfiguration config = (OreConfiguration) configured.config();

        helper.assertValueEqual(config.size, veinSize, key.identifier() + " vein size");
        helper.assertValueEqual(config.targetStates.size(), 1, key.identifier() + " target count");
        helper.assertTrue(config.discardChanceOnAirExposure == 0.0F,
                key.identifier() + " now discards ore on air exposure, which changes how much of it is reachable");

        OreConfiguration.TargetBlockState target = config.targetStates.get(0);
        helper.assertTrue(target.state.is(ore),
                key.identifier() + " places " + target.state + " instead of the mod's ore block");

        // Das Ersetzungs-Muster wird gefahren, nicht nur verglichen: End-Stein ja, alles andere nein.
        RandomSource random = RandomSource.create();
        helper.assertTrue(target.target.test(Blocks.END_STONE.defaultBlockState(), random),
                key.identifier() + " no longer replaces end stone, so it cannot generate in the End");
        helper.assertFalse(target.target.test(Blocks.STONE.defaultBlockState(), random),
                key.identifier() + " also replaces overworld stone, so the ore leaks out of the End");
    }

    private static void assertPlacement(GameTestHelper helper, ResourceKey<PlacedFeature> placedKey,
                                        ResourceKey<ConfiguredFeature<?, ?>> configuredKey,
                                        List<PlacementModifier> expectedModifiers) {
        Registry<PlacedFeature> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature placed = registry.getValue(placedKey);
        helper.assertTrue(placed != null,
                placedKey.identifier() + " is not in the placed feature registry");

        helper.assertTrue(placed.feature().is(configuredKey),
                placedKey.identifier() + " points at " + placed.feature().getRegisteredName()
                        + " instead of " + configuredKey.identifier());

        List<Tag> actual = describe(placed.placement());
        List<Tag> expected = describe(expectedModifiers);
        helper.assertValueEqual(actual.size(), expected.size(),
                placedKey.identifier() + " placement modifier count; it reads " + actual);
        for (int i = 0; i < expected.size(); i++) {
            helper.assertValueEqual(actual.get(i), expected.get(i),
                    placedKey.identifier() + " placement modifier #" + i);
        }
    }

    /**
     * Renders a modifier list through the placement codec - the same shape datagen writes to JSON.
     * The modifiers keep their parameters in private fields with no accessors, so this is the only
     * way to compare the actual numbers instead of just the modifier types. Tags are compared
     * rather than their text, so key order inside a modifier cannot make the test flap.
     */
    private static List<Tag> describe(List<PlacementModifier> modifiers) {
        List<Tag> out = new ArrayList<>(modifiers.size());
        for (PlacementModifier modifier : modifiers) {
            out.add(PlacementModifier.CODEC.encodeStart(NbtOps.INSTANCE, modifier).getOrThrow());
        }
        return out;
    }

    private static void assertOreInBiome(GameTestHelper helper, ResourceKey<Biome> biomeKey,
                                         ResourceKey<PlacedFeature> placedKey) {
        int foundAt = stepOf(helper, biomeKey, placedKey);
        helper.assertTrue(foundAt >= 0,
                placedKey.identifier() + " was never added to " + biomeKey.identifier()
                        + ", so it can never generate; the loader's biome injection is missing");
        helper.assertValueEqual(foundAt, GenerationStep.Decoration.UNDERGROUND_ORES.ordinal(),
                placedKey.identifier() + " generation step in " + biomeKey.identifier());
    }

    private static void assertOreNotInBiome(GameTestHelper helper, ResourceKey<Biome> biomeKey,
                                            ResourceKey<PlacedFeature> placedKey) {
        helper.assertValueEqual(stepOf(helper, biomeKey, placedKey), -1,
                placedKey.identifier() + " is attached to " + biomeKey.identifier()
                        + "; the loader's biome selector is too wide and End ore leaks out of the End "
                        + "(step index, -1 means not attached)");
    }

    /** Index of the generation step {@code placedKey} sits in, or {@code -1} if the biome has it nowhere. */
    private static int stepOf(GameTestHelper helper, ResourceKey<Biome> biomeKey,
                              ResourceKey<PlacedFeature> placedKey) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Biome biome = biomes.getValue(biomeKey);
        helper.assertTrue(biome != null, "the biome " + biomeKey.identifier() + " is not in the registry");

        List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
        for (int step = 0; step < steps.size(); step++) {
            for (Holder<PlacedFeature> holder : steps.get(step)) {
                if (holder.is(placedKey)) {
                    return step;
                }
            }
        }
        return -1;
    }

    // =====================================================================================
    // HELPERS: ITEM FRAME
    // =====================================================================================

    /**
     * The house standard mock player: fully placed in the level, with a connection, and creative
     * whether we like it or not. Handed back when the test ends, because a leaked mock player
     * keeps the player list non-empty and stalls the gametest server on shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer creativePlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(PLAYER_POS);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * A mock player that really is in survival - see
     * {@link #survivalPlayersPayForTheLockAndCannotBreakTheLockedFrame} for why this factory and
     * not the {@code ServerPlayer} one. The abilities are refreshed from the game type because
     * {@code makeMockPlayer} - unlike its {@code ServerPlayer} sibling - does not do it itself,
     * and {@code instabuild} decides whether any {@code hurtAndBreak} in the game does anything
     * at all. Nothing to hand back here: this player never enters the player list, so it cannot
     * stall the shutdown either.
     */
    private static Player survivalPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
        Vec3 pos = helper.absoluteVec(PLAYER_POS);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static ItemFrame frameWithItem(GameTestHelper helper, BlockPos relativePos) {
        ItemFrame frame = emptyFrame(helper, relativePos);
        frame.setItem(new ItemStack(Items.DIAMOND), false);
        return frame;
    }

    /** Spawns a frame plus the block it hangs on, and takes it out again when the test ends. */
    private static ItemFrame emptyFrame(GameTestHelper helper, BlockPos relativePos) {
        helper.setBlock(relativePos.relative(FRAME_FACING.getOpposite()), Blocks.STONE);
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(relativePos), FRAME_FACING);
        helper.getLevel().addFreshEntity(frame);
        TestCleanup.before(helper, frame::discard);
        return frame;
    }

    /**
     * Right clicks the frame server side. The stack is handed in by reference on purpose - the
     * player's hand slot keeps that very object - so a test can check afterwards whether the
     * interaction took something out of it or wrote a component into it.
     */
    private static InteractionResult interact(ItemFrame frame, Player player, ItemStack held, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return frame.interact(player, InteractionHand.MAIN_HAND);
    }

    /**
     * The lock lives in a {@code @Unique} mixin field with no accessor, so it is read the way the
     * game reads it: through the entity's save data. That has the pleasant side effect of testing
     * the save injection at the same time.
     */
    private static boolean isLocked(GameTestHelper helper, ItemFrame frame) {
        return saveOf(helper, frame).getBooleanOr(LOCK_TAG, false);
    }

    private static CompoundTag saveOf(GameTestHelper helper, ItemFrame frame) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        frame.saveWithoutId(output);
        return output.buildResult();
    }

    private static ItemStack magnetWithConstructorsTouch(GameTestHelper helper) {
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        Holder<Enchantment> enchantment = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.CONSTRUCTORS_TOUCH);
        magnet.enchant(enchantment, 1);
        return magnet;
    }

    /** The filter the mixin wrote into the magnet, or {@code ""} when it wrote nothing. */
    private static String filterOf(ItemStack magnet) {
        return magnet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(MAGNET_FILTER_TAG, "");
    }
}
