package com.simplestorage.chest;

import com.simplestorage.registry.StorageBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Verstaerkte, Netherit- und Enderittruhe. Eine Vanilla-Truhe in allem, was die Welt sieht
 * (Form, Doppeltruhen, Wasser, Katzen und Bloecke auf dem Deckel, Piglins, Statistik, Deckel,
 * Klang), mit eigener Block-Entity ({@link TieredChestBlockEntity}) und eigenem Menue.
 *
 * <p><b>Doppeltruhen</b> bilden sich nur aus zwei Truhen derselben Stufe: Vanillas
 * {@code chestCanConnectTo} fragt {@code state.is(this)}, und jede Stufe ist ein eigener Block.
 * Aufgewertet werden beide Haelften zugleich ({@link TieredChests#upgradeInPlace}).
 */
public class TieredChestBlock extends ChestBlock {
    private final ChestTier tier;

    public TieredChestBlock(ChestTier tier, BlockBehaviour.Properties properties) {
        this(tier, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundEvents.CHEST_CLOSE, properties);
    }

    public TieredChestBlock(ChestTier tier, SoundEvent open, SoundEvent close, BlockBehaviour.Properties properties) {
        super(() -> StorageBlockEntities.TIERED_CHEST, open, close, properties);
        this.tier = tier;
    }

    public ChestTier tier() {
        return this.tier;
    }

    public boolean isTrapped() {
        return false;
    }

    public String textureName() {
        return this.tier.textureName() + (isTrapped() ? "_trapped" : "");
    }

    /** The container title stays the normal tier name even for trapped chests. */
    public Component getContainerName() {
        String descriptionId = this.getDescriptionId();
        return Component.translatable(isTrapped() ? descriptionId.replace("_trapped", "") : descriptionId);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredChestBlockEntity(pos, state);
    }

    /**
     * Wie Vanilla, nur mit dem eigenen Menue samt Oeffnungsdaten. Im Schmiedestand (Hammer plus
     * Aufwertungs-Material) geht der Klick an den Hammer weiter, wenn die Aufwertung beginnen kann.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult upgrade = StorageUse.upgradeClick(state, level, pos, player, InteractionHand.MAIN_HAND);
        if (upgrade != null) {
            return upgrade;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            TieredChests.Opening opening = TieredChests.opening(this, state, level, pos);
            if (opening != null) {
                serverPlayer.openMenu(opening.provider());
                player.awardStat(this.getOpenChestStat());
                PiglinAi.angerNearbyPiglins(serverLevel, player, true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        TieredChests.Opening opening = TieredChests.opening(this, state, level, pos);
        return opening == null ? null : opening.provider();
    }

    /** Wie Vanilla, aber gegen die Stapelgrenze dieser Stufe gerechnet (256 Steine sind voll, nicht 64). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return TieredChests.analogSignal(getContainer(this, state, level, pos, false), this.tier);
    }

    /** Fuer Tests und Nachbarn: die Haelfte, mit der diese Truhe verbunden ist, sonst leer. */
    public static Optional<BlockPos> partner(BlockState state, BlockPos pos) {
        return state.getValue(TYPE) == net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                ? Optional.empty() : Optional.of(getConnectedBlockPos(pos, state));
    }
}
