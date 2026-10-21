package com.simplemusic.press;

import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetContent;
import com.simplemusic.sheet.SheetMusicItem;
import com.simplemusic.sheet.SheetSounds;
import net.minecraft.world.item.ItemStack;

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

    /** The disc the press makes from this sheet and blank, or empty. */
    public static ItemStack result(ItemStack sheet, ItemStack blank) {
        if (!blank.is(MusicRegistry.BLANK_DISC) || !pressable(sheet)) return ItemStack.EMPTY;
        ItemStack disc = new ItemStack(MusicRegistry.CUSTOM_DISC);
        var track = SheetMusicItem.track(sheet);
        if (track != null) {
            disc.set(MusicComponents.TRACK, track);
        } else {
            SheetContent content = SheetMusicItem.content(sheet);
            disc.set(MusicComponents.SHEET, new SheetContent(content.code(), content.title(), content.author(), true));
        }
        return disc;
    }
}
