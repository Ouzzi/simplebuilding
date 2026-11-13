package com.simplebuilding.modules.simplemobs.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;

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

    /**
     * Evil / Void Vaults take only their own keys. A Trial Key or the other tier's key puffs smoke at the keyhole; the reject sound
     * is Vanilla's own ({@code VAULT_INSERT_ITEM_FAIL}, played by {@code super} while the vault is active, like for any wrong item).
     * GADGET (owner 01.11., PLAN40 simplemobs-2): no action bar text.
     */
    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind != TrialKind.WEATHERED && level instanceof ServerLevel server && Trials.isAnyKey(stack) && Trials.keyKind(stack) != kind) {
            Vec3 at = hit.getLocation();
            Direction face = hit.getDirection();
            server.sendParticles(ParticleTypes.SMOKE, at.x + face.getStepX() * 0.1, at.y + face.getStepY() * 0.1, at.z + face.getStepZ() * 0.1,
                    6, 0.08, 0.08, 0.08, 0.01);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
