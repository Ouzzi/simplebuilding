package com.simplebuilding.tweaks.block.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import com.simplebuilding.tweaks.easter.EasterEggs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsame Basis aller Simple-Tweaks-Platten: sie merken sich, wer sie gesetzt hat (Schluessel
 * {@code Owner}, UUID als Int-Array wie in Simple Tweaks), und schicken das an den Client, damit der
 * Abbaufortschritt (Besitzer schnell, Fremde langsam) auf beiden Seiten gleich rechnet.
 */
public abstract class OwnedBlockEntity extends BlockEntity {
    private @Nullable UUID owner;
    /** Easter-Stufe des gesetzten Pads (0 = normal), siehe {@link EasterEggs}. */
    private int easterStage;

    protected OwnedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    /** Easter-Stufe (0 = normales Pad); nur Stufen, die zum Block passen, zaehlen. */
    public int easterStage() {
        return EasterEggs.fits(getBlockState().getBlock(), easterStage) ? easterStage : 0;
    }

    public void setEasterStage(int stage) {
        this.easterStage = Math.max(0, stage);
        setChanged();
    }

    /** Beim Setzen: die Easter-Stufe vom Item uebernehmen (Name wird aus der Stufe neu gebildet). */
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer stage = components.get(EasterEggs.EASTER_STAGE);
        this.easterStage = stage == null ? 0 : stage;
        if (this.easterStage > 0) {
            // Der Name folgt aus der Stufe; nicht als lose Komponente an der Block-Entity kleben lassen.
            components.get(DataComponents.ITEM_NAME);
        }
    }

    /** Fuer Strg+Mittelklick und {@code copy_components}: Stufe und Name zurueck aufs Item. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        int stage = easterStage();
        if (stage > 0) {
            components.set(EasterEggs.EASTER_STAGE, stage);
            components.set(DataComponents.ITEM_NAME, EasterEggs.nameFor(getBlockState().getBlock(), stage));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("EasterStage");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            output.store("Owner", UUIDUtil.CODEC, owner);
        }
        if (easterStage > 0) {
            output.putInt("EasterStage", easterStage);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.easterStage = input.getIntOr("EasterStage", 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
