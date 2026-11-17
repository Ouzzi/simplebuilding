package com.simplebuilding.modules.simpleinterfaces.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Opens the key binding page ({@link KeysScreen}) from a key; the Vanilla controls page has a button too. Standard U since N78 (owner 16.11. asked
 * for O, but O is Vanilla 26.3's global "Friends Screen" key that swallows the press before mod keys see it; U sits next to I = Simple Stats).
 * Before: scancode 48 {@code KEY_RBRACKET} ("+" on a German layout); {@link LegacyMigration} moves untouched old lines.
 */
public final class KeysClient {
    public static final KeyMapping KEY = new KeyMapping("key.simpleinterfaces.open_keys",
            com.mojang.blaze3d.platform.InputConstants.KEY_U, StyleToggleClient.CATEGORY);

    private KeysClient() {}

    public static void tick(Minecraft client) {
        while (KEY.consumeClick()) client.setScreenAndShow(new KeysScreen(client.gui.screen()));
    }
}
