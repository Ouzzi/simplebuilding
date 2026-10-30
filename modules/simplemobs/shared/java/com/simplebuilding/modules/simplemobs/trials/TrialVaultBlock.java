package com.simplebuilding.modules.simplemobs.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Weathered Vault, the Evil Vault and the Void Vault: Vanilla's {@link VaultBlock} (same block entity, ticking, ejecting,
 * "once per player") with another key and loot table in the block entity's config. A fresh block entity gets the config of
 * its tier; structures and the in-world upgrade write theirs (see {@link TrialsHooks}).
 */
public class TrialVaultBlock extends VaultBlock {
    private final TrialKind kind;

    public TrialVaultBlock(TrialKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public TrialKind kind() {
        return kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntity entity = super.newBlockEntity(pos, state);
        if (entity instanceof VaultBlockEntity vault) vault.setConfig(Trials.vaultConfig(kind, state.getValue(OMINOUS)));
        return entity;
    }

    /** Evil / Void Vaults take only their own keys: a Trial Key or the other tier's key gets a hint instead of a silent no. */
    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind != TrialKind.WEATHERED && !level.isClientSide() && Trials.isAnyKey(stack) && Trials.keyKind(stack) != kind) {
            player.sendOverlayMessage(Component.translatable("simplemobs.trials.wrong_key." + kind.id()));
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
