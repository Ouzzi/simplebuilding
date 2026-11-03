package com.simplebuilding.modules.simpleinterfaces.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Opens the key binding page ({@link KeysScreen}) from a key (standard "+" of a German layout = scancode 48 {@code KEY_RBRACKET}, the key right of "ü"; "]" on a US layout); the Vanilla controls page has a button too. */
public final class KeysClient {
    public static final KeyMapping KEY = new KeyMapping("key.simpleinterfaces.open_keys",
            com.mojang.blaze3d.platform.InputConstants.KEY_RBRACKET, StyleToggleClient.CATEGORY);

    private KeysClient() {}

    public static void tick(Minecraft client) {
        while (KEY.consumeClick()) client.setScreenAndShow(new KeysScreen(client.gui.screen()));
    }
}
