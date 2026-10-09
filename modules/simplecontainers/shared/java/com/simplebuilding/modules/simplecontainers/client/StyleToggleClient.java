package com.simplebuilding.modules.simplecontainers.client;

import com.simplelib.api.client.ui.UiStyleToggle;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Client key for the screen-style comparison switch. */
public final class StyleToggleClient {
    public static boolean development;
    public static final KeyMapping KEY = new KeyMapping("key.simplecontainers.toggle_style", -1,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("simplecontainers", "general")));

    private StyleToggleClient() {}

    public static void tick(Minecraft client) {
        while (KEY.consumeClick()) UiStyleToggle.toggle();
    }
}
