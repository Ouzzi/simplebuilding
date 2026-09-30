package com.simplebuilding.blocks.custom;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.util.AstralStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Vanilla lid, waterlogging, particles, opener counter and sounds; a personal six-row menu. */
public class AstralVaultBlock extends EnderChestBlock {
    public AstralVaultBlock(Properties properties) { super(properties); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!ServerTuning.get().features.astralVault) return InteractionResult.PASS;
        if (level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above())) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof EnderChestBlockEntity chest) {
            var vanilla = player.getEnderChestInventory();
            var inventory = new CompoundContainer(vanilla, ((AstralStorage) vanilla).simplebuilding$astralStorage()) {
                @Override public boolean stillValid(Player user) {
                    return ServerTuning.get().features.astralVault && super.stillValid(user);
                }
            };
            vanilla.setActiveChest(chest);
            player.openMenu(new SimpleMenuProvider((id, inv, user) -> ChestMenu.sixRows(id, inv, inventory),
                    Component.translatable("block.simplebuilding.astral_vault")));
            player.awardStat(net.minecraft.stats.Stats.OPEN_ENDERCHEST);
            net.minecraft.world.entity.monster.piglin.PiglinAi.angerNearbyPiglins(server, player, true);
        }
        return InteractionResult.SUCCESS;
    }
}
