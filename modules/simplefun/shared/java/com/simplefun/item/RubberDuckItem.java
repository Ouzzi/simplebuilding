package com.simplefun.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** FUN11-9 Quietscheente: placed on solid ground like any block, or onto a water surface like a lily pad. */
public class RubberDuckItem extends BlockItem {
  public RubberDuckItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    if (hit.getType() != HitResult.Type.BLOCK || level.getFluidState(hit.getBlockPos()).isEmpty())
      return InteractionResult.PASS;
    // An air click never passes the packet handler's spawn-protection check of a block click: ask for the building rights here.
    if (level instanceof net.minecraft.server.level.ServerLevel server
        && !com.simplefun.toys.FunToys.mayChange(server, player, hit.getBlockPos().above())) return InteractionResult.FAIL;
    return super.useOn(new UseOnContext(player, hand, hit.withPosition(hit.getBlockPos().above())));
  }
}
