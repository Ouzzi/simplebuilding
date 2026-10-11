package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.entity.vehicle.TieredChestBoat;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

/**
 * Queue N37 (owner 2026-10-11): standing on the chest of an Enderite chest boat "bugs and walks slowly". The real client
 * player stands on a boat floating in still water: for {@link #STAND_TICKS} ticks its feet must stay on the boat's top
 * (no sinking into the hitbox, never in the water), and {@link #WALK_TICKS} ticks of walking forward on the deck must
 * cover about as much ground as on stone. Measured, not pictured; cleans up behind itself.
 */
public final class BoatDeckClientTest {
    private static final int STAND_TICKS = 40;
    private static final int WALK_TICKS = 5;
    private static final double BOAT_X = 8.5;
    private static final double BOAT_Z = 14.5;

    private static final double[] start = new double[3];
    private static final double[] walked = new double[2];
    private static final int[] counted = {0};
    private static double worstGap;
    private static boolean wet;
    private static boolean airborne;

    private BoatDeckClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.TIERED_VEHICLES || ModEntities.ENDERITE_CHEST_BOAT == null) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        script.command("fill 6 -4 12 11 -4 17 minecraft:stone");
        script.command("fill 6 -3 12 11 -1 17 minecraft:water");
        onServer(script, "launch an enderite chest boat on the pool", server -> {
            ServerLevel level = server.overworld();
            TieredChestBoat boat = ModEntities.ENDERITE_CHEST_BOAT.create(level, EntitySpawnReason.COMMAND);
            boat.setPos(BOAT_X, -0.4, BOAT_Z);
            boat.setYRot(0.0F);
            level.addFreshEntity(boat);
        });
        script.awaitPackets();
        script.idle("let the boat settle", 60);

        // Baseline: the same walk on the stone floor.
        script.command("tp @a 2.5 0.0 12.5 0.0 0.0");
        script.awaitPackets();
        script.idle("stand on stone", 10);
        walk(script, 0, "stone");

        // On the boat, at the back (the chest), facing along it.
        script.command("tp @a " + BOAT_X + " 0.4 " + (BOAT_Z - 0.55) + " 0.0 0.0");
        script.awaitPackets();
        script.idle("land on the boat", 20);
        script.act("start watching the deck", client -> {
            counted[0] = 0;
            worstGap = 0.0;
            wet = false;
            airborne = false;
        });
        script.await("stand on the boat for " + STAND_TICKS + " ticks", STAND_TICKS + 40, client -> {
            AABB deck = boat(client);
            if (deck == null) {
                throw new AssertionError("The client has no Enderite chest boat");
            }
            worstGap = Math.max(worstGap, Math.abs(client.player.getY() - deck.maxY));
            if (counted[0] < 12) {
                TestLog.info(String.format(java.util.Locale.ROOT, "boat deck tick %d: y %.5f deck %.5f ground %s dy %.4f colliders %d",
                        counted[0], client.player.getY(), deck.maxY, client.player.onGround(), client.player.getDeltaMovement().y,
                        client.level.getEntityCollisions(client.player, client.player.getBoundingBox().expandTowards(0.0, -0.2, 0.0)).size()));
            }
            wet |= client.player.isInWater();
            airborne |= !client.player.onGround();
            return ++counted[0] >= STAND_TICKS;
        });
        walk(script, 1, "boat");
        script.act("judge the deck", client -> {
            String result = String.format(java.util.Locale.ROOT, "boat deck: worst gap %.4f, wet %s, airborne %s, walked %.3f on stone, %.3f on the boat",
                    worstGap, wet, airborne, walked[0], walked[1]);
            TestLog.info(result);
            if (worstGap > 0.01 || wet || airborne) {
                throw new AssertionError("The player does not stand still on the boat: " + result);
            }
            if (walked[1] < 0.85 * walked[0]) {
                throw new AssertionError("Walking on the boat is slow: " + result);
            }
        });
        script.command("kill @e[type=!minecraft:player]", true);
        script.command("fill 6 -4 12 11 -1 17 minecraft:air", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
    }

    /** Holds the forward key for {@link #WALK_TICKS} ticks and stores the horizontal distance under {@code slot}. */
    private static void walk(Script script, int slot, String where) {
        script.act("note the start on " + where, client -> {
            start[0] = client.player.getX();
            start[2] = client.player.getZ();
            counted[0] = 0;
        });
        script.harness("hold the forward key on " + where, harness -> harness.holdKey(InputConstants.KEY_W));
        script.await("walk " + WALK_TICKS + " ticks on " + where, WALK_TICKS + 40, client -> ++counted[0] >= WALK_TICKS);
        script.harness("release the forward key on " + where, harness -> harness.releaseKey(InputConstants.KEY_W));
        script.act("measure the walk on " + where, client -> {
            double dx = client.player.getX() - start[0];
            double dz = client.player.getZ() - start[2];
            walked[slot] = Math.sqrt(dx * dx + dz * dz);
        });
        script.idle("stop on " + where, 10);
    }

    private static AABB boat(Minecraft client) {
        AABB around = new AABB(BOAT_X - 3, -3, BOAT_Z - 3, BOAT_X + 3, 2, BOAT_Z + 3);
        return client.level.getEntitiesOfClass(TieredChestBoat.class, around).stream().findFirst()
                .map(TieredChestBoat::getBoundingBox).orElse(null);
    }

    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
