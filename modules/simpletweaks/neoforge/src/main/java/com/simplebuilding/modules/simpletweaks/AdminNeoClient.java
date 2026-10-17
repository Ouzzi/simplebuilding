package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;
import com.simplebuilding.modules.simpletweaks.admin.invsee.client.InvseeScreen;

/** NeoForge client: the admin screens. Loaded only on the client. */
final class AdminNeoClient {
    static void init(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) -> {
            e.register(Invsee.INVSEE, InvseeScreen::new);
            e.register(Invsee.ENDERSEE, InvseeScreen::new);
            e.register(Invsee.DEATHS, InvseeScreen::new);
        });
    }
    private AdminNeoClient() {}
}
