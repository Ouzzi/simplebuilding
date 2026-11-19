package com.simpleslabs.item;

import com.simpleslabs.block.HalfSlabBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Half slab item (N86): places {@link HalfSlabBlock} (sneaking on a side face: the side half slab). On its own real slab
 * (bottom or top, not double) it adds a layer: the slab becomes the half slab block with layers 0-1 (bottom) or 2-3 (top) plus
 * the clicked layer - owner N86-2 "slab + half slab". The slab itself is not touched as a class (A12): a slab item on a half
 * slab does nothing special. Waxed copper half slabs say so in one grey line (SimpleLib's aging tooltip).
 */
public class HalfSlabItem extends BlockItem {
    public HalfSlabItem(HalfSlabBlock block, Item.Properties properties) {
        super(block, properties);
    }

    public HalfSlabBlock halfSlab() {
        return (HalfSlabBlock) getBlock();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        BlockState merged = ontoSlab(clicked, context);
        if (merged == null) return super.useOn(context);
        Player player = context.getPlayer();
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlock(pos, merged, 3);
            SoundType sound = merged.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, merged));
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** The half slab state after this item was used on {@code clicked}, null when it is not the material's single slab or the layer is taken. */
    public BlockState ontoSlab(BlockState clicked, UseOnContext context) {
        HalfSlabBlock block = halfSlab();
        if (context.isSecondaryUseActive() || !clicked.is(block.slab()) || !clicked.hasProperty(SlabBlock.TYPE)) return null;
        SlabType type = clicked.getValue(SlabBlock.TYPE);
        if (type == SlabType.DOUBLE) return null;
        int half = (1 << HalfSlabBlock.LAYERS / 2) - 1;
        int slabMask = type == SlabType.BOTTOM ? half : half << HalfSlabBlock.LAYERS / 2;
        int layer = HalfSlabBlock.targetLayer(context, true);
        if (layer < 0 || (slabMask >> layer & 1) != 0) return null;
        boolean water = clicked.hasProperty(SlabBlock.WATERLOGGED) && clicked.getValue(SlabBlock.WATERLOGGED);
        return block.withMask(block.defaultBlockState().setValue(HalfSlabBlock.WATERLOGGED, water), slabMask | 1 << layer);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        var entry = com.simplelib.api.aging.Aging.entry(getBlock());
        if (entry != null && entry.waxed()) {
            lines.accept(Component.translatable(com.simplelib.api.aging.AgingChain.WAXED_TOOLTIP).withStyle(ChatFormatting.GRAY));
        }
    }
}
