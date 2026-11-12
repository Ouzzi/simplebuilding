package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapters of the core audit fixes (AUDIT.md, simplebuilding-8). */
public final class CoreAuditGameTest {
    @GameTest public void operatorOnlyNeedsGamemasterLevel(GameTestHelper helper) { CoreAuditTests.operatorOnlyNeedsGamemasterLevel(helper); }
}
