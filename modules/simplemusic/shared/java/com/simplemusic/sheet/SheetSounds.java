package com.simplemusic.sheet;

import com.simplemusic.config.MusicConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.jspecify.annotations.Nullable;

/**
 * Note code instruments as game sounds (owner decision KONZ20 N4 a: every sound may be an instrument, minus the
 * blocklist). The server policy is the authority; the editor uses the same rules so problems show before saving.
 */
public final class SheetSounds {
    private static @Nullable Set<Identifier> headSounds;

    private SheetSounds() {}

    /** The rules of {@code sheetSounds} and {@code blockedSounds} (server config) for {@code sound <id>} aliases. */
    public static NoteCode.SoundPolicy policy() {
        return id -> {
            if (MusicConfig.sheetSounds == MusicConfig.SheetSounds.INSTRUMENTS) return "sound_not_allowed";
            Identifier key = Identifier.tryParse(id);
            if (key == null) return "bad_sound";
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(key)) return "unknown_sound";
            if (MusicConfig.blocked(key.toString())) return "blocked_sound";
            if (MusicConfig.sheetSounds == MusicConfig.SheetSounds.HEADS && !headSounds().contains(key)) return "sound_not_allowed";
            return null;
        };
    }

    /** Sounds that some head item plays on a note block (Vanilla and Simple Mobheads heads, read from item components). */
    public static Set<Identifier> headSounds() {
        if (headSounds == null) {
            Set<Identifier> out = new HashSet<>();
            for (var item : BuiltInRegistries.ITEM) {
                Identifier sound = item.components().get(DataComponents.NOTE_BLOCK_SOUND);
                if (sound != null) out.add(sound);
            }
            headSounds = out;
        }
        return headSounds;
    }

    /** The sound of an instrument; null when a named sound no longer exists. */
    public static @Nullable Holder<SoundEvent> sound(NoteCode.Instrument instrument) {
        if (instrument.kind() == NoteCode.Kind.SOUND) {
            Identifier key = Identifier.tryParse(instrument.name());
            return key == null ? null : BuiltInRegistries.SOUND_EVENT.get(key).<Holder<SoundEvent>>map(h -> h).orElse(null);
        }
        for (NoteBlockInstrument nbi : NoteBlockInstrument.values()) {
            if (nbi.getSerializedName().equals(instrument.name())) return nbi.getSoundEvent();
        }
        return null;
    }

    /** All instrument sounds of a song by instrument index (entries may be null). */
    @SuppressWarnings("unchecked")
    public static Holder<SoundEvent>[] sounds(NoteCode.Song song) {
        Holder<SoundEvent>[] out = new Holder[song.instruments().size()];
        for (int i = 0; i < out.length; i++) out[i] = sound(song.instruments().get(i));
        return out;
    }
}
