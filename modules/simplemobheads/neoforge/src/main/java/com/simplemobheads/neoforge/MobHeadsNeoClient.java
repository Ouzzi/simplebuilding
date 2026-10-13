package com.simplemobheads.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only NeoForge setup: the config button in the mod list. */
public final class MobHeadsNeoClient {
    private MobHeadsNeoClient() {
    }

    public static void init(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> com.simplemobheads.client.MobHeadsConfigScreen.build(parent));
    }
}
