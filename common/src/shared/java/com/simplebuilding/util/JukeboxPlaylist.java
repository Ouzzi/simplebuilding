package com.simplebuilding.util;

import com.simplebuilding.api.WorldPermissions;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Jukebox with a chest above it (back port of the 26.4 snapshot 4 vanilla feature, owner 2026-10-15, queue N62;
 * facts and sources: docs/ai/BERICHT-OC-JUKEBOX.md). A single chest, trapped chest, copper chest (every variant) or
 * tiered chest directly on top is the playlist:
 * <ul>
 * <li>an empty jukebox takes the next music disc of the chest (slot order, skipping empty slots and other items,
 * wrapping around) and plays it;</li>
 * <li>a disc that has finished swaps places with the next disc: the next one starts, the finished one lies in the
 * slot the next one came from;</li>
 * <li>without any other disc in the chest the finished disc plays again (vanilla loops it);</li>
 * <li>taking the disc out of the jukebox by hand simply starts the next one (the jukebox is empty again).</li>
 * </ul>
 * The slot of the last disc taken is remembered per jukebox ({@link Cursor}, saved with the block entity), so the
 * swapped-in discs do not make the order jump back to the first slot. Off where the line has the vanilla feature
 * ({@link McVersion#JUKEBOX_CHEST_PLAYLIST} false, or the vanilla block tag {@code #minecraft:jukebox_containers}
 * exists), so exactly one implementation is active.
 */
public final class JukeboxPlaylist {
    /** Vanilla 26.4 block tag listing the playlist containers; its presence means vanilla does the job itself. */
    private static final TagKey<Block> VANILLA_CONTAINERS = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("jukebox_containers"));
    /** How often an empty jukebox looks for a disc (a finished one is handled in the very tick it finished). */
    private static final int EMPTY_POLL_TICKS = 10;

    private JukeboxPlaylist() {
    }

    /** Where the jukebox left off; implemented by the block entity mixin (-1 = nothing taken yet). */
    public interface Cursor {
        int simplebuilding$getPlaylistSlot();

        void simplebuilding$setPlaylistSlot(int slot);
    }

    /** True when this mod's implementation is the one in charge on this line and world. */
    public static boolean active(ServerLevel level) {
        if (!McVersion.JUKEBOX_CHEST_PLAYLIST) {
            return false;
        }
        return level.registryAccess().lookupOrThrow(Registries.BLOCK).get(VANILLA_CONTAINERS).map(tag -> tag.size() == 0).orElse(true);
    }

    /** Called every tick of every jukebox (server side) after vanilla's own tick. */
    public static void tick(ServerLevel level, BlockPos pos, JukeboxBlockEntity jukebox) {
        if (jukebox.getSongPlayer().isPlaying()) {
            return;
        }
        boolean empty = jukebox.getTheItem().isEmpty();
        if (empty && (level.getGameTime() + pos.hashCode()) % EMPTY_POLL_TICKS != 0) {
            return;
        }
        step(level, pos, jukebox);
    }

    /** One playlist step of a jukebox that is not playing; returns whether something changed. */
    public static boolean step(ServerLevel level, BlockPos pos, JukeboxBlockEntity jukebox) {
        if (!(jukebox instanceof Cursor cursor) || !active(level)) {
            return false;
        }
        ChestBlockEntity chest = chestAbove(level, pos);
        if (chest == null) {
            return false;
        }
        ItemStack current = jukebox.getTheItem();
        if (!current.isEmpty() && JukeboxSong.fromStack(current).isEmpty()) {
            return false;
        }
        int size = chest.getContainerSize();
        int start = cursor.simplebuilding$getPlaylistSlot();
        for (int i = 1; i <= size; i++) {
            int slot = Math.floorMod(start + i, size);
            ItemStack candidate = chest.getItem(slot);
            if (!isDisc(candidate)) {
                continue;
            }
            if (current.isEmpty()) {
                jukebox.setTheItem(candidate.split(1));
                chest.setChanged();
                cursor.simplebuilding$setPlaylistSlot(slot);
                return true;
            }
            if (candidate.getCount() == 1) {
                ItemStack next = candidate.copy();
                chest.setItem(slot, current.copy());
                jukebox.setTheItem(next);
                cursor.simplebuilding$setPlaylistSlot(slot);
                return true;
            }
        }
        if (!current.isEmpty()) {
            jukebox.tryForcePlaySong();
            return true;
        }
        return false;
    }

    /** The chest on top, or null: a single chest (not half of a double chest) without unopened loot, if automation is allowed. */
    private static ChestBlockEntity chestAbove(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState state = level.getBlockState(above);
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return null;
        }
        if (!(level.getBlockEntity(above) instanceof ChestBlockEntity chest) || chest.getLootTable() != null) {
            return null;
        }
        return WorldPermissions.mayAutomate(level, pos, above) ? chest : null;
    }

    /** A music disc the jukebox can play (an item with a song that is loaded). */
    public static boolean isDisc(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.JUKEBOX_PLAYABLE) && JukeboxSong.fromStack(stack).isPresent();
    }
}
