package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.QuestionaryData;
import com.simplequestionary.QuestionaryRegistry;
import com.simplequestionary.SimpleQuestionary;
import com.simplequestionary.client.QuestionaryScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist. */
final class QuestionaryNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((RegisterMenuScreensEvent event) -> event.register(QuestionaryRegistry.MENU, QuestionaryScreen::new));
        bus.addListener((AddClientReloadListenersEvent event) -> event.addListener(SimpleQuestionary.id("questionary"), QuestionaryData.reloadListener()));
    }

    private QuestionaryNeoClient() {}
}
