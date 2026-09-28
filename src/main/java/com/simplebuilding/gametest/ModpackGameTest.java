package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the modpack and server hooks.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ModpackTests}; the annotation only restates the runner parameters of the shared catalogue
 * in {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ModpackGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void buildingWandSkipsCellsTheLoaderEventsRefuseAndCountsItsBlocks(GameTestHelper helper) {
        ModpackTests.buildingWandSkipsCellsTheLoaderEventsRefuseAndCountsItsBlocks(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantFillSkipsCellsTheLoaderEventsRefuse(GameTestHelper helper) {
        ModpackTests.octantFillSkipsCellsTheLoaderEventsRefuse(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void lensBeamLeavesBlocksTheLoaderEventsProtect(GameTestHelper helper) {
        ModpackTests.lensBeamLeavesBlocksTheLoaderEventsProtect(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void sledgehammerAreaSwingLeavesBlocksTheLoaderEventsProtect(GameTestHelper helper) {
        ModpackTests.sledgehammerAreaSwingLeavesBlocksTheLoaderEventsProtect(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void attractorLeavesDisplayItemsOwnedItemsAndOtherPlayersDeathDropsAlone(GameTestHelper helper) {
        ModpackTests.attractorLeavesDisplayItemsOwnedItemsAndOtherPlayersDeathDropsAlone(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void oreAndCommonTagsCoverEveryModMaterial(GameTestHelper helper) {
        ModpackTests.oreAndCommonTagsCoverEveryModMaterial(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void backpackRefusesItemsFromTheNotAllowedTag(GameTestHelper helper) {
        ModpackTests.backpackRefusesItemsFromTheNotAllowedTag(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void chiselAndUpgradeTablesComeFromTheDatapackAndMatchTheBuiltInTables(GameTestHelper helper) {
        ModpackTests.chiselAndUpgradeTablesComeFromTheDatapackAndMatchTheBuiltInTables(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void datapackFilesExtendAndRemoveChiselAndUpgradeEntries(GameTestHelper helper) {
        ModpackTests.datapackFilesExtendAndRemoveChiselAndUpgradeEntries(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void lootInjectionTablesMatchTheCodeAndVanillaTablesRollThem(GameTestHelper helper) {
        ModpackTests.lootInjectionTablesMatchTheCodeAndVanillaTablesRollThem(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void modStatisticsAreRegisteredAndCountChiselUseAndTeleports(GameTestHelper helper) {
        ModpackTests.modStatisticsAreRegisteredAndCountChiselUseAndTeleports(helper);
    }
}
