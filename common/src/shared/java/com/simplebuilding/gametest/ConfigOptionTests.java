package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.loot.ModLootTableModifications;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * The config options, and whether the mod still reads them.
 *
 * <p>Before this class the only covered options were the two trade switches, and those only
 * indirectly, through the datapack conditions that consume them. An option is a promise to the
 * player, and it is a promise that breaks quietly: the field stays in {@code simplebuilding.json},
 * the GUI keeps offering the toggle, and nothing anywhere reports that the last call site that read
 * it was deleted or renamed. The tests here flip an option and check that the behaviour behind it
 * actually changes.
 *
 * <p><b>Restoring the options.</b> Every test that writes an option captures the previous value
 * first and restores it in a {@code finally} block. A failing assertion throws, so the option is
 * put back on the way out of the very same call - which is what makes these tests independent of
 * each other no matter which one fails. The original value is re-applied a second time from
 * {@link GameTestHelper#runBeforeTestEnd}, purely as a belt and braces for a failure mode that
 * unwinds past the {@code finally}.
 *
 * <p>The suite runs as one batch in a single world and shares one config object, so a flipped
 * option is global while it lasts. It cannot leak into a neighbouring test all the same: none of
 * these bodies yields - no {@code succeedWhen}, no delayed callback - so each runs from its first
 * write to its restore inside one server tick on the server thread, and no other test body can be
 * running in that window.
 *
 * <p>Only two options gate behaviour a server side gametest can reach at all - every other read of
 * {@code Simplebuilding.getConfig()} in the mod sits in {@code client/} or in a client mixin. That
 * is written down per option in the javadoc below rather than papered over with a test that would
 * pass either way.
 */
public final class ConfigOptionTests {

    private ConfigOptionTests() {
    }

    /** Resource folder the shipped trades live in: {@code data/simplebuilding/villager_trade/**}. */
    private static final String TRADE_DIRECTORY = "villager_trade";

    /** Sub folder of {@link #TRADE_DIRECTORY} that belongs to the wandering trader switch. */
    private static final String WANDERING_DIRECTORY = "wandering_trader";

    /** Id both loaders register their "read a config flag" datapack condition under. */
    private static final String CONFIG_CONDITION = SimpleBuildingGameTests.MOD_ID + ":config";

    /** Fully qualified name of Cloth Config's {@code @Config}; read reflectively, see below. */
    private static final String CONFIG_ANNOTATION = "me.shedaniel.autoconfig.annotation.Config";

    private static final String VILLAGER_FLAG = "enableVillagerTrades";
    private static final String WANDERING_FLAG = "enableWanderingTrades";

    /** Vanilla loot tables the mod hands pools to. */
    private static final List<ResourceKey<LootTable>> MODIFIED_TABLES = List.of(
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.END_CITY_TREASURE,
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.BASTION_TREASURE);

    /** Vanilla loot tables the mod must never touch - the control group for the recorder. */
    private static final List<ResourceKey<LootTable>> UNTOUCHED_TABLES = List.of(
            BuiltInLootTables.SPAWN_BONUS_CHEST,
            BuiltInLootTables.DESERT_PYRAMID);

    /**
     * Every option the config persists, as {@code group.name type=default}. Statics carry no
     * default because they are not part of the saved file - see
     * {@link #everyConfigOptionKeepsItsPersistedNameAndDefault}.
     */
    private static final Set<String> EXPECTED_OPTIONS = Set.of(
            "root.tools group:Tools",
            "root.worldGen group:WorldGen",
            "root.enableDoubleJump boolean=true",
            "root.airJumpCooldownTicks int=100",
            "root.enableArmorTrimBenefits boolean=true",
            "root.trimBenefitBaseMultiplier double runtime-only(static)",
            "root.maxMultiplierLimit double runtime-only(static)",
            "tools.invertOctantSneak boolean=false",
            "tools.buildingHighlightOpacity int=40",
            "tools.enableToolAnimations boolean=true",
            "tools.enableChiselAnimation boolean=true",
            "tools.invertBundleInteractions boolean=false",
            "worldGen.enableVillagerTrades boolean=true",
            "worldGen.enableWanderingTrades boolean=true",
            "worldGen.enableLootTableChanges boolean=true");

    // =====================================================================================
    // tools.invertBundleInteractions
    // =====================================================================================

    /**
     * {@code tools.invertBundleInteractions} swaps the two mouse buttons of the reinforced bundle:
     * off, left click stuffs an item in and right click takes one out; on, it is the other way
     * round. The option is read by two helpers, {@code getInsertClick} and {@code getRemoveClick},
     * and those two are consulted from two independent places: {@code overrideStackedOnOther} (the
     * bundle is on the cursor, the item lies in a slot) and {@code overrideOtherStackedOnMe} (the
     * item is on the cursor, the bundle lies in a slot). Both interactions are driven here in both
     * settings and in both directions - eight combinations - because the player meets both of them
     * and losing either leaves a bundle that can only be filled or only be emptied.
     *
     * <p>What breaks it: dropping either config read, so that one of the two buttons keeps its
     * vanilla meaning while the other flips; flipping the branches in only one of the two
     * interaction methods, so the toggle works when the bundle is picked up but not when it lies
     * in the inventory; or a gate that stops seeing config changes at all, in which case the
     * inverted half fails exactly as the player's toggle would.
     */
    public static void bundleClickInversionFollowsTheConfiguredOption(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        ServerPlayer player = mockPlayer(helper);
        boolean original = config.tools.invertBundleInteractions;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().tools.invertBundleInteractions = original);

        try {
            // --- switched off: left click fills, right click empties ---
            setBundleInversion(helper, false);
            assertSlotFillClick(helper, player, ClickAction.PRIMARY, true, "off / left click");
            assertSlotFillClick(helper, player, ClickAction.SECONDARY, false, "off / right click");
            assertSlotEmptyClick(helper, player, ClickAction.SECONDARY, true, "off / right click");
            assertSlotEmptyClick(helper, player, ClickAction.PRIMARY, false, "off / left click");

            assertCursorFillClick(helper, player, ClickAction.PRIMARY, true, "off / cursor / left click");
            assertCursorFillClick(helper, player, ClickAction.SECONDARY, false, "off / cursor / right click");
            assertCursorEmptyClick(helper, player, ClickAction.SECONDARY, true, "off / cursor / right click");
            assertCursorEmptyClick(helper, player, ClickAction.PRIMARY, false, "off / cursor / left click");

            // --- switched on: exactly the other way round ---
            setBundleInversion(helper, true);
            assertSlotFillClick(helper, player, ClickAction.SECONDARY, true, "inverted / right click");
            assertSlotFillClick(helper, player, ClickAction.PRIMARY, false, "inverted / left click");
            assertSlotEmptyClick(helper, player, ClickAction.PRIMARY, true, "inverted / left click");
            assertSlotEmptyClick(helper, player, ClickAction.SECONDARY, false, "inverted / right click");

            assertCursorFillClick(helper, player, ClickAction.SECONDARY, true, "inverted / cursor / right click");
            assertCursorFillClick(helper, player, ClickAction.PRIMARY, false, "inverted / cursor / left click");
            assertCursorEmptyClick(helper, player, ClickAction.PRIMARY, true, "inverted / cursor / left click");
            assertCursorEmptyClick(helper, player, ClickAction.SECONDARY, false, "inverted / cursor / right click");
        } finally {
            Simplebuilding.getConfig().tools.invertBundleInteractions = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // worldGen.enableLootTableChanges
    // =====================================================================================

    /**
     * {@code worldGen.enableLootTableChanges} is the player's only way to keep the mod out of the
     * vanilla chests. It is the very first thing {@code ModLootTableModifications.apply} looks at,
     * and it guards both ways the mod hands a pool to the loader - the builder path
     * ({@code addPool}) and the pre built path ({@code addBuiltPool}).
     *
     * <p>Production calls {@code apply} once per loot table while the datapacks load, which has
     * long happened by the time a gametest runs, so the test drives that entry point directly with
     * a recording editor instead of reloading the world.
     *
     * <p>Four tables are driven rather than one, and the end city is checked on both editor paths.
     * That is what separates "the guard is gone" from "the guard moved into one branch": a gate
     * that only still covers the stronghold would let the end city, ancient city and bastion pools
     * through and fail here. The two vanilla tables the mod never touches are recorded in the
     * switched-on state and have to come back empty - they prove the recorder reports zero when
     * nothing is added, so the switched-off half cannot pass merely because the recorder broke.
     *
     * <p>What breaks it: deleting the guard, so a player who switched the mod's loot off finds mod
     * books in a stronghold library anyway; moving the guard inside one of the branches; or a
     * table quietly losing its pools while the option is on.
     */
    public static void lootTableChangesStopWhenTheOptionIsSwitchedOff(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = config.worldGen.enableLootTableChanges;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        try {
            // --- switched on: the mod puts its pools in ---
            setLootTableChanges(helper, true);

            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                PoolRecorder recorder = recordPools(key, registries);
                helper.assertTrue(recorder.total() > 0,
                        tableName(key) + " got no mod pool although the loot table option is on");
            }

            PoolRecorder endCity = recordPools(BuiltInLootTables.END_CITY_TREASURE, registries);
            helper.assertTrue(endCity.builders > 0,
                    "the end city treasure got no builder pool although the loot table option is on");
            helper.assertTrue(endCity.built > 0,
                    "the end city treasure got no pre built pool although the loot table option is on");

            for (ResourceKey<LootTable> key : UNTOUCHED_TABLES) {
                helper.assertValueEqual(recordPools(key, registries).total(), 0,
                        "pools added to " + tableName(key) + ", a table the mod does not touch");
            }

            // --- switched off: nothing at all, on either path ---
            setLootTableChanges(helper, false);

            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                helper.assertValueEqual(recordPools(key, registries).total(), 0,
                        "pools were added to " + tableName(key)
                                + " although the loot table option is switched off");
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // worldGen.enableVillagerTrades / enableWanderingTrades
    // =====================================================================================

    /**
     * The two trade switches are not read from code at all - each shipped trade json names the flag
     * itself, once for Fabric ({@code fabric:load_conditions}) and once for NeoForge
     * ({@code neoforge:conditions}), and the loader hands that string to the mod's condition. Both
     * conditions resolve the string in a hard coded {@code switch} over exactly two cases and
     * answer anything else with {@code true} plus a log line, so a typo, a copy pasted folder or a
     * trade gated on a third flag does not fail anything: the switch simply stops working and the
     * trade ships regardless.
     *
     * <p>So this test reads the shipped files back out of the server's resource manager and checks
     * the wiring end to end: every mod trade carries both loaders' conditions, both name the same
     * flag, the flag matches the folder the trade lives in, and the set of flags in use is exactly
     * the two the conditions can actually resolve. The reflective look up on
     * {@code SimplebuildingConfig.WorldGen} that follows is the secondary check - it says whether
     * the flag still corresponds to a readable, non static boolean option, which is what the
     * conditions ultimately return.
     *
     * <p>What breaks it: adding a trade with only the Fabric condition, which then ignores the
     * switch on NeoForge (and vice versa); putting a wandering trader offer behind the villager
     * flag, so the wrong toggle turns it off; gating a trade on a flag the two condition classes do
     * not have a {@code case} for - the file would keep shipping with the toggle off, and only a
     * log line would say so. A new trade that is meant to be unconditional fails here too -
     * deliberately, because "this one is not covered by the switch any more" is exactly the
     * decision that should not be made by accident.
     */
    public static void tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders(GameTestHelper helper) {
        liveConfig(helper);
        Map<Identifier, Resource> tradeFiles = helper.getLevel().getServer().getResourceManager()
                .listResources(TRADE_DIRECTORY, id -> id.getPath().endsWith(".json"));

        List<String> problems = new ArrayList<>();
        Set<String> flagsSeen = new TreeSet<>();
        int checked = 0;

        for (Map.Entry<Identifier, Resource> entry : tradeFiles.entrySet()) {
            Identifier id = entry.getKey();
            if (!SimpleBuildingGameTests.MOD_ID.equals(id.getNamespace())) {
                continue; // vanilla or another mod's trades, not ours to police
            }
            checked++;

            JsonObject json;
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                json = GsonHelper.parse(reader);
            } catch (IOException | RuntimeException e) {
                problems.add(id + ": could not be read as json (" + e + ")");
                continue;
            }

            String fabricFlag = configFlag(json, "fabric:load_conditions", "condition");
            String neoFlag = configFlag(json, "neoforge:conditions", "type");
            String expected = id.getPath().startsWith(TRADE_DIRECTORY + "/" + WANDERING_DIRECTORY + "/")
                    ? WANDERING_FLAG
                    : VILLAGER_FLAG;

            if (fabricFlag == null) {
                problems.add(id + ": no \"fabric:load_conditions\" entry of type " + CONFIG_CONDITION
                        + " - on Fabric this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(fabricFlag);
            }
            if (neoFlag == null) {
                problems.add(id + ": no \"neoforge:conditions\" entry of type " + CONFIG_CONDITION
                        + " - on NeoForge this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(neoFlag);
            }
            if (fabricFlag != null && neoFlag != null && !fabricFlag.equals(neoFlag)) {
                problems.add(id + ": the two loaders are gated on different flags (Fabric " + fabricFlag
                        + ", NeoForge " + neoFlag + ")");
            }
            if (fabricFlag != null && !expected.equals(fabricFlag)) {
                problems.add(id + ": gated on " + fabricFlag + ", but the folder it lives in belongs to "
                        + expected);
            }
        }

        helper.assertTrue(checked > 0,
                "no " + SimpleBuildingGameTests.MOD_ID + " trade json was found under data/"
                        + SimpleBuildingGameTests.MOD_ID + "/" + TRADE_DIRECTORY
                        + "; without files to read this test proves nothing, so the listing itself is broken");

        for (String flag : flagsSeen) {
            problems.addAll(worldGenFlagProblems(flag));
        }

        helper.assertTrue(problems.isEmpty(),
                "the trade files and the config no longer line up:\n" + String.join("\n", problems));
        helper.assertValueEqual(flagsSeen, Set.of(VILLAGER_FLAG, WANDERING_FLAG),
                "the set of config flags the shipped trades are gated on - the two loader conditions "
                        + "resolve exactly these two strings and answer every other one with \"true\"");

        helper.succeed();
    }

    // =====================================================================================
    // THE OPTION SET ITSELF
    // =====================================================================================

    /**
     * Pins the saved config's identity: the file it is written to, and every option's name, group,
     * type and - for the persisted ones - default. Renaming or removing an option is silent by
     * construction. The code that reads it is renamed with it, so nothing fails to compile and
     * nothing fails at runtime; what happens instead is that the key in every player's
     * {@code config/simplebuilding.json} stops matching, their setting is dropped on the next load
     * and the option quietly reverts to its default. The file name behaves the same way, one level
     * up: change the {@code name} in the {@code @Config} annotation and every existing
     * {@code simplebuilding.json} is orphaned in place, with the mod happily writing a new file
     * beside it. This test is the place where both have to be a deliberate act.
     *
     * <p>The annotation is read by its fully qualified name instead of importing Cloth Config, so
     * this stays in the loader neutral tree and does not care which loader supplies
     * {@code me.shedaniel.autoconfig} (the Forge module ships a shim of its own).
     *
     * <p>The two multiplier fields are static, and the serializer both loaders use is Gson based,
     * which skips static fields. They are therefore runtime only: the {@code /simplebuilding}
     * command can change them for the session, but nothing writes them to disk and nothing reads
     * them back at startup. They are listed as such rather than with a default, so making one of
     * them an instance field (or making a persisted option static, which would silently stop saving
     * it) fails here. It also keeps this test independent of the trim tests, which write those two
     * statics while they run.
     *
     * <p>What breaks it: renaming, removing, retyping or re-grouping an option, changing a default,
     * adding a new one without listing it, or renaming the config file. All of those are fine
     * changes to make - they just have to be made on purpose, in one place, next to this
     * explanation.
     */
    public static void everyConfigOptionKeepsItsPersistedNameAndDefault(GameTestHelper helper) {
        SimplebuildingConfig defaults = new SimplebuildingConfig();

        List<String> problems = new ArrayList<>();
        Set<String> found = new TreeSet<>();
        collectOptions(found, problems, "root", SimplebuildingConfig.class, defaults);
        collectOptions(found, problems, "tools", SimplebuildingConfig.Tools.class, defaults.tools);
        collectOptions(found, problems, "worldGen", SimplebuildingConfig.WorldGen.class, defaults.worldGen);

        helper.assertTrue(problems.isEmpty(),
                "config options could not be read by reflection:\n" + String.join("\n", problems));
        helper.assertValueEqual(found, new TreeSet<>(EXPECTED_OPTIONS),
                "the set of config options (name, group, type, default)");

        String fileName = declaredConfigFileName();
        helper.assertTrue(fileName != null,
                "SimplebuildingConfig no longer carries a runtime visible " + CONFIG_ANNOTATION
                        + " annotation, so nothing here can tell which file the options are saved to");
        helper.assertValueEqual(fileName, SimpleBuildingGameTests.MOD_ID,
                "the name in @Config on SimplebuildingConfig - it is the base name of the file under "
                        + "config/, so changing it orphans every existing simplebuilding.json");

        // Every option test in this class writes through Simplebuilding.getConfig() and expects the
        // code under test to read the same object back. If that ever stopped being one live object,
        // those tests would fail somewhere far away with a confusing message; this says it plainly.
        SimplebuildingConfig live = liveConfig(helper);
        helper.assertTrue(Simplebuilding.getConfig() == live,
                "getConfig() handed out a different config object on the second call, so an option "
                        + "written through it would never reach the code that reads it");
        helper.assertTrue(Simplebuilding.getConfig().tools == live.tools
                        && Simplebuilding.getConfig().worldGen == live.worldGen,
                "the option groups behind getConfig() are rebuilt per call, so writing an option in a "
                        + "group has no effect on the code that reads it");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** The config object the mod actually reads, with the two groups proven to exist. */
    private static SimplebuildingConfig liveConfig(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null,
                "Simplebuilding.getConfig() handed out no config at all; every option below would be untestable");
        helper.assertTrue(config.tools != null, "the config has no \"tools\" group");
        helper.assertTrue(config.worldGen != null, "the config has no \"worldGen\" group");
        return config;
    }

    /**
     * Creates a mock server player and makes sure it leaves the server again once the test is over.
     * A leaked mock player keeps the player list non-empty and the gametest server then stalls on
     * shutdown - a failing test would cost minutes of wall clock instead of seconds.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Writes an option and reads it straight back through the same accessor the mod uses. If
     * {@code getConfig()} ever started handing out copies instead of the live object, the tests
     * would otherwise fail somewhere far away with a confusing message; this fails right here.
     */
    private static void setBundleInversion(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().tools.invertBundleInteractions = value;
        helper.assertTrue(Simplebuilding.getConfig().tools.invertBundleInteractions == value,
                "tools.invertBundleInteractions did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    /** See {@link #setBundleInversion}. */
    private static void setLootTableChanges(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().worldGen.enableLootTableChanges = value;
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges == value,
                "worldGen.enableLootTableChanges did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    // --- interaction 1: bundle on the cursor, item in the slot (overrideStackedOnOther) ---

    /**
     * One click with the bundle on the cursor while the slot below holds eight stone. Off, that is
     * what fills the bundle; inverted, it is what does nothing at all.
     */
    private static void assertSlotFillClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                            boolean expectFill, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STONE, 8));
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled = item.overrideStackedOnOther(bundle, slot, action, player);

        if (expectFill) {
            helper.assertTrue(handled, what + ": the click that should fill the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone that ended up inside the bundle");
            helper.assertTrue(slot.getItem().isEmpty(),
                    what + ": the slot should be empty afterwards but holds " + slot.getItem());
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone that ended up inside the bundle");
            helper.assertValueEqual(slot.getItem().getCount(), 8, what + ": stone left in the slot");
        }
    }

    /**
     * One click with a bundle that holds eight stone on the cursor, over an empty slot. Off, that
     * is what takes the stone back out; inverted, it is what does nothing at all.
     */
    private static void assertSlotEmptyClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                             boolean expectEmpty, String what) {
        ItemStack bundle = filledBundle(helper, player, what);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled = item.overrideStackedOnOther(bundle, slot, action, player);

        if (expectEmpty) {
            helper.assertTrue(handled, what + ": the click that should empty the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone still inside the bundle");
            helper.assertValueEqual(slot.getItem().getCount(), 8, what + ": stone handed back to the slot");
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone still inside the bundle");
            helper.assertTrue(slot.getItem().isEmpty(),
                    what + ": the slot should have stayed empty but holds " + slot.getItem());
        }
    }

    // --- interaction 2: item on the cursor, bundle in the slot (overrideOtherStackedOnMe) ---

    /**
     * The same fill, from the other side: eight stone on the cursor, clicked onto the bundle lying
     * in a slot. This is the second pair of config reads and the one the player uses most.
     */
    private static void assertCursorFillClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                              boolean expectFill, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        ItemStack cursor = new ItemStack(Items.STONE, 8);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, bundle);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {cursor};

        boolean handled = item.overrideOtherStackedOnMe(bundle, cursor, slot, action, player,
                SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));

        if (expectFill) {
            helper.assertTrue(handled, what + ": the click that should fill the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone that ended up inside the bundle");
            helper.assertTrue(cursor.isEmpty(),
                    what + ": the cursor should be empty afterwards but holds " + cursor);
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone that ended up inside the bundle");
            helper.assertValueEqual(cursor.getCount(), 8, what + ": stone left on the cursor");
        }
    }

    /**
     * The same emptying, from the other side: an empty cursor clicked onto a filled bundle lying in
     * a slot. The stack comes back onto the cursor through the {@code SlotAccess}, which is what
     * this records.
     */
    private static void assertCursorEmptyClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                               boolean expectEmpty, String what) {
        ItemStack bundle = filledBundle(helper, player, what);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, bundle);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {ItemStack.EMPTY};

        boolean handled = item.overrideOtherStackedOnMe(bundle, ItemStack.EMPTY, slot, action, player,
                SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));

        if (expectEmpty) {
            helper.assertTrue(handled, what + ": the click that should empty the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone still inside the bundle");
            helper.assertValueEqual(cursorSlot[0].getCount(), 8,
                    what + ": stone handed back onto the cursor");
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone still inside the bundle");
            helper.assertTrue(cursorSlot[0].isEmpty(),
                    what + ": the cursor should have stayed empty but holds " + cursorSlot[0]);
        }
    }

    /**
     * A reinforced bundle holding eight stone. Filled through the world pickup path on purpose:
     * that one carries no config gate, so the setup cannot quietly depend on the option under test.
     */
    private static ItemStack filledBundle(GameTestHelper helper, ServerPlayer player, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player),
                what + ": could not put stone into the bundle to begin with");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                what + ": stone in the bundle after the world pickup that set the test up");
        return bundle;
    }

    /** How many of {@code item} the bundle holds, across all of its stacks. */
    private static int countInBundle(ItemStack bundle, Item item) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : contents.itemCopyStream().toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Runs the mod's loot table hook for one table and counts what it handed over. */
    private static PoolRecorder recordPools(ResourceKey<LootTable> key, HolderLookup.Provider registries) {
        PoolRecorder recorder = new PoolRecorder();
        ModLootTableModifications.apply(key, recorder, registries);
        return recorder;
    }

    /** Readable name of a loot table for assertion messages. */
    private static String tableName(ResourceKey<LootTable> key) {
        return "the loot table " + key.identifier();
    }

    /** Counts the pools the mod offers, keeping the two editor paths apart. */
    private static final class PoolRecorder implements ModLootTableModifications.Editor {
        private int builders;
        private int built;

        @Override
        public void addPool(LootPool.Builder pool) {
            this.builders++;
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.built++;
        }

        int total() {
            return this.builders + this.built;
        }
    }

    /**
     * The flag of this file's {@code simplebuilding:config} condition, or {@code null} if it has
     * none. Fabric spells the condition id {@code condition}, NeoForge spells it {@code type};
     * both keys sit in the same file and the foreign one is ignored by each loader.
     */
    private static String configFlag(JsonObject json, String arrayKey, String typeKey) {
        JsonElement array = json.get(arrayKey);
        if (array == null || !array.isJsonArray()) {
            return null;
        }
        for (JsonElement child : array.getAsJsonArray()) {
            if (!child.isJsonObject()) {
                continue;
            }
            JsonObject condition = child.getAsJsonObject();
            JsonElement type = condition.get(typeKey);
            if (type == null || !type.isJsonPrimitive() || !CONFIG_CONDITION.equals(type.getAsString())) {
                continue;
            }
            JsonElement flag = condition.get("flag");
            if (flag != null && flag.isJsonPrimitive()) {
                return flag.getAsString();
            }
        }
        return null;
    }

    /** Everything wrong with a flag name the trade files use, as messages; empty means it is fine. */
    private static List<String> worldGenFlagProblems(String flag) {
        List<String> problems = new ArrayList<>();
        Field field;
        try {
            field = SimplebuildingConfig.WorldGen.class.getField(flag);
        } catch (NoSuchFieldException e) {
            problems.add("the trades are gated on \"" + flag + "\", but SimplebuildingConfig.WorldGen has no "
                    + "such field - both loaders answer a flag they cannot resolve with \"true\", so that "
                    + "switch is dead and only a log line says so");
            return problems;
        }
        if (field.getType() != boolean.class) {
            problems.add("worldGen." + flag + " is a " + field.getType().getSimpleName()
                    + ", but the datapack conditions read it as a boolean");
            return problems;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            problems.add("worldGen." + flag + " became static, so it is no longer part of the saved config");
        }
        try {
            field.getBoolean(Simplebuilding.getConfig().worldGen);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            problems.add("worldGen." + flag + " cannot be read off the live config (" + e + ")");
        }
        return problems;
    }

    /**
     * The base name of the file the config is saved under, taken from Cloth Config's
     * {@code @Config} annotation, or {@code null} if the annotation is gone or unreadable. Looked
     * up by name so this class needs no cloth-config import and stays loader neutral.
     */
    private static String declaredConfigFileName() {
        for (Annotation annotation : SimplebuildingConfig.class.getAnnotations()) {
            if (!CONFIG_ANNOTATION.equals(annotation.annotationType().getName())) {
                continue;
            }
            try {
                Object name = annotation.annotationType().getMethod("name").invoke(annotation);
                return name instanceof String text ? text : null;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Describes every option declared by {@code owner} as {@code group.name type=default}, reading
     * the values off {@code instance}. Nested option groups - the classes declared inside
     * {@link SimplebuildingConfig} itself - are described by their type instead of a value, statics
     * by the fact that they are static: the serializer leaves those out of the saved file, so they
     * carry no persisted default to pin.
     */
    private static void collectOptions(Set<String> into, List<String> problems, String group,
                                       Class<?> owner, Object instance) {
        if (instance == null) {
            problems.add(group + ": the option group is null");
            return;
        }
        for (Field field : owner.getFields()) {
            if (field.getDeclaringClass() != owner || field.isSynthetic()) {
                continue;
            }
            String name = group + "." + field.getName();
            String type = field.getType().getSimpleName();
            if (Modifier.isStatic(field.getModifiers())) {
                into.add(name + " " + type + " runtime-only(static)");
            } else if (field.getType().getEnclosingClass() == SimplebuildingConfig.class) {
                into.add(name + " group:" + type);
            } else {
                try {
                    into.add(name + " " + type + "=" + field.get(instance));
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    problems.add(name + " could not be read (" + e + ")");
                }
            }
        }
    }
}
