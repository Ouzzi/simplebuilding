package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist: the Shaping Saw screen. */
final class BlocksNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((RegisterMenuScreensEvent event) -> event.<ShapingSawMenu, ShapingSawScreen>register(Sawing.MENU, ShapingSawScreen::new));
    }

    private BlocksNeoClient() {}
}
