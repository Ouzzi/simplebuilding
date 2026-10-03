package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HammockLayout;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Hangs a hammock between two anchors (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md, {@link HammockLayout}). Places all of
 * its blocks at once; without fitting anchors nothing is placed and the player hears the fail sound (no text).
 */
public class HammockItem extends BlockItem {
    public HammockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        Optional<HammockLayout.Spot> spot = HammockLayout.find(level, context.getClickedPos(), context.getClickedFace(),
                context.getHorizontalDirection());
        Map<BlockPos, BlockState> states = spot.map(s -> HammockLayout.states(getBlock(), ModBlocks.HAMMOCK_ROPE, s)).orElse(Map.of());
        if (spot.isEmpty() || !HammockLayout.unobstructed(level, states) || player != null && !player.mayBuild()) {
            if (player instanceof ServerPlayer serverPlayer) {
                HammockLayout.refuse(serverPlayer, context.getClickedPos());
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            // Without shape updates first, so no half-built hammock tears itself down; then tell the neighbours.
            states.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE));
            states.forEach((pos, state) -> {
                level.updateNeighborsAt(pos, state.getBlock());
                state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            });
            BlockPos head = spot.get().lowerHead();
            SoundType sound = getBlock().defaultBlockState().getSoundType();
            level.playSound(null, head, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, head);
        }
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplebuilding.hammock").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.simplebuilding.hammock.2").withStyle(ChatFormatting.GRAY));
    }
}
