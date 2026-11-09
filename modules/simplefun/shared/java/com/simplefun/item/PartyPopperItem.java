package com.simplefun.item;

import com.simplefun.toys.FunToys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** FUN11-1 Konfetti-Knaller: one use, confetti in the dye's colour (at most 40 particles), 3 s cooldown. */
public class PartyPopperItem extends Item {
  public PartyPopperItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!FunToys.on("partyPopper")) return InteractionResult.PASS;
    if (player instanceof ServerPlayer serverPlayer)
      return FunToys.pop(serverPlayer, player.getItemInHand(hand))
          ? InteractionResult.SUCCESS_SERVER
          : InteractionResult.FAIL;
    return InteractionResult.SUCCESS;
  }
}
