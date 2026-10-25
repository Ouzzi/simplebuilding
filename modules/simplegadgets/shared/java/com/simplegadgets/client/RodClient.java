package com.simplegadgets.client;

import com.simplegadgets.config.GadgetsConfig;
import com.simplegadgets.network.LaserPayload;
import com.simplegadgets.network.RodNetwork;
import com.simplegadgets.rod.ResonanceRodItem;
import com.simplelib.api.client.GadgetHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client logic of the Resonance Rod, loader-neutral (moved from SimpleBuilding's {@code TweaksClient}): reporting the own laser dot,
 * the sparks at the drawn tip, the HUD slot. The loader entry registers {@link #register()} once and {@link #tick(Minecraft)} at the end
 * of every client tick.
 */
public final class RodClient {
    /** The readout box of the rod in SimpleLib's gadget stack: after the Octant (0) and the Gauge (1). */
    public static final String SLOT = "simplegadgets:rod";
    public static final int ORDER = 2;
    private static int laserTicks;

    private RodClient() {
    }

    public static void register() {
        GadgetHud.register(SLOT, ORDER, client -> client.player != null && isAimingLaser(client.player)
                && ResonanceRodItem.measures(client.player.getUseItem(), client.player.level()));
    }

    public static void tick(Minecraft client) {
        if (client.isPaused()) {
            return;
        }
        RodNetwork.expireLasers();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (isAimingLaser(player) && laserTicks % ResonanceRodItem.SPARK_PERIOD == 0 && client.level != null) {
            // Own sparks at the rod's tip (the server's go only to the others).
            Vec3 tip = ResonanceRodItem.tipPosition(player.getEyePosition(), player.getViewVector(1.0f), player.getYRot(),
                    player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT
                            == (player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND));
            for (int i = 0; i < 2; i++) {
                client.level.addParticle(ResonanceRodItem.SPARK, tip.x + (player.getRandom().nextDouble() - 0.5) * 0.06,
                        tip.y + (player.getRandom().nextDouble() - 0.5) * 0.06, tip.z + (player.getRandom().nextDouble() - 0.5) * 0.06,
                        0.0, 0.01, 0.0);
            }
        }
        if (isAimingLaser(player) && laserTicks++ % 2 == 0) {
            Vec3 hit = laserHit(player, 1.0f);
            if (hit != null) {
                RodNetwork.sendToServer(new LaserPayload(player.getUUID(), (float) hit.x, (float) hit.y, (float) hit.z, true));
            }
        }
    }

    public static boolean isAimingLaser(LocalPlayer player) {
        return GadgetsConfig.rodEnabled && player.isUsingItem() && player.getUseItem().getItem() instanceof ResonanceRodItem
                && !ResonanceRodItem.isEmpty(player.getUseItem());
    }

    /** Where the own laser hits (range from the config), else null. */
    public static Vec3 laserHit(LocalPlayer player, float partialTick) {
        HitResult hit = ResonanceRodItem.aim(player, GadgetsConfig.rodRange, partialTick);
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }
}
