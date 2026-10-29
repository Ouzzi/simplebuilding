package com.simplebuilding.modules.simplefarming;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class FarmingScreen {
    private FarmingScreen() {}

    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(SimpleFarming.CONFIG), FarmingConfig.class);
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplefarming.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simplefarming.tab.general"));
        general.addEntry(e.startBooleanToggle(name("preventFarmlandTrampleWithFeatherFalling"), c.preventFarmlandTrampleWithFeatherFalling).setDefaultValue(true)
                .setTooltip(tip("preventFarmlandTrampleWithFeatherFalling")).setSaveConsumer(v -> c.preventFarmlandTrampleWithFeatherFalling = v).build());
        general.addEntry(e.startBooleanToggle(name("sharpnessCutsGrass"), c.sharpnessCutsGrass).setDefaultValue(true)
                .setTooltip(tip("sharpnessCutsGrass")).setSaveConsumer(v -> c.sharpnessCutsGrass = v).build());
        general.addEntry(e.startIntField(name("grassCutMinSharpness"), c.grassCutMinSharpness).setMin(1).setMax(5).setDefaultValue(1)
                .setTooltip(tip("grassCutMinSharpness")).setSaveConsumer(v -> c.grassCutMinSharpness = v).build());
        general.addEntry(e.startIntField(name("grassThroughMinPercent"), c.grassThroughMinPercent).setMin(0).setMax(100).setDefaultValue(10)
                .setTooltip(tip("grassThroughMinPercent")).setSaveConsumer(v -> c.grassThroughMinPercent = v).build());
        general.addEntry(e.startIntField(name("grassThroughMaxPercent"), c.grassThroughMaxPercent).setMin(0).setMax(100).setDefaultValue(90)
                .setTooltip(tip("grassThroughMaxPercent")).setSaveConsumer(v -> c.grassThroughMaxPercent = v).build());
        general.addEntry(e.startIntField(name("grassSweepRadiusPerLevel"), c.grassSweepRadiusPerLevel).setMin(0).setMax(3).setDefaultValue(1)
                .setTooltip(tip("grassSweepRadiusPerLevel")).setSaveConsumer(v -> c.grassSweepRadiusPerLevel = v).build());
        general.addEntry(e.startBooleanToggle(name("enableHoeHarvest"), c.enableHoeHarvest).setDefaultValue(true)
                .setTooltip(tip("enableHoeHarvest")).setSaveConsumer(v -> c.enableHoeHarvest = v).build());
        b.setSavingRunnable(() -> {
            SimpleFarming.CONFIG = c;
            SimpleFarming.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simplefarming.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simplefarming.option." + key + ".tooltip");
    }
}
