package com.simpleoctets.item;

import com.simpleoctets.block.HalfSlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * N86 half slab item (DRAFT, uncompiled): places like a slab item; on the open side of its own real slab it adds a layer
 * (N86-2 'A+': slab 8 px + 4 px = 12 px) - without touching {@code SlabBlock} (A12).
 */
public class HalfSlabItem extends BlockItem {
    public HalfSlabItem(HalfSlabBlock block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        HalfSlabBlock block = (HalfSlabBlock) getBlock();
        BlockState grown = block.ontoSlab(clicked);
        if (grown != null && openSide(clicked, context.getClickedFace())) {
            if (!level.isClientSide()) {
                level.setBlock(pos, grown, 3);
                Player player = context.getPlayer();
                SoundType sound = grown.getSoundType();
                level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
                level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, grown));
                if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    /** Bottom slab: clicked from above; top slab: from below (the side the stack grows to). */
    private static boolean openSide(BlockState slab, Direction face) {
        SlabType type = slab.getValue(SlabBlock.TYPE);
        return type == SlabType.BOTTOM ? face == Direction.UP : face == Direction.DOWN;
    }
}
