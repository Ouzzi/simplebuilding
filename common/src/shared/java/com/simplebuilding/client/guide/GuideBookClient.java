package com.simplebuilding.client.guide;

import com.simplebuilding.items.custom.GuideBookItem;
import net.minecraft.client.Minecraft;

/** Client-Start der Handbuecher, fuer alle Loader gleich: haengt den Buchbildschirm an die Buch-Items. */
public final class GuideBookClient {
    private GuideBookClient() {
    }

    public static void init() {
        GuideBookItem.setClientOpener((book, hand) -> Minecraft.getInstance().gui.setScreen(new GuideBookScreen(book, hand)));
    }
}
