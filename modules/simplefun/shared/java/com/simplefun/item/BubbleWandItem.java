package com.simplefun.item;

import com.simplefun.toys.FunToys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** FUN11-7 Seifenblasen: hold use; every {@link FunToys#BUBBLE_INTERVAL} ticks one bubble, at most 12 per player at once. */
public class BubbleWandItem extends Item {
  public BubbleWandItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!FunToys.on("bubbleWand")) return InteractionResult.PASS;
    player.startUsingItem(hand);
    return InteractionResult.CONSUME;
  }

  @Override
  public ItemUseAnimation getUseAnimation(ItemStack stack) {
    return ItemUseAnimation.TOOT_HORN;
  }

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity user) {
    return 72000;
  }

  @Override
  public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
    if (user instanceof ServerPlayer player && (72000 - ticksRemaining) % FunToys.BUBBLE_INTERVAL == 0)
      FunToys.blowBubble(player, stack);
  }
}
