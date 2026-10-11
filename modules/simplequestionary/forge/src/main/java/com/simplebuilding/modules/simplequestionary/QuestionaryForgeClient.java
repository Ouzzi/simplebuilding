package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryData;
import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.client.QuestionaryScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge setup (dist guard in the entry point). */
final class QuestionaryForgeClient {
    static void init(FMLJavaModLoadingContext context) {
        RegisterClientReloadListenersEvent.BUS.addListener(event -> event.registerReloadListener(QuestionaryData.reloadListener()));
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> MenuScreens.register(QuestionaryRegistry.MENU, QuestionaryScreen::new)));
    }

    private QuestionaryForgeClient() {}
}
