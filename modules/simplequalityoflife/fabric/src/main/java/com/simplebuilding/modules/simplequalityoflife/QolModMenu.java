package com.simplebuilding.modules.simplequalityoflife;
public final class QolModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
 public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory(){return com.simplequalityoflife.client.QolConfigScreen::create;}
}
