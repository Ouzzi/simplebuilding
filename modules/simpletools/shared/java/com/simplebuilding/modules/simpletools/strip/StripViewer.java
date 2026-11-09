package com.simplebuilding.modules.simpletools.strip;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import com.simplelib.api.RecipeViewer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The strip station's info page in the recipe viewer (its axe, hand and hopper rules are code, no recipe shows them). Free of JEI classes. */
public final class StripViewer {
    private StripViewer() {}

    public static void register() {
        RecipeViewer.info(SimpleTools.MOD_ID, StripViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        if (ToolsRegistry.STRIP_STATION_ITEM == null) return List.of();
        return List.of(new RecipeViewer.Info(List.of(new ItemStack(ToolsRegistry.STRIP_STATION_ITEM)),
                List.of(Component.translatable("jei.simpletools.strip_station.1"), Component.translatable("jei.simpletools.strip_station.2"),
                        Component.translatable("jei.simpletools.strip_station.3"))));
    }
}
