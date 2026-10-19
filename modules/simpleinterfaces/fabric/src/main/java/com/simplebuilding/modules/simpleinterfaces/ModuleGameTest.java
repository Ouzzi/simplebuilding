package com.simplebuilding.modules.simpleinterfaces;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge mirrors it in ModuleNeoTests. */
public final class ModuleGameTest {
    @GameTest public void boxLayouts(GameTestHelper h) { ContainerTests.layouts(h); }
    @GameTest public void boxLayoutLimits(GameTestHelper h) { ContainerTests.layoutLimits(h); }
    @GameTest public void narrowBoxLayouts(GameTestHelper h) { ContainerTests.narrowLayouts(h); }
    @GameTest public void styleRegistry(GameTestHelper h) { ContainerTests.registry(h); }
    @GameTest public void blockPalettes(GameTestHelper h) { ContainerTests.palettes(h); }
    @GameTest public void boxMotifs(GameTestHelper h) { ContainerTests.motifs(h); }
    @GameTest public void subModSwitch(GameTestHelper h) { ContainerTests.subModSwitch(h); }
    @GameTest public void configDefaults(GameTestHelper h) { ContainerTests.config(h); }
    @GameTest public void legacyMigration(GameTestHelper h) { ContainerTests.legacyMigration(h); }
    @GameTest public void styleToggle(GameTestHelper h) { ContainerTests.styleToggle(h); }
    @GameTest public void stationRegistry(GameTestHelper h) { ContainerTests.stationRegistry(h); }
    @GameTest public void stationPalettes(GameTestHelper h) { ContainerTests.stationPalettes(h); }
    @GameTest public void stationLayouts(GameTestHelper h) { ContainerTests.stationLayouts(h); }
    @GameTest public void workLayouts(GameTestHelper h) { ContainerTests.workLayouts(h); }
    @GameTest public void workPalettes(GameTestHelper h) { ContainerTests.workPalettes(h); }
    @GameTest(maxTicks = 100) public void linkedMark(GameTestHelper h) { LinkedTests.linkedMark(h); }
    @GameTest(maxTicks = 100) public void linkedRange(GameTestHelper h) { LinkedTests.linkedRange(h); }
    @GameTest(maxTicks = 100) public void linkedTransfer(GameTestHelper h) { LinkedTests.linkedTransfer(h); }
    @GameTest(maxTicks = 100) public void linkedInventory(GameTestHelper h) { LinkedTests.linkedInventory(h); }
    @GameTest(maxTicks = 100) public void linkedEnd(GameTestHelper h) { LinkedTests.linkedEnd(h); }
    @GameTest(maxTicks = 100) public void linkedCreative(GameTestHelper h) { LinkedTests.linkedCreative(h); }
    @GameTest(maxTicks = 100) public void portableShulker(GameTestHelper h) { LinkedTests.portableShulker(h); }
    @GameTest(maxTicks = 100) public void portableEnderChest(GameTestHelper h) { LinkedTests.portableEnderChest(h); }
    @GameTest(maxTicks = 100) public void linkedQolMigration(GameTestHelper h) { LinkedTests.qolMigration(h); }
    @GameTest(maxTicks = 100) public void linkedSync(GameTestHelper h) { LinkedTests.sync(h); }
    @GameTest(maxTicks = 100) public void linkedSuperSwitch(GameTestHelper h) { LinkedTests.superSwitch(h); }
    @GameTest public void tooltipEnderSnapshot(GameTestHelper h) { TooltipTests.enderSnapshot(h); }
    @GameTest public void tooltipSnapshotCodec(GameTestHelper h) { TooltipTests.snapshotCodec(h); }
    @GameTest public void tooltipShulkerGrid(GameTestHelper h) { TooltipTests.shulkerGrid(h); }
    @GameTest public void tooltipConfigOff(GameTestHelper h) { TooltipTests.configOff(h); }
    @GameTest public void tooltipEnderClose(GameTestHelper h) { TooltipTests.enderClose(h); }
}
