package com.simplebuilding.modules.simpledimensions;
import com.terraformersmc.modmenu.api.*;
public final class DimensionModMenu implements ModMenuApi {
 public ConfigScreenFactory<?> getModConfigScreenFactory(){return com.simplebuilding.modules.simpledimensions.client.DimensionConfigScreen::create;}
}
