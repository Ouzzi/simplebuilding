package com.simplebuilding.gametest;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.DynamicLightHandler;
import com.simplebuilding.util.GlowingTrimUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * "Glowing armour and portable light source" ({@code dynamic_light}): the two smithing table
 * upgrades that raise an armour piece's emission and glow level, and the handler that turns a worn
 * emission level into a real {@code minecraft:light} block in the world.
 *
 * <p>The feature is spread over four places and had no in-game coverage at all:
 * <ul>
 *   <li>{@code util/GlowingTrimUtils} - the two counters. Emission lives as a plain int in
 *       {@code CUSTOM_DATA} under the literal key {@code SimpleBuildingEmissionLevel}; glow lives
 *       in the {@code simplebuilding:glow_level} component with a fallback onto the older
 *       {@code simplebuilding:visual_glow} boolean.</li>
 *   <li>{@code mixin/SmithingScreenHandlerMixin} - a {@code @Inject(cancellable = true)} at the
 *       head of {@code SmithingMenu#createResult} that produces the upgraded copy and stops at
 *       each upgrade's cap.</li>
 *   <li>{@code util/DynamicLightHandler} - sums the emission of the four armour slots, multiplies
 *       by three, caps at 15 and writes a light block one above the player's block position.</li>
 *   <li>the datapack: {@code #minecraft:trim_templates} (which vanilla 26.2 does not ship at all -
 *       checked against the extracted server jar), the two additions to
 *       {@code #minecraft:trim_materials} and {@code #minecraft:trimmable_armor}, and the two
 *       {@code *_armor_upgrade_dummy} smithing recipes. Together they are what makes the smithing
 *       table's three input slots accept the items in the first place.</li>
 * </ul>
 *
 * <h2>How the smithing half is driven</h2>
 *
 * <p>A real {@link SmithingMenu} is built server side on a mock player, exactly the way
 * {@code HopperAndTrimTests} builds a hopper menu. Items go in through {@code Slot#set}, which
 * calls {@code Container#setChanged} and therefore {@code ItemCombinerMenu#slotsChanged} ->
 * {@code createResult} - the very method the mixin injects into. The result is read back out of
 * the menu's result slot, so nothing about the mixin is simulated.
 *
 * <p>{@code Slot#set} bypasses {@code Slot#mayPlace}, which is deliberate: the mixin's own
 * {@code isValidArmor} check and the smithing table's slot filter are two different gates and are
 * tested separately ({@link #theSmithingUpgradeOnlyFiresForArmourAndTheMatchingMaterial} vs
 * {@link #theSmithingTableTakesTheModTemplatesAndKeepsTheVanillaOnes}).
 *
 * <h2>Which mock player is used where</h2>
 *
 * <p>The light tests use {@code GameTestHelper#makeMockServerPlayer(GameType)} - a real
 * {@link ServerPlayer} that is <em>not</em> in the level and not in the player list. That matters
 * for more than cleanup: both loaders tick {@code DynamicLightHandler} for every player in the
 * player list every second server tick, so an in-level mock would be lit by that wiring in the
 * middle of a test that wants to call {@code tick} itself. The one test that <em>wants</em> the
 * wiring, {@link #theServerTickWiringLightsTheWearerOnItsOwn}, is the only one using the in-level
 * player. Because a test body runs inside a single server tick, no automatic tick can slip between
 * two statements of the other tests.
 *
 * <p>{@code DynamicLightHandler} remembers the last light position per player UUID in a
 * {@code static} map that outlives the test, so every test hands its player back through
 * {@code onDisconnect} in {@code runBeforeTestEnd}.
 *
 * <h2>Known defect</h2>
 *
 * <p><b>The brightness never changes while the player stands still.</b>
 * {@code DynamicLightHandler} line 55 gates the whole writing block behind
 * {@code currentState.isAir() || (isWater && currentState.getFluidState().isSource())}.
 * {@code minecraft:light} is registered {@code replaceable()} but <em>not</em> {@code air()}, and a
 * light block that is not waterlogged carries an empty fluid state - so as soon as the handler's
 * own light block stands at the position, neither half of that condition holds any more. The inner
 * {@code if (currentState.is(Blocks.LIGHT)) { ... LightBlock.LEVEL ... }} branch on lines 58-64 is
 * therefore dead code on dry land; it is only reachable for a <em>waterlogged</em> light block,
 * whose fluid state is a water source.
 *
 * <p>What that looks like in game: a player who upgrades or swaps armour without moving keeps the
 * old brightness until the next step. Stepping works because the "moved" branch on line 44 removes
 * the old block first and leaves air behind for the new one.
 *
 * <p>The tests below therefore never ask for a level change in place - every reading of a new
 * armour sum is taken after the player has moved on, through {@link #stepTo}. Whoever fixes line 55
 * should add the in-place case back; until then it is listed under <em>Not covered</em>.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>A brightness change without the player moving.</b> The handler cannot do it - see
 *       <em>Known defect</em> above - so nothing here asserts it, and nothing here writes the
 *       defect down as expected behaviour either. What is covered is that the level written into
 *       the world always follows the armour the player is wearing at that moment.</li>
 *   <li><b>The "only re-set the block when the level changed" shortcut</b>
 *       ({@code DynamicLightHandler} line 60). Skipping a {@code setBlock} that would write the
 *       block state that is already there has no server side effect at all - the state objects are
 *       singletons, so before and after are indistinguishable. On dry land the branch is not even
 *       reached; see <em>Known defect</em>.</li>
 *   <li><b>Anything client side.</b> The visual glow ({@code simplebuilding:glow_level},
 *       {@code hasVisualGlow}) is only read by the equipment renderer mixin, and the smithing
 *       screen's own tooltips likewise. Only the server side consequence of the glow level - the
 *       cap the smithing upgrade enforces - is tested here.</li>
 *   <li><b>"Other carriers of an emission level produce no light" for entities.</b>
 *       {@code DynamicLightHandler#tick} takes a {@code Player}; an armour stand cannot be passed
 *       to it at all, and both loaders only ever feed it the server's player list. A test putting
 *       upgraded armour on an armour stand would assert nothing beyond the method signature.</li>
 *   <li><b>Any in-game consequence of the {@code !(player instanceof ServerPlayer)} guard.</b>
 *       Both loaders feed {@code tick} from {@code server.getPlayerList().getPlayers()}, which is a
 *       {@code List<ServerPlayer>}, so the guard has no production call site at all: deleting
 *       {@code DynamicLightHandler} line 22 changes nothing a player could notice. It is still
 *       exercised with the plain (non-server) mock player in
 *       {@link #wornEmissionLevelsAddUpIntoTheLightBlockOverThePlayersHead}, but that probe pins a
 *       precaution, not behaviour - if the guard is ever removed deliberately, delete the probe
 *       with it instead of putting the guard back.</li>
 *   <li><b>The two logout registrations.</b> {@code DynamicLightHandler#onDisconnect} is called
 *       directly by the tests below, but the wiring that calls it in game is not: Fabric's
 *       {@code ServerPlayConnectionEvents.DISCONNECT} in {@code Simplebuilding} and NeoForge's
 *       {@code PlayerEvent.PlayerLoggedOutEvent} in {@code NeoForgeGameplayEvents}. Only the
 *       NeoForge half could be triggered from a shared test at all - {@code PlayerList#remove}
 *       fires the logout event, while Fabric's hangs off the packet listener's own
 *       {@code onDisconnect} and needs a real connection to close. An assertion that holds on one
 *       loader and fails on the other is a false alarm, so the gap is written down here instead.
 *       Losing either registration leaves the last light block of every logging-out player
 *       standing in the world.</li>
 *   <li><b>"Nothing but {@code assemble} reads {@code simplebuilding:light_source}".</b> That is a
 *       statement about the source tree, not about a running server. What is pinned instead is the
 *       consequence: an armour piece carrying only that component stays dark.</li>
 * </ul>
 */
public final class DynamicLightTests {

    private DynamicLightTests() {
    }

    /**
     * Tick budget for {@link #theServerTickWiringLightsTheWearerOnItsOwn}. It has to cover the
     * {@link #WIRING_SETTLE_TICKS} wait plus the framework's own start-up ticks.
     */
    public static final int TICK_WIRING_MAX_TICKS = 60;

    /**
     * How long {@link #theServerTickWiringLightsTheWearerOnItsOwn} waits. The wiring only runs on
     * even server ticks, so anything from two upwards would do; six leaves room for the player to
     * settle on the floor first.
     */
    private static final int WIRING_SETTLE_TICKS = 6;

    /**
     * Spelled out rather than read from {@code GlowingTrimUtils.EMISSION_LEVEL_KEY} on purpose.
     * The key is written into every upgraded item that has ever been saved; renaming the constant
     * would compile, keep every level test green and silently turn every existing piece of
     * emitting armour dark. Only a literal here catches that.
     */
    private static final String EMISSION_NBT_KEY = "SimpleBuildingEmissionLevel";

    /** Highest emission level the smithing upgrade hands out. */
    private static final int MAX_EMISSION_LEVEL = 5;

    /** Highest glow level the smithing upgrade hands out. */
    private static final int MAX_GLOW_LEVEL = 2;

    /** Light points one emission level is worth. */
    private static final int LIGHT_PER_EMISSION_LEVEL = 3;

    /** The four slots {@code DynamicLightHandler} sums, i.e. {@code Type.HUMANOID_ARMOR}. */
    private static final List<EquipmentSlot> ARMOUR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /** Where the light tests put their player; the lit block is one above this. */
    private static final Vec3 STAND = new Vec3(3.5, 2.0, 3.5);

    // =====================================================================================
    // (a) THE TWO LEVEL COUNTERS
    // =====================================================================================

    /**
     * {@code GlowingTrimUtils} keeps two completely separate counters, and this pins down where
     * each of them lives, what it defaults to and where it stops.
     *
     * <p>Emission: an int in {@code CUSTOM_DATA} under {@link #EMISSION_NBT_KEY}, default 0, raised
     * by one per call and frozen at {@value #MAX_EMISSION_LEVEL}. Both directions are checked
     * against the raw NBT, not only through the getter, because a getter and a setter that agree on
     * a <em>wrong</em> key would pass a getter-only test while every already-upgraded item in every
     * existing world went dark.
     *
     * <p>Glow: the {@code simplebuilding:glow_level} component, with the older
     * {@code simplebuilding:visual_glow} boolean as a fallback worth level 1. The fallback is the
     * whole reason {@code getGlowLevel} is not a plain component read - armour upgraded before the
     * level system existed only carries the boolean.
     *
     * <p>What breaks this: renaming or moving the NBT key; changing either cap; making
     * {@code incrementEmissionLevel} overwrite {@code CUSTOM_DATA} instead of merging into it (the
     * foreign entry planted below would disappear); dropping the legacy boolean fallback; or
     * letting a stored glow level of 0 count as "set" and swallow the fallback.
     */
    public static void theTwoLevelCountersKeepTheirOwnStorageAndCaps(GameTestHelper helper) {
        // --- emission: nothing there means nothing shines ---
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(ItemStack.EMPTY), 0,
                "emission level of an empty stack");
        ItemStack helmet = new ItemStack(ModItems.ENDERITE_HELMET);
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(helmet), 0,
                "emission level of an untouched enderite helmet");

        // A foreign entry in the same component, so "increment merges" can be told apart from
        // "increment replaces the whole tag".
        CompoundTag foreign = new CompoundTag();
        foreign.putString("SomeOtherModsKey", "keep me");
        helmet.set(DataComponents.CUSTOM_DATA, CustomData.of(foreign));

        // --- one level per upgrade, and a hard stop at the cap ---
        int[] expected = {1, 2, 3, 4, 5, 5};
        for (int step = 0; step < expected.length; step++) {
            GlowingTrimUtils.incrementEmissionLevel(helmet);
            helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(helmet), expected[step],
                    "emission level after " + (step + 1) + " upgrades");

            CompoundTag stored = helmet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            helper.assertValueEqual(stored.getIntOr(EMISSION_NBT_KEY, -1), expected[step],
                    "the int stored under \"" + EMISSION_NBT_KEY + "\" after " + (step + 1)
                            + " upgrades; every saved piece of emitting armour carries that exact key");
            helper.assertValueEqual(stored.getStringOr("SomeOtherModsKey", ""), "keep me",
                    "an unrelated entry in CUSTOM_DATA was lost while raising the emission level, so "
                            + "the upgrade replaces the whole tag instead of merging into it");
        }
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(helmet), MAX_EMISSION_LEVEL,
                "emission level after six upgrades; " + MAX_EMISSION_LEVEL + " is the cap");

        // --- and the getter really reads that key, not one the setter happens to share ---
        ItemStack handWritten = new ItemStack(ModItems.ENDERITE_BOOTS);
        CompoundTag tag = new CompoundTag();
        tag.putInt(EMISSION_NBT_KEY, 4);
        handWritten.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(handWritten), 4,
                "a stack carrying only the raw \"" + EMISSION_NBT_KEY + "\" tag - this is what an "
                        + "armour piece upgraded in an older world looks like");

        // --- glow: component first, legacy boolean second, 0 otherwise ---
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(ItemStack.EMPTY), 0,
                "glow level of an empty stack");
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(new ItemStack(ModItems.ENDERITE_CHESTPLATE)), 0,
                "glow level of an untouched enderite chestplate");

        ItemStack levelled = new ItemStack(ModItems.ENDERITE_CHESTPLATE);
        GlowingTrimUtils.setGlowLevel(levelled, MAX_GLOW_LEVEL);
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(levelled), MAX_GLOW_LEVEL,
                "glow level of a chestplate carrying the glow_level component");

        // The legacy flag is written straight into the component here rather than through
        // setVisualGlow: what has to keep working is reading armour that was saved before the level
        // system existed, and a setter and a getter that agreed on the *wrong* component would pass
        // a setter-driven probe while every such old piece stopped glowing.
        ItemStack legacy = new ItemStack(ModItems.ENDERITE_CHESTPLATE);
        legacy.set(ModDataComponentTypes.VISUAL_GLOW, true);
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(legacy), 1,
                "a chestplate carrying only the old visual_glow flag has to count as glow level 1; "
                        + "without that fallback every piece upgraded before the level system stops glowing");
        helper.assertTrue(GlowingTrimUtils.hasVisualGlow(legacy),
                "hasVisualGlow no longer reads simplebuilding:visual_glow");

        // setVisualGlow has no caller in the mod outside GlowingTrimUtils itself, so this pins the
        // writer against the component the fallback above reads - and nothing more than that.
        ItemStack setterWritten = new ItemStack(ModItems.ENDERITE_CHESTPLATE);
        GlowingTrimUtils.setVisualGlow(setterWritten, true);
        helper.assertTrue(setterWritten.getOrDefault(ModDataComponentTypes.VISUAL_GLOW, false),
                "setVisualGlow wrote something other than simplebuilding:visual_glow, so it and the "
                        + "legacy fallback in getGlowLevel no longer talk about the same component");

        ItemStack zeroed = new ItemStack(ModItems.ENDERITE_CHESTPLATE);
        GlowingTrimUtils.setGlowLevel(zeroed, 0);
        GlowingTrimUtils.setVisualGlow(zeroed, true);
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(zeroed), 1,
                "a stored glow level of 0 must not count as \"already levelled\" and swallow the "
                        + "legacy flag");

        // --- the two counters do not read each other's storage ---
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(levelled), 0,
                "a chestplate with glow level " + MAX_GLOW_LEVEL + " reports an emission level as well");
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(helmet), 0,
                "a helmet with emission level " + MAX_EMISSION_LEVEL + " reports a glow level as well");

        helper.succeed();
    }

    // =====================================================================================
    // (b) THE SMITHING TABLE UPGRADES
    // =====================================================================================

    /**
     * Both upgrades on a real {@link SmithingMenu}: emitting template plus glowstone dust raises
     * the emission level by one up to {@value #MAX_EMISSION_LEVEL}, glowing template plus a glow
     * ink sac raises the glow level by one up to {@value #MAX_GLOW_LEVEL}, and past the cap the
     * result slot is emptied instead of handing out a free copy.
     *
     * <p>Each step feeds the previous result back in as the base, which is what a player does, and
     * checks that the piece in the base slot is <em>not</em> mutated - the mixin works on a copy,
     * and an in-place upgrade would hand the player a levelled item without ever taking the
     * material.
     *
     * <p>The last block is the interesting one: a chestplate that has already hit the glow cap
     * still takes emitting upgrades, and the emitting upgrade leaves its glow level alone. That is
     * what makes the two caps independent rather than one shared counter.
     *
     * <p>What breaks this: either cap moving; the mixin no longer cancelling (vanilla's own lookup
     * would then answer, and for these three slots the {@code *_armor_upgrade_dummy} recipe would
     * hand out a template item instead of upgraded armour); the "cap reached" branch leaving the
     * previous result in the slot; or the upgrade writing into the base stack.
     */
    public static void bothSmithingUpgradesAddOneLevelPerStepAndStopAtTheirCap(GameTestHelper helper) {
        SmithingMenu table = smithingTable(helper);

        // --- emitting: five steps ---
        ItemStack base = new ItemStack(ModItems.ENDERITE_HELMET);
        for (int level = 1; level <= MAX_EMISSION_LEVEL; level++) {
            ItemStack result = smith(table, ModItems.EMITTING_TRIM_TEMPLATE, base, Items.GLOWSTONE_DUST);
            helper.assertTrue(!result.isEmpty(),
                    "the emitting upgrade produced no result on the way to emission level " + level);
            helper.assertTrue(result.is(ModItems.ENDERITE_HELMET),
                    "the emitting upgrade turned the helmet into " + result + " instead of upgrading it");
            helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(result), level,
                    "emission level of the smithed result at step " + level);
            helper.assertValueEqual(result.getCount(), 1, "stack size of the smithed result");
            helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(base), level - 1,
                    "the helmet lying in the base slot was upgraded in place at step " + level);
            base = result.copy();
        }

        ItemStack overCap = smith(table, ModItems.EMITTING_TRIM_TEMPLATE, base, Items.GLOWSTONE_DUST);
        helper.assertTrue(overCap.isEmpty(),
                "a helmet at emission level " + MAX_EMISSION_LEVEL + " still produced a result ("
                        + overCap + "); the cap has to empty the result slot");

        // --- glowing: two steps ---
        ItemStack glowBase = new ItemStack(ModItems.ENDERITE_CHESTPLATE);
        for (int level = 1; level <= MAX_GLOW_LEVEL; level++) {
            ItemStack result = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, glowBase, Items.GLOW_INK_SAC);
            helper.assertTrue(!result.isEmpty(),
                    "the glowing upgrade produced no result on the way to glow level " + level);
            helper.assertTrue(result.is(ModItems.ENDERITE_CHESTPLATE),
                    "the glowing upgrade turned the chestplate into " + result);
            helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(result), level,
                    "glow level of the smithed result at step " + level);
            helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(result), 0,
                    "the glowing upgrade also raised the emission level, so a purely visual upgrade "
                            + "would light up the world");
            helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(glowBase), level - 1,
                    "the chestplate lying in the base slot was upgraded in place at step " + level);
            glowBase = result.copy();
        }

        ItemStack overGlowCap = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, glowBase, Items.GLOW_INK_SAC);
        helper.assertTrue(overGlowCap.isEmpty(),
                "a chestplate at glow level " + MAX_GLOW_LEVEL + " still produced a result ("
                        + overGlowCap + "); the cap has to empty the result slot");

        // --- the two caps are independent counters, not one ---
        ItemStack mixed = smith(table, ModItems.EMITTING_TRIM_TEMPLATE, glowBase, Items.GLOWSTONE_DUST);
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(mixed), 1,
                "a chestplate that has reached the glow cap could not take an emitting upgrade any "
                        + "more, so the two upgrades share a counter");
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(mixed), MAX_GLOW_LEVEL,
                "the emitting upgrade reset the chestplate's glow level");

        helper.succeed();
    }

    /**
     * Which three items the mixin reacts to at all. {@code isValidArmor} is an OR: the base counts
     * as armour if it is in {@code #minecraft:trimmable_armor} <em>or</em> carries an
     * {@code EQUIPPABLE} component.
     *
     * <p>The second half is isolated with a stick that has been given an {@code EQUIPPABLE}
     * component and nothing else - the same item as the negative case, one component apart, so
     * only that half of the condition can explain the difference. A vanilla armour piece would not
     * do: every member of {@code #minecraft:trimmable_armor} is equippable too, which also means
     * the <em>tag</em> half of the OR cannot be isolated at all. Dropping the tag check alone would
     * change nothing observable, and that is written down here rather than pretended away.
     *
     * <p>The template and the material are checked in both directions, because the mixin's two
     * blocks are gated on an exact pair each: an emitting template with the wrong dust, a vanilla
     * template with the right dust, and the two mod templates crossed with each other's material
     * all have to leave the armour alone.
     *
     * <p>What breaks this: {@code isValidArmor} losing either half of its OR, or losing either of
     * its two call sites - the glowing block (mixin line 84) and the emitting block (line 104) ask
     * independently, so the non-armour probes are run through both; either template or material
     * check widening, which would let the netherite upgrade template or plain redstone trigger the
     * mod's upgrade.
     *
     * <p>Deliberately <em>not</em> claimed: the {@code !armorStack.isEmpty()} guard inside
     * {@code isValidArmor}. Dropping it changes nothing observable - {@code ItemStack.EMPTY} is an
     * air stack, which is neither in {@code #minecraft:trimmable_armor} nor carries an
     * {@code EQUIPPABLE} component, so the OR comes out false either way. The two empty-base probes
     * are still here, but each one sits directly behind a successful upgrade, so what they really
     * catch is a result slot that is not emptied once the inputs stop matching.
     */
    public static void theSmithingUpgradeOnlyFiresForArmourAndTheMatchingMaterial(GameTestHelper helper) {
        SmithingMenu table = smithingTable(helper);

        // --- in #minecraft:trimmable_armor: upgraded ---
        ItemStack helmet = new ItemStack(ModItems.ENDERITE_HELMET);
        helper.assertTrue(helmet.is(ItemTags.TRIMMABLE_ARMOR),
                "test setup broken: " + ModItems.ENDERITE_HELMET + " is not in "
                        + ItemTags.TRIMMABLE_ARMOR.location() + ", so the tag half of isValidArmor is "
                        + "not what is being exercised here");
        ItemStack fromTag = smith(table, ModItems.EMITTING_TRIM_TEMPLATE, helmet, Items.GLOWSTONE_DUST);
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(fromTag), 1,
                "an enderite helmet took no emitting upgrade");

        // --- equippable but not in the tag: upgraded as well ---
        ItemStack equippableStick = new ItemStack(Items.STICK);
        equippableStick.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build());
        helper.assertTrue(!equippableStick.is(ItemTags.TRIMMABLE_ARMOR),
                "test setup broken: minecraft:stick is in " + ItemTags.TRIMMABLE_ARMOR.location()
                        + ", so this probe no longer isolates the EQUIPPABLE half of isValidArmor");
        ItemStack fromComponent =
                smith(table, ModItems.EMITTING_TRIM_TEMPLATE, equippableStick, Items.GLOWSTONE_DUST);
        helper.assertTrue(!fromComponent.isEmpty(),
                "an item with an EQUIPPABLE component but outside " + ItemTags.TRIMMABLE_ARMOR.location()
                        + " was refused; that is the half of isValidArmor that lets modded armour in");
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(fromComponent), 1,
                "emission level of the upgraded equippable item");

        // --- an empty base slot must not conjure anything. It runs directly behind the upgrade
        //     above on purpose: the !isEmpty() guard itself is not observable (see the javadoc),
        //     but a result slot still holding the previous step's output is.
        ItemStack fromNothing = smith(table, ModItems.EMITTING_TRIM_TEMPLATE,
                ItemStack.EMPTY, Items.GLOWSTONE_DUST);
        helper.assertTrue(fromNothing.isEmpty(),
                "an empty base slot produced " + fromNothing + ", and the step before it had just "
                        + "put an upgraded item into that same result slot");

        // --- neither in the tag nor equippable: nothing ---
        ItemStack plainStick = smith(table, ModItems.EMITTING_TRIM_TEMPLATE,
                new ItemStack(Items.STICK), Items.GLOWSTONE_DUST);
        helper.assertTrue(plainStick.isEmpty(),
                "a plain stick was upgraded into " + plainStick + "; isValidArmor accepts anything");

        // --- the same probes through the glowing block, which asks isValidArmor a second time on
        //     its own line. Everything above runs through the emitting block only; deleting the
        //     glowing block's call would let the glowing template turn any item at all into a
        //     "glowing" one while every assertion above stayed green.
        ItemStack glowingHelmet = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, helmet, Items.GLOW_INK_SAC);
        helper.assertValueEqual(GlowingTrimUtils.getGlowLevel(glowingHelmet), 1,
                "test setup broken: the glowing upgrade no longer fires for an enderite helmet, so "
                        + "the two probes below cannot tell a refused base from a dead branch");
        ItemStack glowingFromNothing = smith(table, ModItems.GLOWING_TRIM_TEMPLATE,
                ItemStack.EMPTY, Items.GLOW_INK_SAC);
        helper.assertTrue(glowingFromNothing.isEmpty(),
                "the glowing upgrade produced " + glowingFromNothing + " from an empty base slot");
        ItemStack glowingStick = smith(table, ModItems.GLOWING_TRIM_TEMPLATE,
                new ItemStack(Items.STICK), Items.GLOW_INK_SAC);
        helper.assertTrue(glowingStick.isEmpty(),
                "the glowing template plus a glow ink sac turned a plain stick into " + glowingStick
                        + "; the glowing half of the mixin is not checking isValidArmor");

        // --- right template, wrong material ---
        ItemStack wrongMaterial = smith(table, ModItems.EMITTING_TRIM_TEMPLATE, helmet, Items.REDSTONE);
        helper.assertTrue(wrongMaterial.isEmpty(),
                "the emitting template accepted redstone instead of glowstone dust and produced "
                        + wrongMaterial);

        // --- wrong template, right material: a vanilla template must not trigger the mod upgrade ---
        ItemStack vanillaTemplate = smith(table, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                helmet, Items.GLOWSTONE_DUST);
        helper.assertTrue(vanillaTemplate.isEmpty(),
                "the netherite upgrade template plus glowstone dust produced " + vanillaTemplate
                        + "; the mod's upgrade is not gated on its own templates any more");

        // --- the two mod templates crossed with each other's material ---
        ItemStack crossed = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, helmet, Items.GLOWSTONE_DUST);
        helper.assertTrue(!crossed.is(ModItems.ENDERITE_HELMET),
                "the glowing template plus glowstone dust upgraded the helmet (" + crossed
                        + "); each block of the mixin needs its own template/material pair");
        ItemStack crossedBack = smith(table, ModItems.EMITTING_TRIM_TEMPLATE, helmet, Items.GLOW_INK_SAC);
        helper.assertTrue(!crossedBack.is(ModItems.ENDERITE_HELMET),
                "the emitting template plus a glow ink sac upgraded the helmet (" + crossedBack + ")");

        helper.succeed();
    }

    /**
     * The datapack half: the smithing table has to <em>accept</em> the items before any of the
     * above can happen. A {@code SmithingMenu}'s three input slots filter through
     * {@code RecipePropertySet.SMITHING_TEMPLATE / _BASE / _ADDITION}, which the recipe manager
     * builds out of the ingredients of every loaded smithing recipe - so this asserts the whole
     * chain from the two {@code *_armor_upgrade_dummy} recipes and the three tag files down to what
     * a player can physically drop into the slot.
     *
     * <p>{@code #minecraft:trim_templates} has no vanilla counterpart in 26.2 (the extracted server
     * jar ships {@code trim_materials} and {@code trimmable_armor}, but no {@code trim_templates});
     * the mod's file creates it, and the two dummy recipes are the only thing referencing it. The
     * vanilla control probes therefore sit on the other two tags, where a {@code "replace": true}
     * slipping into the mod's file would push vanilla's own entries out.
     *
     * <p>The same test pins down the deliberately unused {@code simplebuilding:upgrade_smithing}
     * recipe serializer, in the spirit of
     * {@code enchantment_effect_game_test_cover_and_bridge_are_inert_and_this_is_deliberately_pinned_down}:
     * it is registered, no recipe file uses it, and {@code UpgradeSmithingRecipe#assemble} - the
     * only writer of {@code simplebuilding:light_source} - therefore never runs. If a recipe ever
     * starts using it, this test says so instead of the two code paths quietly disagreeing about
     * what an upgrade does.
     *
     * <p>The last block is the counter-test to everything else in this file: two vanilla smithing
     * recipes are pushed through the <em>patched</em> menu and have to come out intact. The mixin
     * is a cancellable {@code @Inject} at the head of {@code createResult}; should its
     * {@code ci.cancel()} ever escape the two {@code if} blocks it sits in, the netherite upgrade
     * and every armour trim stop working silently, and every other probe in this class stays green
     * because they all expect either an empty result slot or the mod's own output. The slot checks
     * above cannot see it either - {@code Slot#mayPlace} goes through {@code RecipePropertySet},
     * not through {@code createResult}.
     *
     * <p>What breaks this: deleting either dummy recipe (the template slot stops taking the mod
     * templates and the recipe book stops showing the upgrade); a {@code "replace": true} in one of
     * the tag files; the enderite armour leaving {@code #minecraft:trimmable_armor}, which would
     * make the mod's own armour unusable in its own upgrade; a recipe file adopting
     * {@code simplebuilding:upgrade_smithing}; and the smithing mixin cancelling
     * {@code createResult} for inputs that are none of its business.
     */
    public static void theSmithingTableTakesTheModTemplatesAndKeepsTheVanillaOnes(GameTestHelper helper) {
        SmithingMenu table = smithingTable(helper);
        Slot template = table.getSlot(SmithingMenu.TEMPLATE_SLOT);
        Slot base = table.getSlot(SmithingMenu.BASE_SLOT);
        Slot addition = table.getSlot(SmithingMenu.ADDITIONAL_SLOT);

        // --- template slot ---
        assertAccepts(helper, template, ModItems.EMITTING_TRIM_TEMPLATE, "template");
        assertAccepts(helper, template, ModItems.GLOWING_TRIM_TEMPLATE, "template");
        assertAccepts(helper, template, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "template");
        assertAccepts(helper, template, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "template");
        assertRefuses(helper, template, Items.STICK, "template");

        // --- base slot: the mod's own armour reaches it through #minecraft:trimmable_armor ---
        assertAccepts(helper, base, ModItems.ENDERITE_HELMET, "base");
        assertAccepts(helper, base, ModItems.ENDERITE_BOOTS, "base");
        assertAccepts(helper, base, Items.DIAMOND_CHESTPLATE, "base");
        assertRefuses(helper, base, Items.STICK, "base");

        // --- addition slot: both upgrade materials, plus a vanilla trim material as control ---
        assertAccepts(helper, addition, Items.GLOWSTONE_DUST, "addition");
        assertAccepts(helper, addition, Items.GLOW_INK_SAC, "addition");
        assertAccepts(helper, addition, Items.REDSTONE, "addition");
        assertRefuses(helper, addition, Items.STICK, "addition");

        // --- the two recipes that put the mod's templates into that filter ---
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        SmithingRecipe emitting = smithingRecipe(helper, recipes, "emitting_armor_upgrade_dummy");
        assertRecipeTakes(helper, emitting, "emitting_armor_upgrade_dummy",
                ModItems.EMITTING_TRIM_TEMPLATE, ModItems.ENDERITE_HELMET, Items.GLOWSTONE_DUST);
        SmithingRecipe glowing = smithingRecipe(helper, recipes, "glowing_armor_upgrade_dummy");
        assertRecipeTakes(helper, glowing, "glowing_armor_upgrade_dummy",
                ModItems.GLOWING_TRIM_TEMPLATE, ModItems.ENDERITE_HELMET, Items.GLOW_INK_SAC);

        // --- and the serializer that is deliberately never used by any recipe file ---
        Identifier unusedId =
                Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, "upgrade_smithing");
        RecipeSerializer<?> unused = BuiltInRegistries.RECIPE_SERIALIZER.getValue(unusedId);
        helper.assertTrue(unused != null,
                "test setup broken: " + unusedId + " is not registered any more, so the pin below "
                        + "would pass without proving anything");
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            helper.assertTrue(holder.value().getSerializer() != unused,
                    holder.id().identifier() + " uses " + unusedId + ". That recipe type was dead "
                            + "code: UpgradeSmithingRecipe#assemble is the only writer of "
                            + "simplebuilding:light_source, and nothing reads that component. Waking "
                            + "it up means the smithing mixin and the recipe now disagree about what "
                            + "an upgrade produces - replace this pin with a real behaviour test.");
        }

        // --- and vanilla smithing still comes out of the patched menu ---
        ItemStack netherite = smith(table, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                new ItemStack(Items.DIAMOND_CHESTPLATE), Items.NETHERITE_INGOT);
        helper.assertTrue(netherite.is(Items.NETHERITE_CHESTPLATE),
                "the vanilla netherite upgrade produced " + netherite + " instead of a netherite "
                        + "chestplate; SmithingScreenHandlerMixin is cancelling createResult for "
                        + "inputs that are none of its business, which turns every vanilla smithing "
                        + "recipe in the game off");
        ItemStack trimmed = smith(table, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
                new ItemStack(Items.DIAMOND_CHESTPLATE), Items.REDSTONE);
        helper.assertTrue(trimmed.is(Items.DIAMOND_CHESTPLATE),
                "a vanilla armour trim produced " + trimmed + " instead of a diamond chestplate");
        helper.assertTrue(trimmed.get(DataComponents.TRIM) != null,
                "the trimmed chestplate came out of the patched menu without a minecraft:trim "
                        + "component: " + trimmed);

        helper.succeed();
    }

    // =====================================================================================
    // (c) THE LIGHT BLOCK
    // =====================================================================================

    /**
     * The arithmetic behind the light: the emission levels of the four armour slots are added up,
     * multiplied by {@value #LIGHT_PER_EMISSION_LEVEL} and capped at 15, and the block lands
     * exactly one above the block the player stands in.
     *
     * <p>The cap is asserted with two pieces at the maximum, i.e. 30 raw points. Note that losing
     * the {@code Math.min} would not merely brighten the room: {@code LightBlock.LEVEL} only
     * accepts 0..15, so {@code setValue} would throw and the test would fail loudly either way.
     *
     * <p>All four slots carry a piece in turn - head, chest, legs and <em>feet</em> - so a slot
     * filter that quietly leaves one of them out shows up as a missing three points rather than as
     * nothing at all. The four pieces sit at emission level I each, which keeps every step (3, 6, 9,
     * 12) clear of the cap and therefore distinguishable from it.
     *
     * <p>Every reading is taken at a <em>new</em> position: the handler cannot raise the level of a
     * light block that is already standing there, see <em>Known defect</em> in the class javadoc.
     * {@link #stepTo} moves the player on and clears the block above the new spot, which is exactly
     * what happens in game when a player walks.
     *
     * <p>Three further branches ride along here because they all feed the same sum:
     * <ul>
     *   <li>an item in the main hand contributes nothing - only
     *       {@code EquipmentSlot.Type.HUMANOID_ARMOR} counts;</li>
     *   <li>a piece carrying only {@code simplebuilding:light_source} (the component the unused
     *       {@code UpgradeSmithingRecipe} would set) contributes nothing - the light comes from the
     *       NBT counter alone;</li>
     *   <li>a player that is not a {@code ServerPlayer} is skipped outright, which is the guard at
     *       the top of {@code tick}. {@code GameTestHelper#makeMockPlayer} builds exactly such a
     *       {@code Player} inside the server level, so the {@code isClientSide} guard above it
     *       cannot be what is being measured. That guard has no production call site - the class
     *       javadoc says so under <em>Not covered</em> - so this probe pins a precaution, not
     *       behaviour a player could observe.</li>
     * </ul>
     *
     * <p>What breaks this: the factor or the cap changing; the {@code above()} disappearing, which
     * would put the light block inside the player's own feet; the slot filter widening to the hands
     * or narrowing so that one armour slot stops counting.
     */
    public static void wornEmissionLevelsAddUpIntoTheLightBlockOverThePlayersHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = lightTestPlayer(helper);

        // --- no armour, no light ---
        BlockPos head = stepTo(helper, player, 0);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, head, Blocks.AIR, "above a player wearing nothing");

        // --- one level is worth three light points, one block above the player ---
        wearOnly(player, EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, 1));
        head = stepTo(helper, player, 1);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, LIGHT_PER_EMISSION_LEVEL, false, "a single emission level I helmet");
        helper.assertTrue(!level.getBlockState(player.blockPosition()).is(Blocks.LIGHT),
                "the light block was written into the player's own block instead of one above it; "
                        + "a light block at foot level is inside the player's collision box");

        // --- all four armour slots are added up, one piece at a time. Every reading is taken after
        //     a step, because the handler cannot raise the level of a light block that is already
        //     standing (see "Known defect" in the class javadoc).
        player.setItemSlot(EquipmentSlot.CHEST, emitting(ModItems.ENDERITE_CHESTPLATE, 1));
        head = stepTo(helper, player, 2);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, 2 * LIGHT_PER_EMISSION_LEVEL, false,
                "a helmet and a chestplate at emission level I");

        player.setItemSlot(EquipmentSlot.LEGS, emitting(ModItems.ENDERITE_LEGGINGS, 1));
        head = stepTo(helper, player, 3);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, 3 * LIGHT_PER_EMISSION_LEVEL, false,
                "a helmet, a chestplate and leggings at emission level I");

        player.setItemSlot(EquipmentSlot.FEET, emitting(ModItems.ENDERITE_BOOTS, 1));
        head = stepTo(helper, player, 0);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, 4 * LIGHT_PER_EMISSION_LEVEL, false,
                "all four armour slots at emission level I; the boots are the slot a hand written "
                        + "list of armour slots leaves out");

        // --- a single piece at the maximum already reaches 15 ---
        wearOnly(player, EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, MAX_EMISSION_LEVEL));
        head = stepTo(helper, player, 1);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, 15, false, "one helmet at emission level " + MAX_EMISSION_LEVEL);

        // --- and two of them stay at 15 instead of running past the block's own range ---
        player.setItemSlot(EquipmentSlot.CHEST, emitting(ModItems.ENDERITE_CHESTPLATE, MAX_EMISSION_LEVEL));
        head = stepTo(helper, player, 2);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, 15, false,
                "two pieces at emission level " + MAX_EMISSION_LEVEL + " (30 raw light points)");

        // --- the hands are not armour ---
        wearOnly(player, EquipmentSlot.MAINHAND, emitting(ModItems.ENDERITE_HELMET, MAX_EMISSION_LEVEL));
        head = stepTo(helper, player, 3);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, head, Blocks.AIR,
                "a player only carrying an emitting helmet in the main hand");

        // --- the light_source component on its own is not a light source ---
        ItemStack componentOnly = new ItemStack(ModItems.ENDERITE_HELMET);
        componentOnly.set(ModDataComponentTypes.LIGHT_SOURCE, true);
        helper.assertValueEqual(GlowingTrimUtils.getEmissionLevel(componentOnly), 0,
                "a helmet carrying only simplebuilding:light_source reports an emission level");
        wearOnly(player, EquipmentSlot.HEAD, componentOnly);
        head = stepTo(helper, player, 0);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, head, Blocks.AIR,
                "a player wearing a helmet that only carries simplebuilding:light_source");

        // --- a Player that is not a ServerPlayer is skipped ---
        Player notAServerPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!(notAServerPlayer instanceof ServerPlayer),
                "test setup broken: GameTestHelper#makeMockPlayer now returns a ServerPlayer, so the "
                        + "instanceof guard in DynamicLightHandler#tick is no longer reachable here");
        Vec3 elsewhere = helper.absoluteVec(new Vec3(5.5, 2.0, 5.5));
        notAServerPlayer.snapTo(elsewhere.x, elsewhere.y, elsewhere.z, 0.0F, 0.0F);
        notAServerPlayer.setItemSlot(EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, MAX_EMISSION_LEVEL));
        BlockPos otherHead = notAServerPlayer.blockPosition().above();
        level.setBlock(otherHead, Blocks.AIR.defaultBlockState(), 3);
        DynamicLightHandler.tick(notAServerPlayer);
        assertBlockIs(helper, otherHead, Blocks.AIR,
                "a Player that is not a ServerPlayer was given a light block");

        helper.succeed();
    }

    /**
     * Where the light block is allowed to go. The handler replaces air and water <em>source</em>
     * blocks only; that condition is the single thing standing between "portable torch" and "walks
     * through your build deleting a block per step".
     *
     * <p>Four arrangements at the same position: air, solid stone, a water source and flowing
     * water. Water also has to come back when the light leaves - {@code removeLight} reads the
     * light block's own {@code WATERLOGGED} value to decide between water and air, and getting that
     * wrong leaves a trail of air bubbles behind a diver.
     *
     * <p>What breaks this: the {@code isAir() || (isWater && isSource())} condition widening (the
     * stone case starts failing and the mod eats blocks); the waterlogged flag not being set from
     * the fluid state (the light block would drain the water it replaced); or {@code removeLight}
     * dropping its waterlogged branch.
     */
    public static void theLightBlockOnlyReplacesAirOrWaterSourcesAndPutsTheWaterBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = lightTestPlayer(helper);
        BlockPos head = player.blockPosition().above();
        int expected = 2 * LIGHT_PER_EMISSION_LEVEL;
        wearOnly(player, EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, 2));

        // This is the only test in the class that leaves a water source standing, and the rooms of
        // the neighbouring tests are a few blocks away. Fluid ticks keep running after succeed()
        // until the runner clears the structure, so the water is taken back out here.
        helper.runBeforeTestEnd(
                () -> helper.getLevel().setBlock(head, Blocks.AIR.defaultBlockState(), 3));

        // --- air: lit, and not waterlogged ---
        level.setBlock(head, Blocks.AIR.defaultBlockState(), 3);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, expected, false, "an emitting helmet in open air");

        // --- stone: nothing may change. The handler is given a clean slate first, otherwise it
        //     would only be removing the light it remembers rather than deciding about the stone.
        DynamicLightHandler.onDisconnect(player);
        level.setBlock(head, Blocks.STONE.defaultBlockState(), 3);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, head, Blocks.STONE,
                "the block above the player's head; the handler overwrote a solid block, which means "
                        + "walking through a build with emitting armour deletes it one block at a time");

        // --- a water source: lit and waterlogged ---
        level.setBlock(head, Blocks.WATER.defaultBlockState(), 3);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, expected, true, "an emitting helmet inside a water source");

        // --- flowing water is not a source: left alone ---
        BlockState flowing = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
        level.setBlock(head, flowing, 3);
        DynamicLightHandler.tick(player);
        BlockState afterFlowing = level.getBlockState(head);
        helper.assertTrue(afterFlowing.is(Blocks.WATER) && afterFlowing.getValue(LiquidBlock.LEVEL) == 1,
                "flowing water above the player's head was replaced by " + afterFlowing
                        + "; only water sources may be waterlogged away");

        // --- and taking the light out of a water source gives the water back ---
        level.setBlock(head, Blocks.WATER.defaultBlockState(), 3);
        DynamicLightHandler.tick(player);
        assertLight(helper, head, expected, true, "an emitting helmet back inside a water source");
        wearOnly(player, EquipmentSlot.HEAD, ItemStack.EMPTY);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, head, Blocks.WATER,
                "the block the light left behind under water; a diver would leave a trail of air "
                        + "bubbles in the sea");
        helper.assertTrue(level.getBlockState(head).getFluidState().isSource(),
                "the water the light gave back is not a source block any more");

        helper.succeed();
    }

    /**
     * The light block's life cycle: it changes with the level, it moves with the player, it goes
     * out when the armour comes off and it is taken away when the player leaves the server.
     *
     * <p>Each of those is a separate {@code lightSources} bookkeeping branch, and each failure mode
     * is the same in the end - a light block nobody can see, nobody can mine and nobody can remove,
     * left standing in the world. The "moved" case is the one that would litter a whole route.
     *
     * <p>The brighter helmet is swapped in <em>together with</em> a step, not while standing still:
     * the handler cannot raise the level of a light block that is already there (see <em>Known
     * defect</em> in the class javadoc). What that step pins is that the level written at the new
     * position is recomputed from the armour instead of being carried over from the old block.
     *
     * <p>"Leaving the server" here means a direct call to {@code onDisconnect}. The two loader
     * registrations that call it in game are <em>not</em> covered - see the class javadoc.
     *
     * <p>What breaks this: the {@code !oldPos.equals(currentPos)} clean-up going away; the
     * {@code lightLevel == 0} branch going away, so armour can be taken off but not switched off;
     * {@code onDisconnect} no longer removing the block or no longer clearing the map entry (the
     * second {@code onDisconnect} below would then act on a stale position).
     */
    public static void theLightFollowsThePlayerAndGoesOutWithTheArmour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = lightTestPlayer(helper);

        BlockPos firstHead = stepTo(helper, player, 0);
        wearOnly(player, EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, 1));
        DynamicLightHandler.tick(player);
        assertLight(helper, firstHead, LIGHT_PER_EMISSION_LEVEL, false, "the first light");

        // --- stepping one block aside takes the light along ---
        BlockPos secondHead = stepTo(helper, player, 1);
        helper.assertTrue(!secondHead.equals(firstHead),
                "test setup broken: the player did not actually change its block position");
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, firstHead, Blocks.AIR,
                "the position the player left; without the clean-up every step leaves an invisible, "
                        + "unbreakable light block standing in the world");
        assertLight(helper, secondHead, LIGHT_PER_EMISSION_LEVEL, false,
                "the position the player moved to");

        // --- a brighter helmet writes a brighter block at the next position the light lands on.
        //     Swap and step have to happen together; the handler cannot change the level of a light
        //     block in place (see "Known defect" in the class javadoc).
        wearOnly(player, EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, 3));
        BlockPos thirdHead = stepTo(helper, player, 2);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, secondHead, Blocks.AIR,
                "the position the player left after swapping in an emission level III helmet");
        assertLight(helper, thirdHead, 3 * LIGHT_PER_EMISSION_LEVEL, false,
                "the position a player wearing an emission level III helmet stepped onto; the LEVEL "
                        + "has to be recomputed from the armour, not carried over from the old block");

        // --- taking the armour off switches the light off ---
        wearOnly(player, EquipmentSlot.HEAD, ItemStack.EMPTY);
        DynamicLightHandler.tick(player);
        assertBlockIs(helper, thirdHead, Blocks.AIR,
                "the position of a player who took the emitting armour off");

        // --- and leaving the server takes the last light with it ---
        wearOnly(player, EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, 3));
        DynamicLightHandler.tick(player);
        assertLight(helper, thirdHead, 3 * LIGHT_PER_EMISSION_LEVEL, false, "the light before logout");
        DynamicLightHandler.onDisconnect(player);
        assertBlockIs(helper, thirdHead, Blocks.AIR,
                "the position of a player who logged out while wearing emitting armour");

        // --- a second disconnect must find nothing left to do ---
        level.setBlock(thirdHead, Blocks.STONE.defaultBlockState(), 3);
        DynamicLightHandler.onDisconnect(player);
        assertBlockIs(helper, thirdHead, Blocks.STONE,
                "a block placed after logout; onDisconnect still remembers the old position and "
                        + "removed something it no longer owns");

        helper.succeed();
    }

    /**
     * The loader wiring. Nobody in this test calls {@code DynamicLightHandler} - the player is put
     * into the level with emitting armour on, and the light block has to appear on its own.
     *
     * <p>This is the only test in the class that covers the two copies of the wiring (Fabric's
     * {@code ServerTickEvents.END_SERVER_TICK} loop in {@code Simplebuilding#registerGameplayEvents}
     * and NeoForge's {@code ServerTickEvent.Post} handler in {@code NeoForgeGameplayEvents}), and it
     * is the reason it needs the in-level mock player: the wiring walks
     * {@code server.getPlayerList().getPlayers()}, which the detached mock is not part of.
     *
     * <p>The head position is read inside the delayed check rather than up front, because an
     * in-level player is ticked by the server and settles onto the floor first; the whole column
     * above the standing spot is cleared so that wherever it lands, the block above it is air.
     *
     * <p>What breaks this: either loader's registration disappearing, the tick modulo growing so
     * large that the light no longer appears within {@value #WIRING_SETTLE_TICKS} ticks, or the
     * loop being narrowed so it no longer reaches every player.
     */
    @SuppressWarnings("removal")
    public static void theServerTickWiringLightsTheWearerOnItsOwn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(STAND);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);

        // Clear the column the player may settle in, so "the block above it is air" holds wherever
        // it ends up.
        BlockPos feet = player.blockPosition();
        for (int dy = -1; dy <= 3; dy++) {
            level.setBlock(feet.above(dy), Blocks.AIR.defaultBlockState(), 3);
        }

        // Cleanup order matters: leave the player list first, so no further automatic tick can
        // re-light the room between the disconnect and the end of the test.
        helper.runBeforeTestEnd(() -> {
            helper.getLevel().getServer().getPlayerList().remove(player);
            for (EquipmentSlot slot : ARMOUR_SLOTS) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            DynamicLightHandler.onDisconnect(player);
        });

        player.setItemSlot(EquipmentSlot.HEAD, emitting(ModItems.ENDERITE_HELMET, 2));

        helper.startSequence()
                .thenExecuteAfter(WIRING_SETTLE_TICKS, () -> {
                    BlockPos head = player.blockPosition().above();
                    assertLight(helper, head, 2 * LIGHT_PER_EMISSION_LEVEL, false,
                            "the block above a player who has been standing in the level for "
                                    + WIRING_SETTLE_TICKS + " ticks wearing an emission level II "
                                    + "helmet, without anyone calling DynamicLightHandler#tick");
                })
                .thenSucceed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * A {@link ServerPlayer} that answers {@code gameMode()} but is deliberately kept out of the
     * level and the player list - see the class javadoc for why the in-level mock would fight with
     * the loaders' automatic tick. It is stripped and handed back to
     * {@code DynamicLightHandler#onDisconnect} at the end of the test, because the handler's
     * position bookkeeping is a {@code static} map that would otherwise outlive the test.
     */
    private static ServerPlayer lightTestPlayer(GameTestHelper helper) {
        Player raw = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) {
            throw helper.assertionException(
                    "GameTestHelper#makeMockServerPlayer no longer returns a ServerPlayer but a "
                            + raw.getClass().getName() + "; DynamicLightHandler only acts on ServerPlayers");
        }
        Vec3 pos = helper.absoluteVec(STAND);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> {
            for (EquipmentSlot slot : ARMOUR_SLOTS) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            DynamicLightHandler.onDisconnect(player);
        });
        return player;
    }

    /**
     * Moves the player {@code step} blocks along +X from {@link #STAND}, clears the block above its
     * new position and returns that position.
     *
     * <p>Every light reading is taken at a fresh spot, because {@code DynamicLightHandler} cannot
     * change the {@code LEVEL} of a light block that is already standing there - see <em>Known
     * defect</em> in the class javadoc. A second reading at the same position would only ever show
     * the first one's value, no matter what the player is wearing. Stepping is what a player does
     * anyway, and it is the path the handler itself keeps working: the "moved" branch removes the
     * old block first, which leaves air behind for the new one.
     *
     * <p>The offsets stay inside the loaders' empty 8x8 room ({@link #STAND} is at relative x 3, so
     * {@code step} 0..3 reaches x 3..6) and keep z at 3, clear of the second player position used
     * in {@link #wornEmissionLevelsAddUpIntoTheLightBlockOverThePlayersHead}.
     */
    private static BlockPos stepTo(GameTestHelper helper, ServerPlayer player, int step) {
        Vec3 pos = helper.absoluteVec(STAND.add(step, 0.0, 0.0));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        BlockPos head = player.blockPosition().above();
        helper.getLevel().setBlock(head, Blocks.AIR.defaultBlockState(), 3);
        return head;
    }

    /** Empties all four armour slots and the main hand, then fills exactly one of them. */
    private static void wearOnly(Player player, EquipmentSlot slot, ItemStack stack) {
        for (EquipmentSlot armour : ARMOUR_SLOTS) {
            player.setItemSlot(armour, ItemStack.EMPTY);
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(slot, stack);
    }

    /** An armour piece at the given emission level, built the way the smithing upgrade builds it. */
    private static ItemStack emitting(Item piece, int level) {
        ItemStack stack = new ItemStack(piece);
        for (int i = 0; i < level; i++) {
            GlowingTrimUtils.incrementEmissionLevel(stack);
        }
        return stack;
    }

    /** A smithing menu on a detached mock player; nothing about it touches the world. */
    private static SmithingMenu smithingTable(GameTestHelper helper) {
        Player raw = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) {
            throw helper.assertionException(
                    "GameTestHelper#makeMockServerPlayer no longer returns a ServerPlayer but a "
                            + raw.getClass().getName());
        }
        return new SmithingMenu(1, player.getInventory());
    }

    /**
     * Fills the three input slots and pushes the menu through {@code slotsChanged}, which is what
     * calls {@code createResult} - the method the mod's mixin injects into. Returns the stack that
     * ends up in the result slot.
     */
    private static ItemStack smith(SmithingMenu table, Item template, ItemStack base, Item material) {
        return smith(table, new ItemStack(template), base, new ItemStack(material));
    }

    private static ItemStack smith(SmithingMenu table, ItemStack template, ItemStack base,
                                   ItemStack material) {
        Container inputs = table.getSlot(SmithingMenu.TEMPLATE_SLOT).container;
        table.getSlot(SmithingMenu.TEMPLATE_SLOT).set(template);
        table.getSlot(SmithingMenu.BASE_SLOT).set(base);
        table.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(material);
        table.slotsChanged(inputs);
        return table.getSlot(table.getResultSlot()).getItem();
    }

    private static void assertAccepts(GameTestHelper helper, Slot slot, Item item, String which) {
        helper.assertTrue(slot.mayPlace(new ItemStack(item)),
                "the smithing table's " + which + " slot refuses " + item + "; without that the "
                        + "upgrade cannot be assembled in game at all, no matter what the mixin does");
    }

    private static void assertRefuses(GameTestHelper helper, Slot slot, Item item, String which) {
        helper.assertTrue(!slot.mayPlace(new ItemStack(item)),
                "the smithing table's " + which + " slot accepts " + item + ", so the slot filter is "
                        + "no longer a filter and the checks above prove nothing");
    }

    private static SmithingRecipe smithingRecipe(GameTestHelper helper, RecipeManager recipes, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, path);
        Optional<RecipeHolder<?>> holder = recipes.byKey(ResourceKey.create(Registries.RECIPE, id));
        helper.assertTrue(holder.isPresent(),
                "the recipe " + id + " is not loaded; it is what puts the mod's trim templates into "
                        + "the smithing table's template filter and what makes the recipe book show "
                        + "the upgrade at all");
        Recipe<?> recipe = holder.get().value();
        helper.assertTrue(recipe instanceof SmithingRecipe,
                id + " is no longer a smithing recipe but a " + recipe.getClass().getSimpleName()
                        + ", so it no longer feeds the smithing table's slot filters");
        return (SmithingRecipe) recipe;
    }

    private static void assertRecipeTakes(GameTestHelper helper, SmithingRecipe recipe, String name,
                                          Item template, Item base, Item addition) {
        helper.assertTrue(recipe.templateIngredient().isPresent()
                        && recipe.templateIngredient().get().test(new ItemStack(template)),
                name + " no longer accepts " + template + " as its template");
        helper.assertTrue(recipe.baseIngredient().test(new ItemStack(base)),
                name + " no longer accepts " + base + " as its base");
        helper.assertTrue(recipe.additionIngredient().isPresent()
                        && recipe.additionIngredient().get().test(new ItemStack(addition)),
                name + " no longer accepts " + addition + " as its addition");
    }

    private static void assertLight(GameTestHelper helper, BlockPos pos, int expectedLevel,
                                    boolean expectedWaterlogged, String what) {
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.is(Blocks.LIGHT),
                what + ": expected a minecraft:light block at " + pos + " but found " + state);
        helper.assertValueEqual(state.getValue(LightBlock.LEVEL), expectedLevel,
                what + ": light level at " + pos);
        helper.assertValueEqual(state.getValue(LightBlock.WATERLOGGED), expectedWaterlogged,
                what + ": waterlogged flag of the light block at " + pos);
    }

    private static void assertBlockIs(GameTestHelper helper, BlockPos pos, Block expected, String what) {
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.is(expected),
                what + ": expected " + expected + " at " + pos + " but found " + state);
    }
}
