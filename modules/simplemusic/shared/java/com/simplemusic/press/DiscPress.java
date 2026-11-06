package com.simplemusic.press;

import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetContent;
import com.simplemusic.sheet.SheetMusicItem;
import com.simplemusic.sheet.SheetSounds;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;
import org.jspecify.annotations.Nullable;

/**
 * The disc press rule (owner 21.10., plan docs/ai/PLAN-B-MUSIC21-2026-10-21.md): a sheet of music on a blank disc gives a
 * custom disc that carries a copy of the sheet's music - its uploaded track ({@code simplemusic:track}) or its
 * composition ({@code simplemusic:sheet}, only when the code parses with the server's sound rules). The sheet itself is
 * never used up. Pure function, shared by the block entity and the tests.
 */
public final class DiscPress {
    /** Ticks one pressing takes. */
    public static final int PRESS_TICKS = 40;

    private DiscPress() {}

    /** Whether the sheet holds music a disc can take (an uploaded track or a composition that parses). */
    public static boolean pressable(ItemStack sheet) {
        if (!(sheet.getItem() instanceof SheetMusicItem)) return false;
        if (SheetMusicItem.track(sheet) != null) return true;
        String code = SheetMusicItem.content(sheet).code();
        return !code.isBlank() && NoteCode.parse(code, SheetSounds.policy()).ok();
    }

    /** The colour of a dye item, or null (WH26-3: the press' optional dye slot). */
    public static @Nullable DyeColor dyeColor(ItemStack dye) {
        for (DyeColor c : DyeColor.values()) {
            if (dye.is(Items.DYE.pick(c))) return c;
        }
        return null;
    }

    /** The disc the press makes from this sheet and blank, or empty. */
    public static ItemStack result(ItemStack sheet, ItemStack blank) {
        return result(sheet, blank, ItemStack.EMPTY);
    }

    /** As {@link #result(ItemStack, ItemStack)}; a dye colours the disc's label ({@code minecraft:dyed_color}, WH26-3). */
    public static ItemStack result(ItemStack sheet, ItemStack blank, ItemStack dye) {
        if (!blank.is(MusicRegistry.BLANK_DISC) || !pressable(sheet)) return ItemStack.EMPTY;
        ItemStack disc = new ItemStack(MusicRegistry.CUSTOM_DISC);
        var track = SheetMusicItem.track(sheet);
        if (track != null) {
            disc.set(MusicComponents.TRACK, track);
        } else {
            SheetContent content = SheetMusicItem.content(sheet);
            disc.set(MusicComponents.SHEET, new SheetContent(content.code(), content.title(), content.author(), true));
        }
        DyeColor color = dyeColor(dye);
        if (color != null) disc.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor() & 0xFFFFFF));
        return disc;
    }
}
