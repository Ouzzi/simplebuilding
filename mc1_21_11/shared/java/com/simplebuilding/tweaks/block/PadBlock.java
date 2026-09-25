package com.simplebuilding.tweaks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsamer Teil aller Simple-Tweaks-Pads mit Block-Entity: flache Form, Besitzer beim Setzen,
 * Abbautempo nach Besitz (siehe {@link PadOwnership}).
 */
public abstract class PadBlock extends BaseEntityBlock {
    private final VoxelShape shape;
    private final float ownerSpeed;
    private final float strangerSpeed;

    protected PadBlock(BlockBehaviour.Properties properties, VoxelShape shape, float ownerSpeed, float strangerSpeed) {
        super(properties);
        this.shape = shape;
        this.ownerSpeed = ownerSpeed;
        this.strangerSpeed = strangerSpeed;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        PadOwnership.onPlaced(level, pos, by);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return PadOwnership.destroyProgress(player, level, pos, ownerSpeed, strangerSpeed,
                super.getDestroyProgress(state, player, level, pos));
    }
}
