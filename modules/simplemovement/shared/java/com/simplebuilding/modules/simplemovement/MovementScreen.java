package com.simplebuilding.modules.simplemovement;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Config screen (Cloth Config; Forge gets native widgets through gradle/forge-native-config.gradle). */
public final class MovementScreen {
    private MovementScreen() {}

    public static Screen create(Screen parent) {
        var gson = new com.google.gson.Gson();
        var c = gson.fromJson(gson.toJson(SimpleMovement.CONFIG), MovementConfig.class);
        var b = me.shedaniel.clothconfig2.api.ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("simplemovement.title"));
        var e = b.entryBuilder();
        var general = b.getOrCreateCategory(Component.translatable("simplemovement.tab.general"));
        general.addEntry(e.startBooleanToggle(name("enableManualCrawl"), c.enableManualCrawl).setDefaultValue(true)
                .setTooltip(tip("enableManualCrawl")).setSaveConsumer(v -> c.enableManualCrawl = v).build());
        general.addEntry(e.startBooleanToggle(name("enableAutowalk"), c.enableAutowalk).setDefaultValue(false)
                .setTooltip(tip("enableAutowalk")).setSaveConsumer(v -> c.enableAutowalk = v).build());
        general.addEntry(e.startDoubleField(name("ladderClimbingSpeed"), c.ladderClimbingSpeed).setMin(0.2).setMax(MovementConfig.MAX_CLIMB_SPEED)
                .setDefaultValue(MovementConfig.DEFAULT_CLIMB_SPEED).setTooltip(tip("ladderClimbingSpeed")).setSaveConsumer(v -> c.ladderClimbingSpeed = v).build());
        general.addEntry(e.startBooleanToggle(name("enableFastLadderSlide"), c.enableFastLadderSlide).setDefaultValue(true)
                .setTooltip(tip("enableFastLadderSlide")).setSaveConsumer(v -> c.enableFastLadderSlide = v).build());
        general.addEntry(e.startDoubleField(name("ladderSlideSpeed"), c.ladderSlideSpeed).setMin(0.15).setMax(MovementConfig.MAX_SLIDE_SPEED)
                .setDefaultValue(MovementConfig.DEFAULT_SLIDE_SPEED).setTooltip(tip("ladderSlideSpeed")).setSaveConsumer(v -> c.ladderSlideSpeed = v).build());
        general.addEntry(e.startEnumSelector(name("ladderSlideActivation"), MovementConfig.SlideActivationMode.class, c.ladderSlideActivation)
                .setDefaultValue(MovementConfig.SlideActivationMode.CAMERA).setTooltip(tip("ladderSlideActivation")).setSaveConsumer(v -> c.ladderSlideActivation = v).build());
        general.addEntry(e.startBooleanToggle(name("frostWalkerWalkOnPowderSnow"), c.frostWalkerWalkOnPowderSnow).setDefaultValue(true)
                .setTooltip(tip("frostWalkerWalkOnPowderSnow")).setSaveConsumer(v -> c.frostWalkerWalkOnPowderSnow = v).build());
        b.setSavingRunnable(() -> {
            SimpleMovement.CONFIG = c;
            SimpleMovement.save();
        });
        return b.build();
    }

    private static Component name(String key) {
        return Component.translatable("simplemovement.option." + key);
    }

    private static Component tip(String key) {
        return Component.translatable("simplemovement.option." + key + ".tooltip");
    }
}
