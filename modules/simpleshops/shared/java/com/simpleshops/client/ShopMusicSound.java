package com.simpleshops.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * One shop music group playing (a-shopmusic37): a streamed, seamlessly looping file that starts part-way
 * ({@code simplelib:seek/<s>/<ns>/<path>}, SimpleLib's {@code SoundSeek}), on the "Jukebox/Note Blocks" slider. No distance
 * attenuation of its own ({@link SoundInstance.Attenuation#NONE}): {@link ShopMusicClient} sets the position (the group's
 * shop nearest to the player) and the volume (linear over the group's reach) every tick; volume changes glide over one
 * second, so starting, switching and leaving fade in and out.
 */
final class ShopMusicSound extends AbstractSoundInstance implements TickableSoundInstance {
    /** Volume step per tick: a full fade takes one second. */
    static final float FADE_STEP = 0.05F;

    final String song;
    final long start;
    private float target;
    private boolean leaving;
    private boolean stopped;

    ShopMusicSound(Identifier file, String song, long start) {
        super(file, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.song = song;
        this.start = start;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.attenuation = Attenuation.NONE;
        this.relative = false;
    }

    /** Where the music comes from and how loud it should become (0-1). */
    void aim(double x, double y, double z, float volume) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.target = Math.max(0.0F, Math.min(1.0F, volume));
        this.leaving = false;
    }

    /** Fades out and stops. */
    void leave() {
        this.leaving = true;
        this.target = 0.0F;
    }

    boolean leaving() {
        return this.leaving;
    }

    @Override
    public void tick() {
        float step = Math.max(-FADE_STEP, Math.min(FADE_STEP, this.target - this.volume));
        this.volume += step;
        if (this.leaving && this.volume <= 0.001F) this.stopped = true;
    }

    /** Stops at once (world left, feature off). */
    void stopNow() {
        this.stopped = true;
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
        Identifier id = getIdentifier();
        this.sound = new Sound(id, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
        this.soundEvent = new WeighedSoundEvents(id, null);
        return this.soundEvent;
    }
}
