package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * A custom disc playing at a block: a streamed sound whose location {@code simplemusic:upload/<sha256>/<seconds>} the
 * {@code SoundBufferLibraryMixin} opens from the {@link TrackCache} (skipping {@code seconds} for a late start), on the
 * "Jukebox/Note Blocks" volume, positional like a jukebox (64 blocks). A placed Pocket Jukebox plays Vanilla discs the
 * same way from {@code simplemusic:seek/<s>/<ns>/<path>} ({@link PocketSound#songFile}).
 */
public final class CustomDiscSound extends AbstractSoundInstance {
    public CustomDiscSound(BlockPos pos, String hash, int offsetSeconds) {
        this(pos, SimpleMusic.id("upload/" + hash + "/" + Math.max(0, offsetSeconds)));
    }

    public CustomDiscSound(BlockPos pos, Identifier file) {
        super(file, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.volume = 4.0F;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.attenuation = Attenuation.LINEAR;
    }

    @Override
    public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
        Identifier id = getIdentifier();
        this.sound = new Sound(id, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
        this.soundEvent = new WeighedSoundEvents(id, null);
        return this.soundEvent;
    }
}
