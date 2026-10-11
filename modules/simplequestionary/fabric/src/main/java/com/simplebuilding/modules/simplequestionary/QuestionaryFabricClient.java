package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryData;
import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.client.QuestionaryScreen;
import com.simplequestionary.net.FactsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.PackType;

public final class QuestionaryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(QuestionaryRegistry.MENU, QuestionaryScreen::new);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(SimpleQuestionary.id("questionary"), QuestionaryData.reloadListener());
        ClientPlayNetworking.registerGlobalReceiver(FactsPayload.TYPE, (payload, context) -> context.client().execute(() -> FactsPayload.receive(payload)));
    }
}
