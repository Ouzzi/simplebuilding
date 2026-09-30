package com.simplefun.item;

import com.simplefun.entity.BrickProjectileEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BrickSnowballItem extends Item {

    public BrickSnowballItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Dedicated item - not gated by the enableThrowableBricks flag.
        BrickProjectileEntity.throwFrom(level, player, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }
}
