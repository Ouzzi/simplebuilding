package com.simplebuilding.client;

import com.simplebuilding.items.custom.CoreHandMotion;
import com.simplebuilding.networking.CoreMotionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/** Client side of {@link CoreMotionPayload}: starts the motion on the local player (client thread). */
public final class CoreMotionClient {
    private CoreMotionClient() {
    }

    public static void apply(CoreMotionPayload payload) {
        InteractionHand[] hands = InteractionHand.values();
        CoreHandMotion.Motion[] motions = CoreHandMotion.Motion.values();
        if (payload.hand() < 0 || payload.hand() >= hands.length || payload.motion() < 0 || payload.motion() >= motions.length) return;
        CoreHandMotion.start(Minecraft.getInstance().player, hands[payload.hand()], motions[payload.motion()]);
    }
}
