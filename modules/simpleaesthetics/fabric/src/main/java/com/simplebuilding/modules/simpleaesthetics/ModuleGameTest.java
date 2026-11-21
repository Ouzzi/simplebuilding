package com.simplebuilding.modules.simpleaesthetics;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 /** N136: configLabels checked only English; title and every switch also have a German name. */
 @GameTest(maxTicks=100) public void configLabelsEnDe(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.labels(h, "simpleaesthetics"); }
 /** N136: the switch tooltips name the real default (EN + DE). */
 @GameTest(maxTicks=100) public void configTooltipDefaults(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.tooltipDefaults(h, "simpleaesthetics"); }
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleaesthetics", false); }
    @GameTest public void subModSwitch(GameTestHelper helper) { AestheticsTests.subModSwitch(helper); }
    @GameTest public void configLabels(GameTestHelper helper) { AestheticsTests.configLabels(helper); }
    @GameTest public void configFile(GameTestHelper helper) { AestheticsTests.configFile(helper); }
}
