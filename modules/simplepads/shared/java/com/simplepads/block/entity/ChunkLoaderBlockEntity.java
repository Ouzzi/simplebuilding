package com.simplepads.block.entity;

import com.mojang.serialization.Codec;
import com.simplepads.SimplePads;
import com.simplepads.block.ChunkLoaderBlock;
import com.simplepads.easter.EasterEggs;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Chunk-Loader (Simple Tweaks): erzwingt seinen Chunk-Bereich und gibt ihn beim Abbau wieder frei.
 *
 * <p>Anders als in Simple Tweaks merkt er sich, welche Chunks ER erzwungen hat (gespeichert unter
 * {@code Forced}), und gibt nur diese frei: Simple Tweaks rief beim Entladen blind
 * {@code setChunkForced(false)} - das hob auch fremde Erzwingungen auf (/forceload, andere Loader,
 * die Spieltest-Umgebung), und weil es schon beim Entladen der Block-Entity (Serverstopp) geschah,
 * tickte der Loader nach einem Neustart nie wieder. Jetzt geschieht die Freigabe nur beim Abbau
 * ({@link #preRemoveSideEffects}, und in {@link #setRemoved} fuer {@code /setblock}/{@code /fill},
 * die mit Flag 256 das Erste ueberspringen) oder wenn der Config-Schalter aus ist.
 *
 * <p>Ueberlappen sich zwei Loader, gehoert ein Chunk dem, der ihn zuerst erzwang; wird der abgebaut,
 * uebernimmt ein anderer Loader, der den Chunk ebenfalls abdeckt, statt dass der Chunk freikommt
 * (Audit 2026-09-26 #32).
 *
 * <p><b>Abgeschaltet</b> (a-chunk36, Besitzer 12.11.2026): ein Redstone-Signal schaltet ihn ab (wie den Vanilla-Trichter
 * und die uebrigen Pads), ebenso das Limit je Spieler ({@link ChunkLoaderRegistry#withinLimit}); dann gibt er frei und
 * zeigt {@link ChunkLoaderBlock#DISABLED}. Ein Signalwechsel wirkt sofort ({@link #redstoneChanged}).
 */
public class ChunkLoaderBlockEntity extends OwnedBlockEntity {
    public static final int CHECK_INTERVAL = 100;
    /**
     * Groesster Radius eines Loaders in Chunks (Stufe II und III: 1, die letzte Easter-Stufe: 2); so
     * weit sucht die Uebergabe nach Nachbarn.
     */
    public static final int MAX_RADIUS = 2;
    private static final Codec<List<Long>> FORCED_CODEC = Codec.LONG.listOf();

    /** Warum er abgeschaltet ist ({@link #offReason}). */
    public static final int OFF_NONE = 0, OFF_REDSTONE = 1, OFF_LIMIT = 2;

    private final Set<Long> ownForced = new LinkedHashSet<>();
    private boolean checked;
    /** Redstone-Signal bei der letzten Pruefung (nur zur Laufzeit, jede Pruefung liest es neu). */
    private boolean powered;
    private int offReason = OFF_NONE;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(PadsBlockEntities.CHUNK_LOADER, pos, state);
    }

    public static boolean enabled() {
        return SimplePads.config().pads.enableChunkLoaders;
    }

    /** Stufe des Loaders (1-3); ein fremder Block zaehlt wie Stufe I. */
    public static int tierOf(BlockState state) {
        return state.getBlock() instanceof ChunkLoaderBlock loader ? loader.getTier() : 1;
    }

    /** Chunks, die dieser Loader erzwungen hat (fuer Tests und die Freigabe). */
    public Set<Long> ownForced() {
        return ownForced;
    }

    public static long key(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity be) {
        if (level instanceof ServerLevel serverLevel && (!be.checked || level.getGameTime() % CHECK_INTERVAL == 0)) {
            be.checked = true;
            be.update(serverLevel);
        }
    }

    /**
     * Erzwingt den Bereich seiner Stufe bzw. gibt die eigenen Chunks frei, wenn er nicht laufen darf
     * ({@link ChunkLoaderRegistry#mayRun}: Pads-Schalter aus, Dimension gesperrt oder - seit 2026-09-28
     * standardmaessig - Besitzer offline), ein Redstone-Signal ihn abschaltet oder das Limit seines Besitzers
     * erreicht ist (a-chunk36). Traegt sich dabei in die {@link ChunkLoaderRegistry} ein.
     */
    public void update(ServerLevel level) {
        int tierNow = tierOf(getBlockState());
        powered = level.hasNeighborSignal(worldPosition);
        boolean mayRun = mayRun(level);
        offReason = powered ? OFF_REDSTONE
                : mayRun && !ChunkLoaderRegistry.withinLimit(level, worldPosition, getOwner()) ? OFF_LIMIT : OFF_NONE;
        if (!mayRun || offReason != OFF_NONE) {
            // Pads aus, Dimension gesperrt oder Besitzer offline (matt) bzw. Redstone/Limit (abgeschaltet): freigeben.
            release(level);
            setState(level, false, offReason != OFF_NONE);
            ChunkLoaderRegistry.update(level, worldPosition, getOwner(), tierNow, false, powered);
            return;
        }
        int cx = worldPosition.getX() >> 4;
        int cz = worldPosition.getZ() >> 4;
        int tier = tierOf(getBlockState());
        boolean doubled = boosted();
        boolean changed = false;
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
            for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                if (!ChunkLoaderBlock.inArea(tier, dx, dz, doubled)) {
                    continue;
                }
                // true = war vorher nicht erzwungen, also unsere Erzwingung.
                if (level.setChunkForced(cx + dx, cz + dz, true)) {
                    changed |= ownForced.add(key(cx + dx, cz + dz));
                }
            }
        }
        if (changed) {
            setChanged();
        }
        ChunkLoaderRegistry.update(level, worldPosition, getOwner(), tier, true, false);
        setState(level, true, false);
    }

    /**
     * Ein Nachbar hat sich geaendert ({@link ChunkLoaderBlock#neighborChanged}): hat das Redstone-Signal gewechselt,
     * sofort neu pruefen statt bis zur naechsten Pruefung zu warten.
     */
    public void redstoneChanged(ServerLevel level) {
        if (level.hasNeighborSignal(worldPosition) != powered || !checked) {
            checked = true;
            update(level);
        }
    }

    /** Warum er abgeschaltet ist: {@link #OFF_NONE}, {@link #OFF_REDSTONE} oder {@link #OFF_LIMIT} (Jade). */
    public int offReason() {
        return offReason;
    }

    /** Wie {@link #setState} ohne den Zustand "abgeschaltet". */
    public void setActive(ServerLevel level, boolean active) {
        setState(level, active, false);
    }

    /**
     * Sichtbarer Zustand ({@link ChunkLoaderBlock#ACTIVE}, {@link ChunkLoaderBlock#DISABLED}): beim Wechsel
     * Leuchtfeuer-Klang und ein Funkenwirbel (an) bzw. der Abschaltklang mit Rauch (aus), beim Abschalten
     * (Redstone/Limit) dazu ein paar Elektro-Funken. Beim Laden der Welt steht der Zustand schon im Block, dann
     * bleibt es still.
     */
    public void setState(ServerLevel level, boolean active, boolean disabled) {
        BlockState state = getBlockState();
        if (!state.hasProperty(ChunkLoaderBlock.ACTIVE) || level.getBlockState(worldPosition) != state) {
            return;
        }
        boolean wasActive = state.getValue(ChunkLoaderBlock.ACTIVE);
        boolean wasDisabled = state.hasProperty(ChunkLoaderBlock.DISABLED) && state.getValue(ChunkLoaderBlock.DISABLED);
        if (wasActive == active && wasDisabled == disabled) {
            return;
        }
        BlockState next = state.setValue(ChunkLoaderBlock.ACTIVE, active);
        if (next.hasProperty(ChunkLoaderBlock.DISABLED)) {
            next = next.setValue(ChunkLoaderBlock.DISABLED, disabled);
        }
        level.setBlock(worldPosition, next, Block.UPDATE_ALL);
        if (wasActive != active) {
            level.playSound(null, worldPosition, active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        double x = worldPosition.getX() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        if (active) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, worldPosition.getY() + 0.2, z, 24, 0.3, 0.1, 0.3, 0.05);
        } else {
            level.sendParticles(ParticleTypes.SMOKE, x, worldPosition.getY() + 0.15, z, 6, 0.25, 0.02, 0.25, 0.0);
            if (disabled && !wasDisabled) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, worldPosition.getY() + 0.15, z, 8, 0.3, 0.05, 0.3, 0.1);
            }
        }
    }

    /** Ob der Loader abgeschaltet ist (Redstone oder Limit; Blockzustand). */
    public boolean isDisabled() {
        BlockState state = getBlockState();
        return state.hasProperty(ChunkLoaderBlock.DISABLED) && state.getValue(ChunkLoaderBlock.DISABLED);
    }

    /** Ob der Loader gerade laedt (Blockzustand). */
    public boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(ChunkLoaderBlock.ACTIVE) && state.getValue(ChunkLoaderBlock.ACTIVE);
    }

    /** Ob dieser Loader gerade Chunks halten darf (siehe {@link ChunkLoaderRegistry#mayRun}). */
    public boolean mayRun(ServerLevel level) {
        return ChunkLoaderRegistry.mayRun(level, getOwner());
    }

    /**
     * Gibt die eigenen Chunks frei; ein Chunk, den ein anderer (eingeschalteter) Loader ebenfalls
     * abdeckt, bleibt erzwungen und geht an diesen ueber.
     */
    public void release(ServerLevel level) {
        if (ownForced.isEmpty()) {
            return;
        }
        for (long key : ownForced) {
            int chunkX = (int) key;
            int chunkZ = (int) (key >> 32);
            ChunkLoaderBlockEntity heir = enabled() ? coveringNeighbour(level, chunkX, chunkZ) : null;
            if (heir != null) {
                heir.ownForced.add(key);
                heir.setChanged();
            } else {
                level.setChunkForced(chunkX, chunkZ, false);
            }
        }
        ownForced.clear();
        setChanged();
    }

    /** Ein anderer geladener, nicht abgeschalteter Loader, dessen Bereich den Chunk abdeckt, oder null. */
    private @Nullable ChunkLoaderBlockEntity coveringNeighbour(ServerLevel level, int chunkX, int chunkZ) {
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
            for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    // a-chunk36: ein abgeschalteter Loader (Redstone/Limit) erbt nichts.
                    if (be != this && !be.isRemoved() && be instanceof ChunkLoaderBlockEntity other && other.covers(chunkX, chunkZ)
                            && other.mayRun(level) && !other.isDisabled()) {
                        return other;
                    }
                }
            }
        }
        return null;
    }

    /** Ob der Chunk im Bereich dieses Loaders liegt. */
    public boolean covers(int chunkX, int chunkZ) {
        return ChunkLoaderBlock.inArea(tierOf(getBlockState()), chunkX - (worldPosition.getX() >> 4), chunkZ - (worldPosition.getZ() >> 4),
                boosted());
    }

    /** Letzte Easter-Stufe ({@link EasterEggs}): doppelter Radius. */
    private boolean boosted() {
        return EasterEggs.isFinal(getBlockState().getBlock(), easterStage());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            release(serverLevel);
            ChunkLoaderRegistry.remove(serverLevel, worldPosition);
        }
        super.preRemoveSideEffects(pos, state);
    }

    /**
     * {@code /setblock} und {@code /fill} setzen mit Flag 256 und ueberspringen
     * {@link #preRemoveSideEffects} - die Tickets blieben fuer immer (Audit #5). Hier wird
     * freigegeben, wenn an der Stelle schon ein anderer Block steht oder die Block-Entity an der
     * Stelle nicht mehr diese ist - {@code /setblock} eines Loader-Typs auf einen anderen laesst dort
     * wieder einen Loader stehen, entfernt aber diese Block-Entity (Nach-Audit N10). Beim Entladen
     * des Chunks (Serverstopp) stehen Block und Block-Entity noch da, dann bleibt alles erzwungen.
     */
    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && replacedInWorld(serverLevel)) {
            release(serverLevel);
            ChunkLoaderRegistry.remove(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    private boolean replacedInWorld(ServerLevel level) {
        // getChunkNow laedt nichts nach; waehrend des Entladens liefert es null oder noch den Loader.
        LevelChunk chunk = level.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        // removeBlockEntity nimmt die Block-Entity vor setRemoved aus der Tabelle, clearAllBlockEntities
        // (Entladen) erst danach; getBlockEntities() erzeugt anders als getBlockEntity nichts neu.
        return chunk != null && (!(chunk.getBlockState(worldPosition).getBlock() instanceof ChunkLoaderBlock)
                || chunk.getBlockEntities().get(worldPosition) != this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!ownForced.isEmpty()) {
            output.store("Forced", FORCED_CODEC, new ArrayList<>(ownForced));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownForced.clear();
        input.read("Forced", FORCED_CODEC).ifPresent(ownForced::addAll);
    }
}
