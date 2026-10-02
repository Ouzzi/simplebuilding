package com.simplebuilding.api;

import java.util.ServiceLoader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Hand hints other mods publish through the framework ({@code com.simplebuilding.framework.api.TransformHints}).
 * The 26.3 overlay supplies the bridge (like {@link WorldPermissions}); lines without the framework have
 * no provider, and no module hint ever shows there.
 */
public final class ModuleTransformHints {
    /** Implemented by the 26.3 framework adapter. */
    public interface Bridge {
        boolean wouldTransform(Level level, Player player, BlockHitResult hit, InteractionHand hand);
    }

    private static final Bridge BRIDGE = ServiceLoader.load(Bridge.class, ModuleTransformHints.class.getClassLoader()).findFirst().orElse(null);

    private ModuleTransformHints() {
    }

    /** Whether any module says this hand's right click would transform the aimed block. */
    public static boolean wouldTransform(Level level, Player player, BlockHitResult hit, InteractionHand hand) {
        return BRIDGE != null && BRIDGE.wouldTransform(level, player, hit, hand);
    }
}
