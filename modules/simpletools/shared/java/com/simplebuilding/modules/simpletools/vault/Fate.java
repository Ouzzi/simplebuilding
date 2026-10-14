package com.simplebuilding.modules.simpletools.vault;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** What became of one death drop (shown in the Death Vault, colour coded; ids are stored in the ledger, never renumber). */
public enum Fate {
    /** Still an item entity in the world. */
    LYING(0, 0xFF55D05A, "lying"),
    /** Picked up by another player (the name is stored with the entry). */
    PICKED_OTHER(2, 0xFFE04848, "picked_other"),
    /** Picked up again by its owner. */
    PICKED_OWNER(3, 0xFF4A90F0, "picked_owner"),
    /** Destroyed by lava, fire or a cactus. */
    BURNED(4, 0xFFF08A24, "burned"),
    /** Removed after its five minutes ran out. */
    DESPAWNED(5, 0xFF9A9AA8, "despawned"),
    /** Fell out of the world. */
    VOID(6, 0xFFA45CE6, "void"),
    /** Gone some other way (hopper, mob, explosion, command ...). */
    OTHER(7, 0xFFD4C060, "other"),
    /** Bought back at the vault (only with the future redeem option). */
    REDEEMED(8, 0xFF3CCBC8, "redeemed");

    /** Legend and sort order: what still matters first. */
    public static final Fate[] DISPLAY_ORDER = {LYING, PICKED_OWNER, PICKED_OTHER, BURNED, DESPAWNED, VOID, OTHER, REDEEMED};

    public final int id;
    public final int color;
    public final String key;

    Fate(int id, int color, String key) {
        this.id = id;
        this.color = color;
        this.key = key;
    }

    /** Unknown ids read as {@link #OTHER}. */
    public static Fate of(int id) {
        for (Fate fate : values()) if (fate.id == id) return fate;
        return OTHER;
    }

    /** Translation key prefix of the legend and the item tooltip lines. */
    public static final String KEY = "screen.simpletools.death_vault.fate.";

    /** Legend text. */
    public Component label() {
        return Component.translatable(KEY + key);
    }

    /** Tooltip line of a shown drop, coloured like its frame; {@code who} names the other player who picked it up. */
    public Component line(String who) {
        MutableComponent text = this == PICKED_OTHER && !who.isEmpty()
                ? Component.translatable(KEY + key + ".named", who) : Component.translatable(KEY + key);
        return text.withStyle(Style.EMPTY.withColor(color & 0xFFFFFF).withItalic(false));
    }

    public int order() {
        for (int i = 0; i < DISPLAY_ORDER.length; i++) if (DISPLAY_ORDER[i] == this) return i;
        return DISPLAY_ORDER.length;
    }
}
