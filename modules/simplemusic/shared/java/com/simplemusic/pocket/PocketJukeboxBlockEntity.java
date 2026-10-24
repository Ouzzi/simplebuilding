package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.DiscPlayback;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
 * their sound event, uploads with their per-listener approval) under the jukebox's session id, so a resumed or late heard
 * disc starts mid-song and a jukebox put down or broken while it plays goes on without a gap (PJ23-5,
 * {@link PocketPlayback#blockRemoved}). Like Vanilla's jukebox it sends {@code jukebox_play} and note particles every second
 * and {@code jukebox_stop_play} on stop. Saved with the position and whether it plays: after a restart or chunk reload it
 * goes on where it was. The shown disc (PJ23-1, -1 = the current one) is synced to clients for the renderer.
 */
public class PocketJukeboxBlockEntity extends BlockEntity {
    private BundleContents contents = BundleContents.EMPTY;
    private PocketState state = PocketState.NEW;
    private boolean playing;
    /** The disc shown above it (counted from the first one in), -1 = the current one. */
    private int shown = -1;
    private long lastScrollSound = Long.MIN_VALUE;
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

    /** The current disc (counted from the first one in). */
    private int currentFromEnd() {
        return contents.isEmpty() ? 0 : Math.floorMod(state.fromEnd(), contents.size());
    }

    /** The disc shown above the case (counted from the first one in): the scrolled one, else the current one. */
    public int shownFromEnd() {
        return shown >= 0 && shown < contents.size() ? shown : currentFromEnd();
    }

    /** The shown disc (renderer), EMPTY when there is none. */
    public ItemStack shownDisc() {
        return PocketJukeboxItem.disc(contents, shownFromEnd());
    }

    /** Shift + mouse wheel: the shown disc one on or back (round). */
    public void cycle(int step) {
        if (contents.isEmpty()) return;
        shown = Math.floorMod(shownFromEnd() + step, contents.size());
        changed();
    }

    /** At most one scroll sound every 2 ticks. */
    boolean scrollSoundAllowed(long now) {
        if (now - lastScrollSound < 2 && now >= lastScrollSound) return false;
        lastScrollSound = now;
        return true;
    }

    /** Puts one disc in (newest first, like the item: the end of the play order); when nothing plays it starts. False when full or no disc. */
    public boolean insert(ItemStack disc) {
        if (!PocketJukeboxItem.isDisc(disc) || contents.size() >= PocketJukeboxItem.CAPACITY) return false;
        List<ItemStackTemplate> items = new ArrayList<>(contents.items().size() + 1);
        items.add(ItemStackTemplate.fromNonEmptyStack(disc, 1));
        items.addAll(contents.items());
        contents = new BundleContents(items);
        changed();
        if (!playing) start();
        return true;
    }

    /**
     * Shift + right click with an empty hand: the shown disc comes out - into the empty main hand, else the inventory, else
     * at the feet (like the placed bundle). Taking the playing disc stops it; taking another one leaves the music running.
     */
    public ItemStack takeShown(Player player) {
        if (contents.isEmpty() || level == null) return ItemStack.EMPTY;
        int taken = shownFromEnd();
        int cur = currentFromEnd();
        int size = contents.size();
        List<ItemStackTemplate> items = new ArrayList<>(contents.items());
        ItemStack out = items.remove(size - 1 - taken).create();
        contents = new BundleContents(items);
        shown = -1;
        if (taken == cur) {
            if (playing) stop();
            state = new PocketState(state.id(), contents.isEmpty() ? 0 : taken % contents.size(), 0);
        } else if (taken < cur) {
            state = new PocketState(state.id(), cur - 1, state().ticks());
        }
        if (player.getMainHandItem().isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, out.copy());
        else {
            ItemStack given = out.copy();
            if (!player.getInventory().add(given) && !given.isEmpty()) player.drop(given, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        level.playSound(null, worldPosition, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        changed();
        return out;
    }

    /** Right click or a rising redstone edge: pause what plays, otherwise play (from the paused position), with the sound. */
    public boolean toggleWithSound() {
        boolean was = playing;
        boolean now = toggle();
        if (level != null && (now || was)) PocketPlayback.startStopSound(level, Vec3.atCenterOf(worldPosition), now);
        return now;
    }

    /** Pause what plays, otherwise play (from the paused position). True when it plays now. */
    public boolean toggle() {
        if (playing) {
            pause();
            return false;
        }
        return start();
    }

    /**
     * Starts at the saved disc and position (a jukebox put down while it played goes on there); skips discs that cannot
     * play (an upload the server lost).
     */
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
        long id = state.id();
        while (id == 0L) id = server.getRandom().nextLong();
        state = new PocketState(id, fromEnd, offset);
        DiscPlayback.start(server, worldPosition, track.key(), track.song(), track.ticks(), offset, id);
        current = track;
        playing = true;
        setPlayingState(true);
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
        setPlayingState(false);
        changed();
    }

    /** The block state's {@code playing}: the turning record and the redstone output. */
    private void setPlayingState(boolean on) {
        if (level == null || isRemoved()) return;
        BlockState blockState = level.getBlockState(worldPosition);
        if (blockState.getBlock() instanceof PocketJukeboxBlock && blockState.getValue(PocketJukeboxBlock.PLAYING) != on) {
            level.setBlock(worldPosition, blockState.setValue(PocketJukeboxBlock.PLAYING, on), Block.UPDATE_ALL);
        }
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
            if (!start()) {
                setPlayingState(false);
                changed();
            }
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
        ItemStack disc = PocketJukeboxItem.disc(contents, currentFromEnd());
        return JukeboxSong.fromStack(disc).map(Holder::value).map(JukeboxSong::comparatorOutput).orElse(1);
    }

    private void changed() {
        setChanged();
        if (level != null && !isRemoved()) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Gone while it plays (broken, pushed, blown up, unloaded): the track is detached without a stop and the session goes on
     * at the dropped item, if one turns up nearby (PJ23-5); otherwise it ends a second later.
     */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server && DiscPlayback.playing(server, worldPosition)) {
            PocketState live = state();
            var told = DiscPlayback.detach(server, worldPosition);
            server.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, worldPosition, GameEvent.Context.of(getBlockState()));
            if (current != null && !contents.isEmpty()) {
                PocketPlayback.blockRemoved(server, worldPosition, live.id(), live.fromEnd(),
                        PocketJukeboxItem.disc(contents, currentFromEnd()), current, live.ticks(), told);
            }
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
        contents = readDiscs(input);
        state = input.read("State", PocketState.CODEC).orElse(PocketState.NEW);
        playing = input.getBooleanOr("Playing", false);
        shown = input.getIntOr("Shown", -1);
        current = null;
    }

    /**
     * The discs disc by disc (REV2-B): the bundle codec fails as a whole, so one disc of a removed mod emptied the whole
     * placed Pocket Jukebox. An unreadable disc is skipped, the others stay in their order.
     */
    static BundleContents readDiscs(ValueInput input) {
        var whole = input.read("Discs", BundleContents.CODEC);
        if (whole.isPresent()) return whole.get();
        List<com.mojang.serialization.Dynamic<?>> raw = input.read("Discs", com.mojang.serialization.Codec.PASSTHROUGH.listOf()).orElse(List.of());
        List<ItemStackTemplate> items = new ArrayList<>();
        for (com.mojang.serialization.Dynamic<?> disc : raw) one(disc).ifPresent(b -> items.addAll(b.items()));
        return items.isEmpty() ? BundleContents.EMPTY : new BundleContents(items);
    }

    private static <T> java.util.Optional<BundleContents> one(com.mojang.serialization.Dynamic<T> disc) {
        return BundleContents.CODEC.parse(disc.getOps(), disc.getOps().createList(java.util.stream.Stream.of(disc.getValue()))).result();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!contents.isEmpty()) output.store("Discs", BundleContents.CODEC, contents);
        output.store("State", PocketState.CODEC, state());
        output.putBoolean("Playing", playing);
        output.putInt("Shown", shown);
    }

    /** Clients get the discs and the shown one (the renderer draws it above the case). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
