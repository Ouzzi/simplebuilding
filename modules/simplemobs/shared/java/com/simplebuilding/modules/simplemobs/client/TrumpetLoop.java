package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.DisguiseRegistry;
import com.simplebuilding.modules.simplemobs.TrumpetItem;
import com.simplebuilding.modules.simplemobs.TrumpetNet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client side of the trumpet (TROMP-3, owner 05.11.): one looping, player-following sound per trumpet player, started and retuned by
 * {@link TrumpetNet.TonePayload}. No attack and no release: release silences it at once. Crossing the sample split fades the old
 * sample out while the other fades in (a few ticks). As a safety net a loop ends by itself when its player no longer holds the trumpet.
 */
public final class TrumpetLoop {
    private static final Map<Integer, Voice> VOICES = new HashMap<>();
    private static final int FADE_TICKS = 4, GRACE_TICKS = 12;

    private TrumpetLoop() {}

    public static void handle(TrumpetNet.TonePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Voice voice = VOICES.get(payload.entityId());
        if (payload.note() < 0) {
            if (voice != null) voice.silence();
            VOICES.remove(payload.entityId());
            return;
        }
        Entity e = mc.level.getEntity(payload.entityId());
        if (!(e instanceof LivingEntity user)) return;
        if (voice == null || voice.done()) VOICES.put(payload.entityId(), voice = new Voice(mc, user));
        voice.set(payload.note());
    }

    public static void clear() {
        VOICES.values().forEach(Voice::silence);
        VOICES.clear();
    }

    /** One player's tone: up to two loop instances (low/high sample) while crossfading. */
    private static final class Voice {
        final Minecraft mc;
        final LivingEntity user;
        final Loop[] loops = new Loop[2];
        int note = -1;

        Voice(Minecraft mc, LivingEntity user) { this.mc = mc; this.user = user; }

        void set(int n) {
            note = n;
            int slot = TrumpetItem.high(n) ? 1 : 0;
            for (int i = 0; i < 2; i++) {
                if (loops[i] != null && loops[i].isStopped()) loops[i] = null;
                if (i != slot && loops[i] != null) loops[i].fading = true;
            }
            if (loops[slot] == null) {
                loops[slot] = new Loop(this, slot == 1, n);
                mc.getSoundManager().play(loops[slot]);
            }
            loops[slot].fading = false;
            loops[slot].note = n;
        }

        void silence() {
            for (Loop l : loops) if (l != null) l.hardStop();
        }

        boolean done() { return loops[0] == null && loops[1] == null || (loops[0] == null || loops[0].isStopped()) && (loops[1] == null || loops[1].isStopped()); }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        final Voice voice;
        int note;
        boolean fading;
        float gain;
        int idle;

        Loop(Voice voice, boolean high, int note) {
            super(TrumpetItem.sound(high ? 12 : 0), SoundSource.PLAYERS, net.minecraft.util.RandomSource.create());
            this.voice = voice;
            this.note = note;
            this.looping = true;
            this.delay = 0;
            this.gain = 0.0F;
            this.volume = 0.0F;
            this.pitch = TrumpetItem.pitch(note);
            follow();
        }

        void follow() {
            this.x = voice.user.getX();
            this.y = voice.user.getEyeY();
            this.z = voice.user.getZ();
        }

        void hardStop() { stop(); }

        @Override
        public void tick() {
            LivingEntity u = voice.user;
            boolean holding = u.isUsingItem() && u.getUseItem().is(DisguiseRegistry.TRUMPET);
            idle = holding ? 0 : idle + 1;
            if (u.isRemoved() || idle > GRACE_TICKS || gain <= 0.0F && fading) { stop(); return; }
            follow();
            this.pitch = TrumpetItem.pitch(note);
            gain = Math.max(0.0F, Math.min(1.0F, gain + (fading ? -1.0F : 1.0F) / FADE_TICKS));
            this.volume = TrumpetItem.VOLUME * gain;
        }
    }
}
