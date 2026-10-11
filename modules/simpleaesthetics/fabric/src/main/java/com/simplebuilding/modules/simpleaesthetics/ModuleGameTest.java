package com.simplebuilding.modules.simpleaesthetics;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
    @GameTest public void subModSwitch(GameTestHelper helper) { AestheticsTests.subModSwitch(helper); }
    @GameTest public void configFile(GameTestHelper helper) { AestheticsTests.configFile(helper); }
}
