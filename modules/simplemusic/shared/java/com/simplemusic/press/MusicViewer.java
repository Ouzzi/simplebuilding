package com.simplemusic.press;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Music in the recipe viewer's "Special Ways" tab (JEI24): the disc press (sheet of music + blank disc -&gt; custom
 * disc, {@link DiscPress}) and copying a sheet at the cartography table ({@code SheetCopy}) - both code rules, no recipe
 * JEI could show. Free of JEI classes; registered from {@link SimpleMusic#init}.
 */
public final class MusicViewer {
    public static final String NOTE = "jei.simplemusic.note.";

    private MusicViewer() {
    }

    public static void register() {
        RecipeViewer.special(SimpleMusic.MOD_ID, MusicViewer::entries);
        RecipeViewer.station(SimpleMusic.MOD_ID, () -> MusicRegistry.DISC_PRESS_ITEM, MusicViewer::pressEntries); // b-rel13: own tab
        RecipeViewer.info(SimpleMusic.MOD_ID, MusicViewer::discPages);
    }

    /** The six structure discs: no recipe, only one structure's chests (or its brushed gravel) - an info page each. */
    public static List<RecipeViewer.Info> discPages() {
        List<RecipeViewer.Info> out = new ArrayList<>();
        var discs = com.simplemusic.disc.MusicDiscs.DISCS;
        var items = com.simplemusic.disc.MusicDiscs.ITEMS;
        for (int i = 0; i < Math.min(discs.size(), items.size()); i++) {
            var disc = discs.get(i);
            out.add(new RecipeViewer.Info(List.of(new ItemStack(items.get(i))), List.of(Component.translatable(
                    NOTE + (disc.archaeology() ? "disc_brushed" : "disc_chest"), disc.table().identifier().toString(), disc.weight()))));
        }
        return out;
    }

    /** The disc press' own tab (b-rel13): sheet + blank disc, and the same with the optional dye that colours the label (WH26-3). */
    public static List<InWorldUpgrade> pressEntries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        if (MusicRegistry.SHEET_MUSIC == null || MusicRegistry.BLANK_DISC == null || MusicRegistry.CUSTOM_DISC == null
                || MusicRegistry.DISC_PRESS_ITEM == null) return out;
        Component time = Component.translatable(NOTE + "press", Math.round(DiscPress.PRESS_TICKS / 20.0F));
        out.add(new InWorldUpgrade("simplemusic/disc_press",
                List.of(InWorldUpgrade.Input.of(MusicRegistry.SHEET_MUSIC, 1), InWorldUpgrade.Input.of(MusicRegistry.BLANK_DISC, 1)),
                List.of(MusicRegistry.DISC_PRESS_ITEM), new ItemStack(MusicRegistry.CUSTOM_DISC),
                List.of(time, Component.translatable(NOTE + "press_sheet"))));
        List<net.minecraft.world.item.Item> dyes = new ArrayList<>();
        for (net.minecraft.world.item.DyeColor c : net.minecraft.world.item.DyeColor.values()) dyes.add(new ItemStack(Items.DYE.pick(c)).getItem());
        out.add(new InWorldUpgrade("simplemusic/disc_press_dyed",
                List.of(InWorldUpgrade.Input.of(MusicRegistry.SHEET_MUSIC, 1), InWorldUpgrade.Input.of(MusicRegistry.BLANK_DISC, 1),
                        new InWorldUpgrade.Input(List.copyOf(dyes), 1)),
                List.of(MusicRegistry.DISC_PRESS_ITEM), new ItemStack(MusicRegistry.CUSTOM_DISC),
                List.of(time, Component.translatable(NOTE + "press_sheet"), Component.translatable(NOTE + "press_dye"))));
        return out;
    }

    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        if (MusicRegistry.SHEET_MUSIC == null || MusicRegistry.BLANK_DISC == null || MusicRegistry.CUSTOM_DISC == null) return out;
        out.add(new InWorldUpgrade("simplemusic/sheet_copy",
                List.of(InWorldUpgrade.Input.of(MusicRegistry.SHEET_MUSIC, 1), InWorldUpgrade.Input.of(MusicRegistry.SHEET_MUSIC, 1)),
                List.of(Items.CARTOGRAPHY_TABLE), new ItemStack(MusicRegistry.SHEET_MUSIC),
                List.of(Component.translatable(NOTE + "copy"), Component.translatable(NOTE + "copy_slots"))));
        return out;
    }
}
