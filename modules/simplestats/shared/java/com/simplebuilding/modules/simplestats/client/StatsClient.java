package com.simplebuilding.modules.simplestats.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simplestats.SimpleStats;
import com.simplebuilding.modules.simplestats.StatsComponents;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Client side of Simple Stats: the key that opens the statistics of an item (hover an item in any inventory screen, or hold it).
 * The loader entries only register {@link #openKey}; everything else hangs on mixins so all loaders share one code path.
 */
public final class StatsClient {
    public static KeyMapping openKey;

    private StatsClient() {}

    /** Creates the key mapping (the loader client entry registers it). */
    public static KeyMapping initKey() {
        if (openKey == null) {
            var category = KeyMapping.Category.register(SimpleStats.id("general"));
            openKey = new KeyMapping("key.simplestats.open", InputConstants.KEY_I, category);
        }
        return openKey;
    }

    public static Component openKeyName() {
        return openKey == null ? Component.empty() : openKey.getTranslatedKeyMessage();
    }

    public static Locale locale() {
        return com.simplebuilding.modules.simplestats.data.StatFormat.locale(Minecraft.getInstance().getLanguageManager().getSelected());
    }

    /** Opens the screen for a stack (nothing for an empty stack); {@code parent} is shown again on close. */
    public static void open(ItemStack stack, net.minecraft.client.gui.screens.Screen parent) {
        if (stack.isEmpty() || StatsComponents.ITEM_STATS == null) return;
        Minecraft.getInstance().gui.setScreen(new ItemStatsScreen(stack, parent));
    }
}
