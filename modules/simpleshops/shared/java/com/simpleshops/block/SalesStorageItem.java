package com.simpleshops.block;

import com.simpleshops.logic.ShopActions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * The sales storage item (SP22-7): it only places with sneak + right click on one's own sales block (any side), facing it.
 * Anywhere else the click does nothing at all, on the client and the server alike (no ghost block, no lost item); the server
 * names the rule in the action bar.
 */
public class SalesStorageItem extends BlockItem {
    public SalesStorageItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!ShopActions.storageTarget(context.getLevel(), context.getClickedPos(), context.getPlayer())) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("message.simpleshops.storage_needs_sales"));
            }
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
}
