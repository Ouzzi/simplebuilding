package com.simplemusic.orchestrion;

import com.simplemusic.config.MusicConfig;
import com.simplemusic.disc.CustomDiscItem;
import com.simplemusic.disc.DiscPlayback;
import com.simplemusic.disc.TrackRef;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.registry.MusicTags;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetMusicItem;
import com.simplemusic.sheet.SheetSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Orchestrion's sequencer (concept §4, variant P1: the server plays the tones of each step itself, like a note
 * block, so every listener is in sync and amplifiers work). A sheet plays step by step, {@code tempo} ticks per step;
 * a custom disc runs through {@link DiscPlayback}. After the last step the piece loops, or swaps with the next
 * sheet/disc of a playlist container on top (tag {@code simplemusic:playlist_containers}; slot order, the finished one
 * takes the slot of the next, like the jukebox playlist). At most {@code maxPlayingOrchestrions} play per dimension.
 */
public class OrchestrionBlockEntity extends BlockEntity {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new HashMap<>();

    private ItemStack item = ItemStack.EMPTY;
    private int step;
    private int tickInStep;
    /** Seen a redstone signal since the item went in: from then on the signal decides (R1). */
    private boolean wired;
    private boolean lastPowered;
    private boolean stopAfterStep;
    private boolean playing;
    private int playlistSlot = -1;
    /** Counts finished runs (tests, comparator pulses). */
    private int runs;
    /** Tones sent since load (tests). */
    private int tonesPlayed;
    private NoteCode.Song song;
    private Holder<SoundEvent>[] sounds;

    public OrchestrionBlockEntity(BlockPos pos, BlockState state) {
        super(MusicRegistry.ORCHESTRION_BE, pos, state);
    }

    /** A sheet whose code parses, or a custom disc. */
    public static boolean playable(ItemStack stack) {
        if (stack.getItem() instanceof SheetMusicItem) {
            String code = SheetMusicItem.content(stack).code();
            return !code.isBlank() && NoteCode.parse(code, SheetSounds.policy()).ok();
        }
        return stack.getItem() instanceof CustomDiscItem && CustomDiscItem.track(stack) != null;
    }

    public ItemStack item() {
        return item;
    }

    public int step() {
        return step;
    }

    public int runs() {
        return runs;
    }

    public int tonesPlayed() {
        return tonesPlayed;
    }

    public boolean playing() {
        return playing;
    }

    public NoteCode.Song song() {
        prepare();
        return song;
    }

    /** Puts one item in (server); false when not playable or already full. */
    public boolean insert(ItemStack stack) {
        if (!item.isEmpty() || stack.isEmpty() || !playable(stack)) return false;
        setItem(stack.copyWithCount(1));
        wired = false;
        lastPowered = level != null && level.hasNeighborSignal(worldPosition);
        restart();
        return true;
    }

    private void setItem(ItemStack stack) {
        stopDisc();
        item = stack;
        song = null;
        sounds = null;
        if (level != null && level.getBlockState(worldPosition).hasProperty(OrchestrionBlock.HAS_RECORD)) {
            level.setBlock(worldPosition, getBlockState().setValue(OrchestrionBlock.HAS_RECORD, !item.isEmpty()), 2);
        }
        setChanged();
    }

    /** Takes the item out and drops it on top, like the jukebox. */
    public void popOut() {
        if (level == null || item.isEmpty()) return;
        ItemStack out = item.copy();
        setItem(ItemStack.EMPTY);
        playing = false;
        deactivate();
        Vec3 at = Vec3.atLowerCornerWithOffset(worldPosition, 0.5, 1.01, 0.5);
        ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, out);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private void restart() {
        step = 0;
        tickInStep = 0;
        stopAfterStep = false;
        stopDisc();
        setChanged();
    }

    private void prepare() {
        if (song != null) return;
        if (item.getItem() instanceof SheetMusicItem) {
            NoteCode.Result r = NoteCode.parse(SheetMusicItem.content(item).code(), SheetSounds.policy());
            song = r.ok() ? r.song() : NoteCode.Song.EMPTY;
        } else {
            song = NoteCode.Song.EMPTY;
        }
        sounds = SheetSounds.sounds(song);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OrchestrionBlockEntity be) {
        if (level instanceof ServerLevel server) be.tick(server);
    }

    void tick(ServerLevel level) {
        if (item.isEmpty() || !com.simplemusic.SimpleMusic.on()) {
            if (playing) {
                playing = false;
                stopDisc();
                deactivate();
            }
            return;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered && !lastPowered) {
            wired = true;
            restart();
            playing = false;
        }
        if (!powered && lastPowered) {
            wired = true;
            if (playing) stopAfterStep = true;
        }
        lastPowered = powered;
        boolean wanted = !wired || powered || stopAfterStep;
        if (!wanted) {
            if (playing) {
                playing = false;
                stopDisc();
                deactivate();
            }
            return;
        }
        if (!playing) {
            if (!activate(level)) return;
            playing = true;
        }
        if (item.getItem() instanceof CustomDiscItem) {
            tickDisc(level);
        } else {
            tickSheet(level);
        }
    }

    private void tickSheet(ServerLevel level) {
        prepare();
        if (song.steps() == 0) return;
        if (tickInStep == 0) playStep(level, song.tonesAt(step));
        if (++tickInStep >= song.tempo()) {
            tickInStep = 0;
            step++;
            if (stopAfterStep) {
                stopAfterStep = false;
                playing = false;
                deactivate();
            }
            if (step >= song.steps()) endOfRun(level);
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void playStep(ServerLevel level, java.util.List<NoteCode.Tone> tones) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        float range = MusicConfig.orchestrionRange / 16.0F;
        int played = 0;
        for (NoteCode.Tone tone : tones) {
            if (played >= MusicConfig.maxTonesPerTick) break;
            Holder<SoundEvent> sound = sounds[tone.instrument()];
            if (sound == null) continue;
            level.playSeededSound(null, x, y, z, sound, SoundSource.RECORDS, range * tone.volume(), tone.pitch(), level.getRandom().nextLong());
            played++;
        }
        tonesPlayed += played;
        if (played > 0) {
            level.sendParticles(ParticleTypes.NOTE, x, y + 0.7, z, 0, tones.get(0).note() / 24.0, 0.0, 0.0, 1.0);
        }
    }

    private void tickDisc(ServerLevel level) {
        TrackRef track = CustomDiscItem.track(item);
        if (!DiscPlayback.playing(level, worldPosition)) DiscPlayback.start(level, worldPosition, track);
        if (level.getGameTime() % 20 == 0) DiscPlayback.update(level, worldPosition);
        long elapsed = DiscPlayback.elapsed(level, worldPosition);
        int total = Math.max(1, track.seconds()) * 20;
        step = (int) Math.min(elapsed, total);
        if (stopAfterStep && elapsed % 20 == 0) {
            // a disc has no steps: "end of the step" is the next full second
            stopAfterStep = false;
            playing = false;
            stopDisc();
            deactivate();
            return;
        }
        if (DiscPlayback.finished(level, worldPosition)) {
            DiscPlayback.stop(level, worldPosition);
            endOfRun(level);
        }
        if (elapsed % 20 == 0) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /** A run finished: swap with the next playlist item, or loop. */
    private void endOfRun(ServerLevel level) {
        runs++;
        Container container = playlistAbove(level);
        if (container != null) swapWithNext(container);
        step = 0;
        tickInStep = 0;
        song = null;
        setChanged();
    }

    private Container playlistAbove(ServerLevel level) {
        BlockPos above = worldPosition.above();
        BlockState state = level.getBlockState(above);
        if (!state.is(MusicTags.PLAYLIST_CONTAINERS)) return null;
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) return null;
        BlockEntity be = level.getBlockEntity(above);
        if (be instanceof RandomizableContainerBlockEntity r && r.getLootTable() != null) return null;
        return be instanceof Container c ? c : null;
    }

    private void swapWithNext(Container chest) {
        int size = chest.getContainerSize();
        for (int i = 1; i <= size; i++) {
            int slot = Math.floorMod(playlistSlot + i, size);
            ItemStack candidate = chest.getItem(slot);
            if (candidate.getCount() != 1 || !playable(candidate)) continue;
            ItemStack next = candidate.copy();
            chest.setItem(slot, item.copy());
            chest.setChanged();
            stopDisc();
            item = next;
            sounds = null;
            playlistSlot = slot;
            return;
        }
    }

    private boolean activate(ServerLevel level) {
        Set<BlockPos> active = ACTIVE.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        active.removeIf(p -> !(level.isLoaded(p) && level.getBlockEntity(p) instanceof OrchestrionBlockEntity o && o.playing));
        if (!active.contains(worldPosition) && active.size() >= MusicConfig.maxPlayingOrchestrions) return false;
        active.add(worldPosition.immutable());
        return true;
    }

    private void deactivate() {
        if (level != null) {
            Set<BlockPos> active = ACTIVE.get(level.dimension());
            if (active != null) active.remove(worldPosition);
        }
    }

    private void stopDisc() {
        if (level instanceof ServerLevel server) DiscPlayback.stop(server, worldPosition);
    }

    /** 0 empty, otherwise 1-15 by progress through the piece. */
    public int comparatorOutput() {
        if (item.isEmpty()) return 0;
        int total;
        if (item.getItem() instanceof CustomDiscItem) {
            TrackRef t = CustomDiscItem.track(item);
            total = t == null ? 1 : Math.max(1, t.seconds()) * 20;
        } else {
            prepare();
            total = Math.max(1, song.steps());
        }
        return 1 + Math.min(14, (int) (14L * step / total));
    }

    @Override
    public void setRemoved() {
        stopDisc();
        deactivate();
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        popOut();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        item = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        step = input.getIntOr("Step", 0);
        tickInStep = input.getIntOr("TickInStep", 0);
        wired = input.getBooleanOr("Wired", false);
        lastPowered = input.getBooleanOr("Powered", false);
        playlistSlot = input.getIntOr("PlaylistSlot", -1);
        runs = input.getIntOr("Runs", 0);
        song = null;
        sounds = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!item.isEmpty()) output.store("Item", ItemStack.CODEC, item);
        output.putInt("Step", step);
        output.putInt("TickInStep", tickInStep);
        output.putBoolean("Wired", wired);
        output.putBoolean("Powered", lastPowered);
        output.putInt("PlaylistSlot", playlistSlot);
        output.putInt("Runs", runs);
    }
}
