package com.simplefun.item;

import com.simplefun.toys.FunToys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** FUN11-5 flat skipping stone and FUN11-6 water balloon: thrown like a snowball, server cooldown, no damage. */
public class ToyThrowItem extends Item {
  public enum Kind {
    SKIPPING_STONE("skipStones", 10),
    WATER_BALLOON("waterBalloon", 100);

    public final String key;
    public final int cooldown;

    Kind(String key, int cooldown) {
      this.key = key;
      this.cooldown = cooldown;
    }
  }

  private final Kind kind;

  public ToyThrowItem(Properties properties, Kind kind) {
    super(properties);
    this.kind = kind;
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!FunToys.on(kind.key)) return InteractionResult.PASS;
    if (player instanceof ServerPlayer serverPlayer)
      return FunToys.throwToy(serverPlayer, player.getItemInHand(hand), kind) != null
          ? InteractionResult.SUCCESS_SERVER
          : InteractionResult.FAIL;
    return InteractionResult.SUCCESS;
  }
}
