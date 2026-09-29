package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.PlacedTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Die abgelegte Schmiedevorlage ({@link PlacedTemplateBlock}): haelt den Vorlagen-Stapel samt allen
 * Komponenten (Schluessel {@code Template}) und schickt ihn zum Client, der ihn mit dem
 * {@code PlacedTemplateRenderer} als flache, leicht erhabene Platte zeichnet.
 *
 * <p>Name ({@link Nameable}): der Name der abgelegten Vorlage bzw. Blaupause, nicht der des Blocks -
 * Jade und alle anderen Anzeigen, die nach dem Namen der Block-Entity fragen, zeigen so
 * "Leuchtender Rüstungsbesatz" statt "Abgelegte Schmiedevorlage" (Besitzer 2026-09-28).
 *
 * <p>Ein abgelegter Oktant merkt sich ausserdem, welche Spieler seine Auswahl eingeblendet haben
 * ({@code OutlineViewers}, gespeichert und zum Client geschickt; jeder Client zeigt nur, was fuer den
 * eigenen Spieler eingeblendet ist).
 *
 * <p>Nicht gespeichert: der Zaehler der Hammerschlaege (verfaellt ohnehin nach
 * {@link PlacedTemplates#HIT_RESET_TICKS}) und der Zeitpunkt des letzten Hinweis-Tons.
 */
public class PlacedTemplateBlockEntity extends BlockEntity implements Nameable {
    private static final String TEMPLATE_TAG = "Template";
    private static final String OUTLINE_VIEWERS_TAG = "OutlineViewers";

    /**
     * Die clientseitig geladenen abgelegten Platten (nur Client-Welten): der Renderer der
     * Oktant-Auswahl ({@code BlockHighlightRenderer}) sucht darin die abgelegten Oktanten, die der
     * eigene Spieler eingeblendet hat, statt jedes Bild alle Block-Entities der Welt abzufragen.
     * Schwach gehalten, damit ein Weltwechsel ohne {@link #setRemoved()} nichts festhaelt.
     */
    public static final Set<PlacedTemplateBlockEntity> CLIENT_LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private ItemStack template = ItemStack.EMPTY;
    private int hits;
    private @Nullable Item hitCatalyst;
    private long lastHitTime = Long.MIN_VALUE / 2;
    private long lastHintSound = Long.MIN_VALUE / 2;
    /** Spieler, die die Auswahl dieses abgelegten Oktanten eingeblendet haben (Besitzer 2026-09-29). */
    private final Set<UUID> outlineViewers = new LinkedHashSet<>();

    public PlacedTemplateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_TEMPLATE_BE, pos, state);
    }

    public ItemStack getTemplate() {
        return this.template;
    }

    public void setTemplate(ItemStack stack) {
        this.template = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.hits = 0;
        this.hitCatalyst = null;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Der Name des abgelegten Stapels (samt Amboss-Namen), ohne Stapel der des Blocks. */
    @Override
    public Component getName() {
        return this.template.isEmpty() ? getBlockState().getBlock().getName() : this.template.getHoverName();
    }

    /** Jade fragt erst hiernach, dann nach {@link #getDisplayName()}. */
    @Override
    public @Nullable Component getCustomName() {
        return this.template.isEmpty() ? null : this.template.getHoverName();
    }

    /** Wer die Auswahl dieses abgelegten Oktanten eingeblendet hat (unveraenderliche Sicht). */
    public Set<UUID> outlineViewers() {
        return Collections.unmodifiableSet(this.outlineViewers);
    }

    /** Hat dieser Spieler die Auswahl eingeblendet? */
    public boolean showsOutlineTo(UUID player) {
        return this.outlineViewers.contains(player);
    }

    /** Blendet die Auswahl fuer diesen Spieler ein oder aus; liefert den neuen Zustand. */
    public boolean toggleOutlineViewer(UUID player) {
        boolean shown = this.outlineViewers.add(player) || !this.outlineViewers.remove(player);
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return shown;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && this.level.isClientSide()) {
            CLIENT_LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_LOADED.remove(this);
    }

    /** Bisher gezaehlte Hammerschlaege (0, solange keiner zaehlt). */
    public int hits() {
        return this.hits;
    }

    /**
     * Zaehlt einen Schlag mit diesem Material zur Spielzeit {@code now} und liefert den neuen Stand.
     * Ein anderes Material als beim letzten Schlag oder eine zu lange Pause beginnen von vorn.
     */
    public int registerHit(Item catalyst, long now) {
        if (catalyst != this.hitCatalyst || now - this.lastHitTime > PlacedTemplates.HIT_RESET_TICKS) {
            this.hits = 0;
        }
        this.hits++;
        this.hitCatalyst = catalyst;
        this.lastHitTime = now;
        return this.hits;
    }

    /** Darf zur Zeit {@code now} wieder ein Hinweis-Ton kommen? Merkt sich den Zeitpunkt, wenn ja. */
    public boolean takeHintSound(long now, int interval) {
        if (now - this.lastHintSound < interval) {
            return false;
        }
        this.lastHintSound = now;
        return true;
    }

    /** Richtung, in die die Schauseite der Vorlage zeigt (weg von Boden, Wand oder Decke). */
    public Direction normal() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof PlacedTemplateBlock ? PlacedTemplateBlock.normal(state) : Direction.UP;
    }

    /** Mitte der Schauseite, fuer Partikel. */
    public Vec3 surfaceCentre() {
        Direction normal = normal();
        double offset = -0.5 + 2.0 / 16.0;
        return Vec3.atCenterOf(this.worldPosition).add(normal.getStepX() * offset, normal.getStepY() * offset, normal.getStepZ() * offset);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlacedTemplateBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        // Abgelegter Attractor: zieht lose Items an (eigener Takt, siehe PlacedAttractors).
        com.simplebuilding.util.PlacedAttractors.tick(server, pos, be);
        if ((level.getGameTime() + pos.asLong()) % PlacedTemplates.HINT_INTERVAL == 0) {
            PlacedTemplates.tryHint(server, pos, be);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.template = input.read(TEMPLATE_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.outlineViewers.clear();
        input.read(OUTLINE_VIEWERS_TAG, UUIDUtil.CODEC.listOf()).ifPresent(this.outlineViewers::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.template.isEmpty()) {
            output.store(TEMPLATE_TAG, ItemStack.CODEC, this.template);
        }
        if (!this.outlineViewers.isEmpty()) {
            output.store(OUTLINE_VIEWERS_TAG, UUIDUtil.CODEC.listOf(), List.copyOf(this.outlineViewers));
        }
    }

    /** Der Client braucht die Vorlage zum Zeichnen und, bei einem Oktanten, wer dessen Auswahl sieht. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
