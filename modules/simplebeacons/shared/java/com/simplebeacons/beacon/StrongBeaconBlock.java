package com.simplebeacons.beacon;

import com.simplebeacons.BeaconsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Block of the Strong, Mighty, Evil and Mighty Evil beacons: like Vanilla's beacon (white beam, opens its menu), with {@link StrongBeaconBlockEntity}. */
public class StrongBeaconBlock extends BaseEntityBlock implements BeaconBeamBlock {
    private final BeaconKind kind;

    public StrongBeaconBlock(BeaconKind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public BeaconKind kind() {
        return this.kind;
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.WHITE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StrongBeaconBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BeaconsRegistry.STRONG_BEACON_BE, StrongBeaconBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StrongBeaconBlockEntity beacon) {
            player.openMenu(beacon);
            player.awardStat(Stats.INTERACT_WITH_BEACON);
        }
        return InteractionResult.SUCCESS;
    }
}
