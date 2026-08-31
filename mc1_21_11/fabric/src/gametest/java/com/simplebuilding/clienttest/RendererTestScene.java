package com.simplebuilding.clienttest;

import java.util.function.Predicate;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Builds the single, fully deterministic scene that every in-world renderer test uses, and
 * strips the client of everything that could change pixels for reasons other than the renderer
 * under test. Down-port of the 26.2 version in {@code src/gametest/java}.
 *
 * <p><b>Why the scene looks the way it does.</b> The renderer tests are difference tests: take a
 * screenshot without the trigger, take one with the trigger, and require the two to differ. That
 * only proves something about the renderer if <em>nothing else</em> on screen reacts to the
 * trigger. Two things normally would:
 * <ul>
 *   <li>the held item model in the bottom right corner, and</li>
 *   <li>the HUD (hotbar, selected-item name, the mod's own HUD overlays).</li>
 * </ul>
 * Both are suppressed by hiding the HUD. 1.21.11 has no {@code Minecraft.gui.hud} object and no
 * {@code Hud#toggle()} - the flag is the plain field {@code Options.hideGui}, and it carries the
 * same two guarantees:
 * <ul>
 *   <li>{@code GameRenderer.renderItemInHand} only reaches
 *       {@code ItemInHandRenderer.renderHandsWithItems} when {@code options.hideGui} is false
 *       (verified in the 1.21.11 bytecode: the call at offset 197 sits behind
 *       {@code getfield Options.hideGui / ifne}), so the first person hand disappears with it;</li>
 *   <li>{@code Gui.render} skips everything from {@code renderCameraOverlays} to
 *       {@code renderTabList} when it is set. The mod's own HUD overlays are attached before
 *       {@code VanillaHudElements.CHAT}, which Fabric wraps around {@code Gui.renderChat} - inside
 *       that skipped region - so they are hidden as well.</li>
 * </ul>
 * What is left on screen is pure world render.
 *
 * <p>The rest of the setup removes every other source of frame-to-frame variance: a walled-in view
 * of a solid block face (so no sky, sun or clouds are ever visible), a frozen time of day, clear
 * weather, no mobs, no random ticks, no view bobbing and no dynamic FOV.
 *
 * <p>Geometry (all values are exact, nothing is rounded at runtime):
 * <pre>
 *   wall   z = 20, x = -12..32, y = -4..24   (fills the whole viewport at this distance)
 *   floor  y = -1, z = 10..19
 *   player x = 10.5, y = 0.0, z = 16.5, yaw 0 (facing +Z / south), pitch 0
 *   eye    y = 1.62  -&gt;  crosshair ray hits block (10, 1, 20) through its NORTH face
 *   reach  3.5 blocks, comfortably inside the 4.5 block interaction range
 * </pre>
 * Because yaw is exactly 0, {@code player.getDirection()} is SOUTH, so the sledgehammer's
 * {@code getHitSideFromPlayer} resolves to NORTH and its 3x3 pattern lies in the X/Y plane of the
 * wall - every one of the eight neighbours of {@link #TARGET} is a solid wall block.
 */
final class RendererTestScene {

    /** Block the crosshair is aimed at. */
    static final BlockPos TARGET = new BlockPos(10, 1, 20);

    /** Face of {@link #TARGET} the crosshair ray enters through. */
    static final Direction TARGET_FACE = Direction.NORTH;

    /** Z plane of the wall. */
    static final int WALL_Z = 20;

    /** Z plane one block in front of the wall - free air, this is where the wand preview lands. */
    static final int FRONT_Z = 19;

    private RendererTestScene() {
    }

    /**
     * Wipes the area, builds wall and floor out of {@code wallBlockId}, puts the player in
     * {@code gameMode} at the fixed position and verifies that the crosshair really is on
     * {@link #TARGET}. Fails loudly if the aim is off - a test that silently missed its target
     * would report "renderer draws nothing" for the wrong reason.
     */
    static void build(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                      String wallBlockId, String gameMode) {
        String[] commands = {
                "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false",
                "gamerule doMobSpawning false",
                "gamerule doPatrolSpawning false",
                "gamerule doTraderSpawning false",
                "gamerule doFireTick false",
                "gamerule mobGriefing false",
                "gamerule randomTickSpeed 0",
                "time set noon",
                "weather clear",
                "gamemode " + gameMode + " @a",
                "kill @e[type=!minecraft:player]",
                // Clear the whole working volume first, then build into it.
                "fill -12 -4 10 32 24 20 minecraft:air",
                "fill -12 -4 " + WALL_Z + " 32 24 " + WALL_Z + " " + wallBlockId,
                "fill -12 -1 10 32 -1 19 " + wallBlockId,
                "clear @a",
                "tp @a 10.5 0.0 16.5 0.0 0.0",
        };

        for (String command : commands) {
            singleplayer.getServer().runCommand(command);
        }

        // The 4.3.5 client gametest API has no TestSingleplayerContext#getConnection() and no
        // waitForClientboundPackets(); waiting for the chunks to be re-rendered plus a fixed
        // settling window is the equivalent barrier for a bulk world edit.
        context.waitTicks(40);
        singleplayer.getClientWorld().waitForChunksRender();
        context.waitTicks(10);

        makeRenderingDeterministic(context);
        context.waitTicks(10);

        assertAimedAt(context, TARGET, TARGET_FACE);
    }

    /**
     * Hides the HUD (and with it the first person hand, see class javadoc) and freezes every
     * client side option that could otherwise move pixels between two screenshots.
     */
    static void makeRenderingDeterministic(ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.fov().set(70);
            client.options.fovEffectScale().set(0.0);
            client.options.screenEffectScale().set(0.0);
            client.options.bobView().set(false);
            client.options.entityShadows().set(false);
            client.options.cloudStatus().set(CloudStatus.OFF);
            client.options.particles().set(ParticleStatus.MINIMAL);
            client.options.setCameraType(CameraType.FIRST_PERSON);

            client.options.hideGui = true;
        });
    }

    /** Undoes {@link #makeRenderingDeterministic}'s HUD flag; options are reset by the harness. */
    static void showHudAgain(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.hideGui = false);
    }

    /**
     * Replacement for the 26.2 {@code getConnection().waitForClientboundPackets()} barrier, which
     * the 4.3.5 API does not have: polls the <em>client's</em> own view until it reflects the
     * effect of a server command, then lets a few more frames pass. Failing here means the test
     * setup never arrived on the client - which must not be mistaken for a silent renderer.
     */
    static void awaitOnClient(ClientGameTestContext context, Predicate<Minecraft> condition, String what) {
        for (int tick = 0; tick < 200; tick++) {
            if (context.computeOnClient(condition::test)) {
                context.waitTicks(10);
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("The client never observed " + what + " within 200 ticks. "
                + describeAim(context));
    }

    /** Aims the player and waits until the crosshair reports the expected block and face. */
    static void aimAt(ClientGameTestContext context, TestSingleplayerContext singleplayer,
                      String yaw, String pitch, BlockPos expected, Direction expectedFace) {
        singleplayer.getServer().runCommand("tp @a 10.5 0.0 16.5 " + yaw + " " + pitch);
        context.waitTicks(10);
        assertAimedAt(context, expected, expectedFace);
    }

    static void assertAimedAt(ClientGameTestContext context, BlockPos expected, Direction expectedFace) {
        for (int tick = 0; tick < 120; tick++) {
            boolean onTarget = context.computeOnClient(client ->
                    client.hitResult instanceof BlockHitResult hit
                            && hit.getType() == HitResult.Type.BLOCK
                            && hit.getBlockPos().equals(expected)
                            && hit.getDirection() == expectedFace);

            if (onTarget) {
                return;
            }

            context.waitTick();
        }

        throw new AssertionError("Scene setup failed: crosshair never landed on " + expected
                + " (face " + expectedFace + "). " + describeAim(context));
    }

    /** Human readable dump of everything the aim assertion depends on. */
    static String describeAim(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            if (client.player == null) {
                return "no client player";
            }

            String hit;

            if (client.hitResult == null) {
                hit = "none";
            } else if (client.hitResult instanceof BlockHitResult blockHit
                    && blockHit.getType() == HitResult.Type.BLOCK) {
                hit = "block " + blockHit.getBlockPos() + " face " + blockHit.getDirection();
            } else {
                hit = String.valueOf(client.hitResult.getType());
            }

            return String.format(
                    "player=(%.3f, %.3f, %.3f) yaw=%.2f pitch=%.2f mainHand=%s hitResult=%s",
                    client.player.getX(), client.player.getY(), client.player.getZ(),
                    client.player.getYRot(), client.player.getXRot(),
                    client.player.getMainHandItem(), hit);
        });
    }
}
