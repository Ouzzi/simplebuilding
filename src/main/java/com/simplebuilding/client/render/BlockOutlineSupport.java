package com.simplebuilding.client.render;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BlockOutlineSupport {
    private BlockOutlineSupport() {
    }

    /** Suppress vanilla block selection outline when a custom highlight is drawn instead. */
    public static boolean suppressVanillaBlockOutline() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }

        ItemStack mainHand = client.player.getMainHandItem();
        if (mainHand.getItem() instanceof SledgehammerItem) {
            HitResult hit = client.hitResult;
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos center = blockHit.getBlockPos();
                if (SledgehammerUtils.canMineOrigin(client.level, center, mainHand)) {
                    return !SledgehammerItem.getBlocksToBeDestroyed(1, center, client.player).isEmpty();
                }
            }
            return false;
        }

        if (mainHand.getItem() instanceof OctantItem) {
            return true;
        }

        return client.player.getOffhandItem().getItem() instanceof OctantItem;
    }
}
