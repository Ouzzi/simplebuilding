package com.simplequalityoflife.event;

import com.simplebuilding.framework.api.TransformHints;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Publishes the hoe harvest as a hand hint through the framework: with SimpleBuilding loaded, the
 * hoe tilts slightly when a right click would harvest and replant the aimed crop. Server-safe
 * (registration touches no client class); without SimpleBuilding nobody asks.
 */
public final class HoeHarvestHint {
    public static final String ID = "simplequalityoflife:hoe_harvest";

    private HoeHarvestHint() {
    }

    public static void register() {
        TransformHints.register(ID, query -> query.player() instanceof Player player
                && query.hit() instanceof BlockHitResult hit
                && HoeHarvestHandler.wouldHarvest(player, query.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, hit.getBlockPos()));
    }
}
