package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.entity.Entity;

/**
 * The sound of one Pocket Jukebox session, wherever the jukebox is (PJ23-5): at the carrier (follows this player, full
 * volume), at a dropped item (follows the entity) or at a placed block - positional like a jukebox (64 blocks, linear),
 * on the "Jukebox/Note Blocks" slider. {@link #moveTo} changes the place while it plays, so putting it down, dropping or
 * picking it up does not start the song again. The file is {@code simplemusic:upload/<hash>/<s>} (cache) or
 * {@code simplemusic:seek/<s>/<ns>/<path>} (a Vanilla disc's file); the {@code SoundBufferLibraryMixin} opens both and
 * skips the first {@code s} seconds, so a paused disc goes on where it stopped.
 */
public final class PocketSound extends AbstractSoundInstance implements TickableSoundInstance {
    /** Where the sound is: carried by this player, at item entity {@code entity}, or at block {@code pos}. */
    public record Anchor(boolean self, int entity, BlockPos pos) {
        public static Anchor carried() {
            return new Anchor(true, -1, BlockPos.ZERO);
        }
    }

    private Anchor anchor;
    private boolean stopped;

    private PocketSound(Identifier file, Anchor anchor) {
        super(file, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.volume = 4.0F;
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;
        this.anchor = anchor;
        follow();
    }

    public static PocketSound upload(String hash, int offsetSeconds, Anchor anchor) {
        return new PocketSound(SimpleMusic.id("upload/" + hash + "/" + Math.max(0, offsetSeconds)), anchor);
    }

    /** A Vanilla disc by its sound event, or null when the client does not know the event. */
    public static PocketSound song(SoundManager sounds, Identifier event, int offsetSeconds, Anchor anchor) {
        Identifier file = songFile(sounds, event, offsetSeconds);
        return file == null ? null : new PocketSound(file, anchor);
    }

    /** {@code simplemusic:seek/<s>/<ns>/<path>} of a Vanilla disc's sound event, or null when the client does not know it. */
    public static Identifier songFile(SoundManager sounds, Identifier event, int offsetSeconds) {
        WeighedSoundEvents events = sounds.getSoundEvent(event);
        if (events == null) return null;
        Identifier file = events.getSound(SoundInstance.createUnseededRandom()).getLocation();
        if (file == null || file.equals(SoundManager.EMPTY_SOUND.getLocation())) return null;
        return SimpleMusic.id("seek/" + Math.max(0, offsetSeconds) + "/" + file.getNamespace() + "/" + file.getPath());
    }

    public Anchor anchor() {
        return anchor;
    }

    /** Goes on playing at the new place. */
    public void moveTo(Anchor anchor) {
        this.anchor = anchor;
        follow();
    }

    void stopNow() {
        stopped = true;
    }

    private void follow() {
        Minecraft mc = Minecraft.getInstance();
        if (anchor.self()) {
            if (mc.player != null) {
                this.x = mc.player.getX();
                this.y = mc.player.getEyeY();
                this.z = mc.player.getZ();
            }
        } else if (anchor.entity() >= 0) {
            Entity entity = mc.level == null ? null : mc.level.getEntity(anchor.entity());
            if (entity != null) {
                this.x = entity.getX();
                this.y = entity.getY() + 0.25;
                this.z = entity.getZ();
            } else if (this.x == 0 && this.y == 0 && this.z == 0) {
                this.x = anchor.pos().getX() + 0.5;
                this.y = anchor.pos().getY() + 0.5;
                this.z = anchor.pos().getZ() + 0.5;
            }
        } else {
            this.x = anchor.pos().getX() + 0.5;
            this.y = anchor.pos().getY() + 0.5;
            this.z = anchor.pos().getZ() + 0.5;
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
        follow();
    }

    @Override
    public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
        Identifier id = getIdentifier();
        this.sound = new Sound(id, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
        this.soundEvent = new WeighedSoundEvents(id, null);
        return this.soundEvent;
    }
}
