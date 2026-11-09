package com.simplefun.item;

import com.simplefun.toys.FunToys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * FUN11-8 Papierflieger: use throws it gliding (no damage, lands as an item). A book and quill or written book in the
 * other hand folds its first page in as a note; sneak + use reads the note instead of throwing.
 */
public class PaperPlaneItem extends Item {
  public PaperPlaneItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!FunToys.on("paperPlane")) return InteractionResult.PASS;
    if (player instanceof ServerPlayer serverPlayer) {
      if (player.isShiftKeyDown()) {
        return FunToys.readNote(serverPlayer, player.getItemInHand(hand))
            ? InteractionResult.SUCCESS_SERVER
            : InteractionResult.PASS;
      }
      return FunToys.throwPlane(serverPlayer, hand) != null
          ? InteractionResult.SUCCESS_SERVER
          : InteractionResult.FAIL;
    }
    return InteractionResult.SUCCESS;
  }
}
