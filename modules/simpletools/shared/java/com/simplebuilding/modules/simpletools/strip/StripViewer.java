package com.simplebuilding.modules.simpletools.strip;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The strip station in the recipe viewer: its info page (axe, hand and hopper rules) and, b-rel13, a tab of its own with
 * one entry per block an axe works in the cage (read from the axe's own rules through {@link StripWork}, so mods' logs
 * show up too). Free of JEI classes.
 */
public final class StripViewer {
    private StripViewer() {}

    public static void register() {
        RecipeViewer.info(SimpleTools.MOD_ID, StripViewer::pages);
        RecipeViewer.station(SimpleTools.MOD_ID, () -> ToolsRegistry.STRIP_STATION_ITEM, StripViewer::entries);
    }

    public static List<RecipeViewer.Info> pages() {
        if (ToolsRegistry.STRIP_STATION_ITEM == null) return List.of();
        return List.of(new RecipeViewer.Info(List.of(new ItemStack(ToolsRegistry.STRIP_STATION_ITEM)),
                List.of(Component.translatable("jei.simpletools.strip_station.1"), Component.translatable("jei.simpletools.strip_station.2"),
                        Component.translatable("jei.simpletools.strip_station.3"))));
    }

    /** Block + any axe in the cage -&gt; what the axe makes of it (strip, scrape, wax off); the whole stack at once. */
    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        if (ToolsRegistry.STRIP_STATION_ITEM == null) return out;
        List<Item> axes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (StripWork.isAxe(item.getDefaultInstance())) axes.add(item);
        if (axes.isEmpty()) axes.add(Items.IRON_AXE); // tags not bound yet: Vanilla's iron axe still does every rule
        ItemStack probe = new ItemStack(Items.IRON_AXE);
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem)) continue;
            StripWork.Step step = StripWork.step(probe, new ItemStack(item));
            if (step == null) continue;
            out.add(new InWorldUpgrade("simpletools/strip_station/" + BuiltInRegistries.ITEM.getKey(item).toString().replace(':', '/'),
                    List.of(InWorldUpgrade.Input.of(item, 1), new InWorldUpgrade.Input(List.copyOf(axes), 1)),
                    List.of(ToolsRegistry.STRIP_STATION_ITEM), step.result(),
                    List.of(Component.translatable("jei.simpletools.strip_station.entry"))));
        }
        return out;
    }
}
