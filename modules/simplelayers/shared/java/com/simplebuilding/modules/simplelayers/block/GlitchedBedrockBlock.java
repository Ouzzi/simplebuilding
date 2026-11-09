package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glitched Bedrock (concept §2.1 "Eingang"): looks like bedrock, one pixel flickers yellow now and then (animated texture, A14).
 * Only a Netherite Pickaxe breaks it (hardness 180: about 30 s without Efficiency); everything else makes no progress. Broken,
 * it leaves a {@link RiftBlock} behind that drops whoever falls in into the Backrooms. With the Backrooms switched off it is as
 * hard as bedrock.
 */
public class GlitchedBedrockBlock extends Block {
    public static final float HARDNESS = 180.0F;

    public GlitchedBedrockBlock(Properties properties) {
        super(properties);
    }

    /** The concept's rule: only the Netherite Pickaxe, and only while the Backrooms are on. */
    public static boolean canMine(Player player) {
        return SimpleLayers.backroomsOn() && player.getMainHandItem().is(Items.NETHERITE_PICKAXE);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return canMine(player) ? super.getDestroyProgress(state, player, level, pos) : 0.0F;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (SimpleLayers.backroomsOn() && !level.isClientSide() && level.getBlockState(pos).isAir()) {
            level.setBlock(pos, LayersRegistry.RIFT.defaultBlockState(), 3);
        }
    }
}
