package com.simplefun.item;

import com.simplefun.toys.FunToys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** FUN11-2 Würfel und Münze: the server rolls (sneak: two dice), the result floats above the head and goes to chat within 16 blocks. */
public class RollItem extends Item {
  public enum Kind {
    DICE,
    COIN
  }

  private final Kind kind;

  public RollItem(Properties properties, Kind kind) {
    super(properties);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!FunToys.on("diceAndCoin")) return InteractionResult.PASS;
    if (player instanceof ServerPlayer serverPlayer)
      return FunToys.roll(serverPlayer, player.getItemInHand(hand), kind) != null
          ? InteractionResult.SUCCESS_SERVER
          : InteractionResult.FAIL;
    return InteractionResult.SUCCESS;
  }
}
