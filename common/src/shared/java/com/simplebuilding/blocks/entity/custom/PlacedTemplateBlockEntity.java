package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.PlacedTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Die abgelegte Schmiedevorlage ({@link PlacedTemplateBlock}): haelt den Vorlagen-Stapel samt allen
 * Komponenten (Schluessel {@code Template}) und schickt ihn zum Client, der ihn mit dem
 * {@code PlacedTemplateRenderer} als flache, leicht erhabene Platte zeichnet.
 *
 * <p>Nicht gespeichert: der Zaehler der Hammerschlaege (verfaellt ohnehin nach
 * {@link PlacedTemplates#HIT_RESET_TICKS}) und der Zeitpunkt des letzten Hinweis-Tons.
 */
public class PlacedTemplateBlockEntity extends BlockEntity {
    private static final String TEMPLATE_TAG = "Template";

    private ItemStack template = ItemStack.EMPTY;
    private int hits;
    private @Nullable Item hitCatalyst;
    private long lastHitTime = Long.MIN_VALUE / 2;
    private long lastHintSound = Long.MIN_VALUE / 2;

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
        if (level instanceof ServerLevel server && (level.getGameTime() + pos.asLong()) % PlacedTemplates.HINT_INTERVAL == 0) {
            PlacedTemplates.tryHint(server, pos, be);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.template = input.read(TEMPLATE_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.template.isEmpty()) {
            output.store(TEMPLATE_TAG, ItemStack.CODEC, this.template);
        }
    }

    /** Der Client braucht die Vorlage zum Zeichnen. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
