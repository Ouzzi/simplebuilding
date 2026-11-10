package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "archive" (BACK22, owned by its level branch): one method per {@code archive_*} test of {@code Band.tests}. */
public final class ArchiveGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void archiveRegistered(GameTestHelper h) {
        LayersTests.ALL.get("archive_registered").accept(h);
    }
}
