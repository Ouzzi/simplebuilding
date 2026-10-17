package com.simplebuilding.modules.simpletweaks.table.client;

import com.simplebuilding.modules.simpletweaks.table.net.TableAreaPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableListPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload;
import net.minecraft.client.Minecraft;

/** Client entry of the claim table packets; the loaders call these from their receivers. Loaded only on the client. */
public final class TableClient {
    private TableClient() {}

    public static void open(TableOpenPayload payload) {
        if (!(Minecraft.getInstance().gui.screen() instanceof TableScreen s && s.refresh(payload))) Minecraft.getInstance().gui.setScreen(new TableScreen(payload));
    }
    public static void area(TableAreaPayload payload) { if (Minecraft.getInstance().gui.screen() instanceof TableScreen s) s.onArea(payload); }
    public static void list(TableListPayload payload) { if (Minecraft.getInstance().gui.screen() instanceof TableScreen s) s.onList(payload); }
}
