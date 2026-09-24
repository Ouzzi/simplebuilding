// ModBlastFurnaceBlock.java
package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.util.SledgehammerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ModBlastFurnaceBlock extends BlastFurnaceBlock {
    public ModBlastFurnaceBlock(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModBlastFurnaceBlockEntity(pos, state);
    }

    /**
     * Vorschlaghammer in der Haupthand und Nugget in der Nebenhand: kann die Aufwertung beginnen
     * (oder kuehlt der Hammer nach einer fertigen noch ab), geht der Rechtsklick am Menue vorbei an
     * den Hammer weiter. Sonst oeffnet das Menue wie immer. Siehe {@link SledgehammerUpgrades}.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, world, pos, player, hand)) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    /**
     * Stufenwechsel innerhalb der Familie - die Aufwertung mit dem Vorschlaghammer, aber auch ein
     * {@code /setblock} - behaelt die Block-Entity mit Inhalt und Fortschritt bei: alle Stufen
     * teilen sich denselben Block-Entity-Typ. Ohne das entfernt {@code LevelChunk#setBlockState}
     * sie bei jedem Blockwechsel und wirft den Inhalt aus.
     */
    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof ModBlastFurnaceBlock;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            this.openContainer(world, pos, player);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    protected void openContainer(Level world, BlockPos pos, Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ModBlastFurnaceBlockEntity) {
            player.openMenu((MenuProvider)blockEntity);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {return null;}
        return createTickerHelper(type, ModBlockEntities.MOD_BLAST_FURNACE_BE, (w, pos, st, be) -> ModBlastFurnaceBlockEntity.tick((ServerLevel) w, pos, st, be));
    }
}