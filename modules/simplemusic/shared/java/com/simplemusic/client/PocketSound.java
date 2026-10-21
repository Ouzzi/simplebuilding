package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * The Pocket Jukebox playing for its carrier: not positional (relative to the listener, no attenuation), full volume
 * on the "Jukebox/Note Blocks" slider. The file is {@code simplemusic:upload/<hash>/<s>} (cache) or
 * {@code simplemusic:seek/<s>/<ns>/<path>} (a Vanilla disc's file); the {@code SoundBufferLibraryMixin} opens both and
 * skips the first {@code s} seconds, so a paused disc goes on where it stopped.
 */
public final class PocketSound extends AbstractSoundInstance {
    private PocketSound(Identifier file) {
        super(file, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.volume = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public static PocketSound upload(String hash, int offsetSeconds) {
        return new PocketSound(SimpleMusic.id("upload/" + hash + "/" + Math.max(0, offsetSeconds)));
    }

    /** A Vanilla disc by its sound event, or null when the client does not know the event. */
    public static PocketSound song(SoundManager sounds, Identifier event, int offsetSeconds) {
        WeighedSoundEvents events = sounds.getSoundEvent(event);
        if (events == null) return null;
        Identifier file = events.getSound(SoundInstance.createUnseededRandom()).getLocation();
        if (file == null || file.equals(SoundManager.EMPTY_SOUND.getLocation())) return null;
        return new PocketSound(SimpleMusic.id("seek/" + Math.max(0, offsetSeconds) + "/" + file.getNamespace() + "/" + file.getPath()));
    }

    @Override
    public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
        Identifier id = getIdentifier();
        this.sound = new Sound(id, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
        this.soundEvent = new WeighedSoundEvents(id, null);
        return this.soundEvent;
    }
}
