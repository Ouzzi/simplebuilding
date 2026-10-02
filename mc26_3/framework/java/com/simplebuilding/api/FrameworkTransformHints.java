package com.simplebuilding.api;

import com.simplebuilding.framework.api.TransformHints;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Minecraft-specific adapter for the module hand hints; only the 26.3 overlay publishes this service. */
public final class FrameworkTransformHints implements ModuleTransformHints.Bridge {
    @Override
    public boolean wouldTransform(Level level, Player player, BlockHitResult hit, InteractionHand hand) {
        return TransformHints.any(new TransformHints.Query(level, player, hit, hand == InteractionHand.MAIN_HAND));
    }
}
