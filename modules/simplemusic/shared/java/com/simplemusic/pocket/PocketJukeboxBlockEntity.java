package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.DiscPlayback;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * What a placed Pocket Jukebox holds and plays (plan docs/ai/PLAN-B-PJUKE21-2026-10-22.md): the item's discs
 * ({@code bundle_contents}) and position ({@code simplemusic:pocket}) come in on placing and go back into the item on
 * breaking ({@link #applyImplicitComponents}/{@link #collectImplicitComponents}). The order, the loop and the pause are the
 * item's ({@link PocketPlayback}): first disc in plays first, after the last the first again, a pause keeps the position.
 *
 * <p>The sound is positional like a jukebox for every player in range, through {@link DiscPlayback} (Vanilla discs by
 * their sound event, uploads with their per-listener approval), so a resumed or late heard disc starts mid-song. Like
 * Vanilla's jukebox it sends {@code jukebox_play} and note particles every second and {@code jukebox_stop_play} on
 * stop. Saved with the position and whether it plays: after a restart or chunk reload it goes on where it was.
 */
public class PocketJukeboxBlockEntity extends BlockEntity {
    private BundleContents contents = BundleContents.EMPTY;
    private PocketState state = PocketState.NEW;
    private boolean playing;
    /** The running disc's track (null = no session yet, e.g. just loaded). */
    private PocketPlayback.Track current;

    public PocketJukeboxBlockEntity(BlockPos pos, BlockState state) {
        super(MusicRegistry.POCKET_JUKEBOX_BE, pos, state);
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean playing() {
        return playing;
    }

    public BundleContents contents() {
        return contents;
    }

    /** The state with the live position while it plays. */
    public PocketState state() {
        if (!playing || !(level instanceof ServerLevel server) || !DiscPlayback.playing(server, worldPosition)) return state;
        return new PocketState(state.id(), state.fromEnd(), (int) Math.min(Integer.MAX_VALUE, DiscPlayback.elapsed(server, worldPosition)));
    }

    /** Puts one disc in (newest first, like the item); when nothing plays it starts. False when full or no disc. */
    public boolean insert(ItemStack disc) {
        if (!PocketJukeboxItem.isDisc(disc) || contents.size() >= PocketJukeboxItem.CAPACITY) return false;
        List<ItemStackTemplate> items = new ArrayList<>(contents.size() + 1);
        items.add(ItemStackTemplate.fromNonEmptyStack(disc, 1));
        items.addAll(contents.items());
        contents = new BundleContents(items);
        changed();
        if (!playing) start();
        return true;
    }

    /** Throws the newest disc out on top like a jukebox; the playing one stops (the next start begins the next disc). */
    public boolean ejectNewest() {
        if (contents.isEmpty() || level == null) return false;
        boolean wasCurrent = Math.floorMod(state.fromEnd(), contents.size()) == contents.size() - 1;
        List<ItemStackTemplate> items = new ArrayList<>(contents.items());
        ItemStack out = items.remove(0).create();
        contents = new BundleContents(items);
        if (wasCurrent) {
            if (playing) stop();
            state = new PocketState(state.id(), contents.isEmpty() ? 0 : state.fromEnd() % contents.size(), 0);
        }
        Vec3 at = Vec3.atLowerCornerWithOffset(worldPosition, 0.5, 0.6, 0.5);
        ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, out);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
        changed();
        return true;
    }

    /** Right click: pause what plays, otherwise play (from the paused position). True when it plays now. */
    public boolean toggle() {
        if (playing) {
            pause();
            return false;
        }
        return start();
    }

    /** Starts at the saved disc and position; skips discs that cannot play (an upload the server lost). */
    public boolean start() {
        if (!(level instanceof ServerLevel server) || contents.isEmpty() || !SimpleMusic.on()) return false;
        int size = contents.size();
        int from = Math.floorMod(state.fromEnd(), size);
        for (int i = 0; i < size; i++) {
            int fromEnd = (from + i) % size;
            PocketPlayback.Track track = PocketPlayback.track(server.getServer(), PocketJukeboxItem.disc(contents, fromEnd), null);
            if (track == null) continue;
            int offset = i == 0 && state.ticks() < track.ticks() ? Math.max(0, state.ticks()) : 0;
            begin(server, fromEnd, track, offset);
            return true;
        }
        return false;
    }

    private void begin(ServerLevel server, int fromEnd, PocketPlayback.Track track, int offset) {
        DiscPlayback.start(server, worldPosition, track.key(), track.song(), track.ticks(), offset);
        current = track;
        playing = true;
        state = new PocketState(state.id(), fromEnd, offset);
        changed();
    }

    /** Plays disc {@code fromEnd} (counted from the first one in) from {@code ticks} on (tests). */
    public void restartAt(int fromEnd, int ticks) {
        if (playing) stop();
        state = new PocketState(state.id(), fromEnd, ticks);
        start();
    }

    /** Pause: keeps the disc and the position. */
    public void pause() {
        state = state();
        stop();
    }

    private void stop() {
        if (level instanceof ServerLevel server) {
            DiscPlayback.stop(server, worldPosition);
            server.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, worldPosition, GameEvent.Context.of(getBlockState()));
        }
        playing = false;
        current = null;
        changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PocketJukeboxBlockEntity be) {
        if (level instanceof ServerLevel server) be.tick(server);
    }

    private void tick(ServerLevel server) {
        if (!playing) return;
        if (!SimpleMusic.on()) {
            pause();
            return;
        }
        if (current == null || !DiscPlayback.playing(server, worldPosition)) {
            // just loaded (sessions live in memory): go on where the save left it
            playing = false;
            if (!start()) changed();
            return;
        }
        long elapsed = DiscPlayback.elapsed(server, worldPosition);
        if (elapsed >= current.ticks()) {
            next(server);
            return;
        }
        if (elapsed % 20 == 0) {
            DiscPlayback.update(server, worldPosition);
            server.gameEvent(GameEvent.JUKEBOX_PLAY, worldPosition, GameEvent.Context.of(getBlockState()));
            Vec3 at = Vec3.atBottomCenterOf(worldPosition).add(0.0, 0.7, 0.0);
            server.sendParticles(ParticleTypes.NOTE, at.x, at.y, at.z, 0, server.getRandom().nextInt(4) / 24.0F, 0.0, 0.0, 1.0);
        }
    }

    /** End of a disc: the next one that can play, after the last the first again; none = stop at the top of it. */
    private void next(ServerLevel server) {
        int size = contents.size();
        for (int i = 1; i <= size; i++) {
            int fromEnd = (state.fromEnd() + i) % size;
            PocketPlayback.Track track = PocketPlayback.track(server.getServer(), PocketJukeboxItem.disc(contents, fromEnd), null);
            if (track == null) continue;
            begin(server, fromEnd, track, 0);
            return;
        }
        state = new PocketState(state.id(), state.fromEnd(), 0);
        stop();
    }

    /** 0 empty, else the current disc's Vanilla comparator value (an upload disc: 1). */
    public int comparatorOutput() {
        if (contents.isEmpty()) return 0;
        ItemStack disc = PocketJukeboxItem.disc(contents, Math.floorMod(state.fromEnd(), contents.size()));
        return JukeboxSong.fromStack(disc).map(Holder::value).map(JukeboxSong::comparatorOutput).orElse(1);
    }

    private void changed() {
        setChanged();
        if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server && DiscPlayback.playing(server, worldPosition)) {
            DiscPlayback.stop(server, worldPosition);
            server.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, worldPosition, GameEvent.Context.of(getBlockState()));
        }
        current = null;
        super.setRemoved();
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        contents = new BundleContents(components.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).items());
        state = components.getOrDefault(MusicComponents.POCKET, PocketState.NEW);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.BUNDLE_CONTENTS, contents);
        components.set(MusicComponents.POCKET, state());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Discs");
        output.discard("State");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        contents = input.read("Discs", BundleContents.CODEC).orElse(BundleContents.EMPTY);
        state = input.read("State", PocketState.CODEC).orElse(PocketState.NEW);
        playing = input.getBooleanOr("Playing", false);
        current = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!contents.isEmpty()) output.store("Discs", BundleContents.CODEC, contents);
        output.store("State", PocketState.CODEC, state());
        output.putBoolean("Playing", playing);
    }
}
