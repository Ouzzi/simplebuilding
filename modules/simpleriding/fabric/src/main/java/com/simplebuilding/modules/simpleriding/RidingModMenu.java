package com.simplebuilding.modules.simpleriding;
import com.terraformersmc.modmenu.api.*;
public final class RidingModMenu implements ModMenuApi {
 public ConfigScreenFactory<?> getModConfigScreenFactory(){return com.simpleriding.client.RidingConfigScreen::create;}
}
