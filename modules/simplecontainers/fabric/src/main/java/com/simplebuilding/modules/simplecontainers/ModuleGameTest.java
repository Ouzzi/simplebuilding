package com.simplebuilding.modules.simplecontainers;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge mirrors it in ModuleNeoTests. */
public final class ModuleGameTest {
    @GameTest public void boxLayouts(GameTestHelper h) { ContainerTests.layouts(h); }
    @GameTest public void boxLayoutLimits(GameTestHelper h) { ContainerTests.layoutLimits(h); }
    @GameTest public void narrowBoxLayouts(GameTestHelper h) { ContainerTests.narrowLayouts(h); }
    @GameTest public void styleRegistry(GameTestHelper h) { ContainerTests.registry(h); }
    @GameTest public void blockPalettes(GameTestHelper h) { ContainerTests.palettes(h); }
    @GameTest public void configDefaults(GameTestHelper h) { ContainerTests.config(h); }
    @GameTest public void workLayouts(GameTestHelper h) { ContainerTests.workLayouts(h); }
    @GameTest public void workPalettes(GameTestHelper h) { ContainerTests.workPalettes(h); }
}
